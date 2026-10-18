import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:wooftalk/audio/animal_synth.dart';
import 'package:wooftalk/audio/translator_engine.dart';

Uint8List _speech({double gain = 0.4}) {
  const sr = 16000;
  final rng = math.Random(3);
  final bursts = [
    [0.3, 0.2],
    [0.7, 0.25],
    [1.2, 0.3],
    [1.8, 0.2],
    [2.1, 0.4],
  ];
  final n = (3 * sr).round();
  final d = ByteData(n * 2);
  for (var i = 0; i < n; i++) {
    final t = i / sr;
    var v = (rng.nextDouble() * 2 - 1) * 0.004;
    for (final b in bursts) {
      if (t >= b[0] && t < b[0] + b[1]) {
        v +=
            math.sin((t - b[0]) / b[1] * math.pi) *
            gain *
            math.sin(2 * math.pi * 180 * t);
      }
    }
    d.setInt16(i * 2, (v * 32767).round(), Endian.little);
  }
  return d.buffer.asUint8List();
}

void main() {
  test('finds syllables in speech', () {
    final s = analyzeSpeech(_speech(), 16000);
    expect(s.length, inInclusiveRange(4, 7));
  });

  test('silence is rejected', () {
    expect(analyzeSpeech(Uint8List(16000 * 2 * 2), 16000), isEmpty);
    expect(
      translate(TranslateRequest(Uint8List(64000), 16000, 'dog', 1)),
      isNull,
    );
  });

  test('every voice renders a valid, non-silent wav', () {
    for (final id in voiceProfiles.keys) {
      final r = translate(TranslateRequest(_speech(), 16000, id, 42))!;
      expect(String.fromCharCodes(r.wav.sublist(0, 4)), 'RIFF');
      expect(r.duration, greaterThan(0.5));
      expect(r.duration, lessThan(16));
      expect(r.mouth.reduce(math.max), closeTo(1, 0.01));
      expect(r.voiceId, id);
    }
  });

  test('each translation sounds different', () {
    final a = translate(TranslateRequest(_speech(), 16000, 'dog', 1))!;
    final b = translate(TranslateRequest(_speech(), 16000, 'dog', 2))!;
    expect(a.wav, isNot(equals(b.wav)));
  });
}
