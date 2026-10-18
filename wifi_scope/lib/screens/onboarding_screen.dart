import 'package:flutter/material.dart';

import '../core/theme.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import 'home_shell.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _pager = PageController();
  int _page = 0;

  static const _pages = [
    (
      icon: Icons.wifi_rounded,
      title: 'See your Wi-Fi\nin real time',
      body: 'Live signal strength, link speed, band and channel of the network you are connected to.',
    ),
    (
      icon: Icons.map_rounded,
      title: 'Map where coverage\nis strongest',
      body: 'Walk through your home, tap where you stand, and watch a heatmap of real measurements build up.',
    ),
    (
      icon: Icons.speed_rounded,
      title: 'Test real speed\nanywhere',
      body: 'Measure download, upload and ping, and find the best spot for your router or an extender.',
    ),
  ];

  bool get _isLast => _page == _pages.length;

  Future<void> _finish({bool requestPermission = true}) async {
    if (requestPermission) await WifiService.instance.checkAccess(request: true);
    await Storage.instance.setOnboarded();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 500),
        pageBuilder: (_, _, _) => const HomeShell(),
        transitionsBuilder: (_, a, _, child) => FadeTransition(opacity: a, child: child),
      ),
    );
  }

  void _next() {
    _pager.nextPage(duration: const Duration(milliseconds: 420), curve: Curves.easeOutCubic);
  }

  @override
  void dispose() {
    _pager.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: AnimatedOpacity(
                opacity: _isLast ? 0 : 1,
                duration: const Duration(milliseconds: 200),
                child: TextButton(
                  onPressed: _isLast ? null : () => _pager.jumpToPage(_pages.length),
                  child: const Text('Skip', style: TextStyle(color: AppColors.textMuted)),
                ),
              ),
            ),
            Expanded(
              child: PageView(
                controller: _pager,
                onPageChanged: (p) => setState(() => _page = p),
                children: [
                  for (final p in _pages) _IntroPage(icon: p.icon, title: p.title, body: p.body),
                  const _PermissionPage(),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 20),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(_pages.length + 1, (i) {
                      final active = i == _page;
                      return AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        curve: Curves.easeOutCubic,
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: active ? 26 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: active ? AppColors.primary : AppColors.stroke,
                          borderRadius: BorderRadius.circular(8),
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: _isLast ? _finish : _next,
                      child: Text(_isLast ? 'Allow location access' : 'Continue'),
                    ),
                  ),
                  AnimatedSize(
                    duration: const Duration(milliseconds: 250),
                    child: _isLast
                        ? TextButton(
                            onPressed: () => _finish(requestPermission: false),
                            child: Text('Not now', style: t.bodyMedium?.copyWith(color: AppColors.textMuted)),
                          )
                        : const SizedBox(width: double.infinity),
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

class _IntroPage extends StatelessWidget {
  const _IntroPage({required this.icon, required this.title, required this.body});

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _Hero(icon: icon),
          const SizedBox(height: 48),
          Text(title, textAlign: TextAlign.center, style: t.headlineMedium),
          const SizedBox(height: 16),
          Text(body, textAlign: TextAlign.center, style: t.bodyLarge?.copyWith(color: AppColors.textMuted, height: 1.5)),
        ],
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.icon});
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 200,
      height: 200,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: [AppColors.primary.withValues(alpha: 0.28), AppColors.accent.withValues(alpha: 0.05)],
        ),
      ),
      child: Center(
        child: Container(
          width: 112,
          height: 112,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: AppColors.brandGradient,
            boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.45), blurRadius: 40)],
          ),
          child: Icon(icon, size: 54, color: Colors.white),
        ),
      ),
    );
  }
}

/// Prominent disclosure required by Google Play before asking for location.
class _PermissionPage extends StatelessWidget {
  const _PermissionPage();

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    Widget point(IconData icon, String text) => Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, size: 20, color: AppColors.primary),
              const SizedBox(width: 12),
              Expanded(child: Text(text, style: t.bodyMedium?.copyWith(color: AppColors.textMuted))),
            ],
          ),
        );
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
      child: Column(
        children: [
          const _Hero(icon: Icons.location_on_rounded),
          const SizedBox(height: 36),
          Text('Location access', style: t.headlineMedium),
          const SizedBox(height: 14),
          Text(
            'Android only shares Wi-Fi details (network name, signal strength and nearby networks) '
            'with apps that have location permission.',
            textAlign: TextAlign.center,
            style: t.bodyLarge?.copyWith(color: AppColors.textMuted, height: 1.5),
          ),
          const SizedBox(height: 28),
          point(Icons.wifi_find_rounded, 'Used only to read Wi-Fi information while you use the app.'),
          point(Icons.gps_off_rounded, 'WiFi Scope never tracks or stores your GPS location.'),
          point(Icons.phone_android_rounded, 'Your surveys and test results stay on this device.'),
        ],
      ),
    );
  }
}
