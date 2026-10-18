import 'dart:async';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

enum SpeedPhase { idle, ping, download, upload, done, error }

class SpeedSnapshot {
  const SpeedSnapshot({
    this.phase = SpeedPhase.idle,
    this.pingMs,
    this.jitterMs,
    this.downloadMbps,
    this.uploadMbps,
    this.liveMbps = 0,
    this.progress = 0,
    this.error,
  });

  final SpeedPhase phase;
  final double? pingMs;
  final double? jitterMs;
  final double? downloadMbps;
  final double? uploadMbps;
  final double liveMbps;

  /// 0–1 progress of the current phase.
  final double progress;
  final String? error;

  SpeedSnapshot copyWith({
    SpeedPhase? phase,
    double? pingMs,
    double? jitterMs,
    double? downloadMbps,
    double? uploadMbps,
    double? liveMbps,
    double? progress,
    String? error,
  }) =>
      SpeedSnapshot(
        phase: phase ?? this.phase,
        pingMs: pingMs ?? this.pingMs,
        jitterMs: jitterMs ?? this.jitterMs,
        downloadMbps: downloadMbps ?? this.downloadMbps,
        uploadMbps: uploadMbps ?? this.uploadMbps,
        liveMbps: liveMbps ?? this.liveMbps,
        progress: progress ?? this.progress,
        error: error ?? this.error,
      );
}

/// Measures latency and throughput against Cloudflare's public speed test
/// endpoints, using several parallel streams like real speed test apps do.
class SpeedTestService {
  static const _host = 'speed.cloudflare.com';
  static const _streams = 4;
  static const _downloadDuration = Duration(seconds: 8);
  static const _uploadDuration = Duration(seconds: 7);

  // Ignore the first part of each phase while TCP ramps up.
  static const _warmup = Duration(milliseconds: 1200);

  final _controller = StreamController<SpeedSnapshot>.broadcast();
  Stream<SpeedSnapshot> get updates => _controller.stream;

  HttpClient? _client;
  bool _cancelled = false;
  SpeedSnapshot _state = const SpeedSnapshot();

  void _emit(SpeedSnapshot s) {
    _state = s;
    if (!_controller.isClosed) _controller.add(s);
  }

