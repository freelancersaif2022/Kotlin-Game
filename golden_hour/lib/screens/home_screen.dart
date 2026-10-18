import 'package:flutter/material.dart';

import '../astro/day_light.dart';
import '../astro/extras.dart';
import '../astro/sun_calc.dart';
import '../services/app_state.dart';
import '../services/location_service.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';
import '../widgets/common.dart';
import '../widgets/day_arc.dart';
import '../widgets/places_sheet.dart';
import '../widgets/sky/sky_view.dart';
import 'settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  DateTime? _scrub;

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final clock = ServicesScope.of(context).clock;
    final place = state.place;

    return ValueListenableBuilder<DateTime>(
      valueListenable: clock,
      builder: (context, now, _) {
        if (place == null) return _NoLocation(now: now);
        final t = _scrub ?? now;
        final sun = sunPosition(t, place.lat, place.lng);
        final moon = moonPosition(t, place.lat, place.lng);
        final illum = moonIllumination(t);
        final day = DayLight.forDate(t, place.lat, place.lng);
        final riseB = day.sunrise == null
            ? null
            : sunPosition(day.sunrise!, place.lat, place.lng).bearing;
        final setB = day.sunset == null
            ? null
            : sunPosition(day.sunset!, place.lat, place.lng).bearing;
        final view = homeViewBearing(
          sun.bearing,
          riseB,
          setB,
          t.isAfter(day.solarNoon),
        );

        return Stack(
          fit: StackFit.expand,
          children: [
            SkyView(
              sunBearing: sun.bearing,
              sunAltitude: sun.altitude,
              viewBearing: view,
              moon: SkyMoon(
                bearing: moon.bearing,
                altitude: moon.altitude,
                phase: illum.phase,
                tilt: place.lat < 0 ? 3.14159 : 0,
              ),
              horizon: 0.5,
              hFov: 100,
            ),
            // Legibility scrim for the content below the horizon.
            const IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color(0x55000000),
                      Color(0x00000000),
                      Color(0x00000000),
                      Color(0xCC05060B),
                    ],
                    stops: [0, 0.16, 0.5, 0.78],
                  ),
                ),
              ),
            ),
            SafeArea(
              bottom: false,
              child: CustomScrollView(
                physics: const BouncingScrollPhysics(),
                slivers: [
                  SliverToBoxAdapter(child: _TopBar(place: place)),
                  SliverToBoxAdapter(
                    child: _Hero(
                      now: now,
                      scrub: _scrub,
                      place: place,
                      sunAlt: sun.altitude,
                    ),
                  ),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 120),
                    sliver: SliverList.list(
                      children: [
                        _ArcCard(
                          day: day,
                          time: t,
                          live: _scrub == null,
                          onScrub: (v) => setState(() => _scrub = v),
                          onLive: () => setState(() => _scrub = null),
                        ),
                        const SizedBox(height: 14),
                        _Windows(day: day, now: now),
                        const SectionLabel('Sun right now'),
                        _SunNow(
                          day: day,
                          alt: sun.altitude,
                          bearing: sun.bearing,
                        ),
                        const SectionLabel('Photo settings'),
                        _PhotoTips(alt: sun.altitude),
                        const SectionLabel('Timeline'),
                        _Timeline(day: day, now: now),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({required this.place});
  final Place place;

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 12, 0),
      child: Row(
        children: [
          Expanded(
            child: Align(
              alignment: Alignment.centerLeft,
              child: Pressable(
                onTap: () => showPlacesSheet(context),
                child: Glass(
                  radius: 18,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        place.isGps
                            ? Icons.near_me_rounded
                            : Icons.place_rounded,
                        size: 16,
                        color: GH.goldLight,
                      ),
                      const SizedBox(width: 8),
                      Flexible(
                        child: Text(
                          place.name,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontWeight: FontWeight.w600,
                            fontSize: 14,
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(
                        Icons.expand_more_rounded,
                        size: 18,
                        color: GH.textMid,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          if (!state.isPro)
            Pressable(
              onTap: () => openPaywall(context),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  gradient: GH.goldGradient,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Row(
                  children: [
                    Icon(
                      Icons.workspace_premium_rounded,
                      size: 16,
                      color: GH.ink,
                    ),
                    SizedBox(width: 4),
                    Text(
                      'PRO',
                      style: TextStyle(
                        color: GH.ink,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          IconButton(
            onPressed: () => Navigator.of(
              context,
            ).push(MaterialPageRoute(builder: (_) => const SettingsScreen())),
            icon: const Icon(Icons.tune_rounded, color: GH.textHi),
          ),
        ],
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({
    required this.now,
    required this.scrub,
    required this.place,
    required this.sunAlt,
  });
  final DateTime now;
  final DateTime? scrub;
  final Place place;
  final double sunAlt;

  @override
  Widget build(BuildContext context) {
    final h = MediaQuery.sizeOf(context).height;
    final status = LightStatus.at(now, place.lat, place.lng);
    final phase = LightPhase.fromAltitude(sunAlt);
    final color = switch (phase) {
      LightPhase.goldenHour => GH.gold,
      LightPhase.blueHour => GH.blue,
      LightPhase.day => GH.goldLight,
      _ => const Color(0xFFB9C6FF),
    };

    final String headline;
    final String big;
    final String sub;
    if (scrub != null) {
      headline = 'Preview';
      big = fmtTime(context, scrub);
      sub = '${phase.label} · sun ${sunAlt.toStringAsFixed(1)}°';
    } else if (status.target != null) {
      headline = status.headline;
      big = fmtCountdown(status.target!.difference(now));
      final verb = status.activeWindow != null ? 'Ends' : 'Starts';
      sub = '$verb at ${fmtTime(context, status.target)}';
    } else {
      headline = status.headline;
      big = fmtTime(context, now);
      sub = phase.label;
    }

    return SizedBox(
      height: h * 0.40,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _PhaseChip(label: phase.label, color: color),
          const SizedBox(height: 16),
          Text(
            headline,
            style: GH.body.copyWith(
              color: GH.textHi.withValues(alpha: 0.85),
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 6),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 250),
            child: FittedBox(
              key: ValueKey(scrub == null),
              fit: BoxFit.scaleDown,
              child: Text(
                big,
                style: GH.display.copyWith(
                  fontSize: 76,
                  shadows: [
                    Shadow(
                      color: Colors.black.withValues(alpha: 0.35),
                      blurRadius: 24,
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            sub,
            style: GH.body.copyWith(color: GH.textHi.withValues(alpha: 0.8)),
          ),
        ],
      ),
    );
  }
}

class _PhaseChip extends StatefulWidget {
  const _PhaseChip({required this.label, required this.color});
  final String label;
  final Color color;

  @override
  State<_PhaseChip> createState() => _PhaseChipState();
}

class _PhaseChipState extends State<_PhaseChip>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1800),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Glass(
      radius: 30,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedBuilder(
            animation: _pulse,
            builder: (_, _) => Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: widget.color,
                boxShadow: [
                  BoxShadow(
                    color: widget.color.withValues(
                      alpha: 0.4 + 0.5 * _pulse.value,
                    ),
                    blurRadius: 6 + 8 * _pulse.value,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            widget.label.toUpperCase(),
            style: GH.label.copyWith(color: GH.textHi, fontSize: 11),
          ),
        ],
      ),
    );
  }
}

class _ArcCard extends StatelessWidget {
  const _ArcCard({
    required this.day,
    required this.time,
    required this.live,
    required this.onScrub,
    required this.onLive,
  });
  final DayLight day;
  final DateTime time;
  final bool live;
  final ValueChanged<DateTime> onScrub;
  final VoidCallback onLive;

  @override
  Widget build(BuildContext context) {
    return Glass(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text("TODAY'S LIGHT", style: GH.label),
              const Spacer(),
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 250),
                child: live
                    ? Text(
                        'Drag to preview',
                        key: const ValueKey('h'),
                        style: GH.label.copyWith(
                          fontSize: 9.5,
                          color: GH.textLo,
                        ),
                      )
                    : Pressable(
                        key: const ValueKey('l'),
                        onTap: onLive,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: GH.gold,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Text(
                            '● LIVE',
                            style: TextStyle(
                              color: GH.ink,
                              fontSize: 10,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1,
                            ),
                          ),
                        ),
                      ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          DayArc(day: day, time: time, onScrub: onScrub),
        ],
      ),
    );
  }
}

class _Windows extends StatelessWidget {
  const _Windows({required this.day, required this.now});
  final DayLight day;
  final DateTime now;

  @override
  Widget build(BuildContext context) {
    final tiles = [
      (
        'Morning',
        'Blue Hour',
        day.blueHourMorning,
        GH.blue,
        Icons.nights_stay_rounded,
      ),
      (
        'Morning',
        'Golden Hour',
        day.goldenHourMorning,
        GH.gold,
        Icons.wb_twilight_rounded,
      ),
      (
        'Evening',
        'Golden Hour',
        day.goldenHourEvening,
        GH.amber,
        Icons.wb_sunny_rounded,
      ),
      (
        'Evening',
        'Blue Hour',
        day.blueHourEvening,
        GH.blueDeep,
        Icons.location_city_rounded,
      ),
    ];
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      padding: EdgeInsets.zero,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      childAspectRatio: 1.32,
      children: [
        for (final (part, name, w, color, icon) in tiles)
          _WindowTile(
            when: part,
            name: name,
            window: w,
            color: color,
            icon: icon,
            now: now,
          ),
      ],
    );
  }
}

class _WindowTile extends StatelessWidget {
  const _WindowTile({
    required this.when,
    required this.name,
    required this.window,
    required this.color,
    required this.icon,
    required this.now,
  });
  final String when;
  final String name;
  final LightWindow? window;
  final Color color;
  final IconData icon;
  final DateTime now;

  @override
  Widget build(BuildContext context) {
    final active = window?.contains(now) ?? false;
    final past = window != null && window!.end.isBefore(now);
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 400),
      opacity: past ? 0.55 : 1,
      child: Glass(
        padding: const EdgeInsets.all(14),
        tint: active ? color : null,
        opacity: active ? 0.22 : 0.10,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, size: 16, color: color),
                ),
                const Spacer(),
                if (active)
                  Text(
                    'NOW',
                    style: GH.label.copyWith(color: color, fontSize: 10),
                  )
                else if (window != null)
                  Text(
                    fmtDuration(window!.length),
                    style: GH.label.copyWith(fontSize: 10, letterSpacing: 0.5),
                  ),
              ],
            ),
            const Spacer(),
            Text(
              '$when ${name.split(' ').first}'.toUpperCase(),
              style: GH.label.copyWith(fontSize: 9.5),
            ),
            const SizedBox(height: 4),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(
                window == null
                    ? 'Not today'
                    : '${fmtTime(context, window!.start)} – ${fmtTime(context, window!.end)}',
                style: GH.value.copyWith(fontSize: 16.5),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SunNow extends StatelessWidget {
  const _SunNow({required this.day, required this.alt, required this.bearing});
  final DayLight day;
  final double alt;
  final double bearing;

  @override
  Widget build(BuildContext context) {
    return Glass(
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Stat(
                  label: 'Altitude',
                  value: '${alt.toStringAsFixed(1)}°',
                  icon: Icons.height_rounded,
                ),
              ),
              Expanded(
                child: Stat(
                  label: 'Direction',
                  value: fmtBearing(bearing),
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
                  label: 'Day length',
                  value: fmtDuration(day.dayLength),
                  icon: Icons.timelapse_rounded,
                ),
              ),
              Expanded(
                child: Stat(
                  label: 'Solar noon',
                  value: fmtTime(context, day.solarNoon),
                  icon: Icons.light_mode_outlined,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _PhotoTips extends StatelessWidget {
  const _PhotoTips({required this.alt});
  final double alt;

  @override
  Widget build(BuildContext context) {
    final tip = photoTipFor(LightPhase.fromAltitude(alt), alt);
    return Column(
      children: [
        Glass(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(
                Icons.tips_and_updates_rounded,
                color: GH.goldLight,
                size: 20,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  tip.advice,
                  style: GH.body.copyWith(color: GH.textHi),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        FeatureGate(
          title: 'Pro camera settings',
          description:
              'ISO, shutter, aperture & white balance for this exact light',
          minHeight: 120,
          child: Glass(
            child: Row(
              children: [
                Expanded(
                  child: Stat(label: 'ISO', value: tip.iso),
                ),
                Expanded(
                  child: Stat(label: 'Shutter', value: tip.shutter),
                ),
                Expanded(
                  child: Stat(label: 'Aperture', value: tip.aperture),
                ),
                Expanded(
                  child: Stat(label: 'WB', value: tip.whiteBalance),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _Timeline extends StatelessWidget {
  const _Timeline({required this.day, required this.now});
  final DayLight day;
  final DateTime now;

  Color _color(DayEventKind k) => switch (k) {
    DayEventKind.goldenHourMorning || DayEventKind.goldenHourEvening => GH.gold,
    DayEventKind.sunrise || DayEventKind.sunset => GH.amber,
    DayEventKind.blueHourMorning || DayEventKind.blueHourEvening => GH.blue,
    DayEventKind.solarNoon => GH.goldLight,
    _ => const Color(0xFF8790B8),
  };

  @override
  Widget build(BuildContext context) {
    final events = day.events;
    final nextIndex = events.indexWhere((e) => e.time.isAfter(now));
    return Glass(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      child: Column(
        children: [
          for (var i = 0; i < events.length; i++)
            Opacity(
              opacity: events[i].time.isBefore(now) ? 0.45 : 1,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: Row(
                  children: [
                    Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: _color(events[i].kind),
                        boxShadow: i == nextIndex
                            ? [
                                BoxShadow(
                                  color: _color(events[i].kind),
                                  blurRadius: 10,
                                ),
                              ]
                            : null,
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            events[i].title,
                            style: const TextStyle(
                              fontWeight: FontWeight.w600,
                              fontSize: 15,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            events[i].subtitle,
                            style: GH.body.copyWith(fontSize: 12.5),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      fmtTime(context, events[i].time),
                      style: GH.value.copyWith(fontSize: 16),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _NoLocation extends StatefulWidget {
  const _NoLocation({required this.now});
  final DateTime now;

  @override
  State<_NoLocation> createState() => _NoLocationState();
}

class _NoLocationState extends State<_NoLocation> {
  bool _busy = false;
  String? _msg;

  Future<void> _locate() async {
    setState(() => _busy = true);
    final s = ServicesScope.of(context);
    final (res, place) = await LocationService.current();
    if (place != null) {
      await s.state.setGpsPlace(place);
      await s.notifications.reschedule();
    }
    if (!mounted) return;
    setState(() {
      _busy = false;
      _msg = switch (res) {
        LocationResult.ok => null,
        LocationResult.serviceOff =>
          'Turn on location (GPS) in your phone settings.',
        LocationResult.denied =>
          'Location permission is needed to calculate your light.',
        LocationResult.deniedForever =>
          'Location is blocked. Enable it in app settings.',
        LocationResult.error =>
          'Could not get a GPS fix. Try outdoors or add a place manually.',
      };
    });
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        const SkyView(
          sunBearing: 255,
          sunAltitude: 2.5,
          viewBearing: 255,
          horizon: 0.55,
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Glass(
                  padding: const EdgeInsets.all(22),
                  child: Column(
                    children: [
                      const Icon(
                        Icons.near_me_rounded,
                        color: GH.goldLight,
                        size: 34,
                      ),
                      const SizedBox(height: 12),
                      const Text('Where are you shooting?', style: GH.title),
                      const SizedBox(height: 8),
                      Text(
                        _msg ?? 'Your location stays on this phone. It is only used to calculate the light — no internet needed.',
                        style: GH.body,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 20),
                      GoldButton(
                        label: 'Use my location',
                        icon: Icons.my_location_rounded,
                        busy: _busy,
                        onTap: _locate,
                      ),
                      const SizedBox(height: 10),
                      TextButton(
                        onPressed: () => showPlacesSheet(context),
                        child: const Text(
                          'Add a place manually',
                          style: TextStyle(color: GH.textHi),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 90),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
