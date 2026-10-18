import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../astro/day_light.dart';
import '../astro/extras.dart';
import '../astro/sun_calc.dart';
import '../services/app_state.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';
import '../widgets/common.dart';
import '../widgets/moon_disc.dart';
import '../widgets/places_sheet.dart';

class PlannerScreen extends StatefulWidget {
  const PlannerScreen({super.key});

  @override
  State<PlannerScreen> createState() => _PlannerScreenState();
}

class _PlannerScreenState extends State<PlannerScreen> {
  DateTime? _date;

  Future<void> _select(DateTime d) async {
    final today = DateTime.now();
    if (!sameDay(d, today) &&
        !await ensureAccess(
          context,
          reward: RewardFeature.anyDate,
          title: 'Plan any date',
        )) {
      return;
    }
    setState(() => _date = d);
  }

  Future<void> _pick() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date ?? now,
      firstDate: DateTime(now.year - 2),
      lastDate: DateTime(now.year + 5),
    );
    if (picked != null) await _select(picked);
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final place = state.place;
    final now = ServicesScope.of(context).clock.value;
    final date = _date ?? now;

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF1A1424), Color(0xFF0B0B14), Color(0xFF06060B)],
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: place == null
            ? const Center(child: Text('Set a location first', style: GH.body))
            : ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 120),
                children: [
                  Row(
                    children: [
                      const Text('Planner', style: GH.title),
                      const Spacer(),
                      Pressable(
                        onTap: () => showPlacesSheet(context),
                        child: Glass(
                          radius: 14,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 8,
                          ),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.place_rounded,
                                size: 15,
                                color: GH.goldLight,
                              ),
                              const SizedBox(width: 6),
                              ConstrainedBox(
                                constraints: const BoxConstraints(
                                  maxWidth: 140,
                                ),
                                child: Text(
                                  place.name,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  _DateStrip(
                    selected: date,
                    unlocked: state.hasAccess(RewardFeature.anyDate),
                    onSelect: _select,
                    onPick: _pick,
                  ),
                  const SizedBox(height: 6),
                  Padding(
                    padding: const EdgeInsets.only(left: 4, top: 8),
                    child: Text(
                      fmtDateLong(date),
                      style: GH.body.copyWith(color: GH.textHi),
                    ),
                  ),
                  _DaySchedule(place: place, date: date),
                  const SectionLabel('Shadow calculator'),
                  FeatureGate(
                    reward: RewardFeature.shadowCalculator,
                    title: 'Shadow calculator',
                    description: 'Shadow length and direction at any time',
                    minHeight: 260,
                    child: _ShadowCalculator(place: place, date: date),
                  ),
                  const SectionLabel(
                    'Sun alignment finder',
                    trailing: ProBadge(small: true),
                  ),
                  FeatureGate(
                    title: 'Find sun alignments',
                    description: 'Dates when the sun sets exactly along a street or landmark',
                    minHeight: 220,
                    child: _AlignmentFinder(place: place),
                  ),
                  const SectionLabel(
                    'Milky Way',
                    trailing: ProBadge(small: true),
                  ),
                  FeatureGate(
                    title: 'Milky Way finder',
                    description: 'When and where the galactic core is visible',
                    minHeight: 200,
                    child: _MilkyWay(place: place, date: date),
                  ),
                ],
              ),
      ),
    );
  }
}

