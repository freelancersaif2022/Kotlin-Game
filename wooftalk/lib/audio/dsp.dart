import 'dart:typed_data';

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
