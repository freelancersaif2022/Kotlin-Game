import 'dart:io';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/heatmap_engine.dart';
import '../core/signal.dart';
import '../core/theme.dart';
import '../services/ad_service.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import '../widgets/access_gate.dart';
import '../widgets/common.dart';
import '../widgets/gauges.dart';

enum _Tool { measure, router }

class SurveyScreen extends StatefulWidget {
  const SurveyScreen({super.key, required this.survey});

  final Survey survey;

  @override
  State<SurveyScreen> createState() => _SurveyScreenState();
}

class _SurveyScreenState extends State<SurveyScreen> {
  Survey get s => widget.survey;

  _Tool _tool = _Tool.measure;
  ui.Image? _heat;
  int _renderGen = 0;
  Offset? _sampling;
  double _sampleProgress = 0;
  bool _showSuggestions = false;
  final _viewer = TransformationController();

  @override
  void initState() {
    super.initState();
    if (s.router == null) _tool = _Tool.router;
    _rerender();
  }

  @override
  void dispose() {
    _viewer.dispose();
    _heat?.dispose();
    super.dispose();
  }

  Future<void> _rerender() async {
    final gen = ++_renderGen;
    final img = await HeatmapEngine.render(s.points, s.aspect);
    if (!mounted || gen != _renderGen) {
      img?.dispose();
      return;
    }
    setState(() {
      _heat?.dispose();
      _heat = img;
    });
  }

  Future<void> _save() => Storage.instance.saveSurvey(s);