  Future<SpeedSnapshot> run() async {
    _cancelled = false;
    _client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 8)
      ..maxConnectionsPerHost = _streams + 1;
    _emit(const SpeedSnapshot(phase: SpeedPhase.ping));
    try {
      await _ping();
      if (_cancelled) return _state;
      _emit(_state.copyWith(phase: SpeedPhase.download, progress: 0, liveMbps: 0));
      final down = await _download();
      if (_cancelled) return _state;
      _emit(_state.copyWith(downloadMbps: down, phase: SpeedPhase.upload, progress: 0, liveMbps: 0));
      final up = await _upload();
      if (_cancelled) return _state;
      _emit(_state.copyWith(uploadMbps: up, phase: SpeedPhase.done, progress: 1, liveMbps: 0));
    } on Object catch (e) {
      if (!_cancelled) {
        _emit(_state.copyWith(
          phase: SpeedPhase.error,
          error: e is SocketException || e is TimeoutException || e is HttpException
              ? 'Could not reach the test server. Check your connection and try again.'
              : 'The test was interrupted. Please try again.',
        ));
      }
    } finally {
      _client?.close(force: true);
      _client = null;
    }
    return _state;
  }

  void cancel() {
    _cancelled = true;
    _client?.close(force: true);
    _emit(const SpeedSnapshot());
  }

  void dispose() {
    cancel();
    _controller.close();
  }

  Future<void> _ping() async {
    final samples = <double>[];
    // The first request also pays for DNS + TLS; discard it.
    for (var i = 0; i < 11 && !_cancelled; i++) {
      final sw = Stopwatch()..start();
      final req = await _client!.getUrl(Uri.https(_host, '/__down', {'bytes': '0'}));
      final res = await req.close().timeout(const Duration(seconds: 5));
      await res.drain<void>();
      sw.stop();
      if (i > 0) samples.add(sw.elapsedMicroseconds / 1000);
      _emit(_state.copyWith(progress: (i + 1) / 11));
    }
    if (samples.isEmpty) return;
    samples.sort();
    final median = samples[samples.length ~/ 2];
    var jitter = 0.0;
    for (var i = 1; i < samples.length; i++) {
      jitter += (samples[i] - samples[i - 1]).abs();
    }
    jitter /= math.max(1, samples.length - 1);
    _emit(_state.copyWith(pingMs: median, jitterMs: jitter));
  }

  Future<double> _download() async {
    var bytes = 0;
    var measuredBytes = 0;
    final sw = Stopwatch()..start();
    final deadline = _downloadDuration;

    Future<void> worker() async {
      while (!_cancelled && sw.elapsed < deadline) {
        final req = await _client!.getUrl(Uri.https(_host, '/__down', {'bytes': '25000000'}));
        final res = await req.close();
        await for (final chunk in res) {
          bytes += chunk.length;
          if (sw.elapsed > _warmup) measuredBytes += chunk.length;
          if (_cancelled || sw.elapsed >= deadline) break;
        }
      }
    }

    final ticker = _liveTicker(sw, deadline, () => bytes);
    try {
      await Future.wait(List.generate(_streams, (_) => worker()));
    } finally {
      ticker.cancel();
    }
    final seconds = (sw.elapsedMicroseconds / 1e6) - _warmup.inMilliseconds / 1000;
    return seconds <= 0 ? 0 : measuredBytes * 8 / seconds / 1e6;
  }

  Future<double> _upload() async {
    var bytes = 0;
    var measuredBytes = 0;
    final sw = Stopwatch()..start();
    final deadline = _uploadDuration;
    final chunk = Uint8List(64 * 1024);
    final rnd = math.Random();
    for (var i = 0; i < chunk.length; i++) {
      chunk[i] = rnd.nextInt(256);
    }
    const perRequest = 8 * 1024 * 1024;

    Future<void> worker() async {
      while (!_cancelled && sw.elapsed < deadline) {
        final req = await _client!.postUrl(Uri.https(_host, '/__up'));
        req.headers.contentType = ContentType.binary;
        req.contentLength = perRequest;
        var sent = 0;
        while (sent < perRequest && !_cancelled) {
          final n = math.min(chunk.length, perRequest - sent);
          req.add(n == chunk.length ? chunk : Uint8List.sublistView(chunk, 0, n));
          // flush() completes once the socket has accepted the data, which
          // keeps our byte count close to what actually left the device.
          await req.flush();
          sent += n;
          bytes += n;
          if (sw.elapsed > _warmup) measuredBytes += n;
          if (sw.elapsed >= deadline) break;
        }
        if (sent < perRequest) {
          req.abort();
          break;
        }
        final res = await req.close();
        await res.drain<void>();
      }
    }

    final ticker = _liveTicker(sw, deadline, () => bytes);
    try {
      await Future.wait(List.generate(_streams, (_) => worker().catchError((Object _) {})));
    } finally {
      ticker.cancel();
    }
    final seconds = (sw.elapsedMicroseconds / 1e6) - _warmup.inMilliseconds / 1000;
    return seconds <= 0 ? 0 : measuredBytes * 8 / seconds / 1e6;
  }

  /// Emits a smoothed live speed ~8 times per second for the gauge.
  Timer _liveTicker(Stopwatch sw, Duration deadline, int Function() bytes) {
    var lastBytes = 0;
    var lastTime = 0;
    var smoothed = 0.0;
    return Timer.periodic(const Duration(milliseconds: 125), (_) {
      final now = sw.elapsedMicroseconds;
      final b = bytes();
      final dt = (now - lastTime) / 1e6;
      if (dt > 0) {
        final inst = (b - lastBytes) * 8 / dt / 1e6;
        smoothed = smoothed == 0 ? inst : smoothed * 0.8 + inst * 0.2;
      }
      lastBytes = b;
      lastTime = now;
      _emit(_state.copyWith(
        liveMbps: smoothed,
        progress: (sw.elapsed.inMilliseconds / deadline.inMilliseconds).clamp(0.0, 1.0),
      ));
    });
  }
}
