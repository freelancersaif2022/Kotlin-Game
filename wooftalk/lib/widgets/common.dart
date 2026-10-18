import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';

/// Squishy tap feedback used across the app.
class Pressable extends StatefulWidget {
  const Pressable({
    super.key,
    required this.child,
    this.onTap,
    this.scale = 0.94,
  });

  final Widget child;
  final VoidCallback? onTap;
  final double scale;

  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable> {
  bool _down = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: widget.onTap == null
          ? null
          : (_) => setState(() => _down = true),
      onTapCancel: () => setState(() => _down = false),
      onTapUp: (_) => setState(() => _down = false),
      onTap: widget.onTap == null
          ? null
          : () {
              HapticFeedback.lightImpact();
              widget.onTap!();
            },
      child: AnimatedScale(
        scale: _down ? widget.scale : 1,
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOutBack,
        child: widget.child,
      ),
    );
  }
}

class GradientButton extends StatelessWidget {
  const GradientButton({
    super.key,
    required this.label,
    required this.onTap,
    this.icon,
    this.gradient = AppColors.primaryGradient,
    this.height = 58,
    this.loading = false,
    this.textColor = Colors.white,
  });

  final String label;
  final IconData? icon;
  final VoidCallback? onTap;
  final Gradient gradient;
  final double height;
  final bool loading;
  final Color textColor;

  @override
  Widget build(BuildContext context) {
    final first = gradient.colors.first;
    return Pressable(
      onTap: loading ? null : onTap,
      child: Container(
        height: height,
        decoration: BoxDecoration(
          gradient: gradient,
          borderRadius: BorderRadius.circular(height / 2),
          boxShadow: [
            BoxShadow(
              color: first.withValues(alpha: 0.4),
              blurRadius: 18,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Stack(
          children: [
            // glossy highlight
            Positioned(
              left: 14,
              right: 14,
              top: 4,
              height: height * 0.42,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(height),
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.white.withValues(alpha: 0.35),
                      Colors.white.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
            Center(
              child: loading
                  ? SizedBox.square(
                      dimension: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 3,
                        color: textColor,
                      ),
                    )
                  : Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (icon != null) ...[
                          Icon(icon, color: textColor, size: 22),
                          const SizedBox(width: 8),
                        ],
                        Flexible(
                          child: Text(
                            label,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppTheme.title(18, color: textColor),
                          ),
                        ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Round icon button with a soft card look.
class CircleIconButton extends StatelessWidget {
  const CircleIconButton({
    super.key,
    required this.icon,
    required this.onTap,
    this.size = 46,
    this.color = AppColors.ink,
    this.background = Colors.white,
  });

  final IconData icon;
  final VoidCallback onTap;
  final double size;
  final Color color;
  final Color background;

  @override
  Widget build(BuildContext context) {
    return Pressable(
      onTap: onTap,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: background,
          shape: BoxShape.circle,
          boxShadow: AppTheme.softShadow(),
        ),
        child: Icon(icon, color: color, size: size * 0.48),
      ),
    );
  }
}

/// Animated cartoon background: soft gradient, floating bubbles & paw prints.
class PlayfulBackground extends StatefulWidget {
  const PlayfulBackground({super.key, required this.child, this.colors});

  final Widget child;
  final List<Color>? colors;

  @override
  State<PlayfulBackground> createState() => _PlayfulBackgroundState();
}

class _PlayfulBackgroundState extends State<PlayfulBackground>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 18),
  )..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned.fill(
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors:
                    widget.colors ??
                    const [
                      Color(0xFFFFF1DF),
                      Color(0xFFFFE6EF),
                      Color(0xFFEDE7FF),
                    ],
              ),
            ),
          ),
        ),
        Positioned.fill(
          child: RepaintBoundary(
            child: AnimatedBuilder(
              animation: _c,
              builder: (_, _) =>
                  CustomPaint(painter: _BubblesPainter(_c.value)),
            ),
          ),
        ),
        widget.child,
      ],
    );
  }
}

class _BubblesPainter extends CustomPainter {
  _BubblesPainter(this.t);

  final double t;

  static const _bubbles = [
    // x, y, radius, speed, color
    [0.1, 0.15, 60.0, 1.0, 0xFFFFD08A],
    [0.92, 0.1, 80.0, -1.0, 0xFFC9B8FF],
    [0.85, 0.55, 50.0, 1.0, 0xFFFFB4C8],
    [0.05, 0.7, 70.0, -1.0, 0xFFB8E8FF],
    [0.6, 0.92, 90.0, 1.0, 0xFFFFE08A],
  ];

  @override
  void paint(Canvas canvas, Size size) {
    final a = t * 2 * math.pi;
    for (final b in _bubbles) {
      final dx = math.sin(a * (b[3] as double) + (b[0] as double) * 6) * 18;
      final dy = math.cos(a * (b[3] as double) + (b[1] as double) * 6) * 22;
      final p = Paint()
        ..color = Color(b[4].toInt()).withValues(alpha: 0.35)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 30);
      canvas.drawCircle(
        Offset(
          size.width * (b[0] as double) + dx,
          size.height * (b[1] as double) + dy,
        ),
        b[2] as double,
        p,
      );
    }
    // drifting paw prints
    final paw = Paint()..color = AppColors.purple.withValues(alpha: 0.06);
    for (var i = 0; i < 7; i++) {
      final seed = i * 0.37;
      final x = (seed * 997 % 1) * size.width;
      final y =
          ((seed * 613 % 1) - t * (0.2 + (i % 3) * 0.08)) % 1.0 * size.height;
      _paw(canvas, Offset(x, y), 14 + (i % 3) * 4.0, i * 0.7, paw);
    }
  }

  void _paw(Canvas c, Offset o, double s, double rot, Paint p) {
    c.save();
    c.translate(o.dx, o.dy);
    c.rotate(rot);
    c.drawOval(
      Rect.fromCenter(
        center: Offset(0, s * 0.35),
        width: s * 1.1,
        height: s * 0.9,
      ),
      p,
    );
    for (var i = 0; i < 4; i++) {
      final ang = -math.pi / 2 + (i - 1.5) * 0.55;
      c.drawCircle(
        Offset(math.cos(ang) * s * 0.8, math.sin(ang) * s * 0.75),
        s * 0.24,
        p,
      );
    }
    c.restore();
  }

  @override
  bool shouldRepaint(_BubblesPainter old) => old.t != t;
}

/// Small gradient pill (e.g. "PRO", "BEST VALUE").
class Badge2 extends StatelessWidget {
  const Badge2(
    this.text, {
    super.key,
    this.gradient = AppColors.goldGradient,
    this.icon,
  });

  final String text;
  final Gradient gradient;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        gradient: gradient,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: gradient.colors.last.withValues(alpha: 0.35),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: Colors.white),
            const SizedBox(width: 3),
          ],
          Text(
            text,
            style: AppTheme.label(
              11,
              color: Colors.white,
              weight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}
