import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';

enum CharacterMood { idle, listening, thinking, talking }

/// Animated cartoon animal. Blinks, breathes, wiggles its ears and opens its
/// mouth following [mouth] (0..1) so it lip-syncs with the generated voice.
class AnimalCharacter extends StatefulWidget {
  const AnimalCharacter({
    super.key,
    required this.voiceId,
    this.size = 220,
    this.mouth = 0,
    this.mood = CharacterMood.idle,
    this.shadow = true,
    this.alive = true,
  });

  /// When false the character is drawn as a still pose (no blinking/idle).
  final bool alive;
  final String voiceId;
  final double size;
  final double mouth;
  final CharacterMood mood;
  final bool shadow;

  @override
  State<AnimalCharacter> createState() => _AnimalCharacterState();
}

class _AnimalCharacterState extends State<AnimalCharacter>
    with TickerProviderStateMixin {
  late final AnimationController _life = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 6),
  )..repeat();
  late final AnimationController _blink = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 180),
  );
  final _rng = math.Random();
  Timer? _blinkTimer;

  @override
  void initState() {
    super.initState();
    if (widget.alive) {
      _scheduleBlink();
    } else {
      _life.stop();
    }
  }

  void _scheduleBlink() {
    _blinkTimer = Timer(
      Duration(milliseconds: 1800 + _rng.nextInt(3200)),
      () async {
        if (!mounted) return;
        final twice = _rng.nextDouble() < 0.25;
        for (var i = 0; i < (twice ? 2 : 1); i++) {
          await _blink.forward();
          if (!mounted) return;
          await _blink.reverse();
          if (!mounted) return;
        }
        _scheduleBlink();
      },
    );
  }

  @override
  void dispose() {
    _blinkTimer?.cancel();
    _life.dispose();
    _blink.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: AnimatedBuilder(
        animation: Listenable.merge([_life, _blink]),
        builder: (context, _) {
          final t = _life.value * 2 * math.pi;
          final mood = widget.mood;
          double tilt = math.sin(t) * 0.03;
          double ear = math.sin(t * 2) * 0.12;
          double bob = math.sin(t * 3) * 2;
          double look = math.sin(t * 0.5) * 0.4;
          if (mood == CharacterMood.listening) {
            tilt = 0.14 + math.sin(t * 4) * 0.03;
            ear = math.sin(t * 9) * 0.25;
            look = 0.6;
          } else if (mood == CharacterMood.thinking) {
            tilt = math.sin(t * 5) * 0.08;
            ear = math.sin(t * 12) * 0.2;
            look = math.sin(t * 6);
            bob = math.sin(t * 10) * 3;
          } else if (mood == CharacterMood.talking) {
            bob = -widget.mouth * 6;
            tilt = math.sin(t * 6) * 0.05;
            ear = widget.mouth * 0.35 + math.sin(t * 8) * 0.08;
            look = 0;
          }
          return SizedBox.square(
            dimension: widget.size,
            child: CustomPaint(
              painter: _AnimalPainter(
                id: widget.voiceId,
                mouth: widget.mouth,
                blink: _blink.value,
                ear: ear,
                tilt: tilt,
                bob: bob,
                look: look,
                breath: math.sin(t * 2) * 0.012,
                shadow: widget.shadow,
                happy: mood != CharacterMood.thinking,
              ),
            ),
          );
        },
      ),
    );
  }
}

class _Palette {
  const _Palette(
    this.base,
    this.light,
    this.dark,
    this.ear,
    this.muzzle, {
    this.iris = const Color(0xFF3A2416),
  });

  final Color base, light, dark, ear, muzzle, iris;
}

const _palettes = {
  'dog': _Palette(
    Color(0xFFE9A35B),
    Color(0xFFF8CF98),
    Color(0xFFB86E2E),
    Color(0xFF8A4B22),
    Color(0xFFFFF1DC),
  ),
  'puppy': _Palette(
    Color(0xFFF6D29F),
    Color(0xFFFFEED3),
    Color(0xFFD5A160),
    Color(0xFFDE9F5C),
    Color(0xFFFFFFFF),
  ),
  'cat': _Palette(
    Color(0xFFB9B2CB),
    Color(0xFFE2DDF0),
    Color(0xFF857D9C),
    Color(0xFFFFB3C7),
    Color(0xFFF7F4FD),
    iris: Color(0xFF3E8E41),
  ),
  'wolf': _Palette(
    Color(0xFF8D9AB1),
    Color(0xFFC2CBDA),
    Color(0xFF5B6781),
    Color(0xFFE9D9DC),
    Color(0xFFF3F5F9),
    iris: Color(0xFFB7791F),
  ),
  'duck': _Palette(
    Color(0xFFFFD43B),
    Color(0xFFFFEC94),
    Color(0xFFE9A800),
    Color(0xFFFF9F1C),
    Color(0xFFFFB347),
  ),
  'cow': _Palette(
    Color(0xFFFFFFFF),
    Color(0xFFFFFFFF),
    Color(0xFFD5DAE3),
    Color(0xFFFFB7C5),
    Color(0xFFFFB7C5),
  ),
  'pig': _Palette(
    Color(0xFFFFB3C1),
    Color(0xFFFFD9E0),
    Color(0xFFE68A9E),
    Color(0xFFF39AAE),
    Color(0xFFFF9AB0),
  ),
  'sheep': _Palette(
    Color(0xFFF6E9DA),
    Color(0xFFFFF8F0),
    Color(0xFFD9C4AD),
    Color(0xFFC9A98A),
    Color(0xFFFFFFFF),
  ),
  'goat': _Palette(
    Color(0xFFEFE4D6),
    Color(0xFFFFFAF2),
    Color(0xFFCDB89F),
    Color(0xFFD9C3A8),
    Color(0xFFFFFFFF),
    iris: Color(0xFF8A6A1F),
  ),
  'horse': _Palette(
    Color(0xFFB97A4B),
    Color(0xFFD9A273),
    Color(0xFF8A5530),
    Color(0xFF5A3420),
    Color(0xFFE9C7A2),
  ),
  'rooster': _Palette(
    Color(0xFFFFF4E2),
    Color(0xFFFFFFFF),
    Color(0xFFE9D3B0),
    Color(0xFFFF3B3B),
    Color(0xFFFFB627),
  ),
  'tiger': _Palette(
    Color(0xFFFF9A3C),
    Color(0xFFFFC27E),
    Color(0xFFE0701F),
    Color(0xFFFFE9D6),
    Color(0xFFFFFFFF),
    iris: Color(0xFF7A9A2A),
  ),
  'lion': _Palette(
    Color(0xFFF5C26B),
    Color(0xFFFFDFA3),
    Color(0xFFD99A3D),
    Color(0xFFC8742A),
    Color(0xFFFFF3DC),
    iris: Color(0xFF8A5A1F),
  ),
  'zebra': _Palette(
    Color(0xFFFFFFFF),
    Color(0xFFFFFFFF),
    Color(0xFFD9DCE4),
    Color(0xFF2B2833),
    Color(0xFF4A4552),
  ),
  'deer': _Palette(
    Color(0xFFC98B55),
    Color(0xFFE8B585),
    Color(0xFF9E6435),
    Color(0xFFF2C9A2),
    Color(0xFFFFF4E8),
  ),
  'bird': _Palette(
    Color(0xFF5BB8FF),
    Color(0xFFA8DCFF),
    Color(0xFF2F86D6),
    Color(0xFF2F86D6),
    Color(0xFFFFB627),
    iris: Color(0xFF1C2A44),
  ),
  'elephant': _Palette(
    Color(0xFFA8B2C2),
    Color(0xFFCDD4E0),
    Color(0xFF7D8799),
    Color(0xFFFFC4D0),
    Color(0xFFBCC5D3),
  ),
  'monkey': _Palette(
    Color(0xFF9C6B45),
    Color(0xFFC08A5E),
    Color(0xFF6E4527),
    Color(0xFFF2D7B8),
    Color(0xFFF2D7B8),
  ),
  'owl': _Palette(
    Color(0xFFB07A4C),
    Color(0xFFD39E6C),
    Color(0xFF7E5232),
    Color(0xFFF6E6CC),
    Color(0xFFFFC233),
    iris: Color(0xFFE08A00),
  ),
  'frog': _Palette(
    Color(0xFF7ED957),
    Color(0xFFB5F08A),
    Color(0xFF3FA83A),
    Color(0xFF3FA83A),
    Color(0xFFDDF7C4),
    iris: Color(0xFF2D6A1F),
  ),
};

