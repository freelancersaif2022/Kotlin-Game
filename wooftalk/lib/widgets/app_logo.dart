import 'package:flutter/material.dart';

/// Flat, modern WoofTalk mark: a speech bubble with a dog face and sound
/// waves on a violet → pink gradient tile. Also used to render the app icon.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.size = 96, this.tile = true, this.radius});

  final double size;

  /// Draw the gradient rounded tile behind the bubble.
  final bool tile;
  final double? radius;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: CustomPaint(
        size: Size.square(size),
        painter: _LogoPainter(tile, radius),
      ),
    );
  }
}

class LogoColors {
  static const a = Color(0xFF5B4BFF);
  static const b = Color(0xFFA247FF);
  static const c = Color(0xFFFF4F8B);
  static const tile = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [a, b, c],
  );
}

class _LogoPainter extends CustomPainter {
  _LogoPainter(this.tile, this.radius);

  final bool tile;
  final double? radius;

  @override
  void paint(Canvas canvas, Size size) {
    final k = size.width / 100;
    canvas.scale(k);
    if (tile) {
      final r = RRect.fromRectAndRadius(
        const Rect.fromLTWH(0, 0, 100, 100),
        Radius.circular(radius ?? 24),
      );
      canvas.drawRRect(
        r,
        Paint()..shader = LogoColors.tile.createShader(r.outerRect),
      );
      // soft top highlight
      canvas.drawRRect(
        r,
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.center,
            colors: [
              Colors.white.withValues(alpha: 0.18),
              Colors.white.withValues(alpha: 0),
            ],
          ).createShader(r.outerRect),
      );
    }

    // Speech bubble with a soft shadow.
    final bubble = Path.combine(
      PathOperation.union,
      Path()..addRRect(
        RRect.fromRectAndRadius(
          const Rect.fromLTWH(16, 20, 68, 52),
          const Radius.circular(20),
        ),
      ),
      Path()
        ..moveTo(26, 62)
        ..lineTo(22, 84)
        ..lineTo(44, 70)
        ..close(),
    );
    canvas.drawPath(
      bubble.shift(const Offset(0, 2.5)),
      Paint()
        ..color = const Color(0xFF2A1466).withValues(alpha: 0.28)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 3),
    );
    canvas.drawPath(bubble, Paint()..color = Colors.white);

    // Dog face (flat).
    const cx = 44.0, cy = 47.0;
    final ear = Paint()..color = const Color(0xFF8A4B22);
    for (final side in [-1.0, 1.0]) {
      canvas.save();
      canvas.translate(cx + side * 12, cy - 11);
      canvas.rotate(-side * 0.5);
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          const Rect.fromLTWH(-5, -2, 10, 20),
          const Radius.circular(5),
        ),
        ear,
      );
      canvas.restore();
    }
    canvas.drawCircle(
      const Offset(cx, cy),
      15,
      Paint()..color = const Color(0xFFFFA23A),
    );
    canvas.drawOval(
      Rect.fromCenter(center: const Offset(cx, cy + 6), width: 17, height: 12),
      Paint()..color = const Color(0xFFFFF1DC),
    );
    final ink = Paint()..color = const Color(0xFF2B2140);
    canvas.drawCircle(const Offset(cx - 5.5, cy - 3), 2.2, ink);
    canvas.drawCircle(const Offset(cx + 5.5, cy - 3), 2.2, ink);
    canvas.drawOval(
      Rect.fromCenter(center: const Offset(cx, cy + 3), width: 6, height: 4),
      ink,
    );
    canvas.drawOval(
      Rect.fromCenter(center: const Offset(cx, cy + 9), width: 5, height: 4),
      Paint()..color = const Color(0xFFFF6F8E),
    );

    // Sound waves.
    final wave = Paint()
      ..color = LogoColors.b
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.6
      ..strokeCap = StrokeCap.round;
    for (final r in [6.0, 12.0]) {
      canvas.drawArc(
        Rect.fromCircle(center: const Offset(62, 47), radius: r),
        -0.8,
        1.6,
        false,
        wave,
      );
    }
  }

  @override
  bool shouldRepaint(_LogoPainter old) => old.tile != tile;
}
