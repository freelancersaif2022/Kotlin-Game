import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/theme.dart';

/// 270° arc gauge. The needle value animates smoothly between updates.
class ArcGauge extends StatelessWidget {
  const ArcGauge({
    super.key,
    required this.fraction,
    required this.color,
    required this.center,
    this.size = 240,
    this.ticks = const [],
    this.strokeWidth = 16,
  });

  /// 0–1 fill of the arc.
  final double fraction;
  final Color color;
  final Widget center;
  final double size;
  final List<String> ticks;
  final double strokeWidth;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: size,
      child: TweenAnimationBuilder<double>(
        tween: Tween(end: fraction.clamp(0.0, 1.0)),
        duration: const Duration(milliseconds: 450),
        curve: Curves.easeOutCubic,
        builder: (context, v, child) => TweenAnimationBuilder<Color?>(
          tween: ColorTween(end: color),
          duration: const Duration(milliseconds: 450),
          builder: (context, c, _) => RepaintBoundary(
            child: CustomPaint(
              painter: _ArcPainter(v, c ?? color, ticks, strokeWidth),
              child: child,
            ),
          ),
        ),
        child: Center(child: center),
      ),
    );
  }
}

class _ArcPainter extends CustomPainter {
  _ArcPainter(this.value, this.color, this.ticks, this.stroke);

  final double value;
  final Color color;
  final List<String> ticks;
  final double stroke;

  static const _start = math.pi * 0.75;
  static const _sweep = math.pi * 1.5;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2 - stroke / 2 - (ticks.isEmpty ? 0 : 18);
    final rect = Rect.fromCircle(center: c, radius: r);

    canvas.drawArc(
      rect,
      _start,
      _sweep,
      false,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = StrokeCap.round
        ..color = AppColors.surfaceHigh,
    );

    if (value > 0.001) {
      final sweep = _sweep * value;
      // Angles are kept inside 0–2π and rotated into place, because a sweep
      // gradient cannot span past a full turn.
      // The small lead-in keeps the round start cap on the start colour.
      const lead = 0.25;
      final shader = SweepGradient(
        startAngle: lead,
        endAngle: lead + _sweep * value,
        colors: [color.withValues(alpha: 0.35), color],
        transform: const GradientRotation(_start - lead),
      ).createShader(rect);
      // Soft glow under the arc.
      canvas.drawArc(
        rect,
        _start,
        sweep,
        false,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = stroke + 10
          ..strokeCap = StrokeCap.round
          ..color = color.withValues(alpha: 0.18)
          ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 12),
      );
      canvas.drawArc(
        rect,
        _start,
        sweep,
        false,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = stroke
          ..strokeCap = StrokeCap.round
          ..shader = shader,
      );
      final end = _start + sweep;
      final knob = c + Offset(math.cos(end), math.sin(end)) * r;
      canvas.drawCircle(knob, stroke * 0.42, Paint()..color = Colors.white);
    }

    for (var i = 0; i < ticks.length; i++) {
      final t = ticks.length == 1 ? 0.0 : i / (ticks.length - 1);
      final a = _start + _sweep * t;
      final p = c + Offset(math.cos(a), math.sin(a)) * (r + stroke / 2 + 12);
      final tp = TextPainter(
        text: TextSpan(text: ticks[i], style: const TextStyle(fontSize: 10, color: AppColors.textFaint)),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, p - Offset(tp.width / 2, tp.height / 2));
    }
  }

  @override
  bool shouldRepaint(_ArcPainter old) => old.value != value || old.color != color;
}

/// Small area chart of recent RSSI readings.
class Sparkline extends StatelessWidget {
  const Sparkline({super.key, required this.values, required this.color, this.height = 90, this.min = -95, this.max = -30});

  final List<int> values;
  final Color color;
  final double height;
  final double min;
  final double max;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: RepaintBoundary(child: CustomPaint(painter: _SparkPainter(values, color, min, max))),
    );
  }
}

class _SparkPainter extends CustomPainter {
  _SparkPainter(this.values, this.color, this.min, this.max);

  final List<int> values;
  final Color color;
  final double min;
  final double max;

  @override
  void paint(Canvas canvas, Size size) {
    final grid = Paint()
      ..color = AppColors.stroke.withValues(alpha: 0.6)
      ..strokeWidth = 1;
    for (var i = 1; i < 4; i++) {
      final y = size.height * i / 4;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), grid);
    }
    if (values.length < 2) return;
    const slots = 60;
    final dx = size.width / (slots - 1);
    final offset = slots - values.length;
    Offset pt(int i) {
      final v = ((values[i] - min) / (max - min)).clamp(0.0, 1.0);
      return Offset((i + offset) * dx, size.height * (1 - v));
    }

    final path = Path()..moveTo(pt(0).dx, pt(0).dy);
    for (var i = 1; i < values.length; i++) {
      final p0 = pt(i - 1);
      final p1 = pt(i);
      final mid = (p0.dx + p1.dx) / 2;
      path.cubicTo(mid, p0.dy, mid, p1.dy, p1.dx, p1.dy);
    }
    final fill = Path.from(path)
      ..lineTo(pt(values.length - 1).dx, size.height)
      ..lineTo(pt(0).dx, size.height)
      ..close();
    canvas.drawPath(
      fill,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [color.withValues(alpha: 0.35), color.withValues(alpha: 0)],
        ).createShader(Offset.zero & size),
    );
    canvas.drawPath(
      path,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.4
        ..strokeJoin = StrokeJoin.round
        ..color = color,
    );
    final last = pt(values.length - 1);
    canvas.drawCircle(last, 6, Paint()..color = color.withValues(alpha: 0.25));
    canvas.drawCircle(last, 3.5, Paint()..color = color);
  }

  @override
  bool shouldRepaint(_SparkPainter old) => true;
}

/// Expanding rings behind an icon, used while sampling or testing.
class PulseRings extends StatefulWidget {
  const PulseRings({super.key, required this.color, required this.child, this.active = true, this.size = 120});

  final Color color;
  final Widget child;
  final bool active;
  final double size;

  @override
  State<PulseRings> createState() => _PulseRingsState();
}

class _PulseRingsState extends State<PulseRings> with SingleTickerProviderStateMixin {
  late final _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 2200));

  @override
  void initState() {
    super.initState();
    if (widget.active) _c.repeat();
  }

  @override
  void didUpdateWidget(PulseRings old) {
    super.didUpdateWidget(old);
    if (widget.active && !_c.isAnimating) _c.repeat();
    if (!widget.active && _c.isAnimating) _c.stop();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: widget.size,
      child: RepaintBoundary(
        child: AnimatedBuilder(
          animation: _c,
          builder: (context, child) => CustomPaint(
            painter: _RingsPainter(widget.active ? _c.value : -1, widget.color),
            child: child,
          ),
          child: Center(child: widget.child),
        ),
      ),
    );
  }
}

class _RingsPainter extends CustomPainter {
  _RingsPainter(this.t, this.color);
  final double t;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    if (t < 0) return;
    final c = size.center(Offset.zero);
    for (var i = 0; i < 3; i++) {
      final p = (t + i / 3) % 1;
      canvas.drawCircle(
        c,
        size.width * 0.18 + size.width * 0.32 * p,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2
          ..color = color.withValues(alpha: (1 - p) * 0.5),
      );
    }
  }

  @override
  bool shouldRepaint(_RingsPainter old) => old.t != t;
}
