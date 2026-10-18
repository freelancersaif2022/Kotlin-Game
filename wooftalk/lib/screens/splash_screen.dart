import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../services/ad_service.dart';
import '../theme/app_theme.dart';
import '../widgets/animal_character.dart';
import 'home_screen.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with TickerProviderStateMixin {
  late final AnimationController _intro = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1600),
  )..forward();
  late final AnimationController _loop = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 900),
  )..repeat();

  @override
  void initState() {
    super.initState();
    AdService.instance.init();
    Future.delayed(const Duration(milliseconds: 2800), _goHome);
  }

  void _goHome() {
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 600),
        pageBuilder: (_, _, _) => const HomeScreen(),
        transitionsBuilder: (_, a, _, child) => FadeTransition(
          opacity: a,
          child: ScaleTransition(
            scale: Tween(
              begin: 1.06,
              end: 1.0,
            ).animate(CurvedAnimation(parent: a, curve: Curves.easeOut)),
            child: child,
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _intro.dispose();
    _loop.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const title = 'WoofTalk';
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFFFA23A), Color(0xFFFF4E7A), Color(0xFF7B5CFF)],
          ),
        ),
        child: AnimatedBuilder(
          animation: Listenable.merge([_intro, _loop]),
          builder: (context, _) {
            final t = _intro.value;
            final hero = Curves.elasticOut.transform(
              ((t - 0.05) / 0.6).clamp(0.0, 1.0),
            );
            return Stack(
              children: [
                // sun-burst rays
                Positioned.fill(
                  child: CustomPaint(painter: _RaysPainter(_loop.value, t)),
                ),
                Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Transform.scale(
                        scale: hero,
                        child: Container(
                          width: 200,
                          height: 200,
                          decoration: BoxDecoration(
                            color: Colors.white,
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.18),
                                blurRadius: 30,
                                offset: const Offset(0, 16),
                              ),
                            ],
                          ),
                          child: const Center(
                            child: AnimalCharacter(
                              voiceId: 'dog',
                              size: 180,
                              shadow: false,
                              mood: CharacterMood.listening,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 28),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          for (var i = 0; i < title.length; i++)
                            Builder(
                              builder: (_) {
                                final start = 0.35 + i * 0.05;
                                final p = Curves.elasticOut.transform(
                                  ((t - start) / 0.5).clamp(0.0, 1.0),
                                );
                                return Transform.translate(
                                  offset: Offset(0, (1 - p) * 40),
                                  child: Opacity(
                                    opacity: p.clamp(0.0, 1.0),
                                    child: Text(
                                      title[i],
                                      style:
                                          AppTheme.title(
                                            48,
                                            color: Colors.white,
                                          ).copyWith(
                                            shadows: [
                                              Shadow(
                                                color: Colors.black.withValues(
                                                  alpha: 0.2,
                                                ),
                                                offset: const Offset(0, 4),
                                                blurRadius: 8,
                                              ),
                                            ],
                                          ),
                                    ),
                                  ),
                                );
                              },
                            ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Opacity(
                        opacity: ((t - 0.7) / 0.3).clamp(0.0, 1.0),
                        child: Text(
                          'Talk like an animal in any language',
                          style: AppTheme.label(
                            16,
                            color: Colors.white.withValues(alpha: 0.92),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Positioned(
                  bottom: 70,
                  left: 0,
                  right: 0,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      for (var i = 0; i < 3; i++)
                        Builder(
                          builder: (_) {
                            final p = math.sin(
                              (_loop.value - i * 0.15) * 2 * math.pi,
                            );
                            return Transform.translate(
                              offset: Offset(0, -p.clamp(0.0, 1.0) * 10),
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6,
                                ),
                                child: Icon(
                                  Icons.pets_rounded,
                                  color: Colors.white.withValues(alpha: 0.9),
                                  size: 22,
                                ),
                              ),
                            );
                          },
                        ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _RaysPainter extends CustomPainter {
  _RaysPainter(this.spin, this.fade);

  final double spin, fade;

  @override
  void paint(Canvas canvas, Size size) {
    final c = Offset(size.width / 2, size.height / 2 - 60);
    final p = Paint()..color = Colors.white.withValues(alpha: 0.07 * fade);
    final r = size.longestSide;
    for (var i = 0; i < 12; i++) {
      final a = i * math.pi / 6 + spin * math.pi / 6;
      final path = Path()
        ..moveTo(c.dx, c.dy)
        ..lineTo(c.dx + math.cos(a - 0.12) * r, c.dy + math.sin(a - 0.12) * r)
        ..lineTo(c.dx + math.cos(a + 0.12) * r, c.dy + math.sin(a + 0.12) * r)
        ..close();
      canvas.drawPath(path, p);
    }
  }

  @override
  bool shouldRepaint(_RaysPainter old) => old.spin != spin || old.fade != fade;
}
