import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'sky/shaders.dart';

/// Realistic rendered moon for a given phase (0..1).
class MoonDisc extends StatelessWidget {
  const MoonDisc({
    super.key,
    required this.phase,
    this.size = 120,
    this.southern = false,
    this.glow = true,
  });
  final double phase;
  final double size;
  final bool southern;
  final bool glow;

  @override
  Widget build(BuildContext context) => SizedBox.square(
    dimension: size,
    child: CustomPaint(
      painter: _MoonDiscPainter(phase, southern ? math.pi : 0, glow),
    ),
  );
}

class _MoonDiscPainter extends CustomPainter {
  _MoonDiscPainter(this.phase, this.tilt, this.glow);
  final double phase;
  final double tilt;
  final bool glow;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2;
    if (glow) {
      final lit = math.sin(phase * math.pi);
      canvas.drawCircle(
        c,
        r * 1.6,
        Paint()
          ..shader = RadialGradient(
            colors: [
              const Color(0xFFDDE6FF).withValues(alpha: 0.18 * lit),
              Colors.transparent,
            ],
          ).createShader(Rect.fromCircle(center: c, radius: r * 1.6)),
      );
    }
    final program = Shaders.moon;
    if (program == null) {
      canvas.drawCircle(c, r, Paint()..color = const Color(0xFF2A2D38));
      return;
    }
    // Shader canvas is 1.35x the disc so the soft glow fits.
    final box = r * 1.35;
    final s = program.fragmentShader()
      ..setFloat(0, box * 2)
      ..setFloat(1, box * 2)
      ..setFloat(2, phase)
      ..setFloat(3, tilt)
      ..setFloat(4, 0.16);
    canvas.save();
    canvas.translate(c.dx - box, c.dy - box);
    canvas.drawRect(Rect.fromLTWH(0, 0, box * 2, box * 2), Paint()..shader = s);
    canvas.restore();
    s.dispose();
  }

  @override
  bool shouldRepaint(_MoonDiscPainter old) =>
      old.phase != phase || old.tilt != tilt;
}