class _AnimalPainter extends CustomPainter {
  _AnimalPainter({
    required this.id,
    required this.mouth,
    required this.blink,
    required this.ear,
    required this.tilt,
    required this.bob,
    required this.look,
    required this.breath,
    required this.shadow,
    required this.happy,
  });

  final String id;
  final double mouth, blink, ear, tilt, bob, look, breath;
  final bool shadow, happy;

  static const ink = Color(0xFF2B2140);
  late final _Palette pal = _palettes[id] ?? _palettes['dog']!;

  Paint get _stroke => Paint()
    ..color = ink
    ..style = PaintingStyle.stroke
    ..strokeWidth = 3.4
    ..strokeJoin = StrokeJoin.round
    ..strokeCap = StrokeCap.round;

  Paint _fill(Color c) => Paint()..color = c;

  Paint _shade(Rect r, Color light, Color base, Color dark) =>
      Paint()
        ..shader = RadialGradient(
          center: const Alignment(-0.35, -0.5),
          radius: 1.05,
          colors: [light, base, dark],
          stops: const [0, 0.55, 1],
        ).createShader(r);

  @override
  void paint(Canvas canvas, Size size) {
    canvas.scale(size.width / 200);
    if (shadow) {
      canvas.drawOval(
        Rect.fromCenter(
          center: const Offset(100, 186),
          width: 110 - bob * 2,
          height: 14,
        ),
        Paint()
          ..color = ink.withValues(alpha: 0.13)
          ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 5),
      );
    }
    canvas.save();
    canvas.translate(100, 112 + bob);
    canvas.rotate(tilt);
    canvas.scale(1 + breath, 1 - breath);
    canvas.translate(-100, -112);