class _DateStrip extends StatelessWidget {
  const _DateStrip({
    required this.selected,
    required this.unlocked,
    required this.onSelect,
    required this.onPick,
  });
  final DateTime selected;
  final bool unlocked;
  final ValueChanged<DateTime> onSelect;
  final VoidCallback onPick;

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    return SizedBox(
      height: 78,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: 15,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, i) {
          if (i == 14) {
            return Pressable(
              onTap: onPick,
              child: Container(
                width: 58,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: Colors.white24),
                ),
                child: const Icon(
                  Icons.calendar_month_rounded,
                  color: GH.goldLight,
                ),
              ),
            );
          }
          final d = DateTime(today.year, today.month, today.day + i);
          final sel = sameDay(d, selected);
          return Pressable(
            onTap: () => onSelect(d),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 260),
              width: 58,
              decoration: BoxDecoration(
                gradient: sel ? GH.goldGradient : null,
                color: sel ? null : Colors.white.withValues(alpha: 0.06),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(
                  color: sel ? Colors.transparent : Colors.white12,
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    i == 0 ? 'TODAY' : weekday(d).toUpperCase(),
                    style: GH.label.copyWith(
                      fontSize: 9,
                      color: sel ? GH.ink : GH.textLo,
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${d.day}',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w600,
                      color: sel ? GH.ink : GH.textHi,
                      fontFeatures: GH.tabular,
                    ),
                  ),
                  if (i > 0 && !unlocked)
                    Icon(
                      Icons.lock_rounded,
                      size: 10,
                      color: sel ? GH.ink : GH.textLo,
                    )
                  else
                    const SizedBox(height: 10),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _DaySchedule extends StatelessWidget {
  const _DaySchedule({required this.place, required this.date});
  final Place place;
  final DateTime date;

  @override
  Widget build(BuildContext context) {
    final day = DayLight.forDate(date, place.lat, place.lng);
    final noonMoon = DateTime(date.year, date.month, date.day, 21);
    final illum = moonIllumination(noonMoon);
    final mt = moonTimes(date, place.lat, place.lng);

    Widget row(String name, LightWindow? w, Color c, IconData icon) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 9),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: c.withValues(alpha: 0.18),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, size: 18, color: c),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
                Text(
                  w == null ? 'Not on this day' : fmtDuration(w.length),
                  style: GH.body.copyWith(fontSize: 12.5),
                ),
              ],
            ),
          ),
          Text(
            w == null
                ? '—'
                : '${fmtTime(context, w.start)} – ${fmtTime(context, w.end)}',
            style: GH.value.copyWith(fontSize: 15.5),
          ),
        ],
      ),
    );

    return Column(
      children: [
        const SizedBox(height: 12),
        Glass(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Column(
            children: [
              row(
                'Morning blue hour',
                day.blueHourMorning,
                GH.blue,
                Icons.nights_stay_rounded,
              ),
              row(
                'Morning golden hour',
                day.goldenHourMorning,
                GH.gold,
                Icons.wb_twilight_rounded,
              ),
              row(
                'Evening golden hour',
                day.goldenHourEvening,
                GH.amber,
                Icons.wb_sunny_rounded,
              ),
              row(
                'Evening blue hour',
                day.blueHourEvening,
                GH.blueDeep,
                Icons.location_city_rounded,
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Glass(
          child: Row(
            children: [
              Expanded(
                child: Stat(
                  label: 'Sunrise',
                  value: fmtTime(context, day.sunrise),
                  icon: Icons.arrow_upward_rounded,
                  color: GH.gold,
                ),
              ),
              Expanded(
                child: Stat(
                  label: 'Noon',
                  value: fmtTime(context, day.solarNoon),
                  icon: Icons.light_mode_outlined,
                ),
              ),
              Expanded(
                child: Stat(
                  label: 'Sunset',
                  value: fmtTime(context, day.sunset),
                  icon: Icons.arrow_downward_rounded,
                  color: GH.rose,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Glass(
          child: Row(
            children: [
              MoonDisc(phase: illum.phase, size: 54, southern: place.lat < 0),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      illum.phaseName,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${(illum.fraction * 100).round()}% lit',
                      style: GH.body.copyWith(fontSize: 13),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    '↑ ${fmtTime(context, mt.rise)}',
                    style: GH.value.copyWith(fontSize: 14),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '↓ ${fmtTime(context, mt.set)}',
                    style: GH.value.copyWith(fontSize: 14),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ShadowCalculator extends StatefulWidget {
  const _ShadowCalculator({required this.place, required this.date});
  final Place place;
  final DateTime date;

  @override
  State<_ShadowCalculator> createState() => _ShadowCalculatorState();
}

class _ShadowCalculatorState extends State<_ShadowCalculator> {
  double _height = 1.8;
  double _minutes = 17 * 60;

  @override
  Widget build(BuildContext context) {
    final d = widget.date;
    final t = DateTime(
      d.year,
      d.month,
      d.day,
    ).add(Duration(minutes: _minutes.round()));
    final s = shadowAt(t, widget.place.lat, widget.place.lng, _height);
    return Glass(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              SizedBox(
                width: 92,
                height: 92,
                child: CustomPaint(painter: _ShadowPainter(s)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Shadow length'.toUpperCase(),
                      style: GH.label.copyWith(fontSize: 10),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      s.length == null
                          ? 'No shadow (sun down)'
                          : '${s.length!.toStringAsFixed(s.length! > 100 ? 0 : 1)} m',
                      style: GH.value.copyWith(fontSize: 24),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Points ${fmtBearing(s.bearing)} · sun ${s.sunAltitude.toStringAsFixed(1)}°',
                      style: GH.body.copyWith(fontSize: 13),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            'Object height · ${_height.toStringAsFixed(1)} m',
            style: GH.label.copyWith(fontSize: 10),
          ),
          Slider(
            value: _height,
            min: 0.5,
            max: 50,
            onChanged: (v) => setState(() => _height = v),
          ),
          Text(
            'Time · ${fmtTime(context, t)}',
            style: GH.label.copyWith(fontSize: 10),
          ),
          Slider(
            value: _minutes,
            min: 0,
            max: 1439,
            onChanged: (v) => setState(() => _minutes = v),
          ),
        ],
      ),
    );
  }
}

class _ShadowPainter extends CustomPainter {
  _ShadowPainter(this.s);
  final ShadowInfo s;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2;
    canvas.drawCircle(
      c,
      r,
      Paint()..color = Colors.white.withValues(alpha: 0.05),
    );
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..style = PaintingStyle.stroke
        ..color = Colors.white12,
    );
    final tp = TextPainter(
      text: const TextSpan(
        text: 'N',
        style: TextStyle(
          color: GH.rose,
          fontSize: 10,
          fontWeight: FontWeight.w700,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, Offset(c.dx - tp.width / 2, 2));
    if (s.length == null) return;
    final a = (s.bearing - 90) * math.pi / 180;
    final len = (r - 6) * (1 - math.exp(-s.length! / 6)).clamp(0.15, 1.0);
    final end = c + Offset(math.cos(a), math.sin(a)) * len;
    canvas.drawLine(
      c,
      end,
      Paint()
        ..strokeWidth = 7
        ..strokeCap = StrokeCap.round
        ..shader = LinearGradient(
          colors: [
            Colors.black.withValues(alpha: 0.9),
            Colors.black.withValues(alpha: 0.15),
          ],
        ).createShader(Rect.fromPoints(c, end)),
    );
    canvas.drawCircle(c, 5, Paint()..color = GH.goldLight);
  }

  @override
  bool shouldRepaint(_ShadowPainter old) =>
      old.s.length != s.length || old.s.bearing != s.bearing;
}

class _AlignmentFinder extends StatefulWidget {
  const _AlignmentFinder({required this.place});
  final Place place;

  @override
  State<_AlignmentFinder> createState() => _AlignmentFinderState();
}

class _AlignmentFinderState extends State<_AlignmentFinder> {
  double _bearing = 295;
  List<AlignmentMatch>? _results;

  void _search() {
    setState(() {
      _results = findSunAlignments(
        lat: widget.place.lat,
        lng: widget.place.lng,
        targetBearing: _bearing,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final r = _results;
    return Glass(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Street / landmark direction',
            style: GH.label.copyWith(fontSize: 10),
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              Text(
                fmtBearing(_bearing),
                style: GH.value.copyWith(fontSize: 24),
              ),
              const Spacer(),
              Text(
                'Use the Compass tab to measure it',
                style: GH.body.copyWith(fontSize: 11.5),
              ),
            ],
          ),
          Slider(
            value: _bearing,
            min: 0,
            max: 359,
            onChanged: (v) => setState(() {
              _bearing = v.roundToDouble();
              _results = null;
            }),
          ),
          GoldButton(
            label: 'Find dates',
            icon: Icons.search_rounded,
            onTap: _search,
          ),
          if (r != null) ...[
            const SizedBox(height: 14),
            if (r.isEmpty)
              Text(
                'The sun never rises or sets in that direction here. Try a bearing between sunrise and sunset extremes.',
                style: GH.body.copyWith(fontSize: 13),
              )
            else
              for (final m in r.take(8))
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                    children: [
                      Icon(
                        m.isSunrise
                            ? Icons.wb_twilight_rounded
                            : Icons.wb_sunny_rounded,
                        size: 18,
                        color: m.isSunrise ? GH.gold : GH.rose,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          fmtDateLong(m.time),
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                      Text(
                        '${m.isSunrise ? 'Rise' : 'Set'} ${fmtTime(context, m.time)}',
                        style: GH.value.copyWith(fontSize: 14),
                      ),
                    ],
                  ),
                ),
          ],
        ],
      ),
    );
  }
}

class _MilkyWay extends StatelessWidget {
  const _MilkyWay({required this.place, required this.date});
  final Place place;
  final DateTime date;

  @override
  Widget build(BuildContext context) {
    final mw = milkyWayForNight(date, place.lat, place.lng);
    return Glass(
      child: mw == null
          ? Row(
              children: [
                const Icon(Icons.blur_on_rounded, color: GH.blue),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'The galactic core is not visible in a dark sky on this night. Best season is roughly March–October.',
                    style: GH.body.copyWith(fontSize: 13.5),
                  ),
                ),
              ],
            )
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.blur_on_rounded, color: Color(0xFFCAD5FF)),
                    const SizedBox(width: 10),
                    Text(
                      'Visible ${fmtTime(context, mw.start)} – ${fmtTime(context, mw.end)}',
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: Stat(
                        label: 'Best time',
                        value: fmtTime(context, mw.bestTime),
                      ),
                    ),
                    Expanded(
                      child: Stat(
                        label: 'Direction',
                        value: fmtBearing(mw.bestBearing),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: Stat(
                        label: 'Height',
                        value: '${mw.bestAltitude.toStringAsFixed(0)}°',
                      ),
                    ),
                    Expanded(
                      child: Stat(label: 'Conditions', value: mw.quality),
                    ),
                  ],
                ),
              ],
            ),
    );
  }
}
