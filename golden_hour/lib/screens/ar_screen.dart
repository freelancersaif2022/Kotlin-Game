import 'dart:math' as math;

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';

import '../astro/day_light.dart';
import '../astro/sun_calc.dart';
import '../services/app_state.dart';
import '../services/orientation_tracker.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';
import '../widgets/common.dart';

/// Live camera with the sun's (and moon's) path for the chosen day drawn
/// exactly where it will be in the real sky.
class ArScreen extends StatefulWidget {
  const ArScreen({super.key, required this.active});
  final bool active;

  @override
  State<ArScreen> createState() => _ArScreenState();
}

class _ArScreenState extends State<ArScreen> with WidgetsBindingObserver {
  CameraController? _camera;
  String? _cameraError;
  OrientationTracker? _tracker;
  DateTime? _date; // null = today
  bool _showMoon = false;
  bool _starting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _sync();
  }

  @override
  void didUpdateWidget(ArScreen old) {
    super.didUpdateWidget(old);
    _sync();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState s) {
    if (!mounted) return;
    if (s == AppLifecycleState.inactive || s == AppLifecycleState.paused) {
      setState(_stop);
    } else if (s == AppLifecycleState.resumed) {
      setState(_sync);
    }
  }

  void _sync() {
    final place = AppScope.read(context).place;
    if (widget.active && place != null) {
      _start(place);
    } else {
      _stop();
    }
  }

  Future<void> _start(Place place) async {
    if (_starting) return;
    if (_tracker == null ||
        _tracker!.lat != place.lat ||
        _tracker!.lng != place.lng) {
      _tracker?.dispose();
      _tracker = OrientationTracker(lat: place.lat, lng: place.lng)..start();
    }
    if (_camera != null) return;
    _starting = true;
    try {
      final cams = await availableCameras();
      final back = cams.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.back,
        orElse: () => cams.first,
      );
      final ctrl = CameraController(
        back,
        ResolutionPreset.high,
        enableAudio: false,
      );
      await ctrl.initialize();
      if (!mounted || !widget.active) {
        await ctrl.dispose();
      } else {
        setState(() {
          _camera = ctrl;
          _cameraError = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(
          () => _cameraError = 'Camera unavailable — showing virtual sky',
        );
      }
    } finally {
      _starting = false;
    }
  }

  void _stop() {
    final c = _camera;
    _camera = null;
    c?.dispose();
    _tracker?.dispose();
    _tracker = null;
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _stop();
    super.dispose();
  }

  Future<void> _pickDate() async {
    if (!await ensureAccess(
      context,
      reward: RewardFeature.anyDate,
      title: 'AR for any date',
    )) {
      return;
    }
    if (!mounted) return;
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 3),
    );
    if (picked != null) {
      setState(() => _date = sameDay(picked, now) ? null : picked);
    }
  }

  Future<void> _toggleMoon() async {
    if (!_showMoon && !await ensureAccess(context, title: 'AR moon path')) {
      return;
    }
    setState(() => _showMoon = !_showMoon);
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final place = state.place;
    if (!widget.active) return const ColoredBox(color: Colors.black);
    if (place == null) {
      return const Center(child: Text('Set a location first', style: GH.body));
    }
    final now = ServicesScope.of(context).clock.value;
    final date = _date ?? now;
    final day = DayLight.forDate(date, place.lat, place.lng);
    final cam = _camera;

    return Stack(
      fit: StackFit.expand,
      children: [
        if (cam != null && cam.value.isInitialized)
          ClipRect(
            child: FittedBox(
              fit: BoxFit.cover,
              child: SizedBox(
                width: cam.value.previewSize!.height,
                height: cam.value.previewSize!.width,
                child: CameraPreview(cam),
              ),
            ),
          )
        else
          const DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Color(0xFF0B1330),
                  Color(0xFF233A6B),
                  Color(0xFF9D6A4E),
                  Color(0xFF15100D),
                ],
                stops: [0, 0.45, 0.62, 0.7],
              ),
            ),
          ),
        if (_tracker != null)
          ListenableBuilder(
            listenable: _tracker!,
            builder: (context, _) => CustomPaint(
              painter: _ArPainter(
                o: _tracker!.orientation,
                day: day,
                now: _date == null ? now : null,
                showMoon: _showMoon,
                labelTime: (t) => fmtTime(context, t),
              ),
            ),
          ),
        SafeArea(
          child: Column(
            children: [
              if (_tracker != null)
                ListenableBuilder(
                  listenable: _tracker!,
                  builder: (_, _) => _CompassRibbon(
                    heading: _tracker!.orientation?.cameraBearing,
                    sunrise: day.sunrise == null
                        ? null
                        : sunPosition(
                            day.sunrise!,
                            place.lat,
                            place.lng,
                          ).bearing,
                    sunset: day.sunset == null
                        ? null
                        : sunPosition(
                            day.sunset!,
                            place.lat,
                            place.lng,
                          ).bearing,
                  ),
                ),
              const SizedBox(height: 10),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  children: [
                    Pressable(
                      onTap: _pickDate,
                      child: Glass(
                        radius: 16,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 10,
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.event_rounded,
                              size: 16,
                              color: GH.goldLight,
                            ),
                            const SizedBox(width: 8),
                            Text(
                              _date == null ? 'Today' : fmtDate(_date!),
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            if (!state.hasAccess(RewardFeature.anyDate)) ...[
                              const SizedBox(width: 6),
                              const Icon(
                                Icons.lock_rounded,
                                size: 13,
                                color: GH.textLo,
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                    const Spacer(),
                    Pressable(
                      onTap: _toggleMoon,
                      child: Glass(
                        radius: 16,
                        tint: _showMoon ? GH.blue : null,
                        opacity: _showMoon ? 0.25 : 0.10,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 10,
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.dark_mode_rounded,
                              size: 16,
                              color: GH.textHi,
                            ),
                            const SizedBox(width: 6),
                            const Text(
                              'Moon',
                              style: TextStyle(fontWeight: FontWeight.w600),
                            ),
                            if (!state.isPro) ...[
                              const SizedBox(width: 6),
                              const ProBadge(small: true),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              if (_cameraError != null || (_tracker?.needsCalibration ?? false))
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                  child: Glass(
                    radius: 14,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.info_outline_rounded,
                          size: 16,
                          color: GH.goldLight,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            (_tracker?.needsCalibration ?? false)
                                ? 'Compass interference — move your phone in a figure-8'
                                : _cameraError!,
                            style: GH.body.copyWith(
                              fontSize: 12.5,
                              color: GH.textHi,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              const Spacer(),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                child: Glass(
                  tint: Colors.black,
                  opacity: 0.25,
                  child: Row(
                    children: [
                      Expanded(
                        child: Stat(
                          label: 'Sunrise',
                          value: fmtTime(context, day.sunrise),
                          icon: Icons.wb_twilight_rounded,
                          color: GH.gold,
                        ),
                      ),
                      Expanded(
                        child: Stat(
                          label: 'Golden',
                          value: fmtTime(context, day.goldenHourEvening?.start),
                          icon: Icons.wb_sunny_rounded,
                          color: GH.amber,
                        ),
                      ),
                      Expanded(
                        child: Stat(
                          label: 'Sunset',
                          value: fmtTime(context, day.sunset),
                          icon: Icons.wb_twilight_rounded,
                          color: GH.rose,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ArPainter extends CustomPainter {
  _ArPainter({
    required this.o,
    required this.day,
    required this.now,
    required this.showMoon,
    required this.labelTime,
  });
  final DeviceOrientation3D? o;
  final DayLight day;
  final DateTime? now;
  final bool showMoon;
  final String Function(DateTime) labelTime;

  static const _vFov = 64.0; // long-side field of view of a typical main camera

  @override
  void paint(Canvas canvas, Size size) {
    final o = this.o;
    if (o == null) {
      _text(
        canvas,
        'Move your phone to start',
        size.center(Offset.zero),
        GH.textHi,
        15,
        center: true,
      );
      return;
    }
    final f = (size.height / 2) / math.tan(_vFov / 2 * math.pi / 180);
    final c = size.center(Offset.zero);

    Offset? project(double bearing, double alt) {
      final v = o.toDevice(bearing, alt);
      if (v.z > -0.05) return null;
      return Offset(c.dx + f * v.x / -v.z, c.dy - f * v.y / -v.z);
    }

    bool onScreen(Offset p, [double m = 60]) =>
        p.dx > -m &&
        p.dx < size.width + m &&
        p.dy > -m &&
        p.dy < size.height + m;

    // Horizon
    final horizon = Paint()
      ..color = Colors.white.withValues(alpha: 0.45)
      ..strokeWidth = 1;
    Offset? prev;
    for (var b = 0.0; b <= 360; b += 2) {
      final p = project(b, 0);
      if (p != null && prev != null && onScreen(p) && onScreen(prev)) {
        canvas.drawLine(prev, p, horizon);
      }
      prev = p;
    }
    for (final (b, name) in [
      (0.0, 'N'),
      (90.0, 'E'),
      (180.0, 'S'),
      (270.0, 'W'),
    ]) {
      final p = project(b, 0);
      if (p != null && onScreen(p, 0)) {
        _text(
          canvas,
          name,
          p + const Offset(0, 8),
          GH.textHi,
          14,
          center: true,
          bold: true,
        );
      }
    }

    // Sun path
    final lat = day.lat, lng = day.lng;
    final glow = Paint()
      ..strokeWidth = 10
      ..strokeCap = StrokeCap.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6);
    final line = Paint()
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    Offset? last;
    for (var m = 0; m <= 1440; m += 4) {
      final t = day.date.add(Duration(minutes: m));
      final s = sunPosition(t, lat, lng);
      final p = s.altitude > -8 ? project(s.bearing, s.altitude) : null;
      if (p != null && last != null && onScreen(p) && onScreen(last)) {
        final col = switch (LightPhase.fromAltitude(s.altitude)) {
          LightPhase.goldenHour => GH.gold,
          LightPhase.blueHour => GH.blue,
          LightPhase.day => const Color(0xFFFFF0C2),
          _ => const Color(0xFF6E7BB8),
        };
        glow.color = col.withValues(alpha: 0.45);
        line.color = col;
        canvas.drawLine(last, p, glow);
        canvas.drawLine(last, p, line);
      }
      last = p;
    }

    // Hour markers
    for (var h = 0; h < 24; h++) {
      final t = day.date.add(Duration(hours: h));
      final s = sunPosition(t, lat, lng);
      if (s.altitude < -4) continue;
      final p = project(s.bearing, s.altitude);
      if (p == null || !onScreen(p, 0)) continue;
      canvas.drawCircle(p, 4.5, Paint()..color = Colors.white);
      _text(canvas, labelTime(t), p + const Offset(10, -8), GH.textHi, 12);
    }

    // Sunrise / sunset markers
    for (final (t, name, col) in [
      (day.sunrise, 'Sunrise', GH.gold),
      (day.sunset, 'Sunset', GH.rose),
    ]) {
      if (t == null) continue;
      final s = sunPosition(t, lat, lng);
      final p = project(s.bearing, 0);
      if (p == null || !onScreen(p, 0)) continue;
      canvas.drawLine(
        p,
        p + const Offset(0, -46),
        Paint()
          ..color = col
          ..strokeWidth = 2,
      );
      canvas.drawCircle(p, 6, Paint()..color = col);
      _text(
        canvas,
        '$name ${labelTime(t)}',
        p + const Offset(0, -64),
        col,
        13,
        center: true,
        bold: true,
      );
    }

    // Moon path (Pro)
    if (showMoon) {
      final moonLine = Paint()
        ..color = const Color(0xFFCAD5FF)
        ..strokeWidth = 2;
      Offset? lp;
      for (var m = 0; m <= 1440; m += 6) {
        final mp = moonPosition(day.date.add(Duration(minutes: m)), lat, lng);
        final p = mp.altitude > -2 ? project(mp.bearing, mp.altitude) : null;
        if (p != null && lp != null && onScreen(p) && onScreen(lp)) {
          canvas.drawLine(lp, p, moonLine);
        }
        lp = p;
      }
    }

    // Current sun / moon
    final n = now;
    if (n != null) {
      final s = sunPosition(n, lat, lng);
      final p = project(s.bearing, s.altitude);
      if (p != null && onScreen(p, 0)) {
        canvas.drawCircle(
          p,
          38,
          Paint()
            ..shader = RadialGradient(
              colors: [
                GH.goldLight.withValues(alpha: 0.8),
                GH.gold.withValues(alpha: 0),
              ],
            ).createShader(Rect.fromCircle(center: p, radius: 38)),
        );
        canvas.drawCircle(p, 13, Paint()..color = const Color(0xFFFFF6DE));
        _text(
          canvas,
          'Sun now',
          p + const Offset(0, 22),
          GH.textHi,
          12,
          center: true,
          bold: true,
        );
      } else {
        _edgeArrow(
          canvas,
          size,
          project(s.bearing, s.altitude),
          o.toDevice(s.bearing, s.altitude),
          'Sun',
        );
      }
      if (showMoon) {
        final mp = moonPosition(n, lat, lng);
        final p = project(mp.bearing, mp.altitude);
        if (p != null && onScreen(p, 0)) {
          canvas.drawCircle(p, 11, Paint()..color = const Color(0xFFE9EEFF));
          _text(
            canvas,
            'Moon',
            p + const Offset(0, 18),
            GH.textHi,
            12,
            center: true,
          );
        }
      }
    }

    // Reticle
    final r = Paint()
      ..style = PaintingStyle.stroke
      ..color = Colors.white.withValues(alpha: 0.6)
      ..strokeWidth = 1.2;
    canvas.drawCircle(c, 18, r);
    canvas.drawLine(c - const Offset(28, 0), c - const Offset(20, 0), r);
    canvas.drawLine(c + const Offset(20, 0), c + const Offset(28, 0), r);
    _text(
      canvas,
      '${o.cameraPitch.toStringAsFixed(0)}°',
      c + const Offset(26, 10),
      GH.textMid,
      11,
    );
  }

  void _edgeArrow(Canvas canvas, Size size, Offset? p, V3 v, String label) {
    // Direction in screen space (x right, y down).
    var dir = Offset(v.x, -v.y);
    if (dir.distance < 1e-3) dir = const Offset(0, -1);
    dir = dir / dir.distance;
    final c = size.center(Offset.zero);
    final edge =
        c +
        Offset(
          dir.dx * (size.width / 2 - 40),
          dir.dy * (size.height / 2 - 150),
        );
    final angle = math.atan2(dir.dy, dir.dx);
    canvas.save();
    canvas.translate(edge.dx, edge.dy);
    canvas.rotate(angle);
    final path = Path()
      ..moveTo(14, 0)
      ..lineTo(-8, -10)
      ..lineTo(-3, 0)
      ..lineTo(-8, 10)
      ..close();
    canvas.drawPath(path, Paint()..color = GH.goldLight);
    canvas.restore();
    _text(
      canvas,
      label,
      edge + Offset(-dir.dx * 30, -dir.dy * 30),
      GH.goldLight,
      12,
      center: true,
      bold: true,
    );
  }

  void _text(
    Canvas canvas,
    String s,
    Offset at,
    Color color,
    double size, {
    bool center = false,
    bool bold = false,
  }) {
    final tp = TextPainter(
      text: TextSpan(
        text: s,
        style: TextStyle(
          color: color,
          fontSize: size,
          fontWeight: bold ? FontWeight.w700 : FontWeight.w500,
          shadows: const [Shadow(color: Colors.black87, blurRadius: 6)],
          fontFeatures: GH.tabular,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, center ? at - Offset(tp.width / 2, 0) : at);
  }

  @override
  bool shouldRepaint(_ArPainter old) => true;
}

/// Heading tape at the top of the AR view.
class _CompassRibbon extends StatelessWidget {
  const _CompassRibbon({required this.heading, this.sunrise, this.sunset});
  final double? heading;
  final double? sunrise;
  final double? sunset;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Glass(
        radius: 18,
        padding: EdgeInsets.zero,
        tint: Colors.black,
        opacity: 0.2,
        child: SizedBox(
          height: 52,
          child: CustomPaint(
            painter: _RibbonPainter(heading, sunrise, sunset),
            size: Size.infinite,
          ),
        ),
      ),
    );
  }
}

class _RibbonPainter extends CustomPainter {
  _RibbonPainter(this.heading, this.sunrise, this.sunset);
  final double? heading;
  final double? sunrise;
  final double? sunset;

  @override
  void paint(Canvas canvas, Size size) {
    final h = heading;
    if (h == null) return;
    const span = 90.0;
    final pxPerDeg = size.width / span;
    final tick = Paint()..color = Colors.white54;
    for (var d = (h - span / 2).floor(); d <= h + span / 2; d++) {
      if (d % 5 != 0) continue;
      final x = size.width / 2 + (d - h) * pxPerDeg;
      final major = d % 15 == 0;
      canvas.drawLine(
        Offset(x, size.height - (major ? 16 : 9)),
        Offset(x, size.height),
        tick..strokeWidth = major ? 1.4 : 0.8,
      );
      if (d % 45 == 0) {
        final tp = TextPainter(
          text: TextSpan(
            text: cardinal(d.toDouble()),
            style: const TextStyle(
              color: GH.textHi,
              fontWeight: FontWeight.w700,
              fontSize: 12,
            ),
          ),
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(canvas, Offset(x - tp.width / 2, 6));
      }
    }
    for (final (b, col) in [(sunrise, GH.gold), (sunset, GH.rose)]) {
      if (b == null) continue;
      var d = (b - h) % 360;
      if (d > 180) d -= 360;
      if (d.abs() > span / 2) continue;
      final x = size.width / 2 + d * pxPerDeg;
      canvas.drawCircle(Offset(x, size.height - 22), 4, Paint()..color = col);
    }
    final tp = TextPainter(
      text: TextSpan(
        text: '${h.round() % 360}°',
        style: const TextStyle(
          color: GH.goldLight,
          fontWeight: FontWeight.w800,
          fontSize: 13,
          fontFeatures: GH.tabular,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    final cx = size.width / 2;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromCenter(
          center: Offset(cx, 14),
          width: tp.width + 14,
          height: 20,
        ),
        const Radius.circular(8),
      ),
      Paint()..color = Colors.black.withValues(alpha: 0.55),
    );
    tp.paint(canvas, Offset(cx - tp.width / 2, 14 - tp.height / 2));
    canvas.drawLine(
      Offset(cx, size.height - 20),
      Offset(cx, size.height),
      Paint()
        ..color = GH.goldLight
        ..strokeWidth = 2,
    );
  }

  @override
  bool shouldRepaint(_RibbonPainter old) => old.heading != heading;
}
