import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Prank-style WoofTalk mark: a cheeky winking dog with its tongue out,
/// barking into a microphone on a bright comic-burst tile. Also used to
/// render the app icon.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.size = 96, this.tile = true, this.radius});

  final double size;

  /// Draw the comic-burst rounded tile behind the dog.
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
  static const a = Color(0xFFFFC21A);
  static const b = Color(0xFFFF7A00);
  static const c = Color(0xFFFF3D7F);
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

  static const ink = Color(0xFF2B1B3D);

  Paint get _line => Paint()
    ..color = ink
    ..style = PaintingStyle.stroke
    ..strokeWidth = 2.6
    ..strokeJoin = StrokeJoin.round
    ..strokeCap = StrokeCap.round;

  Paint _fill(Color c) => Paint()..color = c;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.scale(size.width / 100);
    if (tile) _burst(canvas);

    canvas.save();
    canvas.translate(50, 54);
    canvas.rotate(-0.14);
    canvas.translate(-50, -54);
    _dog(canvas);
    canvas.restore();
    _mic(canvas);
  }

  void _burst(Canvas c) {
    final r = RRect.fromRectAndRadius(
      const Rect.fromLTWH(0, 0, 100, 100),
      Radius.circular(radius ?? 24),
    );
    c.save();
    c.clipRRect(r);
    c.drawRect(
      r.outerRect,
      Paint()
        ..shader = const RadialGradient(
          center: Alignment(-0.1, -0.1),
          radius: 0.9,
          colors: [Color(0xFFFFE45C), LogoColors.a, LogoColors.b],
          stops: [0, 0.45, 1],
        ).createShader(r.outerRect),
    );
    // comic rays
    final ray = _fill(Colors.white.withValues(alpha: 0.16));
    const center = Offset(46, 46);
    for (var i = 0; i < 16; i += 2) {
      final a1 = i * math.pi / 8, a2 = (i + 1) * math.pi / 8;
      c.drawPath(
        Path()
          ..moveTo(center.dx, center.dy)
          ..lineTo(
            center.dx + math.cos(a1) * 120,
            center.dy + math.sin(a1) * 120,
          )
          ..lineTo(
            center.dx + math.cos(a2) * 120,
            center.dy + math.sin(a2) * 120,
          )
          ..close(),
        ray,
      );
    }
    // halftone dots in the corner for a comic look
    final dot = _fill(LogoColors.c.withValues(alpha: 0.2));
    for (var y = 70.0; y < 104; y += 6) {
      for (var x = 0.0; x < 40; x += 6) {
        final d = (x + (104 - y)) / 60;
        if (d < 1) c.drawCircle(Offset(x, y), 2.2 * (1 - d), dot);
      }
    }
    c.restore();
  }

  void _dog(Canvas c) {
    const tan = Color(0xFFF2A65A);
    const tanLight = Color(0xFFFFCB8E);
    const brown = Color(0xFF8A4B22);

    // floppy ears
    for (final side in [-1.0, 1.0]) {
      c.save();
      c.translate(50 + side * 22, 34);
      c.rotate(-side * 0.55);
      final ear = Path()
        ..moveTo(-6, 0)
        ..cubicTo(-13, 12, -9, 30, 0, 30)
        ..cubicTo(9, 30, 12, 12, 6, -1)
        ..close();
      c.drawPath(ear, _fill(brown));
      c.drawPath(ear, _line);
      c.restore();
    }

    // head
    final head = RRect.fromRectAndRadius(
      const Rect.fromLTWH(24, 24, 52, 48),
      const Radius.elliptical(25, 24),
    );
    c.drawRRect(
      head,
      Paint()
        ..shader = const RadialGradient(
          center: Alignment(-0.4, -0.5),
          colors: [tanLight, tan],
        ).createShader(head.outerRect),
    );

    // patch over the open eye
    c.save();
    c.clipRRect(head);
    c.drawOval(
      Rect.fromCenter(center: const Offset(61, 40), width: 17, height: 18),
      _fill(const Color(0xFFD9853F)),
    );
    c.restore();
    c.drawRRect(head, _line);

    // winking eye ^
    c.drawPath(
      Path()
        ..moveTo(33, 43)
        ..quadraticBezierTo(39, 35, 45, 42),
      _line..strokeWidth = 3,
    );
    // big crazy open eye
    final eye = Rect.fromCenter(
      center: const Offset(61, 41),
      width: 13,
      height: 14,
    );
    c.drawOval(eye, _fill(Colors.white));
    c.drawOval(eye, _line..strokeWidth = 2.2);
    c.drawCircle(const Offset(62.5, 42.5), 3.8, _fill(ink));
    c.drawCircle(const Offset(61.2, 41), 1.3, _fill(Colors.white));
    // cheeky raised eyebrow
    c.drawPath(
      Path()
        ..moveTo(55, 31)
        ..quadraticBezierTo(61, 26, 68, 30),
      _line..strokeWidth = 2.6,
    );

    // muzzle
    final muzzle = Rect.fromCenter(
      center: const Offset(50, 58),
      width: 34,
      height: 24,
    );
    c.drawOval(muzzle, _fill(const Color(0xFFFFF1DC)));
    c.drawOval(muzzle, _line..strokeWidth = 2);

    // laughing open mouth
    final mouth = Path()
      ..moveTo(39, 57)
      ..quadraticBezierTo(50, 61, 61, 57)
      ..quadraticBezierTo(59, 72, 50, 72)
      ..quadraticBezierTo(41, 72, 39, 57)
      ..close();
    c.drawPath(mouth, _fill(const Color(0xFF6A1A2E)));
    c.drawPath(mouth, _line..strokeWidth = 2.2);
    // tongue hanging out to the side
    final tongue = Path()
      ..moveTo(46, 66)
      ..quadraticBezierTo(46, 80, 53, 80)
      ..quadraticBezierTo(60, 80, 59, 64)
      ..close();
    c.drawPath(tongue, _fill(const Color(0xFFFF6F8E)));
    c.drawPath(tongue, _line..strokeWidth = 2);
    c.drawLine(
      const Offset(52.5, 68),
      const Offset(53, 75),
      _line
        ..strokeWidth = 1.4
        ..color = const Color(0xFFD9466A),
    );

    // nose
    c.drawPath(
      Path()
        ..moveTo(44, 50)
        ..quadraticBezierTo(50, 46, 56, 50)
        ..quadraticBezierTo(55, 55, 50, 56)
        ..quadraticBezierTo(45, 55, 44, 50)
        ..close(),
      _fill(ink),
    );
    c.drawOval(
      Rect.fromCenter(center: const Offset(48, 50), width: 4, height: 1.8),
      _fill(Colors.white.withValues(alpha: 0.6)),
    );
  }

  void _mic(Canvas c) {
    c.save();
    c.translate(80, 76);
    c.rotate(0.75);
    final handle = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-4, 2, 8, 22),
      const Radius.circular(4),
    );
    c.drawRRect(handle, _fill(const Color(0xFF3B3350)));
    c.drawRRect(handle, _line..strokeWidth = 2);
    c.drawCircle(Offset.zero, 8.5, _fill(const Color(0xFFCFD3DE)));
    c.drawCircle(
      const Offset(-2.5, -2.5),
      3,
      _fill(Colors.white.withValues(alpha: 0.8)),
    );
    c.drawCircle(Offset.zero, 8.5, _line..strokeWidth = 2.2);
    c.restore();

    // sound blasts from the mouth toward the mic
    final blast = _line
      ..strokeWidth = 2.6
      ..color = Colors.white;
    for (final r in [7.0, 12.5]) {
      c.drawArc(
        Rect.fromCircle(center: const Offset(70, 62), radius: r),
        -0.2,
        1.1,
        false,
        blast,
      );
    }
  }

  @override
  bool shouldRepaint(_LogoPainter old) => old.tile != tile;
}
