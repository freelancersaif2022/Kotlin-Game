import 'dart:math' as math;
import 'dart:typed_data';

import 'animal_synth.dart';
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

class TranslateRequest {
  TranslateRequest(this.pcm, this.sampleRate, this.voiceId, this.seed);

  final Uint8List pcm;
  final int sampleRate;
  final String voiceId;
  final int seed;
}

/// Full pipeline: human speech → animal speech WAV.
/// Returns null when no speech could be detected.
TranslationResult? translate(TranslateRequest req) {
  final syllables = analyzeSpeech(req.pcm, req.sampleRate);
  if (syllables.isEmpty) return null;
  return synthesizeFromSyllables(syllables, req.voiceId, req.seed);
}

TranslationResult synthesizeFromSyllables(
  List<Syllable> syllables,
  String voiceId,
  int seed,
) {
  final rng = math.Random(seed);
  final profile = voiceProfiles[voiceId] ?? voiceProfiles['dog']!;

  // Group syllables so long-call animals (wolf, cow) don't get spammed.
  final groups = <Syllable>[];
  final perWord = math.max(1, (syllables.length / profile.maxWords).ceil());
  for (var i = 0; i < syllables.length; i += perWord) {
    final g = syllables.sublist(i, math.min(i + perWord, syllables.length));
    final dur = g.last.start + g.last.duration - g.first.start;
    final inten = g.map((e) => e.intensity).reduce(math.max);
    groups.add(Syllable(g.first.start, dur / g.length, inten));
  }

  // Lay the animal words on a timeline following the speaker's rhythm.
  final placed = <(CallSpec, double)>[];
  var cursor = 0.12;
  final firstStart = groups.first.start;
  for (final g in groups) {
    final want = 0.12 + (g.start - firstStart) * 0.8;
    var t = math.max(cursor, want);
    final word = profile.builder(rng, g.duration, g.intensity);
    var end = t;
    for (final c in word) {
      final at = t + c.delay;
      placed.add((c.spec.withGain(0.75 + 0.25 * g.intensity), at));
      end = math.max(end, at + c.spec.duration);
    }
    cursor = end + profile.minGap + rng.nextDouble() * 0.12;
    if (cursor > 14) break;
  }

  final total = cursor + 0.6;
  final buf = Float64List((total * kSampleRate).round());
  for (final (spec, at) in placed) {
    renderCall(spec, buf, (at * kSampleRate).round(), rng);
  }
  applyReverb(buf, profile.reverb);
  removeDc(buf);
  normalize(buf);
  _fadeTail(buf);

  // Mouth animation envelope.
  const hop = 1323; // 30 ms
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
    voiceId,
    encodeWav(buf),
    mouth,
    buf.length / kSampleRate,
    groups.length,
  );
}

void _fadeTail(Float64List buf) {
  const n = 4410;
  for (var i = 0; i < n && i < buf.length; i++) {
    buf[buf.length - 1 - i] *= i / n;
  }
}
