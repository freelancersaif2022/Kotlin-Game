import 'dart:math' as math;
import 'dart:typed_data';

import 'dsp.dart';

/// A chunk of speech (roughly one syllable or word).
class Syllable {
  const Syllable(this.start, this.duration, this.intensity);

  final double start;
  final double duration;
  final double intensity;
}

/// Finds syllables in 16-bit mono PCM by tracking the loudness envelope.
/// Works for any human language because it only looks at rhythm & energy.
List<Syllable> analyzeSpeech(Uint8List pcm16, int sampleRate) {
  final data = ByteData.sublistView(pcm16);
  final count = pcm16.length ~/ 2;
  final frame = sampleRate ~/ 50; // 20 ms
  final frames = count ~/ frame;
  if (frames < 10) return const [];

  final db = Float64List(frames);
  for (var f = 0; f < frames; f++) {
    var sum = 0.0;
    for (var i = 0; i < frame; i++) {
      final s = data.getInt16((f * frame + i) * 2, Endian.little) / 32768.0;
      sum += s * s;
    }
    db[f] = 10 * math.log(sum / frame + 1e-10) / math.ln10;
  }
  // Light smoothing.
  final sm = Float64List(frames);
  for (var f = 0; f < frames; f++) {
    final a = db[math.max(0, f - 1)],
        b = db[f],
        c = db[math.min(frames - 1, f + 1)];
    sm[f] = (a + 2 * b + c) / 4;
  }

  final sorted = List<double>.from(sm)..sort();
  final floor = sorted[(frames * 0.15).floor()];
  final peak = sorted[(frames * 0.97).floor().clamp(0, frames - 1)];
  if (peak < -52 || peak - floor < 8) return const [];
  final threshold = floor + (peak - floor) * 0.38;

  // Active regions.
  final segs = <List<int>>[];
  int? start;
  for (var f = 0; f < frames; f++) {
    final on = sm[f] > threshold;
    if (on && start == null) start = f;
    if (!on && start != null) {
      segs.add([start, f]);
      start = null;
    }
  }
  if (start != null) segs.add([start, frames]);

  // Merge tiny gaps, drop clicks.
  final merged = <List<int>>[];
  for (final s in segs) {
    if (merged.isNotEmpty && s[0] - merged.last[1] <= 3) {
      merged.last[1] = s[1];
    } else {
      merged.add([s[0], s[1]]);
    }
  }
  merged.removeWhere((s) => s[1] - s[0] < 4);

  // Split long stretches of continuous speech at energy dips.
  final out = <Syllable>[];
  for (final s in merged) {
    final pieces = <List<int>>[];
    var a = s[0];
    while (s[1] - a > 24) {
      // look for the quietest frame between 10..24 frames ahead
      var best = a + 16;
      for (var f = a + 10; f < math.min(a + 25, s[1] - 6); f++) {
        if (sm[f] < sm[best]) best = f;
      }
      pieces.add([a, best]);
      a = best;
    }
    pieces.add([a, s[1]]);
    for (final p in pieces) {
      var loud = -120.0;
      for (var f = p[0]; f < p[1]; f++) {
        if (sm[f] > loud) loud = sm[f];
      }
      out.add(
        Syllable(
          p[0] * 0.02,
          (p[1] - p[0]) * 0.02,
          ((loud - floor) / (peak - floor)).clamp(0.2, 1.0),
        ),
      );
    }
  }
  return out;
}

class TranslationResult {
  TranslationResult(
    this.voiceId,
    this.wav,
    this.mouth,
    this.duration,
    this.words,
  );

  final String voiceId;
  final Uint8List wav;

  /// Loudness per 30 ms frame (0..1) – drives the cartoon mouth.
  final List<double> mouth;
  final double duration;
  final int words;
}

/// Real recorded animal calls (mono, [clipRate] Hz) for one voice.
class VoiceClips {
  const VoiceClips(this.voiceId, this.clips);

  final String voiceId;
  final List<Float32List> clips;
}

