import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../services/location_service.dart';
import '../services/services.dart';
import '../theme.dart';
import '../widgets/common.dart';
import '../widgets/sky/sky_view.dart';

/// Three cinematic pages: the sky animates from blue hour to golden hour to
/// sunset as the user swipes, then location + notification permissions.
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key, required this.onDone});
  final Future<void> Function() onDone;

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _pages = PageController();
  double _page = 0;
  bool _busy = false;

  static const _content = [
    (
      'Chase the perfect light',
      'Golden hour, blue hour, sunrise and sunset — calculated to the minute, fully offline.',
    ),
    (
      'See the sun before it moves',
      'Point your camera at the sky and watch the sun\'s exact path in augmented reality.',
    ),
    (
      'Never miss it again',
      'Smart alerts tell you when the magic light is coming. Your location never leaves your phone.',
    ),
  ];

  @override
  void initState() {
    super.initState();
    _pages.addListener(() => setState(() => _page = _pages.page ?? 0));
  }

  @override
  void dispose() {
    _pages.dispose();
    super.dispose();
  }

  Future<void> _finish() async {
    setState(() => _busy = true);
    final s = ServicesScope.of(context);
    final (_, place) = await LocationService.current();
    if (place != null) await s.state.setGpsPlace(place);
    await s.notifications.requestPermission();
    await widget.onDone();
  }

  @override
  Widget build(BuildContext context) {
    // Sun altitude follows the swipe: blue hour -> golden hour -> sunset glow.
    final alt = -5.0 + _page * 4.2;
    final last = _page > 1.5;
    return Scaffold(
      backgroundColor: GH.ink,
      body: Stack(
        fit: StackFit.expand,
        children: [
          SkyView(
            sunBearing: 250,
            sunAltitude: alt,
            viewBearing: 250 + math.sin(_page) * 6,
            horizon: 0.56,
          ),
          const DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Color(0x00000000),
                  Color(0x00000000),
                  Color(0xE605060B),
                ],
                stops: [0, 0.5, 0.85],
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                Expanded(
                  child: PageView.builder(
                    controller: _pages,
                    itemCount: _content.length,
                    itemBuilder: (_, i) {
                      final d = (_page - i).abs().clamp(0.0, 1.0);
                      return Opacity(
                        opacity: 1 - d,
                        child: Transform.translate(
                          offset: Offset(0, 30 * d),
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(28, 0, 28, 30),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                Text(
                                  _content[i].$1,
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(
                                    fontSize: 34,
                                    fontWeight: FontWeight.w300,
                                    letterSpacing: -1,
                                    height: 1.1,
                                  ),
                                ),
                                const SizedBox(height: 14),
                                Text(
                                  _content[i].$2,
                                  textAlign: TextAlign.center,
                                  style: GH.body.copyWith(fontSize: 16),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < _content.length; i++)
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: (_page.round() == i) ? 26 : 7,
                        height: 7,
                        decoration: BoxDecoration(
                          color: _page.round() == i ? GH.gold : Colors.white24,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
                  child: GoldButton(
                    label: last ? 'Use my location' : 'Continue',
                    icon: last ? Icons.my_location_rounded : null,
                    busy: _busy,
                    onTap: last
                        ? _finish
                        : () => _pages.nextPage(
                            duration: const Duration(milliseconds: 700),
                            curve: Curves.easeInOutCubic,
                          ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