  void _toast(String msg) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(msg), duration: const Duration(seconds: 2)));
  }

  Future<void> _onTap(Offset normalized) async {
    if (_sampling != null) return;
    if (_tool == _Tool.router) {
      HapticFeedback.selectionClick();
      setState(() {
        s.router = normalized;
        _tool = _Tool.measure;
      });
      await _save();
      _toast('Router placed. Now walk around and tap where you stand.');
      return;
    }
    final wifi = WifiService.instance;
    if (!wifi.connection.connected) {
      _toast('Connect to Wi-Fi to take measurements.');
      return;
    }
    HapticFeedback.lightImpact();
    setState(() {
      _sampling = normalized;
      _sampleProgress = 0;
    });
    final r = await wifi.sampleAveraged(onProgress: (p) {
      if (mounted) setState(() => _sampleProgress = p);
    });
    if (!mounted) return;
    setState(() => _sampling = null);
    if (!r.connected) {
      _toast('Lost the Wi-Fi connection while measuring. Try again.');
      return;
    }
    HapticFeedback.mediumImpact();
    s.ssid ??= r.ssid;
    s.points.add(SurveyPoint(x: normalized.dx, y: normalized.dy, rssi: r.rssi, linkSpeed: r.linkSpeed));
    setState(() {});
    await _save();
    _rerender();
  }

  Future<void> _undo() async {
    if (s.points.isEmpty) return;
    HapticFeedback.selectionClick();
    setState(() => s.points.removeLast());
    await _save();
    _rerender();
  }

  Future<void> _removePoint(SurveyPoint p) async {
    HapticFeedback.mediumImpact();
    setState(() => s.points.remove(p));
    await _save();
    _rerender();
  }

  Future<void> _openInsights() async {
    if (s.points.length < 3) {
      _toast('Measure at least 3 spots to get insights.');
      return;
    }
    if (!s.insightsUnlocked) {
      final unlock = await showModalBottomSheet<bool>(context: context, builder: (_) => const _UnlockSheet());
      if (unlock != true || !mounted) return;
      final earned = await AdService.instance.showRewarded();
      if (!mounted) return;
      if (!earned) {
        _toast('Watch the full video to unlock insights.');
        return;
      }
      s.insightsUnlocked = true;
      await _save();
    }
    if (!mounted) return;
    final show = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _InsightsSheet(survey: s, showing: _showSuggestions),
    );
    if (show != null && mounted) setState(() => _showSuggestions = show);
  }

  Future<void> _leave() async {
    await _save();
    await AdService.instance.showInterstitial();
    if (mounted) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final insights = _showSuggestions && s.insightsUnlocked ? SurveyInsights.analyze(s) : null;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _leave();
      },
      child: Scaffold(
        appBar: AppBar(
          leading: BackButton(onPressed: _leave),
          title: Text(s.name, maxLines: 1, overflow: TextOverflow.ellipsis),
          actions: [
            IconButton(
              tooltip: 'Undo last point',
              onPressed: s.points.isEmpty || _sampling != null ? null : _undo,
              icon: const Icon(Icons.undo_rounded),
            ),
            IconButton(
              tooltip: 'Reset zoom',
              onPressed: () => _viewer.value = Matrix4.identity(),
              icon: const Icon(Icons.fit_screen_rounded),
            ),
          ],
        ),
        body: AccessGate(
          child: Column(
            children: [
              const Padding(padding: EdgeInsets.fromLTRB(16, 0, 16, 10), child: _LiveStrip()),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(22),
                    child: ColoredBox(
                      color: AppColors.surface,
                      child: InteractiveViewer(
                        transformationController: _viewer,
                        maxScale: 5,
                        boundaryMargin: const EdgeInsets.all(80),
                        child: Center(
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: AspectRatio(
                              aspectRatio: s.aspect,
                              child: LayoutBuilder(builder: (context, box) => _canvas(box.biggest, insights)),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              _BottomBar(
                tool: _tool,
                onTool: (t) => setState(() => _tool = t),
                points: s.points.length,
                unlocked: s.insightsUnlocked,
                onInsights: _openInsights,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _canvas(Size size, SurveyInsights? insights) {
    Offset px(Offset n) => Offset(n.dx * size.width, n.dy * size.height);
    final plan = s.floorPlanPath;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapUp: (d) => _onTap(Offset(
        (d.localPosition.dx / size.width).clamp(0.0, 1.0),
        (d.localPosition.dy / size.height).clamp(0.0, 1.0),
      )),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned.fill(
            child: plan != null && File(plan).existsSync()
                ? Opacity(opacity: 0.55, child: Image.file(File(plan), fit: BoxFit.fill, gaplessPlayback: true))
                : RepaintBoundary(child: CustomPaint(painter: _GridPainter(s.widthMeters, s.heightMeters))),
          ),
          if (_heat != null)
            Positioned.fill(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 350),
                layoutBuilder: (current, previous) => Stack(fit: StackFit.expand, children: [...previous, ?current]),
                child: RawImage(
                  key: ValueKey(_heat),
                  image: _heat,
                  fit: BoxFit.fill,
                  filterQuality: FilterQuality.high,
                ),
              ),
            ),
          for (final p in s.points)
            _place(
              px(p.offset),
              36,
              GestureDetector(
                onLongPress: () => _removePoint(p),
                child: _PointDot(rssi: p.rssi),
              ),
            ),
          if (s.router != null) _place(px(s.router!), 44, const _Marker(icon: Icons.router_rounded, color: Colors.white)),
          if (insights?.suggestedRouter != null)
            _place(px(insights!.suggestedRouter!), 44,
                const _Marker(icon: Icons.router_outlined, color: AppColors.excellent, dashed: true)),
          if (insights?.suggestedExtender != null)
            _place(px(insights!.suggestedExtender!), 44,
                const _Marker(icon: Icons.settings_input_antenna_rounded, color: AppColors.fair, dashed: true)),
          if (_sampling != null)
            _place(
              px(_sampling!),
              110,
              PulseRings(
                color: AppColors.primary,
                size: 110,
                child: SizedBox.square(
                  dimension: 34,
                  child: CircularProgressIndicator(
                    value: _sampleProgress,
                    strokeWidth: 3,
                    backgroundColor: AppColors.stroke,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _place(Offset center, double size, Widget child) => Positioned(
        left: center.dx - size / 2,
        top: center.dy - size / 2,
        width: size,
        height: size,
        child: Center(child: child),
      );
}

class _LiveStrip extends StatelessWidget {
  const _LiveStrip();

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: WifiService.instance,
      builder: (context, _) {
        final c = WifiService.instance.connection;
        final color = c.connected ? Signal.color(c.rssi) : AppColors.textMuted;
        return AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          child: Row(
            children: [
              SignalBars(quality: c.connected ? c.quality : 0, color: color),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  c.connected ? (c.ssid ?? 'Connected') : 'Not connected',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
              if (c.connected) ...[
                Text('${c.rssi} dBm', style: TextStyle(color: color, fontWeight: FontWeight.w700)),
                const SizedBox(width: 10),
                Text('${c.linkSpeed} Mbps', style: const TextStyle(color: AppColors.textMuted)),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _BottomBar extends StatelessWidget {
  const _BottomBar({
    required this.tool,
    required this.onTool,
    required this.points,
    required this.unlocked,
    required this.onInsights,
  });

  final _Tool tool;
  final ValueChanged<_Tool> onTool;
  final int points;
  final bool unlocked;
  final VoidCallback onInsights;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const _Legend(),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: SegmentedButton<_Tool>(
                    showSelectedIcon: false,
                    style: SegmentedButton.styleFrom(
                      minimumSize: const Size(0, 50),
                      backgroundColor: AppColors.surface,
                      selectedBackgroundColor: AppColors.primary.withValues(alpha: 0.18),
                      selectedForegroundColor: AppColors.primary,
                      side: const BorderSide(color: AppColors.stroke),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    segments: [
                      ButtonSegment(
                        value: _Tool.measure,
                        icon: const Icon(Icons.touch_app_rounded),
                        label: const Text('Measure'),
                      ),
                      const ButtonSegment(value: _Tool.router, icon: Icon(Icons.router_rounded), label: Text('Router')),
                    ],
                    selected: {tool},
                    onSelectionChanged: (v) => onTool(v.first),
                  ),
                ),
                const SizedBox(width: 10),
                Tooltip(
                  message: 'Placement insights',
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(54, 50),
                      padding: EdgeInsets.zero,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    onPressed: onInsights,
                    child: Icon(unlocked ? Icons.insights_rounded : Icons.auto_awesome_rounded, size: 22),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              tool == _Tool.router
                  ? 'Tap where your router is'
                  : '${points == 0 ? 'Stand still and tap your position' : '$points points'} · long-press a point to delete',
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.textFaint, fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Text('Weak', style: TextStyle(fontSize: 11, color: AppColors.textMuted)),
        const SizedBox(width: 8),
        Expanded(
          child: Container(
            height: 8,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              gradient: LinearGradient(colors: [for (var r = -90.0; r <= -45; r += 5) Signal.ramp(r)]),
            ),
          ),
        ),
        const SizedBox(width: 8),
        const Text('Strong', style: TextStyle(fontSize: 11, color: AppColors.textMuted)),
      ],
    );
  }
}

class _PointDot extends StatelessWidget {
  const _PointDot({required this.rssi});
  final int rssi;

  @override
  Widget build(BuildContext context) {
    final c = Signal.color(rssi);
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 420),
      curve: Curves.easeOutBack,
      builder: (context, v, child) => Transform.scale(scale: v, child: child),
      child: Container(
        width: 30,
        height: 30,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppColors.bg.withValues(alpha: 0.85),
          border: Border.all(color: c, width: 2),
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.4), blurRadius: 6)],
        ),
        child: Text('${rssi.abs()}', style: TextStyle(fontSize: 10, fontWeight: FontWeight.w800, color: c)),
      ),
    );
  }
}

class _Marker extends StatelessWidget {
  const _Marker({required this.icon, required this.color, this.dashed = false});

  final IconData icon;
  final Color color;
  final bool dashed;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: dashed ? AppColors.bg.withValues(alpha: 0.8) : null,
        gradient: dashed ? null : AppColors.brandGradient,
        border: Border.all(color: dashed ? color : Colors.white, width: 2),
        boxShadow: [BoxShadow(color: (dashed ? color : AppColors.primary).withValues(alpha: 0.5), blurRadius: 14)],
      ),
      child: Icon(icon, size: 20, color: color),
    );
  }
}

class _GridPainter extends CustomPainter {
  _GridPainter(this.wMeters, this.hMeters);
  final double wMeters;
  final double hMeters;

  @override
  void paint(Canvas canvas, Size size) {
    final step = wMeters > 20 || hMeters > 20 ? 2.0 : 1.0;
    final minor = Paint()
      ..color = AppColors.stroke.withValues(alpha: 0.55)
      ..strokeWidth = 0.7;
    for (var m = step; m < wMeters; m += step) {
      final x = size.width * m / wMeters;
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), minor);
    }
    for (var m = step; m < hMeters; m += step) {
      final y = size.height * m / hMeters;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), minor);
    }
    canvas.drawRRect(
      RRect.fromRectAndRadius(Offset.zero & size, const Radius.circular(6)),
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2
        ..color = AppColors.textFaint,
    );
    final tp = TextPainter(
      text: TextSpan(
        text: '${wMeters.round()} m × ${hMeters.round()} m',
        style: const TextStyle(color: AppColors.textFaint, fontSize: 11),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, Offset(size.width - tp.width - 8, size.height - tp.height - 6));
  }

  @override
  bool shouldRepaint(_GridPainter old) => old.wMeters != wMeters || old.hMeters != hMeters;
}

class _UnlockSheet extends StatelessWidget {
  const _UnlockSheet();

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: const BoxDecoration(shape: BoxShape.circle, gradient: AppColors.brandGradient),
              child: const Icon(Icons.insights_rounded, color: Colors.white, size: 32),
            ),
            const SizedBox(height: 16),
            Text('Unlock placement insights', style: t.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(
              'Coverage score, dead zones, and the best spots for your router and a Wi-Fi extender, shown on your map.',
              textAlign: TextAlign.center,
              style: t.bodyMedium?.copyWith(color: AppColors.textMuted),
            ),
            const SizedBox(height: 22),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => Navigator.pop(context, true),
                icon: const Icon(Icons.play_circle_fill_rounded),
                label: const Text('Watch a short video to unlock'),
              ),
            ),
            const SizedBox(height: 6),
            Text('Unlocks insights for this survey permanently.', style: t.bodySmall?.copyWith(color: AppColors.textFaint)),
          ],
        ),
      ),
    );
  }
}