const clipRate = 22050;

/// How a voice lays its calls over the speaker's rhythm.
class VoiceTiming {
  const VoiceTiming(
    this.maxWords,
    this.minGap, {
    this.doubleOnLong = false,
    this.pitch = 1,
  });

  /// Upper bound of calls per translation (long calls like howls need fewer).
  final int maxWords;

  /// Silence between two calls, in seconds.
  final double minGap;

  /// Short calls (barks, quacks) answer a long syllable with two calls.
  final bool doubleOnLong;

  /// Playback-rate multiplier applied to the recordings (puppy = small dog
  /// barks played slightly higher).
  final double pitch;
}

const voiceTimings = <String, VoiceTiming>{
  'dog': VoiceTiming(14, 0.08, doubleOnLong: true),
  'puppy': VoiceTiming(16, 0.06, doubleOnLong: true, pitch: 1.22),
  'cat': VoiceTiming(7, 0.12),
  'wolf': VoiceTiming(3, 0.2),
  'duck': VoiceTiming(12, 0.06, doubleOnLong: true),
  'cow': VoiceTiming(4, 0.2),
  'rooster': VoiceTiming(3, 0.25),
  'sheep': VoiceTiming(6, 0.12),
  'pig': VoiceTiming(12, 0.06, doubleOnLong: true),
  'horse': VoiceTiming(3, 0.2),
  'goat': VoiceTiming(6, 0.12),
  'frog': VoiceTiming(12, 0.06, doubleOnLong: true),
  'tiger': VoiceTiming(4, 0.2),
  'lion': VoiceTiming(4, 0.25),
  'zebra': VoiceTiming(6, 0.12),
  'deer': VoiceTiming(8, 0.1),
  'bird': VoiceTiming(16, 0.05, doubleOnLong: true),
  'elephant': VoiceTiming(3, 0.25),
  'monkey': VoiceTiming(10, 0.08, doubleOnLong: true),
  'owl': VoiceTiming(6, 0.15),
};

class TranslateRequest {
  TranslateRequest(this.pcm, this.sampleRate, this.voice, this.seed);

  final Uint8List pcm;
  final int sampleRate;
  final VoiceClips voice;
  final int seed;
}

/// Full pipeline: human speech → real animal calls following its rhythm.
/// Returns null when no speech could be detected.
TranslationResult? translate(TranslateRequest req) {
  final syllables = analyzeSpeech(req.pcm, req.sampleRate);
  if (syllables.isEmpty || req.voice.clips.isEmpty) return null;
  return arrangeCalls(syllables, req.voice, req.seed);
}

