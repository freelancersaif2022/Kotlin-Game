import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../astro/day_light.dart';
import '../astro/sun_calc.dart';
import '../theme.dart';
import '../util/format.dart';

/// 24-hour sun altitude curve with golden / blue hour bands.
/// Drag horizontally to scrub through the day.
class DayArc extends StatefulWidget {
  const DayArc({
    super.key,
    required this.day,
    required this.time,
    this.onScrub,
    this.height = 150,
  });

  final DayLight day;
  final DateTime time;
  final ValueChanged<DateTime>? onScrub;
  final double height;

  @override
  State<DayArc> createState() => _DayArcState();
}

class _DayArcState extends State<DayArc> {
  late List<double> _alts;
  late double _max;
  int _lastHapticSlot = -1;

  @override
  void initState() {
    super.initState();
    _sample();
  }

  @override
  void didUpdateWidget(DayArc old) {
    super.didUpdateWidget(old);
    if (!sameDay(old.day.date, widget.day.date) ||
        old.day.lat != widget.day.lat ||
        old.day.lng != widget.day.lng) {
      _sample();
    }
  }

  void _sample() {
    final d = widget.day;
    _alts = List.generate(
      145,
      (i) => sunPosition(
        d.date.add(Duration(minutes: i * 10)),
        d.lat,
        d.lng,
      ).altitude,
    );
    _max = math.max(20, _alts.reduce(math.max) + 6);
  }

  void _handle(Offset local, double width) {
    final f = (local.dx / width).clamp(0.0, 1.0);
    final t = widget.day.date.add(Duration(minutes: (f * 1439).round()));
    final slot = (f * 48).floor();
    if (slot != _lastHapticSlot) {
      _lastHapticSlot = slot;
      HapticFeedback.selectionClick();
    }
    widget.onScrub?.call(t);
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, c) {
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onHorizontalDragStart: (e) => _handle(e.localPosition, c.maxWidth),
          onHorizontalDragUpdate: (e) => _handle(e.localPosition, c.maxWidth),
          onTapDown: (e) => _handle(e.localPosition, c.maxWidth),
          child: SizedBox(
            height: widget.height,
            width: c.maxWidth,
            child: CustomPaint(
              painter: _ArcPainter(
                alts: _alts,
                max: _max,
                day: widget.day,
                time: widget.time,
                labels: [
                  for (final h in [0, 6, 12, 18])
                    fmtTime(
                      context,
                      widget.day.date.add(Duration(hours: h)),
                    ).replaceAll(':00', ''),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _ArcPainter extends CustomPainter {
  _ArcPainter({
    required this.alts,
    required this.max,
    required this.day,
    required this.time,
    required this.labels,
  });
  final List<double> alts;
  final double max;
  final DayLight day;
  final DateTime time;
  final List<String> labels;

  static const _min = -24.0;

  @override
  void paint(Canvas canvas, Size size) {
    final chartH = size.height - 18;
    double y(double alt) =>
        chartH * (1 - (alt.clamp(_min, max) - _min) / (max - _min));
    double x(double minutes) => size.width * minutes / 1440;

    // Altitude bands
    void band(double a, double b, Color c) {
      final r = Rect.fromLTRB(0, y(b), size.width, y(a));
      canvas.drawRect(r, Paint()..color = c);
    }

    band(
      SunAngle.blueGolden,
      SunAngle.goldenTop,
      GH.gold.withValues(alpha: 0.10),
    );
    band(SunAngle.civil, SunAngle.blueGolden, GH.blue.withValues(alpha: 0.12));

    // Horizon
    final hy = y(0);
    canvas.drawLine(
      Offset(0, hy),
      Offset(size.width, hy),
      Paint()
        ..color = Colors.white.withValues(alpha: 0.25)
        ..strokeWidth = 0.8,
    );

    // Curve
    final path = Path();
    for (var i = 0; i < alts.length; i++) {
      final p = Offset(x(i * 10.0), y(alts[i]));
      i == 0 ? path.moveTo(p.dx, p.dy) : path.lineTo(p.dx, p.dy);
    }
    final fill = Path.from(path)
      ..lineTo(size.width, chartH)
      ..lineTo(0, chartH)
      ..close();
    canvas.save();
    canvas.clipRect(Rect.fromLTRB(0, 0, size.width, hy));
    canvas.drawPath(
      fill,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            GH.goldLight.withValues(alpha: 0.28),
            GH.gold.withValues(alpha: 0.02),
          ],
        ).createShader(Rect.fromLTRB(0, 0, size.width, hy)),
    );
    canvas.restore();

    final stroke = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2
      ..strokeCap = StrokeCap.round
      ..shader = const LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          Color(0xFFFFF1C9),
          GH.gold,
          GH.rose,
          GH.blue,
          Color(0xFF2B3566),
        ],
        stops: [0.0, 0.45, 0.58, 0.7, 1.0],
      ).createShader(Rect.fromLTRB(0, y(max), size.width, y(_min)));
    canvas.drawPath(path, stroke);

    // Hour labels
    for (var i = 0; i < 4; i++) {
      final tp = TextPainter(
        text: TextSpan(
          text: labels[i],
          style: GH.label.copyWith(
            fontSize: 9,
            letterSpacing: 0.6,
            color: GH.textLo,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, Offset(x(i * 360.0) + 2, size.height - 12));
    }

    // Current / scrub marker
    final minutes = time.difference(day.date).inSeconds / 60;
    if (minutes >= 0 && minutes <= 1440) {
      final alt = sunPosition(time, day.lat, day.lng).altitude;
      final p = Offset(x(minutes), y(alt));
      canvas.drawLine(
        Offset(p.dx, 0),
        Offset(p.dx, chartH),
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.white.withValues(alpha: 0),
              Colors.white.withValues(alpha: 0.35),
              Colors.white.withValues(alpha: 0),
            ],
          ).createShader(Rect.fromLTRB(p.dx - 1, 0, p.dx + 1, chartH))
          ..strokeWidth = 1,
      );
      final up = alt > -0.8;
      final c = up ? GH.goldLight : GH.blue;
      canvas.drawCircle(
        p,
        16,
        Paint()
          ..shader = RadialGradient(
            colors: [c.withValues(alpha: 0.55), c.withValues(alpha: 0)],
          ).createShader(Rect.fromCircle(center: p, radius: 16)),
      );
      canvas.drawCircle(
        p,
        6.5,
        Paint()..color = up ? const Color(0xFFFFF4D6) : const Color(0xFFDCE6FF),
      );
      canvas.drawCircle(
        p,
        6.5,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.5
          ..color = c,
      );
    }
  }

  @override
  bool shouldRepaint(_ArcPainter old) => old.time != time || old.alts != alts;
}
