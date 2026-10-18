// Dev tool: renders every voice from a synthetic "speech" clip so the
// synthesizer can be checked without a phone.  dart run tool/render_voices.dart <outDir>
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:wooftalk/audio/translator_engine.dart';
import 'package:wooftalk/audio/animal_synth.dart';

void main(List<String> args) {
  final outDir = args.isEmpty ? '.' : args.first;
  const sr = 16000;
  final rng = math.Random(1);
  // "hello how are you doing today" ~ syllable bursts over noise.
  final bursts = [
    [0.3, 0.18],
    [0.5, 0.22],
    [0.9, 0.25],
    [1.2, 0.15],
    [1.4, 0.3],
    [2.0, 0.2],
    [2.25, 0.2],
    [2.5, 0.45],
  ];
  final n = (3.4 * sr).round();
  final pcm = ByteData(n * 2);
  for (var i = 0; i < n; i++) {
    final t = i / sr;
    var v = (rng.nextDouble() * 2 - 1) * 0.004;
    for (final b in bursts) {
      if (t >= b[0] && t < b[0] + b[1]) {
        final p = (t - b[0]) / b[1];
        v += math.sin(p * math.pi) * 0.4 * math.sin(2 * math.pi * 160 * t);
      }
    }
    pcm.setInt16(i * 2, (v * 32767).round(), Endian.little);
  }
  final bytes = pcm.buffer.asUint8List();
  final syl = analyzeSpeech(bytes, sr);
  stdout.writeln(
    'syllables: ${syl.length} '
    '${syl.map((s) => '${s.start.toStringAsFixed(2)}/${s.duration.toStringAsFixed(2)}').join(' ')}',
  );
  for (final id in voiceProfiles.keys) {
    final sw = Stopwatch()..start();
    final r = translate(
      TranslateRequest(bytes, sr, id, DateTime.now().microsecond),
    )!;
    File('$outDir/$id.wav').writeAsBytesSync(r.wav);
    stdout.writeln(
      '$id: ${r.duration.toStringAsFixed(2)}s words=${r.words} '
      'mouth=${r.mouth.length} in ${sw.elapsedMilliseconds}ms',
    );
  }
}
