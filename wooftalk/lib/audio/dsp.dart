import 'dart:math' as math;
import 'dart:typed_data';

/// Small DSP toolbox used by the animal voice synthesizer.
/// Everything here is pure Dart so it can run inside a background isolate.

const double kSampleRate = 44100;
const double _twoPi = math.pi * 2;

/// Band-limited sawtooth oscillator (PolyBLEP) – the "vocal cords".
class SawOsc {
  double _phase = 0;

  SawOsc([double startPhase = 0]) : _phase = startPhase;

  double next(double freq) {
    final dt = (freq / kSampleRate).clamp(0.0, 0.45);
    _phase += dt;
    if (_phase >= 1) _phase -= 1;
    var v = 2 * _phase - 1;
    v -= _polyBlep(_phase, dt);
    return v;
  }

  static double _polyBlep(double t, double dt) {
    if (dt <= 0) return 0;
    if (t < dt) {
      final x = t / dt;
      return x + x - x * x - 1;
    } else if (t > 1 - dt) {
      final x = (t - 1) / dt;
      return x * x + x + x + 1;
    }
    return 0;
  }
}

/// Simple sine oscillator with phase accumulation (safe for gliding pitch).
class SineOsc {
  double _phase;

  SineOsc([this._phase = 0]);

  double next(double freq) {
    _phase += _twoPi * freq / kSampleRate;
    if (_phase > _twoPi) _phase -= _twoPi;
    return math.sin(_phase);
  }

  double get phase => _phase;
}

/// RBJ constant-peak band-pass biquad. Coefficients can change every sample,
/// which lets formants glide like a real mouth opening and closing.
class Formant {
  double _x1 = 0, _x2 = 0, _y1 = 0, _y2 = 0;

  double process(double x, double freq, double bandwidth) {
    final f = freq.clamp(40.0, kSampleRate * 0.45);
    final q = (f / bandwidth.clamp(20.0, 4000.0)).clamp(0.3, 40.0);
    final w0 = _twoPi * f / kSampleRate;
    final alpha = math.sin(w0) / (2 * q);
    final a0 = 1 + alpha;
    final b0 = alpha / a0;
    final b2 = -alpha / a0;
    final a1 = -2 * math.cos(w0) / a0;
    final a2 = (1 - alpha) / a0;
    final y = b0 * x + b2 * _x2 - a1 * _y1 - a2 * _y2;
    _x2 = _x1;
    _x1 = x;
    _y2 = _y1;
    _y1 = y;
    return y;
  }
}

/// One-pole low-pass filter.
class OnePole {
  double _y = 0;

  double process(double x, double cutoff) {
    final a = math.exp(-_twoPi * cutoff.clamp(20.0, 20000.0) / kSampleRate);
    _y = (1 - a) * x + a * _y;
    return _y;
  }
}

/// One-pole DC blocker / high-pass, removes rumble.
class DcBlock {
  double _x1 = 0, _y1 = 0;

  double process(double x) {
    final y = x - _x1 + 0.995 * _y1;
    _x1 = x;
    _y1 = y;
    return y;
  }
}

/// Slowly varying random value in [-1, 1] – used for natural jitter.
class SmoothNoise {
  SmoothNoise(this._rng, this.rateHz);

  final math.Random _rng;
  final double rateHz;
  double _cur = 0, _target = 0, _t = 1;

  double next() {
    _t += rateHz / kSampleRate;
    if (_t >= 1) {
      _t -= 1;
      _cur = _target;
      _target = _rng.nextDouble() * 2 - 1;
    }
    final s = _t * _t * (3 - 2 * _t);
    return _cur + (_target - _cur) * s;
  }
}

/// Smooth interpolation through (position, value) keyframes, position in 0..1.
double curve(List<List<double>> keys, double p) {
  if (p <= keys.first[0]) return keys.first[1];
  for (var i = 1; i < keys.length; i++) {
    if (p <= keys[i][0]) {
      final a = keys[i - 1], b = keys[i];
      final t = (p - a[0]) / (b[0] - a[0]);
      final s = t * t * (3 - 2 * t);
      return a[1] + (b[1] - a[1]) * s;
    }
  }
  return keys.last[1];
}

