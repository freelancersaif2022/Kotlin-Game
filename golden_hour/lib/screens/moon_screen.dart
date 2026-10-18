import 'package:flutter/material.dart';

import '../astro/sun_calc.dart';
import '../services/app_state.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';
import '../widgets/common.dart';
import '../widgets/moon_disc.dart';

class MoonScreen extends StatelessWidget {
  const MoonScreen({super.key});

  /// Next time the phase crosses [target] (0 = new, 0.5 = full).
  static DateTime nextPhase(DateTime from, double target) {
    double diff(DateTime t) {
      var d = moonIllumination(t).phase - target;
      if (d < -0.5) d += 1;
      if (d > 0.5) d -= 1;
      return d;
    }

    var t = from;
    var prev = diff(t);
    for (var h = 0; h < 24 * 31; h += 6) {
      final n = from.add(Duration(hours: h + 6));
      final d = diff(n);
      if (prev < 0 && d >= 0) {
        // refine
        var lo = t, hi = n;
        for (var i = 0; i < 16; i++) {
          final mid = lo.add(
            Duration(milliseconds: hi.difference(lo).inMilliseconds ~/ 2),
          );
          if (diff(mid) < 0) {
            lo = mid;
          } else {
            hi = mid;
          }
        }
        return hi;
      }
      prev = d;
      t = n;
    }
    return from;
  }

  @override
  Widget build(BuildContext context) {
    final place = AppScope.of(context).place;
    final now = ServicesScope.of(context).clock.value;
    final illum = moonIllumination(now);
    final southern = (place?.lat ?? 0) < 0;
    final pos = place == null ? null : moonPosition(now, place.lat, place.lng);
    final times = place == null ? null : moonTimes(now, place.lat, place.lng);
    final full = nextPhase(now, 0.5);
    final newMoon = nextPhase(now, 0.0);
    final age = illum.phase * 29.53;

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: RadialGradient(
          center: Alignment(0, -0.45),
          radius: 1.1,
          colors: [Color(0xFF1B2140), Color(0xFF0A0C18), Color(0xFF040509)],
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 120),
          children: [
            const Text('Moon', style: GH.title),
            const SizedBox(height: 24),
            Center(
              child: MoonDisc(
                phase: illum.phase,
                size: 230,
                southern: southern,
              ),
            ),
            const SizedBox(height: 26),
            Center(
              child: Text(
                illum.phaseName,
                style: GH.title.copyWith(
                  fontSize: 28,
                  fontWeight: FontWeight.w300,
                ),
              ),
            ),
            const SizedBox(height: 6),
            Center(
              child: Text(
                '${(illum.fraction * 100).toStringAsFixed(0)}% illuminated · ${age.toStringAsFixed(1)} days old',
                style: GH.body,
              ),
            ),
            const SizedBox(height: 26),
            Glass(
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Stat(
                          label: 'Moonrise',
                          value: fmtTime(context, times?.rise),
                          icon: Icons.arrow_upward_rounded,
                        ),
                      ),
                      Expanded(
                        child: Stat(
                          label: 'Moonset',
                          value: fmtTime(context, times?.set),
                          icon: Icons.arrow_downward_rounded,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  Row(
                    children: [
                      Expanded(
                        child: Stat(
                          label: 'Altitude',
                          value: pos == null
                              ? '—'
                              : '${pos.altitude.toStringAsFixed(1)}°',
                          icon: Icons.height_rounded,
                        ),
                      ),
                      Expanded(
                        child: Stat(
                          label: 'Direction',
                          value: pos == null ? '—' : fmtBearing(pos.bearing),
                          icon: Icons.explore_outlined,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  Row(
                    children: [
                      Expanded(
                        child: Stat(
                          label: 'Distance',
                          value: pos == null
                              ? '—'
                              : '${(pos.distanceKm! / 1000).toStringAsFixed(0)}k km',
                          icon: Icons.straighten_rounded,
                        ),
                      ),
                      const Expanded(child: SizedBox()),
                    ],
                  ),
                ],
              ),
            ),
            const SectionLabel('Coming up'),
            Row(
              children: [
                Expanded(
                  child: _PhaseCard(
                    name: 'Full Moon',
                    date: full,
                    phase: 0.5,
                    southern: southern,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _PhaseCard(
                    name: 'New Moon',
                    date: newMoon,
                    phase: 0.0,
                    southern: southern,
                  ),
                ),
              ],
            ),
            const SectionLabel('Next 30 nights'),
            Glass(
              padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
              child: SizedBox(
                height: 86,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: 30,
                  itemBuilder: (_, i) {
                    final d = DateTime(now.year, now.month, now.day + i, 21);
                    final p = moonIllumination(d).phase;
                    return SizedBox(
                      width: 52,
                      child: Column(
                        children: [
                          Text(
                            i == 0 ? 'Tonight' : weekday(d),
                            style: GH.label.copyWith(
                              fontSize: 8.5,
                              letterSpacing: 0.5,
                            ),
                          ),
                          const SizedBox(height: 8),
                          MoonDisc(
                            phase: p,
                            size: 34,
                            southern: southern,
                            glow: false,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '${d.day} ${month(d)}',
                            style: GH.body.copyWith(fontSize: 10.5),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PhaseCard extends StatelessWidget {
  const _PhaseCard({
    required this.name,
    required this.date,
    required this.phase,
    required this.southern,
  });
  final String name;
  final DateTime date;
  final double phase;
  final bool southern;

  @override
  Widget build(BuildContext context) {
    final days = date.difference(DateTime.now()).inHours / 24;
    return Glass(
      child: Row(
        children: [
          MoonDisc(
            phase: phase == 0 ? 0.02 : phase,
            size: 40,
            southern: southern,
            glow: false,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 2),
                Text(fmtDate(date), style: GH.body.copyWith(fontSize: 12.5)),
                Text(
                  'in ${days.ceil()} days',
                  style: GH.label.copyWith(fontSize: 9.5, color: GH.goldLight),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
