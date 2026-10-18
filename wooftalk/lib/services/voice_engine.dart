import 'dart:async';
import 'dart:io';
import 'dart:isolate';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:just_audio/just_audio.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../audio/translator_engine.dart';

/// Live mic capture (kept in memory only – nothing is saved), translation in
/// a background isolate and playback of the animal voice.
class VoiceEngine {
  VoiceEngine() {
    _player.playerStateStream.listen((s) {
      if (s.processingState == ProcessingState.completed) {
        _player.pause();
        _player.seek(Duration.zero);
      }
    });
  }

  static const sampleRate = 16000;
  static const maxSeconds = 15;

  final _recorder = AudioRecorder();
  final _player = AudioPlayer();
  final _bytes = BytesBuilder(copy: false);
  StreamSubscription<Uint8List>? _sub;
  final _levels = StreamController<double>.broadcast();

  /// Mic loudness 0..1, roughly every 50 ms while recording.
  Stream<double> get levels => _levels.stream;
  AudioPlayer get player => _player;

  TranslationResult? lastResult;
  String? lastFile;

  /// Asks for the microphone if needed. Returns true when it can be used.
  Future<bool> ensurePermission() => _recorder.hasPermission();

  Future<void> startRecording() async {
    await _player.stop();
    _bytes.clear();
    final stream = await _recorder.startStream(
      const RecordConfig(
        encoder: AudioEncoder.pcm16bits,
        sampleRate: sampleRate,
        numChannels: 1,
        autoGain: true,
        noiseSuppress: true,
      ),
    );
    _sub = stream.listen((chunk) {
      _bytes.add(chunk);
      _levels.add(_level(chunk));
    });
  }

  double get recordedSeconds => _bytes.length / 2 / sampleRate;

  Future<Uint8List> stopRecording() async {
    await _recorder.stop();
    await _sub?.cancel();
    _sub = null;
    return _bytes.takeBytes();
  }

  Future<void> cancelRecording() async {
    await _recorder.cancel();
    await _sub?.cancel();
    _sub = null;
    _bytes.clear();
  }

  /// Translates the captured speech. Returns null when no voice was heard.
  Future<TranslationResult?> translateSpeech(
    Uint8List pcm,
    String voiceId,
  ) async {
    final req = TranslateRequest(
      pcm,
      sampleRate,
      voiceId,
      DateTime.now().microsecondsSinceEpoch,
    );
    final result = await Isolate.run(() => translate(req));
    if (result != null) await _prepare(result);
    return result;
  }

  /// Short sample used to preview a voice before unlocking it.
  Future<TranslationResult> preview(String voiceId) async {
    final seed = DateTime.now().microsecondsSinceEpoch;
    final result = await Isolate.run(
      () => synthesizeFromSyllables(
        const [
          Syllable(0, 0.25, 1),
          Syllable(0.45, 0.3, 0.8),
          Syllable(0.95, 0.5, 0.9),
        ],
        voiceId,
        seed,
      ),
    );
    await _prepare(result);
    return result;
  }

  Future<void> _prepare(TranslationResult r) async {
    final dir = await getTemporaryDirectory();
    final old = lastFile;
    final file = File(
      '${dir.path}/wooftalk_${DateTime.now().millisecondsSinceEpoch}.wav',
    );
    await file.writeAsBytes(r.wav, flush: true);
    lastResult = r;
    lastFile = file.path;
    await _player.setFilePath(file.path);
    if (old != null) {
      try {
        await File(old).delete();
      } catch (_) {}
    }
  }

  Future<void> play() async {
    await _player.seek(Duration.zero);
    await _player.play();
  }

  Future<void> stopPlayback() => _player.stop();

  double _level(Uint8List chunk) {
    final data = ByteData.sublistView(chunk);
    final n = chunk.length ~/ 2;
    if (n == 0) return 0;
    var sum = 0.0;
    for (var i = 0; i < n; i++) {
      final s = data.getInt16(i * 2, Endian.little) / 32768.0;
      sum += s * s;
    }
    final db = 10 * math.log(sum / n + 1e-10) / math.ln10;
    return ((db + 55) / 45).clamp(0.0, 1.0);
  }

  Future<void> dispose() async {
    await _sub?.cancel();
    await _recorder.dispose();
    await _player.dispose();
    await _levels.close();
  }
}