/// Attack / release envelope with a natural exponential body.
double envelope(
  double t,
  double dur, {
  double attack = 0.01,
  double release = 0.05,
  double decay = 0,
}) {
  if (t < 0 || t > dur) return 0;
  var e = 1.0;
  if (t < attack) e = math.sin(t / attack * math.pi / 2);
  if (decay > 0) e *= math.exp(-decay * math.max(0, t - attack));
  final r = dur - t;
  if (r < release) e *= math.sin(r / release * math.pi / 2);
  return e;
}

double softClip(double x) {
  final e = math.exp(2 * x);
  return (e - 1) / (e + 1);
}

/// Classic Schroeder reverb – gives the voice some "room" so it sounds
/// recorded in real life instead of dry and synthetic.
void applyReverb(Float64List buf, double wet, {double size = 1}) {
  if (wet <= 0) return;
  final combDelays = [
    1116,
    1188,
    1277,
    1356,
  ].map((d) => (d * size).round()).toList(growable: false);
  final apDelays = [556, 441];
  final combs = combDelays.map((d) => Float64List(d)).toList();
  final combIdx = List<int>.filled(4, 0);
  final combLp = List<double>.filled(4, 0);
  final aps = apDelays.map((d) => Float64List(d)).toList();
  final apIdx = List<int>.filled(2, 0);
  const feedback = 0.78, damp = 0.3;
  for (var i = 0; i < buf.length; i++) {
    final input = buf[i] * 0.25;
    var out = 0.0;
    for (var c = 0; c < 4; c++) {
      final line = combs[c];
      final y = line[combIdx[c]];
      combLp[c] = y * (1 - damp) + combLp[c] * damp;
      line[combIdx[c]] = input + combLp[c] * feedback;
      combIdx[c] = (combIdx[c] + 1) % line.length;
      out += y;
    }
    for (var a = 0; a < 2; a++) {
      final line = aps[a];
      final bufOut = line[apIdx[a]];
      final y = -out + bufOut;
      line[apIdx[a]] = out + bufOut * 0.5;
      apIdx[a] = (apIdx[a] + 1) % line.length;
      out = y;
    }
    buf[i] = buf[i] * (1 - wet * 0.5) + out * wet;
  }
}

/// Removes DC offset / sub-bass thumps left by the soft clipper.
void removeDc(Float64List buf) {
  for (var pass = 0; pass < 2; pass++) {
    final hp = DcBlock();
    for (var i = 0; i < buf.length; i++) {
      buf[i] = hp.process(buf[i]);
    }
  }
}

void normalize(Float64List buf, [double peak = 0.89]) {
  var max = 0.0;
  for (final v in buf) {
    final a = v.abs();
    if (a > max) max = a;
  }
  if (max < 1e-9) return;
  final g = peak / max;
  for (var i = 0; i < buf.length; i++) {
    buf[i] *= g;
  }
}

/// Encodes mono samples (-1..1) as a 16-bit PCM WAV file.
Uint8List encodeWav(Float64List samples, {int sampleRate = 44100}) {
  final dataLen = samples.length * 2;
  final bytes = ByteData(44 + dataLen);
  void str(int o, String s) {
    for (var i = 0; i < s.length; i++) {
      bytes.setUint8(o + i, s.codeUnitAt(i));
    }
  }

  str(0, 'RIFF');
  bytes.setUint32(4, 36 + dataLen, Endian.little);
  str(8, 'WAVE');
  str(12, 'fmt ');
  bytes.setUint32(16, 16, Endian.little);
  bytes.setUint16(20, 1, Endian.little);
  bytes.setUint16(22, 1, Endian.little);
  bytes.setUint32(24, sampleRate, Endian.little);
  bytes.setUint32(28, sampleRate * 2, Endian.little);
  bytes.setUint16(32, 2, Endian.little);
  bytes.setUint16(34, 16, Endian.little);
  str(36, 'data');
  bytes.setUint32(40, dataLen, Endian.little);
  var o = 44;
  for (final s in samples) {
    bytes.setInt16(o, (s.clamp(-1.0, 1.0) * 32767).round(), Endian.little);
    o += 2;
  }
  return bytes.buffer.asUint8List();
}