class _InsightsSheet extends StatelessWidget {
  const _InsightsSheet({required this.survey, required this.showing});

  final Survey survey;
  final bool showing;

  String _where(Offset p) {
    final col = p.dx < 0.34 ? 'left' : p.dx > 0.66 ? 'right' : 'centre';
    final row = p.dy < 0.34 ? 'top' : p.dy > 0.66 ? 'bottom' : 'middle';
    if (col == 'centre' && row == 'middle') return 'the centre';
    return 'the $row-$col';
  }

  String _meters(Offset a, Offset b) {
    final dx = (a.dx - b.dx) * survey.widthMeters;
    final dy = (a.dy - b.dy) * survey.heightMeters;
    return math.sqrt(dx * dx + dy * dy).toStringAsFixed(1);
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final i = SurveyInsights.analyze(survey)!;
    final scoreColor = i.score >= 75 ? AppColors.excellent : i.score >= 50 ? AppColors.fair : AppColors.bad;
    final router = survey.router;

    Widget item(IconData icon, Color color, String title, String body) => Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(color: color.withValues(alpha: 0.14), borderRadius: BorderRadius.circular(12)),
                child: Icon(icon, size: 19, color: color),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
                    const SizedBox(height: 2),
                    Text(body, style: t.bodyMedium?.copyWith(color: AppColors.textMuted)),
                  ],
                ),
              ),
            ],
          ),
        );

    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.75,
      maxChildSize: 0.92,
      builder: (context, scroll) => ListView(
        controller: scroll,
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
        children: [
          Row(
            children: [
              ArcGauge(
                fraction: i.score / 100,
                color: scoreColor,
                size: 110,
                strokeWidth: 10,
                center: Text('${i.score}', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w800, color: scoreColor)),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Coverage score', style: t.titleLarge),
                    const SizedBox(height: 4),
                    Text('Average ${i.average.round()} dBm across ${survey.points.length} spots', style: t.bodyMedium?.copyWith(color: AppColors.textMuted)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: SizedBox(
              height: 12,
              child: Row(
                children: [
                  if (i.coverage.strong > 0) Expanded(flex: (i.coverage.strong * 1000).round(), child: const ColoredBox(color: AppColors.excellent)),
                  if (i.coverage.fair > 0) Expanded(flex: (i.coverage.fair * 1000).round(), child: const ColoredBox(color: AppColors.fair)),
                  if (i.coverage.weak > 0) Expanded(flex: (i.coverage.weak * 1000).round(), child: const ColoredBox(color: AppColors.bad)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('${(i.coverage.strong * 100).round()}% strong', style: const TextStyle(color: AppColors.excellent, fontSize: 12)),
              Text('${(i.coverage.fair * 100).round()}% fair', style: const TextStyle(color: AppColors.fair, fontSize: 12)),
              Text('${(i.coverage.weak * 100).round()}% weak', style: const TextStyle(color: AppColors.bad, fontSize: 12)),
            ],
          ),
          const SectionHeader('Findings'),
          item(Icons.star_rounded, AppColors.excellent, 'Best spot: ${i.best.rssi} dBm',
              'Strongest signal is at ${_where(i.best.offset)} of the map. Use it for gaming consoles, TVs or a work desk.'),
          item(Icons.signal_wifi_bad_rounded, AppColors.bad, 'Weakest spot: ${i.worst.rssi} dBm',
              i.worst.rssi < -75
                  ? 'Signal at ${_where(i.worst.offset)} is too weak for reliable video calls.'
                  : 'Even your weakest spot is usable. Nice coverage!'),
          if (i.deadZones.isNotEmpty)
            item(Icons.block_rounded, AppColors.weak, '${i.deadZones.length} dead zone${i.deadZones.length == 1 ? '' : 's'}',
                'Spots below −75 dBm. Thick walls, mirrors, aquariums and appliances are common causes.'),
          const SectionHeader('Recommendations'),
          if (i.suggestedRouter != null)
            item(
              Icons.router_outlined,
              AppColors.excellent,
              router == null ? 'Best router position' : 'Move your router',
              router == null
                  ? 'Place your router near ${_where(i.suggestedRouter!)} for the most even coverage.'
                  : 'Moving it about ${_meters(router, i.suggestedRouter!)} m toward ${_where(i.suggestedRouter!)} '
                      'should balance coverage. Keep it high and in the open.',
            )
          else
            item(Icons.check_circle_rounded, AppColors.excellent, 'Router is well placed',
                'Your router is already close to the ideal spot for this space.'),
          if (i.suggestedExtender != null)
            item(Icons.settings_input_antenna_rounded, AppColors.fair, 'Add an extender or mesh node',
                'Place it near ${_where(i.suggestedExtender!)}: close enough to the router to receive a strong signal, '
                    'and close enough to your dead zones to cover them.'),
          item(Icons.tips_and_updates_rounded, AppColors.primary, 'Quick wins',
              'Raise the router off the floor, keep it away from metal and microwaves, and use the 5 GHz band in rooms near the router.'),
          const SizedBox(height: 8),
          FilledButton.icon(
            onPressed: () => Navigator.pop(context, !showing),
            icon: Icon(showing ? Icons.visibility_off_rounded : Icons.visibility_rounded),
            label: Text(showing ? 'Hide suggestions on map' : 'Show suggestions on map'),
          ),
          const SizedBox(height: 10),
          Text(
            'Suggestions are based on your measurements. Real results depend on walls and building materials.',
            textAlign: TextAlign.center,
            style: t.bodySmall?.copyWith(color: AppColors.textFaint),
          ),
        ],
      ),
    );
  }
}
