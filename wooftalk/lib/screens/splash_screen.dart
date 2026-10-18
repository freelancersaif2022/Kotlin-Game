import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import '../widgets/app_logo.dart';
import 'home_screen.dart';

/// Lightweight animated splash. Everything is driven by a single controller
/// and only transforms/opacity change per frame, so it stays smooth even
/// while the engine is still warming up.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 2200),
  );

  late final Animation<double> _logo = CurvedAnimation(
    parent: _c,
    curve: const Interval(0, 0.45, curve: Curves.easeOutBack),
  );
  late final Animation<double> _title = CurvedAnimation(
    parent: _c,
    curve: const Interval(0.25, 0.6, curve: Curves.easeOutCubic),
  );
  late final Animation<double> _progress = CurvedAnimation(
    parent: _c,
    curve: const Interval(0.35, 1, curve: Curves.easeInOut),
  );

  @override
  void initState() {
    super.initState();
    // Start after the first frame so the animation never skips its opening.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _c.forward().whenComplete(_goHome);
    });
  }

  void _goHome() {
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 450),
        pageBuilder: (_, _, _) => const HomeScreen(),
        transitionsBuilder: (_, a, _, child) =>
            FadeTransition(opacity: a, child: child),
      ),
    );
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(gradient: LogoColors.tile),
        child: SafeArea(
          child: Column(
            children: [
              const Spacer(flex: 3),
              ScaleTransition(
                scale: _logo,
                child: FadeTransition(
                  opacity: _logo,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(40),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFF2A1466)
                              .withValues(alpha: 0.35),
                          blurRadius: 30,
                          offset: const Offset(0, 14),
                        ),
                      ],
                    ),
                    child: const AppLogo(size: 150, radius: 26),
                  ),
                ),
              ),
              const SizedBox(height: 28),
              FadeTransition(
                opacity: _title,
                child: SlideTransition(
                  position: Tween(
                    begin: const Offset(0, 0.4),
                    end: Offset.zero,
                  ).animate(_title),
                  child: Column(
                    children: [
                      Text(
                        'WoofTalk',
                        style: AppTheme.title(44, color: Colors.white),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Talk like an animal in any language',
                        style: AppTheme.label(
                          15.5,
                          color: Colors.white.withValues(alpha: 0.9),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const Spacer(flex: 3),
              SizedBox(
                width: 140,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: AnimatedBuilder(
                    animation: _progress,
                    builder: (_, _) => LinearProgressIndicator(
                      value: _progress.value,
                      minHeight: 6,
                      color: Colors.white,
                      backgroundColor: Colors.white.withValues(alpha: 0.25),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 56),
            ],
          ),
        ),
      ),
    );
  }
}