TranslationResult arrangeCalls(
  List<Syllable> syllables,
  VoiceClips voice,
  int seed,
) {
  final rng = math.Random(seed);
  final timing = voiceTimings[voice.voiceId] ?? const VoiceTiming(8, 0.1);

  // Group syllables so long-call animals (wolf, cow) don't get spammed.
  final groups = <Syllable>[];
  final perWord = math.max(1, (syllables.length / timing.maxWords).ceil());
  for (var i = 0; i < syllables.length; i += perWord) {
    final g = syllables.sublist(i, math.min(i + perWord, syllables.length));
    final dur = g.last.start + g.last.duration - g.first.start;
    final inten = g.map((e) => e.intensity).reduce(math.max);
    groups.add(Syllable(g.first.start, dur / g.length, inten));
  }

  // Pick clips: random, never the same one twice in a row, each slightly
  // re-pitched so no two translations sound the same.
  final placed =
      <(Float32List, double, double, double)>[]; // clip, at, rate, gain
  var cursor = 0.1;
  var last = -1;
  final firstStart = groups.first.start;
  Float32List pick() {
    var i = rng.nextInt(voice.clips.length);
    if (voice.clips.length > 1 && i == last) {
      i = (i + 1 + rng.nextInt(voice.clips.length - 1)) % voice.clips.length;
    }
    last = i;
    return voice.clips[i];
  }

  for (final g in groups) {
    final want = 0.1 + (g.start - firstStart) * 0.8;
    var t = math.max(cursor, want);
    final calls = timing.doubleOnLong && g.duration > 0.4 ? 2 : 1;
    for (var k = 0; k < calls; k++) {
      final clip = pick();
      final rate = timing.pitch * (0.9 + rng.nextDouble() * 0.22);
      final gain = (0.7 + 0.3 * g.intensity) * (k == 0 ? 1 : 0.85);
      placed.add((clip, t, rate, gain));
      t += clip.length / clipRate / rate + timing.minGap * (k == 0 ? 0.6 : 1);
    }
    cursor = t + timing.minGap + rng.nextDouble() * 0.1;
    if (cursor > 14) break;
  }

  final total = cursor + 0.25;
  final buf = Float64List((total * clipRate).round());
  for (final (clip, at, rate, gain) in placed) {
    final offset = (at * clipRate).round();
    final n = (clip.length / rate).floor();
    for (var i = 0; i < n; i++) {
      final idx = offset + i;
      if (idx >= buf.length) break;
      // linear-interpolated resample = natural pitch/speed variation
      final pos = i * rate;
      final j = pos.floor();
      if (j + 1 >= clip.length) break;
      final f = pos - j;
      buf[idx] += (clip[j] * (1 - f) + clip[j + 1] * f) * gain;
    }
  }
  normalize(buf);

  // Mouth animation envelope.
  const hop = clipRate * 30 ~/ 1000;
  final mouth = <double>[];
  var maxRms = 1e-9;
  for (var i = 0; i + hop <= buf.length; i += hop) {
    var s = 0.0;
    for (var j = 0; j < hop; j++) {
      s += buf[i + j] * buf[i + j];
    }
    final r = math.sqrt(s / hop);
    mouth.add(r);
    if (r > maxRms) maxRms = r;
  }
  for (var i = 0; i < mouth.length; i++) {
    mouth[i] = math.pow(mouth[i] / maxRms, 0.7).toDouble();
  }

  return TranslationResult(
    voice.voiceId,
    encodeWav(buf, sampleRate: clipRate),
    mouth,
    buf.length / clipRate,
    placed.length,
  );
}

/// Reads a 16-bit PCM mono WAV (as produced by tool/fetch_sounds.py).
Float32List decodeWav16(Uint8List bytes) {
  final d = ByteData.sublistView(bytes);
  var o = 12;
  while (o + 8 <= bytes.length) {
    final id = String.fromCharCodes(bytes.sublist(o, o + 4));
    final size = d.getUint32(o + 4, Endian.little);
    if (id == 'data') {
      final n = math.min(size, bytes.length - o - 8) ~/ 2;
      final out = Float32List(n);
      for (var i = 0; i < n; i++) {
        out[i] = d.getInt16(o + 8 + i * 2, Endian.little) / 32768.0;
      }
      return trimSilence(out);
    }
    o += 8 + size + (size.isOdd ? 1 : 0);
  }
  return Float32List(0);
}

/// Cuts quiet lead-in/tail so a call starts right on its beat.
Float32List trimSilence(Float32List x, {double threshold = 0.06}) {
  var peak = 0.0;
  for (final v in x) {
    if (v.abs() > peak) peak = v.abs();
  }
  if (peak == 0) return x;
  final t = peak * threshold;
  var a = 0, b = x.length - 1;
  while (a < b && x[a].abs() < t) {
    a++;
  }
  while (b > a && x[b].abs() < t) {
    b--;
  }
  const pad = clipRate ~/ 50; // 20 ms
  a = math.max(0, a - pad);
  b = math.min(x.length, b + pad);
  final out = Float32List.sublistView(x, a, b);
  // short fades so cuts never click
  final fade = math.min(clipRate ~/ 100, out.length ~/ 4);
  final copy = Float32List.fromList(out);
  for (var i = 0; i < fade; i++) {
    copy[i] *= i / fade;
    copy[copy.length - 1 - i] *= i / fade;
  }
  return copy;
}
