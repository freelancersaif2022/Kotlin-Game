import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

import '../astro/day_light.dart';
import '../astro/sun_calc.dart';
import '../services/app_state.dart';
import '../services/orientation_tracker.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';
import '../widgets/common.dart';

class CompassScreen extends StatefulWidget {
  const CompassScreen({super.key, required this.active});
  final bool active;

  @override
  State<CompassScreen> createState() => _CompassScreenState();
}

class _CompassScreenState extends State<CompassScreen>
    with SingleTickerProviderStateMixin {
  OrientationTracker? _tracker;
  late final Ticker _ticker = createTicker(_tick);
  final _heading = ValueNotifier<double>(0);
  int _lastCardinal = -1;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _sync();
  }

  @override
  void didUpdateWidget(CompassScreen old) {
    super.didUpdateWidget(old);
    _sync();
  }

  void _sync() {
    final place = AppScope.read(context).place;
    if (widget.active && place != null) {
      if (_tracker == null ||
          _tracker!.lat != place.lat ||
          _tracker!.lng != place.lng) {
        _tracker?.dispose();
        _tracker = OrientationTracker(lat: place.lat, lng: place.lng)..start();
      }
      if (!_ticker.isActive) _ticker.start();
    } else {
      _ticker.stop();
      _tracker?.dispose();
      _tracker = null;
    }
  }

  /// Critically-damped smoothing along the shortest way round.
  void _tick(Duration _) {
    final target = _tracker?.orientation?.compassHeading;
    if (target == null) return;
    var d = (target - _heading.value) % 360;
    if (d > 180) d -= 360;
    if (d.abs() < 0.05) return;
    _heading.value = (_heading.value + d * 0.14 + 360) % 360;
    final card = ((_heading.value / 90) + 0.5).floor() % 4;
    if (card != _lastCardinal) {
      if (_lastCardinal != -1) HapticFeedback.lightImpact();
      _lastCardinal = card;
    }
  }

  @override
  void dispose() {
    _ticker.dispose();
    _tracker?.dispose();
    _heading.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final place = AppScope.of(context).place;
    final now = ServicesScope.of(context).clock.value;
    if (place == null) {
      return const Center(child: Text('Set a location first', style: GH.body));
    }
    final day = DayLight.forDate(now, place.lat, place.lng);
    final sun = sunPosition(now, place.lat, place.lng);
    final moon = moonPosition(now, place.lat, place.lng);
    final rise = day.sunrise == null
        ? null
        : sunPosition(day.sunrise!, place.lat, place.lng).bearing;
    final set = day.sunset == null
        ? null
        : sunPosition(day.sunset!, place.lat, place.lng).bearing;

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: RadialGradient(
          center: Alignment(0, -0.35),
          radius: 1.2,
          colors: [Color(0xFF1C2033), Color(0xFF0A0B12), Color(0xFF050509)],
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 120),
          children: [
            Row(
              children: [
                const Text('Sun Compass', style: GH.title),
                const Spacer(),
                Text(place.name, style: GH.label.copyWith(fontSize: 10)),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'True north · lay flat or point the camera',
              style: GH.body.copyWith(fontSize: 13),
            ),
            const SizedBox(height: 18),
            AspectRatio(
              aspectRatio: 1,
              child: ValueListenableBuilder<double>(
                valueListenable: _heading,
                builder: (context, h, _) => CustomPaint(
                  painter: _DialPainter(
                    heading: h,
                    sunBearing: sun.bearing,
                    sunUp: sun.altitude > -0.8,
                    moonBearing: moon.bearing,
                    moonUp: moon.altitude > 0,
                    sunrise: rise,
                    sunset: set,
                    noonBearing: sunPosition(
                      day.solarNoon,
                      place.lat,
                      place.lng,
                    ).bearing,
                    goldenEveningStart: day.goldenHourEvening == null
                        ? null
                        : sunPosition(
                            day.goldenHourEvening!.start,
                            place.lat,
                            place.lng,
                          ).bearing,
                  ),
                  child: Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          '${h.round() % 360}°',
                          style: GH.display.copyWith(fontSize: 54),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          cardinal(h),
                          style: GH.label.copyWith(
                            fontSize: 14,
                            color: GH.goldLight,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            if (_tracker?.needsCalibration ?? false) ...[
              const SizedBox(height: 12),
              Text(
                'Magnetic interference detected — wave your phone in a figure-8.',
                textAlign: TextAlign.center,
                style: GH.body.copyWith(color: GH.goldLight, fontSize: 13),
              ),
            ],
            const SizedBox(height: 22),
            Row(
              children: [
                Expanded(
                  child: _BearingCard(
                    title: 'Sunrise',
                    time: fmtTime(context, day.sunrise),
                    bearing: rise,
                    color: GH.gold,
                    icon: Icons.wb_twilight_rounded,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _BearingCard(
                    title: 'Sunset',
                    time: fmtTime(context, day.sunset),
                    bearing: set,
                    color: GH.rose,
                    icon: Icons.wb_twilight_rounded,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _BearingCard(
                    title: 'Sun now',
                    time: '${sun.altitude.toStringAsFixed(1)}° high',
                    bearing: sun.bearing,
                    color: GH.goldLight,
                    icon: Icons.wb_sunny_rounded,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _BearingCard(
                    title: 'Moon now',
                    time: '${moon.altitude.toStringAsFixed(1)}° high',
                    bearing: moon.bearing,
                    color: const Color(0xFFCAD5FF),
                    icon: Icons.dark_mode_rounded,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _BearingCard extends StatelessWidget {
  const _BearingCard({
    required this.title,
    required this.time,
    required this.bearing,
    required this.color,
    required this.icon,
  });
  final String title;
  final String time;
  final double? bearing;
  final Color color;
  final IconData icon;

  @override
  Widget build(BuildContext context) => Glass(
    padding: const EdgeInsets.all(16),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 16, color: color),
            const SizedBox(width: 6),
            Text(title.toUpperCase(), style: GH.label.copyWith(fontSize: 10)),
          ],
        ),
        const SizedBox(height: 10),
        Text(bearing == null ? '—' : fmtBearing(bearing!), style: GH.value),
        const SizedBox(height: 2),
        Text(time, style: GH.body.copyWith(fontSize: 13)),
      ],
    ),
  );
}

class _DialPainter extends CustomPainter {
  _DialPainter({
    required this.heading,
    required this.sunBearing,
    required this.sunUp,
    required this.moonBearing,
    required this.moonUp,
    this.sunrise,
    this.sunset,
    this.goldenEveningStart,
    this.noonBearing,
  });

  final double? noonBearing;
  final double heading;
  final double sunBearing;
  final bool sunUp;
  final double moonBearing;
  final bool moonUp;
  final double? sunrise;
  final double? sunset;
  final double? goldenEveningStart;

  static double _rad(double d) => d * math.pi / 180;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final R = size.width / 2;

    // Drop shadow
    canvas.drawCircle(
      c + const Offset(0, 14),
      R * 0.96,
      Paint()
        ..color = Colors.black.withValues(alpha: 0.6)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 26),
    );

    // Metallic bezel
    final bezel = Rect.fromCircle(center: c, radius: R);
    canvas.drawCircle(
      c,
      R,
      Paint()
        ..shader = const SweepGradient(
          colors: [
            Color(0xFF6B6F7C),
            Color(0xFFE4E6EC),
            Color(0xFF7A7E8B),
            Color(0xFF2E313B),
            Color(0xFF9B9FAA),
            Color(0xFFF2F3F6),
            Color(0xFF5D616D),
            Color(0xFF2A2C35),
            Color(0xFF6B6F7C),
          ],
        ).createShader(bezel),
    );
    canvas.drawCircle(c, R * 0.93, Paint()..color = const Color(0xFF16181F));

    // Dial face
    final faceR = R * 0.915;
    canvas.drawCircle(
      c,
      faceR,
      Paint()
        ..shader = const RadialGradient(
          center: Alignment(-0.2, -0.3),
          colors: [Color(0xFF242838), Color(0xFF12141D), Color(0xFF07080D)],
          stops: [0, 0.7, 1],
        ).createShader(Rect.fromCircle(center: c, radius: faceR)),
    );
    // Inner shadow from the bezel
    canvas.drawCircle(
      c,
      faceR,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 10
        ..color = Colors.black.withValues(alpha: 0.6)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8),
    );

    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(_rad(-heading));

    // Daylight arc on the outer track (sunrise -> sunset through the sun side).
    final track = Rect.fromCircle(center: Offset.zero, radius: faceR * 0.9);
    if (sunrise != null && sunset != null) {
      var sweep = (sunset! - sunrise!) % 360;
      // Southern hemisphere: the sun travels anticlockwise through north.
      final clockwise =
          noonBearing == null || (noonBearing! - sunrise!) % 360 <= sweep;
      if (!clockwise) sweep -= 360;
      canvas.drawArc(
        track,
        _rad(sunrise! - 90),
        _rad(sweep),
        false,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 6
          ..strokeCap = StrokeCap.round
          ..shader = SweepGradient(
            startAngle: _rad(sunrise! - 90),
            endAngle: _rad(sunrise! - 90 + sweep),
            colors: const [GH.gold, Color(0xFFFFF0C2), GH.amber, GH.rose],
          ).createShader(track),
      );
      if (goldenEveningStart != null) {
        var g = (sunset! - goldenEveningStart!) % 360;
        if (!clockwise) g -= 360;
        canvas.drawArc(
          track,
          _rad(goldenEveningStart! - 90),
          _rad(g),
          false,
          Paint()
            ..style = PaintingStyle.stroke
            ..strokeWidth = 12
            ..color = GH.amber.withValues(alpha: 0.35)
            ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4),
        );
      }
    }

    // Ticks
    for (var d = 0; d < 360; d++) {
      final major = d % 30 == 0;
      final mid = d % 10 == 0;
      if (!major && !mid && d % 2 != 0) continue;
      final len = major ? 16.0 : (mid ? 10.0 : 5.0);
      final a = _rad(d.toDouble() - 90);
      final o = Offset(math.cos(a), math.sin(a));
      final r0 = faceR * 0.80;
      canvas.drawLine(
        o * r0,
        o * (r0 - len),
        Paint()
          ..color = d == 0
              ? GH.rose
              : Colors.white.withValues(alpha: major ? 0.95 : (mid ? 0.6 : 0.3))
          ..strokeWidth = major ? 2.2 : 1,
      );
    }

    // Labels
    const cardinals = {0: 'N', 90: 'E', 180: 'S', 270: 'W'};
    for (var d = 0; d < 360; d += 30) {
      final a = _rad(d.toDouble() - 90);
      final isCard = cardinals.containsKey(d);
      final p =
          Offset(math.cos(a), math.sin(a)) * faceR * (isCard ? 0.6 : 0.63);
      canvas.save();
      canvas.translate(p.dx, p.dy);
      canvas.rotate(_rad(d.toDouble()));
      final tp = TextPainter(
        text: TextSpan(
          text: isCard ? cardinals[d] : '$d',
          style: TextStyle(
            color: d == 0 ? GH.rose : (isCard ? GH.textHi : GH.textMid),
            fontSize: isCard ? 22 : 12,
            fontWeight: isCard ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, Offset(-tp.width / 2, -tp.height / 2));
      canvas.restore();
    }

    // Rays & markers
    void marker(
      double bearing,
      Color color, {
      double r = 7,
      bool ray = false,
      IconData? icon,
    }) {
      final a = _rad(bearing - 90);
      final o = Offset(math.cos(a), math.sin(a));
      if (ray) {
        canvas.drawLine(
          o * faceR * 0.2,
          o * faceR * 0.86,
          Paint()
            ..shader = LinearGradient(
              colors: [
                color.withValues(alpha: 0),
                color.withValues(alpha: 0.85),
              ],
            ).createShader(Rect.fromPoints(o * faceR * 0.2, o * faceR * 0.86))
            ..strokeWidth = 2,
        );
      }
      final p = o * faceR * 0.9;
      canvas.drawCircle(
        p,
        r * 2.2,
        Paint()
          ..color = color.withValues(alpha: 0.25)
          ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6),
      );
      canvas.drawCircle(p, r, Paint()..color = color);
    }

    if (sunrise != null) marker(sunrise!, GH.gold, ray: true, r: 5);
    if (sunset != null) marker(sunset!, GH.rose, ray: true, r: 5);
    if (moonUp) marker(moonBearing, const Color(0xFFDCE4FF), r: 6);
    marker(
      sunBearing,
      sunUp ? const Color(0xFFFFE7A8) : const Color(0xFF8C93B5),
      r: 9,
      ray: sunUp,
    );

    canvas.restore();

    // Glass reflection
    final glare = Path()
      ..addArc(
        Rect.fromCircle(center: c, radius: faceR * 0.98),
        _rad(200),
        _rad(140),
      )
      ..arcTo(
        Rect.fromCircle(
          center: c + Offset(0, faceR * 0.35),
          radius: faceR * 0.9,
        ),
        _rad(-30),
        _rad(-120),
        false,
      )
      ..close();
    canvas.drawPath(
      glare,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.white.withValues(alpha: 0.09),
            Colors.white.withValues(alpha: 0),
          ],
        ).createShader(Rect.fromCircle(center: c, radius: faceR)),
    );

    // Lubber line
    final top = c - Offset(0, faceR * 0.99);
    final tri = Path()
      ..moveTo(top.dx, top.dy + 18)
      ..lineTo(top.dx - 9, top.dy)
      ..lineTo(top.dx + 9, top.dy)
      ..close();
    canvas.drawPath(tri, Paint()..color = GH.goldLight);
  }

  @override
  bool shouldRepaint(_DialPainter old) =>
      old.heading != heading ||
      old.sunBearing != sunBearing ||
      old.moonBearing != moonBearing;
}