    switch (id) {
      case 'duck':
        _duck(canvas);
      case 'cow':
        _cow(canvas);
      case 'cat':
        _cat(canvas);
      case 'wolf':
        _wolf(canvas);
      case 'puppy':
        _dog(canvas, puppy: true);
      case 'pig':
        _pig(canvas);
      case 'sheep':
        _sheep(canvas);
      case 'goat':
        _goat(canvas);
      case 'horse':
        _horse(canvas);
      case 'rooster':
        _rooster(canvas);
      case 'frog':
        _frog(canvas);
      case 'tiger':
        _bigCat(canvas, lion: false);
      case 'lion':
        _bigCat(canvas, lion: true);
      case 'zebra':
        _zebra(canvas);
      case 'deer':
        _deer(canvas);
      case 'bird':
        _bird(canvas);
      case 'elephant':
        _elephant(canvas);
      case 'monkey':
        _monkey(canvas);
      case 'owl':
        _owl(canvas);
      default:
        _dog(canvas);
    }
    canvas.restore();
  }

  // ---- shared parts --------------------------------------------------------

  Path _headPath({double w = 128, double h = 118, double cy = 110}) {
    final r = Rect.fromCenter(center: Offset(100, cy), width: w, height: h);
    return Path()..addRRect(
      RRect.fromRectAndRadius(r, Radius.elliptical(w * 0.48, h * 0.5)),
    );
  }

  void _drawHead(Canvas c, Path head) {
    final b = head.getBounds();
    c.drawPath(head, _shade(b, pal.light, pal.base, pal.dark));
    c.drawPath(head, _stroke);
  }

  void _eyes(
    Canvas c, {
    double y = 96,
    double dx = 24,
    double r = 13,
    Color? iris,
  }) {
    for (final side in [-1, 1]) {
      final cx = 100 + side * dx;
      final open = (1 - blink).clamp(0.08, 1.0);
      final rect = Rect.fromCenter(
        center: Offset(cx, y),
        width: r * 2,
        height: r * 2.2 * open,
      );
      if (open < 0.2) {
        final p = Path()
          ..moveTo(cx - r, y)
          ..quadraticBezierTo(cx, y + 5, cx + r, y);
        c.drawPath(p, _stroke);
        continue;
      }
      c.drawOval(rect, _fill(Colors.white));
      c.save();
      c.clipPath(Path()..addOval(rect));
      final ic = Offset(cx + look * r * 0.3, y + r * 0.12);
      c.drawCircle(ic, r * 0.68, _fill(iris ?? pal.iris));
      c.drawCircle(ic, r * 0.4, _fill(const Color(0xFF120B1C)));
      c.drawCircle(
        ic.translate(-r * 0.26, -r * 0.3),
        r * 0.22,
        _fill(Colors.white),
      );
      c.drawCircle(
        ic.translate(r * 0.22, r * 0.2),
        r * 0.1,
        _fill(Colors.white.withValues(alpha: 0.85)),
      );
      c.restore();
      c.drawOval(rect, _stroke..strokeWidth = 3);
    }
  }

  void _cheeks(Canvas c, {double y = 120, double dx = 40}) {
    final p = Paint()
      ..color = const Color(0xFFFF7A9A).withValues(alpha: 0.32)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 3);
    c.drawOval(
      Rect.fromCenter(center: Offset(100 - dx, y), width: 20, height: 12),
      p,
    );
    c.drawOval(
      Rect.fromCenter(center: Offset(100 + dx, y), width: 20, height: 12),
      p,
    );
  }

  /// Happy smile when closed, a big cartoon mouth with tongue when talking.
  void _mouth(Canvas c, {double y = 140, double w = 30}) {
    final open = mouth.clamp(0.0, 1.0);
    if (open < 0.08) {
      final p = Path()
        ..moveTo(100 - w / 2, y - 2)
        ..quadraticBezierTo(100 - w / 4, y + (happy ? 8 : 3), 100, y)
        ..quadraticBezierTo(
          100 + w / 4,
          y + (happy ? 8 : 3),
          100 + w / 2,
          y - 2,
        );
      c.drawPath(p, _stroke);
      return;
    }
    final h = 6 + open * 24;
    final ow = w * (0.75 + open * 0.3);
    final m = Path()
      ..moveTo(100 - ow / 2, y - 2)
      ..quadraticBezierTo(100, y + 4, 100 + ow / 2, y - 2)
      ..quadraticBezierTo(100 + ow / 2, y + h, 100, y + h)
      ..quadraticBezierTo(100 - ow / 2, y + h, 100 - ow / 2, y - 2)
      ..close();
    c.drawPath(m, _fill(const Color(0xFF5E1A2B)));
    c.save();
    c.clipPath(m);
    c.drawOval(
      Rect.fromCenter(
        center: Offset(100, y + h * 0.95),
        width: ow * 0.7,
        height: h * 0.8,
      ),
      _fill(const Color(0xFFFF7A90)),
    );
    c.restore();
    c.drawPath(m, _stroke);
  }

  void _nose(
    Canvas c, {
    double y = 124,
    double w = 20,
    Color color = const Color(0xFF241A2E),
  }) {
    final p = Path()
      ..moveTo(100 - w / 2, y - 4)
      ..quadraticBezierTo(100, y - 9, 100 + w / 2, y - 4)
      ..quadraticBezierTo(100 + w / 2 - 1, y + 4, 100, y + 7)
      ..quadraticBezierTo(100 - w / 2 + 1, y + 4, 100 - w / 2, y - 4)
      ..close();
    c.drawPath(p, _fill(color));
    c.drawOval(
      Rect.fromCenter(
        center: Offset(100 - w * 0.18, y - 3),
        width: w * 0.35,
        height: 4,
      ),
      _fill(Colors.white.withValues(alpha: 0.55)),
    );
  }

  // ---- animals -------------------------------------------------------------

  void _dog(Canvas c, {bool puppy = false}) {
    final head = _headPath(w: puppy ? 124 : 128, h: puppy ? 116 : 118);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    if (puppy) {
      c.drawOval(
        Rect.fromCenter(center: const Offset(84, 66), width: 30, height: 22),
        _fill(pal.dark.withValues(alpha: 0.45)),
      );
    } else {
      c.drawOval(
        Rect.fromCenter(center: const Offset(126, 94), width: 40, height: 44),
        _fill(pal.dark.withValues(alpha: 0.42)),
      );
    }
    c.restore();
    // Floppy ears hang over the sides of the head.
    final earLen = puppy ? 54.0 : 64.0;
    final earW = puppy ? 15.0 : 18.0;
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * (puppy ? 44 : 46), puppy ? 58 : 58);
      c.rotate(-side * (0.42 + ear));
      final e = Path()
        ..moveTo(-earW * 0.7, 0)
        ..cubicTo(-earW * 1.5, earLen * 0.45, -earW, earLen, 0, earLen)
        ..cubicTo(earW, earLen, earW * 1.5, earLen * 0.45, earW * 0.7, 0)
        ..quadraticBezierTo(0, -6, -earW * 0.7, 0)
        ..close();
      c.drawPath(
        e,
        _shade(
          e.getBounds(),
          Color.lerp(pal.ear, Colors.white, 0.15)!,
          pal.ear,
          Color.lerp(pal.ear, Colors.black, 0.25)!,
        ),
      );
      c.drawPath(e, _stroke);
      c.restore();
    }
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 136),
      width: 62,
      height: 46,
    );
    c.drawOval(
      muzzle,
      _shade(
        muzzle,
        Colors.white,
        pal.muzzle,
        Color.lerp(pal.muzzle, pal.dark, 0.35)!,
      ),
    );
    c.drawOval(muzzle, _stroke..strokeWidth = 2.6);
    _eyes(c, y: 96, dx: 25, r: puppy ? 15 : 13);
    _brows(c, y: puppy ? 74 : 78);
    _cheeks(c, y: 124, dx: 42);
    _nose(c, y: 126);
    _mouth(c, y: 141, w: 30);
  }

  void _brows(Canvas c, {double y = 78}) {
    final p = _stroke..strokeWidth = 3;
    for (final side in [-1, 1]) {
      final x = 100.0 + side * 25;
      final lift = happy ? -2.0 : 2.0;
      c.drawPath(
        Path()
          ..moveTo(x - 8, y + 2)
          ..quadraticBezierTo(x, y - 3 + lift, x + 8, y + 2),
        p,
      );
    }
  }

  void _cat(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 40, 68);
      c.rotate(side * (0.2 + ear * 0.6));
      final outer = Path()
        ..moveTo(-24, 14)
        ..quadraticBezierTo(-10, -30, 2, -44)
        ..quadraticBezierTo(16, -28, 24, 12)
        ..close();
      c.drawPath(
        outer,
        _shade(outer.getBounds(), pal.light, pal.base, pal.dark),
      );
      c.drawPath(outer, _stroke);
      final inner = Path()
        ..moveTo(-12, 8)
        ..quadraticBezierTo(-4, -18, 2, -30)
        ..quadraticBezierTo(10, -16, 13, 8)
        ..close();
      c.drawPath(inner, _fill(pal.ear));
      c.restore();
    }
    final head = _headPath(w: 132, h: 112, cy: 112);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    final stripe = Paint()
      ..color = pal.dark.withValues(alpha: 0.55)
      ..strokeWidth = 5
      ..strokeCap = StrokeCap.round;
    c.drawLine(const Offset(100, 58), const Offset(100, 74), stripe);
    c.drawLine(const Offset(88, 60), const Offset(91, 72), stripe);
    c.drawLine(const Offset(112, 60), const Offset(109, 72), stripe);
    c.drawLine(const Offset(36, 108), const Offset(50, 110), stripe);
    c.drawLine(const Offset(164, 108), const Offset(150, 110), stripe);
    c.restore();
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 134),
      width: 54,
      height: 36,
    );
    c.drawOval(muzzle, _fill(pal.muzzle));
    _eyes(c, y: 100, dx: 27, r: 14);
    _cheeks(c, y: 124, dx: 44);
    _nose(c, y: 124, w: 14, color: const Color(0xFFFF7FA0));
    final wh = Paint()
      ..color = ink.withValues(alpha: 0.75)
      ..strokeWidth = 1.8
      ..strokeCap = StrokeCap.round;
    for (final side in [-1, 1]) {
      for (var i = -1; i <= 1; i++) {
        c.drawLine(
          Offset(100 + side * 22, 130 + i * 5.0),
          Offset(100 + side * 58, 124 + i * 9.0 - ear * 6),
          wh,
        );
      }
    }
    _mouth(c, y: 136, w: 24);
  }

  void _wolf(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 38, 64);
      c.rotate(side * (0.12 + ear * 0.5));
      final outer = Path()
        ..moveTo(-22, 16)
        ..lineTo(0, -52)
        ..lineTo(22, 14)
        ..close();
      c.drawPath(
        outer,
        _shade(outer.getBounds(), pal.light, pal.base, pal.dark),
      );
      c.drawPath(outer, _stroke);
      c.drawPath(
        Path()
          ..moveTo(-10, 10)
          ..lineTo(0, -32)
          ..lineTo(10, 10)
          ..close(),
        _fill(pal.ear),
      );
      c.restore();
    }
    // Fluffy cheeks silhouette.
    final head = Path()
      ..moveTo(100, 50)
      ..cubicTo(140, 50, 160, 76, 162, 108)
      ..lineTo(172, 118)
      ..lineTo(160, 124)
      ..lineTo(166, 136)
      ..lineTo(150, 140)
      ..cubicTo(140, 162, 120, 170, 100, 170)
      ..cubicTo(80, 170, 60, 162, 50, 140)
      ..lineTo(34, 136)
      ..lineTo(40, 124)
      ..lineTo(28, 118)
      ..lineTo(38, 108)
      ..cubicTo(40, 76, 60, 50, 100, 50)
      ..close();
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    final face = Path()
      ..moveTo(100, 96)
      ..cubicTo(78, 100, 56, 112, 44, 140)
      ..lineTo(100, 180)
      ..lineTo(156, 140)
      ..cubicTo(144, 112, 122, 100, 100, 96)
      ..close();
    c.drawPath(face, _fill(pal.muzzle));
    c.drawPath(
      Path()
        ..moveTo(100, 52)
        ..lineTo(92, 80)
        ..lineTo(100, 92)
        ..lineTo(108, 80)
        ..close(),
      _fill(pal.light.withValues(alpha: 0.7)),
    );
    c.restore();
    c.drawPath(head, _stroke);
    _eyes(c, y: 98, dx: 25, r: 13);
    _brows(c, y: 80);
    _cheeks(c, y: 126, dx: 40);
    _nose(c, y: 126, w: 22);
    _mouth(c, y: 141, w: 30);
  }

  void _duck(Canvas c) {
    // Feather tuft.
    for (var i = -1; i <= 1; i++) {
      c.save();
      c.translate(100 + i * 7.0, 56);
      c.rotate(i * 0.45 + ear * 0.8);
      final f = Path()
        ..moveTo(-5, 4)
        ..quadraticBezierTo(-6, -16, 2, -24)
        ..quadraticBezierTo(8, -12, 5, 4)
        ..close();
      c.drawPath(f, _fill(pal.dark));
      c.drawPath(f, _stroke..strokeWidth = 2.6);
      c.restore();
    }
    final head = _headPath(w: 130, h: 124, cy: 114);
    _drawHead(c, head);
    _eyes(c, y: 98, dx: 26, r: 14, iris: const Color(0xFF1C2A44));
    _cheeks(c, y: 122, dx: 46);
    final open = mouth.clamp(0.0, 1.0) * 16;
    final lower = Path()
      ..moveTo(70, 134)
      ..quadraticBezierTo(100, 146 + open * 1.4, 130, 134)
      ..quadraticBezierTo(124, 150 + open * 1.6, 100, 152 + open * 1.6)
      ..quadraticBezierTo(76, 150 + open * 1.6, 70, 134)
      ..close();
    if (open > 1) {
      c.drawPath(
        Path()
          ..moveTo(74, 133)
          ..quadraticBezierTo(100, 150 + open * 1.4, 126, 133)
          ..close(),
        _fill(const Color(0xFF5E1A2B)),
      );
    }
    c.drawPath(
      lower,
      _shade(lower.getBounds(), pal.muzzle, pal.ear, const Color(0xFFE07A00)),
    );
    c.drawPath(lower, _stroke..strokeWidth = 3);
    final upper = Path()
      ..moveTo(64, 128)
      ..quadraticBezierTo(70, 112, 100, 112)
      ..quadraticBezierTo(130, 112, 136, 128)
      ..quadraticBezierTo(120, 140 - open * 0.2, 100, 138 - open * 0.2)
      ..quadraticBezierTo(80, 140 - open * 0.2, 64, 128)
      ..close();
    c.drawPath(
      upper,
      _shade(
        upper.getBounds(),
        const Color(0xFFFFC46B),
        pal.ear,
        const Color(0xFFE07A00),
      ),
    );
    c.drawPath(upper, _stroke..strokeWidth = 3);
    c.drawCircle(const Offset(92, 120), 2.2, _fill(ink.withValues(alpha: 0.6)));
    c.drawCircle(
      const Offset(108, 120),
      2.2,
      _fill(ink.withValues(alpha: 0.6)),
    );
  }

  void _cow(Canvas c) {
    for (final side in [-1, 1]) {
      // horns
      c.save();
      c.translate(100 + side * 28, 62);
      c.rotate(side * 0.3);
      final h = Path()
        ..moveTo(-6, 6)
        ..quadraticBezierTo(-6, -14, side * 6.0, -20)
        ..quadraticBezierTo(8, -10, 7, 6)
        ..close();
      c.drawPath(h, _fill(const Color(0xFFF3E3C0)));
      c.drawPath(h, _stroke..strokeWidth = 2.8);
      c.restore();
      // ears
      c.save();
      c.translate(100 + side * 60, 88);
      c.rotate(side * (-0.25 + ear * 0.6));
      final e = Rect.fromCenter(
        center: Offset(side * 14, 0),
        width: 36,
        height: 20,
      );
      c.drawOval(e, _fill(Colors.white));
      c.drawOval(e.deflate(5), _fill(pal.ear));
      c.drawOval(e, _stroke..strokeWidth = 3);
      c.restore();
    }
    final head = _headPath(w: 118, h: 124, cy: 110);
    c.drawPath(
      head,
      _shade(head.getBounds(), Colors.white, Colors.white, pal.dark),
    );
    c.save();
    c.clipPath(head);
    final spot = _fill(const Color(0xFF2E2B35));
    c.drawPath(
      Path()
        ..moveTo(120, 48)
        ..cubicTo(150, 50, 168, 80, 150, 96)
        ..cubicTo(136, 106, 118, 90, 122, 76)
        ..cubicTo(126, 64, 110, 56, 120, 48)
        ..close(),
      spot,
    );
    c.drawOval(
      Rect.fromCenter(center: const Offset(56, 116), width: 30, height: 40),
      spot,
    );
    c.restore();
    c.drawPath(head, _stroke);
    _eyes(c, y: 92, dx: 24, r: 13);
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 142),
      width: 92,
      height: 56,
    );
    c.drawOval(
      muzzle,
      _shade(
        muzzle,
        const Color(0xFFFFD3DC),
        pal.muzzle,
        const Color(0xFFE88DA0),
      ),
    );
    c.drawOval(muzzle, _stroke);
    final nos = _fill(const Color(0xFFB34A62));
    c.drawOval(
      Rect.fromCenter(center: const Offset(84, 134), width: 10, height: 14),
      nos,
    );
    c.drawOval(
      Rect.fromCenter(center: const Offset(116, 134), width: 10, height: 14),
      nos,
    );
    _mouth(c, y: 152, w: 28);
  }

  void _pig(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 42, 64);
      c.rotate(side * (0.35 + ear * 0.6));
      final e = Path()
        ..moveTo(-16, 10)
        ..quadraticBezierTo(-6, -26, 4, -30)
        ..quadraticBezierTo(16, -14, 18, 10)
        ..close();
      c.drawPath(e, _shade(e.getBounds(), pal.light, pal.ear, pal.dark));
      c.drawPath(e, _stroke);
      c.restore();
    }
    final head = _headPath(w: 136, h: 120, cy: 112);
    _drawHead(c, head);
    _eyes(c, y: 96, dx: 28, r: 12);
    _brows(c, y: 78);
    _cheeks(c, y: 118, dx: 48);
    final snout = Rect.fromCenter(
      center: const Offset(100, 128),
      width: 58,
      height: 38,
    );
    c.drawOval(snout, _shade(snout, pal.light, pal.muzzle, pal.dark));
    c.drawOval(snout, _stroke..strokeWidth = 3);
    final nos = _fill(const Color(0xFFB34A62));
    for (final side in [-1, 1]) {
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100 + side * 11.0, 128),
          width: 9,
          height: 14,
        ),
        nos,
      );
    }
    _mouth(c, y: 152, w: 26);
  }

  /// Puffy wool cloud used by the sheep.
  void _wool(Canvas c, List<Offset> puffs, double r) {
    final fill = _shade(
      Rect.fromLTWH(30, 30, 140, 140),
      Colors.white,
      const Color(0xFFFBF8F4),
      const Color(0xFFDCD5CC),
    );
    for (final o in puffs) {
      c.drawCircle(o, r + 1.7, _fill(ink));
    }
    for (final o in puffs) {
      c.drawCircle(o, r, fill);
    }
  }

  void _sheep(Canvas c) {
    _wool(c, const [
      Offset(52, 78),
      Offset(70, 56),
      Offset(100, 48),
      Offset(130, 56),
      Offset(148, 78),
      Offset(156, 106),
      Offset(44, 106),
      Offset(150, 132),
      Offset(50, 132),
    ], 24);
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 46, 100);
      c.rotate(side * (0.2 + ear * 0.6));
      final e = Rect.fromCenter(
        center: Offset(side * 18.0, 0),
        width: 38,
        height: 18,
      );
      c.drawOval(e, _fill(pal.ear));
      c.drawOval(e, _stroke..strokeWidth = 3);
      c.restore();
    }
    final face = _headPath(w: 98, h: 116, cy: 116);
    _drawHead(c, face);
    _wool(c, const [Offset(84, 64), Offset(100, 58), Offset(116, 64)], 13);
    _eyes(c, y: 104, dx: 21, r: 12);
    _cheeks(c, y: 128, dx: 34);
    _nose(c, y: 136, w: 16, color: const Color(0xFF5E4636));
    _mouth(c, y: 150, w: 24);
  }

  void _goat(Canvas c) {
    for (final side in [-1, 1]) {
      // curved horns
      final h = Path()
        ..moveTo(100 + side * 18.0, 66)
        ..quadraticBezierTo(100 + side * 26.0, 30, 100 + side * 52.0, 34)
        ..quadraticBezierTo(100 + side * 34.0, 40, 100 + side * 30.0, 70)
        ..close();
      c.drawPath(
        h,
        _shade(
          h.getBounds(),
          const Color(0xFFE0D8CE),
          const Color(0xFFB9AFA3),
          const Color(0xFF8D8277),
        ),
      );
      c.drawPath(h, _stroke..strokeWidth = 3);
      c.save();
      c.translate(100 + side * 44, 92);
      c.rotate(side * (0.45 + ear * 0.6));
      final e = Path()
        ..moveTo(0, -9)
        ..quadraticBezierTo(side * 34.0, -6, side * 44.0, 8)
        ..quadraticBezierTo(side * 26.0, 12, 0, 9)
        ..close();
      c.drawPath(e, _fill(pal.ear));
      c.drawPath(e, _stroke..strokeWidth = 3);
      c.restore();
    }
    final head = _headPath(w: 104, h: 130, cy: 112);
    _drawHead(c, head);
    final beard = Path()
      ..moveTo(88, 166)
      ..quadraticBezierTo(100, 200, 112, 166)
      ..close();
    c.drawPath(beard, _fill(pal.dark));
    c.drawPath(beard, _stroke..strokeWidth = 3);
    _eyes(c, y: 98, dx: 22, r: 12);
    _brows(c, y: 80);
    _cheeks(c, y: 124, dx: 36);
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 142),
      width: 64,
      height: 40,
    );
    c.drawOval(muzzle, _fill(pal.light));
    _nose(c, y: 134, w: 18, color: const Color(0xFF6B4E3D));
    _mouth(c, y: 150, w: 24);
  }

  void _horse(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 30, 58);
      c.rotate(side * (0.25 + ear * 0.5));
      final e = Path()
        ..moveTo(-12, 12)
        ..quadraticBezierTo(-6, -22, 0, -34)
        ..quadraticBezierTo(8, -20, 12, 12)
        ..close();
      c.drawPath(e, _shade(e.getBounds(), pal.light, pal.base, pal.dark));
      c.drawPath(e, _stroke);
      c.restore();
    }
    final head = _headPath(w: 108, h: 136, cy: 110);
    _drawHead(c, head);
    // mane tuft
    final mane = Path()
      ..moveTo(78, 52)
      ..quadraticBezierTo(90, 30, 100, 46)
      ..quadraticBezierTo(112, 26, 122, 52)
      ..quadraticBezierTo(108, 70, 100, 62)
      ..quadraticBezierTo(90, 72, 78, 52)
      ..close();
    c.drawPath(mane, _fill(pal.ear));
    c.drawPath(mane, _stroke..strokeWidth = 3);
    c.drawPath(
      Path()
        ..moveTo(100, 66)
        ..lineTo(94, 86)
        ..lineTo(106, 86)
        ..close(),
      _fill(Colors.white.withValues(alpha: 0.85)),
    );
    _eyes(c, y: 94, dx: 24, r: 12);
    _brows(c, y: 76);
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 146),
      width: 86,
      height: 54,
    );
    c.drawOval(muzzle, _shade(muzzle, Colors.white, pal.muzzle, pal.light));
    c.drawOval(muzzle, _stroke..strokeWidth = 3);
    final nos = _fill(const Color(0xFF5A3420));
    for (final side in [-1, 1]) {
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100 + side * 18.0, 138),
          width: 9,
          height: 13,
        ),
        nos,
      );
    }
    _mouth(c, y: 156, w: 28);
  }

  void _rooster(Canvas c) {
    // comb
    final comb = Path()
      ..moveTo(76, 66)
      ..quadraticBezierTo(70, 40, 86, 44)
      ..quadraticBezierTo(88, 24, 102, 32)
      ..quadraticBezierTo(112, 18, 120, 38)
      ..quadraticBezierTo(136, 40, 126, 66)
      ..close();
    c.drawPath(
      comb,
      _shade(
        comb.getBounds(),
        const Color(0xFFFF7A6B),
        pal.ear,
        const Color(0xFFC81E1E),
      ),
    );
    c.drawPath(comb, _stroke);
    final head = _headPath(w: 124, h: 124, cy: 114);
    _drawHead(c, head);
    _eyes(c, y: 98, dx: 26, r: 13, iris: const Color(0xFFC25B00));
    _brows(c, y: 78);
    _cheeks(c, y: 120, dx: 44);
    // wattle
    final wattle = Path()
      ..moveTo(92, 146)
      ..quadraticBezierTo(86, 178, 100, 176)
      ..quadraticBezierTo(114, 178, 108, 146)
      ..close();
    c.drawPath(wattle, _fill(pal.ear));
    c.drawPath(wattle, _stroke..strokeWidth = 3);
    final open = mouth.clamp(0.0, 1.0) * 14;
    final beakColor = _shade(
      const Rect.fromLTWH(76, 110, 48, 50),
      const Color(0xFFFFD36B),
      pal.muzzle,
      const Color(0xFFE08A00),
    );
    final lower = Path()
      ..moveTo(84, 134)
      ..lineTo(116, 134)
      ..lineTo(100, 148 + open)
      ..close();
    if (open > 1) {
      c.drawPath(
        Path()
          ..moveTo(86, 133)
          ..lineTo(114, 133)
          ..lineTo(100, 144 + open)
          ..close(),
        _fill(const Color(0xFF5E1A2B)),
      );
    }
    c.drawPath(lower, beakColor);
    c.drawPath(lower, _stroke..strokeWidth = 3);
    final upper = Path()
      ..moveTo(80, 132 - open * 0.3)
      ..quadraticBezierTo(100, 112, 120, 132 - open * 0.3)
      ..lineTo(100, 146 - open * 0.3)
      ..close();
    c.drawPath(upper, beakColor);
    c.drawPath(upper, _stroke..strokeWidth = 3);
  }

  void _frog(Canvas c) {
    // eye bumps
    for (final side in [-1, 1]) {
      final r = Rect.fromCenter(
        center: Offset(100 + side * 32.0, 72),
        width: 50,
        height: 46,
      );
      c.drawOval(r, _shade(r, pal.light, pal.base, pal.dark));
      c.drawOval(r, _stroke);
    }
    final head = _headPath(w: 158, h: 108, cy: 122);
    _drawHead(c, head);
    for (final side in [-1, 1]) {
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100 + side * 32.0, 76),
          width: 48,
          height: 42,
        ),
        _shade(
          Rect.fromCenter(
            center: Offset(100 + side * 32.0, 76),
            width: 48,
            height: 42,
          ),
          pal.light,
          pal.base,
          pal.dark,
        ),
      );
    }
    _eyes(c, y: 74, dx: 32, r: 15);
    _cheeks(c, y: 124, dx: 52);
    c.drawCircle(const Offset(92, 112), 2.4, _fill(ink));
    c.drawCircle(const Offset(108, 112), 2.4, _fill(ink));
    final open = mouth.clamp(0.0, 1.0);
    if (open < 0.08) {
      c.drawPath(
        Path()
          ..moveTo(52, 128)
          ..quadraticBezierTo(100, 158, 148, 128),
        _stroke,
      );
    } else {
      final m = Path()
        ..moveTo(56, 128)
        ..quadraticBezierTo(100, 140, 144, 128)
        ..quadraticBezierTo(100, 150 + open * 30, 56, 128)
        ..close();
      c.drawPath(m, _fill(const Color(0xFF5E1A2B)));
      c.save();
      c.clipPath(m);
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100, 150 + open * 24),
          width: 50,
          height: 26,
        ),
        _fill(const Color(0xFFFF7A90)),
      );
      c.restore();
      c.drawPath(m, _stroke);
    }
  }

  void _bigCat(Canvas c, {required bool lion}) {
    if (lion) {
      // scalloped mane
      final mane = Path();
      const n = 16;
      for (var i = 0; i <= n; i++) {
        final a = i / n * 2 * math.pi;
        final r = 86.0 + (i.isEven ? 0 : 0);
        final p = Offset(100 + math.cos(a) * r * 0.92, 108 + math.sin(a) * r);
        if (i == 0) {
          mane.moveTo(p.dx, p.dy);
        } else {
          final prev = (i - 0.5) / n * 2 * math.pi;
          final ctrl = Offset(
            100 + math.cos(prev) * (r + 18) * 0.92,
            108 + math.sin(prev) * (r + 18),
          );
          mane.quadraticBezierTo(ctrl.dx, ctrl.dy, p.dx, p.dy);
        }
      }
      mane.close();
      c.drawPath(
        mane,
        _shade(
          mane.getBounds(),
          const Color(0xFFE39448),
          pal.ear,
          const Color(0xFF9A5418),
        ),
      );
      c.drawPath(mane, _stroke);
    }
    for (final side in [-1, 1]) {
      final e = Offset(100 + side * 44.0, 66 - (ear * side * 4));
      c.drawCircle(e, 18, _fill(pal.base));
      c.drawCircle(e, 18, _stroke);
      c.drawCircle(e, 10, _fill(lion ? pal.dark : pal.ear));
    }
    final head = _headPath(w: 132, h: 118, cy: 112);
    _drawHead(c, head);
    if (!lion) {
      c.save();
      c.clipPath(head);
      final stripe = _fill(const Color(0xFF2B1B1B));
      void tri(double x, double y, double w, double h, double rot) {
        c.save();
        c.translate(x, y);
        c.rotate(rot);
        c.drawPath(
          Path()
            ..moveTo(-w / 2, 0)
            ..quadraticBezierTo(0, h * 0.2, w / 2, 0)
            ..lineTo(0, h)
            ..close(),
          stripe,
        );
        c.restore();
      }

      tri(100, 52, 14, 22, 0);
      tri(84, 54, 9, 16, -0.25);
      tri(116, 54, 9, 16, 0.25);
      for (final side in [-1, 1]) {
        tri(100 + side * 64.0, 104, 10, 26, side * 1.45);
        tri(100 + side * 64.0, 124, 8, 22, side * 1.55);
      }
      c.restore();
    }
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 136),
      width: 70,
      height: 46,
    );
    c.drawOval(muzzle, _fill(pal.muzzle));
    _eyes(c, y: 98, dx: 27, r: 13);
    _brows(c, y: 78);
    _cheeks(c, y: 124, dx: 46);
    _nose(c, y: 126, w: 20, color: const Color(0xFF5E2F2F));
    _mouth(c, y: 141, w: 30);
  }

  void _zebra(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 30, 58);
      c.rotate(side * (0.3 + ear * 0.5));
      final e = Path()
        ..moveTo(-12, 12)
        ..quadraticBezierTo(-6, -22, 0, -34)
        ..quadraticBezierTo(8, -20, 12, 12)
        ..close();
      c.drawPath(e, _fill(Colors.white));
      c.save();
      c.clipPath(e);
      c.drawRect(const Rect.fromLTWH(-14, -36, 28, 14), _fill(pal.ear));
      c.restore();
      c.drawPath(e, _stroke);
      c.restore();
    }
    // striped mohawk mane
    final mane = Path()
      ..moveTo(84, 60)
      ..quadraticBezierTo(86, 26, 100, 30)
      ..quadraticBezierTo(114, 26, 116, 60)
      ..close();
    c.drawPath(mane, _fill(pal.ear));
    c.drawPath(mane, _stroke..strokeWidth = 3);
    final head = _headPath(w: 108, h: 136, cy: 110);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    final stripe = Paint()
      ..color = pal.ear
      ..style = PaintingStyle.stroke
      ..strokeWidth = 7
      ..strokeCap = StrokeCap.round;
    for (var i = 0; i < 4; i++) {
      final y = 52.0 + i * 13;
      c.drawPath(
        Path()
          ..moveTo(84, y)
          ..quadraticBezierTo(100, y + 8, 116, y),
        stripe,
      );
    }
    for (final side in [-1, 1]) {
      for (var i = 0; i < 4; i++) {
        final y = 84.0 + i * 15;
        c.drawPath(
          Path()
            ..moveTo(100 + side * 58.0, y)
            ..quadraticBezierTo(
              100 + side * 46.0,
              y + 6,
              100 + side * 36.0,
              y + 14,
            ),
          stripe,
        );
      }
    }
    c.restore();
    c.drawPath(head, _stroke);
    _eyes(c, y: 94, dx: 24, r: 12);
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 148),
      width: 84,
      height: 52,
    );
    c.drawOval(
      muzzle,
      _shade(muzzle, const Color(0xFF6A6474), pal.muzzle, pal.ear),
    );
    c.drawOval(muzzle, _stroke..strokeWidth = 3);
    for (final side in [-1, 1]) {
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100 + side * 17.0, 140),
          width: 9,
          height: 12,
        ),
        _fill(const Color(0xFF15121A)),
      );
    }
    _cheeks(c, y: 118, dx: 38);
    _mouth(c, y: 156, w: 26);
  }

  void _deer(Canvas c) {
    // antlers
    final antler = Paint()
      ..color = const Color(0xFF7A4A26)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 7
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    for (final side in [-1.0, 1.0]) {
      final p = Path()
        ..moveTo(100 + side * 22, 62)
        ..quadraticBezierTo(100 + side * 30, 36, 100 + side * 48, 22)
        ..moveTo(100 + side * 30, 44)
        ..lineTo(100 + side * 16, 30)
        ..moveTo(100 + side * 40, 30)
        ..lineTo(100 + side * 52, 38);
      c.drawPath(p, _stroke..strokeWidth = 11);
      c.drawPath(p, antler);
    }
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 52, 84);
      c.rotate(side * (0.4 + ear * 0.6));
      final e = Rect.fromCenter(
        center: Offset(side * 16.0, 0),
        width: 40,
        height: 20,
      );
      c.drawOval(e, _fill(pal.base));
      c.drawOval(e.deflate(5), _fill(pal.ear));
      c.drawOval(e, _stroke..strokeWidth = 3);
      c.restore();
    }
    final head = _headPath(w: 112, h: 128, cy: 112);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    final spot = _fill(Colors.white.withValues(alpha: 0.75));
    for (final o in const [
      Offset(84, 62),
      Offset(100, 56),
      Offset(116, 62),
      Offset(92, 70),
      Offset(108, 70),
    ]) {
      c.drawCircle(o, 3.2, spot);
    }
    c.restore();
    final muzzle = Rect.fromCenter(
      center: const Offset(100, 144),
      width: 60,
      height: 42,
    );
    c.drawOval(muzzle, _fill(pal.muzzle));
    _eyes(c, y: 100, dx: 24, r: 14);
    _brows(c, y: 80);
    _cheeks(c, y: 124, dx: 40);
    _nose(c, y: 134, w: 20);
    _mouth(c, y: 150, w: 24);
  }

  void _bird(Canvas c) {
    // head feather tuft
    for (var i = -1; i <= 1; i++) {
      c.save();
      c.translate(100 + i * 8.0, 54);
      c.rotate(i * 0.4 + ear * 0.8);
      final f = Path()
        ..moveTo(-6, 6)
        ..quadraticBezierTo(-6, -18, 2, -26)
        ..quadraticBezierTo(8, -12, 6, 6)
        ..close();
      c.drawPath(f, _fill(pal.dark));
      c.drawPath(f, _stroke..strokeWidth = 2.6);
      c.restore();
    }
    // wings
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 62, 130);
      c.rotate(side * (0.5 + ear * 0.8));
      final w = Path()
        ..moveTo(0, -24)
        ..quadraticBezierTo(side * 22.0, -4, side * 6.0, 26)
        ..quadraticBezierTo(side * -6.0, 0, 0, -24)
        ..close();
      c.drawPath(w, _fill(pal.dark));
      c.drawPath(w, _stroke..strokeWidth = 3);
      c.restore();
    }
    final head = _headPath(w: 132, h: 128, cy: 116);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    c.drawOval(
      Rect.fromCenter(center: const Offset(100, 168), width: 100, height: 80),
      _fill(Colors.white.withValues(alpha: 0.9)),
    );
    c.restore();
    c.drawPath(head, _stroke);
    _eyes(c, y: 100, dx: 26, r: 15);
    _cheeks(c, y: 124, dx: 46);
    final open = mouth.clamp(0.0, 1.0) * 12;
    final beak = _shade(
      const Rect.fromLTWH(80, 110, 40, 40),
      const Color(0xFFFFD36B),
      pal.muzzle,
      const Color(0xFFE08A00),
    );
    if (open > 1) {
      c.drawPath(
        Path()
          ..moveTo(88, 128)
          ..lineTo(112, 128)
          ..lineTo(100, 140 + open)
          ..close(),
        _fill(const Color(0xFF5E1A2B)),
      );
    }
    final lower = Path()
      ..moveTo(88, 130)
      ..lineTo(112, 130)
      ..lineTo(100, 144 + open)
      ..close();
    c.drawPath(lower, beak);
    c.drawPath(lower, _stroke..strokeWidth = 2.8);
    final upper = Path()
      ..moveTo(84, 128 - open * 0.3)
      ..quadraticBezierTo(100, 112, 116, 128 - open * 0.3)
      ..lineTo(100, 140 - open * 0.3)
      ..close();
    c.drawPath(upper, beak);
    c.drawPath(upper, _stroke..strokeWidth = 2.8);
  }

  void _elephant(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 58, 104);
      c.rotate(side * ear * 0.5);
      final e = Rect.fromCenter(
        center: Offset(side * 10.0, 0),
        width: 66,
        height: 84,
      );
      c.drawOval(e, _shade(e, pal.light, pal.base, pal.dark));
      c.drawOval(e.deflate(10), _fill(pal.ear.withValues(alpha: 0.7)));
      c.drawOval(e, _stroke);
      c.restore();
    }
    final head = _headPath(w: 118, h: 120, cy: 106);
    _drawHead(c, head);
    _eyes(c, y: 92, dx: 26, r: 12);
    _brows(c, y: 74);
    _cheeks(c, y: 118, dx: 40);
    // mouth peeks out beside the trunk
    final open = mouth.clamp(0.0, 1.0);
    final mouthCenter = const Offset(124, 146);
    if (open < 0.08) {
      c.drawPath(
        Path()
          ..moveTo(mouthCenter.dx - 8, mouthCenter.dy - 2)
          ..quadraticBezierTo(
            mouthCenter.dx,
            mouthCenter.dy + 6,
            mouthCenter.dx + 10,
            mouthCenter.dy - 3,
          ),
        _stroke,
      );
    } else {
      final m = Rect.fromCenter(
        center: mouthCenter.translate(0, 4 * open),
        width: 20,
        height: 8 + 16 * open,
      );
      c.drawOval(m, _fill(const Color(0xFF5E1A2B)));
      c.drawOval(m, _stroke..strokeWidth = 2.6);
    }
    // trunk (swings up while talking)
    final lift = open * 22;
    final trunk = Path()
      ..moveTo(88, 116)
      ..cubicTo(86, 150, 84, 168, 96 - lift * 0.4, 182 - lift)
      ..quadraticBezierTo(
        106 - lift * 0.2,
        186 - lift,
        110 - lift * 0.1,
        176 - lift,
      )
      ..cubicTo(102, 166, 108, 146, 112, 116)
      ..close();
    c.drawPath(trunk, _shade(trunk.getBounds(), pal.light, pal.base, pal.dark));
    c.drawPath(trunk, _stroke);
    final wrinkle = _stroke
      ..strokeWidth = 2
      ..color = ink.withValues(alpha: 0.5);
    for (final y in [138.0, 150.0, 162.0]) {
      c.drawLine(Offset(90, y), Offset(104, y + 1), wrinkle);
    }
  }

  void _monkey(Canvas c) {
    for (final side in [-1, 1]) {
      final e = Offset(100 + side * 64.0, 104 - ear * side * 4);
      c.drawCircle(e, 22, _fill(pal.base));
      c.drawCircle(e, 22, _stroke);
      c.drawCircle(e, 13, _fill(pal.ear));
    }
    final head = _headPath(w: 132, h: 124, cy: 110);
    _drawHead(c, head);
    // heart-shaped face
    final face = Path()
      ..moveTo(100, 80)
      ..cubicTo(86, 60, 50, 66, 56, 104)
      ..cubicTo(58, 128, 70, 166, 100, 168)
      ..cubicTo(130, 166, 142, 128, 144, 104)
      ..cubicTo(150, 66, 114, 60, 100, 80)
      ..close();
    c.drawPath(
      face,
      _shade(
        face.getBounds(),
        Colors.white,
        pal.muzzle,
        const Color(0xFFDDBA94),
      ),
    );
    c.drawPath(face, _stroke..strokeWidth = 2.6);
    _eyes(c, y: 100, dx: 22, r: 13);
    _cheeks(c, y: 124, dx: 34);
    final nos = _fill(const Color(0xFF6E4527));
    c.drawOval(
      Rect.fromCenter(center: const Offset(94, 126), width: 6, height: 8),
      nos,
    );
    c.drawOval(
      Rect.fromCenter(center: const Offset(106, 126), width: 6, height: 8),
      nos,
    );
    _mouth(c, y: 142, w: 32);
  }

  void _owl(Canvas c) {
    for (final side in [-1, 1]) {
      c.save();
      c.translate(100 + side * 44, 62);
      c.rotate(side * (0.35 + ear * 0.5));
      final t = Path()
        ..moveTo(-12, 12)
        ..lineTo(0, -26)
        ..lineTo(12, 12)
        ..close();
      c.drawPath(t, _fill(pal.dark));
      c.drawPath(t, _stroke);
      c.restore();
    }
    final head = _headPath(w: 140, h: 128, cy: 112);
    _drawHead(c, head);
    c.save();
    c.clipPath(head);
    final feather = _stroke
      ..strokeWidth = 2
      ..color = pal.dark.withValues(alpha: 0.5);
    for (var row = 0; row < 3; row++) {
      for (var i = -2; i <= 2; i++) {
        final x = 100 + i * 16.0 + (row.isOdd ? 8 : 0), y = 150 + row * 10.0;
        c.drawArc(
          Rect.fromCenter(center: Offset(x, y), width: 12, height: 10),
          0,
          math.pi,
          false,
          feather,
        );
      }
    }
    c.restore();
    for (final side in [-1, 1]) {
      final disc = Rect.fromCenter(
        center: Offset(100 + side * 28.0, 102),
        width: 54,
        height: 54,
      );
      c.drawOval(disc, _fill(pal.ear));
      c.drawOval(disc, _stroke..strokeWidth = 2.4);
    }
    _eyes(c, y: 102, dx: 28, r: 17);
    final open = mouth.clamp(0.0, 1.0) * 10;
    final beak = _fill(pal.muzzle);
    if (open > 1) {
      c.drawOval(
        Rect.fromCenter(
          center: Offset(100, 138 + open / 2),
          width: 12,
          height: 6 + open,
        ),
        _fill(const Color(0xFF5E1A2B)),
      );
    }
    final b = Path()
      ..moveTo(92, 122)
      ..quadraticBezierTo(100, 116, 108, 122)
      ..quadraticBezierTo(104, 134, 100, 140 - open * 0.3)
      ..quadraticBezierTo(96, 134, 92, 122)
      ..close();
    c.drawPath(b, beak);
    c.drawPath(b, _stroke..strokeWidth = 2.6);
  }

  @override
  bool shouldRepaint(_AnimalPainter old) =>
      old.mouth != mouth ||
      old.blink != blink ||
      old.ear != ear ||
      old.tilt != tilt ||
      old.bob != bob ||
      old.look != look ||
      old.breath != breath ||
      old.id != id ||
      old.happy != happy;
}
