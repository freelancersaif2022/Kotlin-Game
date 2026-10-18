import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/theme.dart';
import '../services/ad_service.dart';
import '../services/speed_test_service.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import '../widgets/common.dart';
import '../widgets/gauges.dart';
import 'settings_screen.dart';

class SpeedTestScreen extends StatefulWidget {
  const SpeedTestScreen({super.key});

  @override
  State<SpeedTestScreen> createState() => _SpeedTestScreenState();
}

class _SpeedTestScreenState extends State<SpeedTestScreen> {
  final _service = SpeedTestService();
  StreamSubscription<SpeedSnapshot>? _sub;
  SpeedSnapshot _s = const SpeedSnapshot();

  bool get _running =>
      _s.phase == SpeedPhase.ping || _s.phase == SpeedPhase.download || _s.phase == SpeedPhase.upload;

  @override
  void initState() {
    super.initState();
    _sub = _service.updates.listen((s) {
      if (mounted) setState(() => _s = s);
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    _service.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    HapticFeedback.mediumImpact();
    final result = await _service.run();
    if (!mounted || result.phase != SpeedPhase.done) return;
    HapticFeedback.heavyImpact();
    final conn = WifiService.instance.connection;
    await Storage.instance.addSpeedResult(SpeedResult(
      at: DateTime.now(),
      download: result.downloadMbps ?? 0,
      upload: result.uploadMbps ?? 0,
      ping: result.pingMs ?? 0,
      jitter: result.jitterMs ?? 0,
      ssid: conn.connected ? conn.ssid : null,
      rssi: conn.connected ? conn.rssi : null,
    ));
    // A finished test is a natural break for an interstitial.
    await AdService.instance.showInterstitial();
  }

  /// Log scale so both 5 Mbps and 900 Mbps read well on the same gauge.
  static double _fraction(double mbps) {
    if (mbps <= 0) return 0;
    return (math.log(1 + mbps) / math.log(1 + 1000)).clamp(0.0, 1.0);
  }

  String get _phaseLabel => switch (_s.phase) {
        SpeedPhase.idle => 'Ready',
        SpeedPhase.ping => 'Measuring ping',
        SpeedPhase.download => 'Download',
        SpeedPhase.upload => 'Upload',
        SpeedPhase.done => 'Complete',
        SpeedPhase.error => 'Failed',
      };

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final phaseColor = _s.phase == SpeedPhase.upload ? AppColors.accent : AppColors.primary;
    final showLive = _s.phase == SpeedPhase.download || _s.phase == SpeedPhase.upload;
    final gaugeValue = showLive ? _s.liveMbps : (_s.phase == SpeedPhase.done ? (_s.downloadMbps ?? 0) : 0.0);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Speed test'),
        actions: [SettingsScreen.button(context), const SizedBox(width: 4)],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        children: [
          AppCard(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 22),
            child: Column(
              children: [
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: Text(_phaseLabel.toUpperCase(),
                      key: ValueKey(_s.phase), style: t.labelSmall?.copyWith(color: phaseColor, fontSize: 12)),
                ),
                const SizedBox(height: 6),
                Stack(
                  alignment: Alignment.center,
                  children: [
                    ArcGauge(
                      fraction: _fraction(gaugeValue),
                      color: phaseColor,
                      size: 270,
                      ticks: const ['0', '5', '25', '100', '300', '1000'],
                      center: _running || _s.phase == SpeedPhase.done
                          ? _GaugeReadout(
                              value: _s.phase == SpeedPhase.ping ? (_s.pingMs ?? 0) : gaugeValue,
                              unit: _s.phase == SpeedPhase.ping ? 'ms' : 'Mbps',
                              loading: _s.phase == SpeedPhase.ping && _s.pingMs == null,
                            )
                          : _GoButton(onTap: _start, error: _s.phase == SpeedPhase.error),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: _running
                      ? Column(
                          key: const ValueKey('progress'),
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: _s.progress,
                                minHeight: 4,
                                color: phaseColor,
                                backgroundColor: AppColors.surfaceHigh,
                              ),
                            ),
                            const SizedBox(height: 10),
                            TextButton(onPressed: _service.cancel, child: const Text('Cancel')),
                          ],
                        )
                      : _s.phase == SpeedPhase.done
                          ? OutlinedButton.icon(
                              key: const ValueKey('again'),
                              onPressed: _start,
                              icon: const Icon(Icons.refresh_rounded),
                              label: const Text('Test again'),
                            )
                          : Text(
                              key: const ValueKey('hint'),
                              _s.error ?? 'Tap GO to measure your real internet speed',
                              textAlign: TextAlign.center,
                              style: t.bodyMedium?.copyWith(color: _s.error != null ? AppColors.bad : null),
                            ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _ResultTile(
                  icon: Icons.south_rounded,
                  label: 'Download',
                  value: _s.downloadMbps,
                  unit: 'Mbps',
                  color: AppColors.primary,
                  active: _s.phase == SpeedPhase.download,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _ResultTile(
                  icon: Icons.north_rounded,
                  label: 'Upload',
                  value: _s.uploadMbps,
                  unit: 'Mbps',
                  color: AppColors.accent,
                  active: _s.phase == SpeedPhase.upload,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _ResultTile(
                  icon: Icons.timer_outlined,
                  label: 'Ping',
                  value: _s.pingMs,
                  unit: 'ms',
                  color: AppColors.excellent,
                  active: _s.phase == SpeedPhase.ping,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _ResultTile(
                  icon: Icons.graphic_eq_rounded,
                  label: 'Jitter',
                  value: _s.jitterMs,
                  unit: 'ms',
                  color: AppColors.fair,
                  active: _s.phase == SpeedPhase.ping,
                ),
              ),
            ],
          ),
          if (_s.phase == SpeedPhase.done && _s.downloadMbps != null) ...[
            const SizedBox(height: 12),
            _Verdict(down: _s.downloadMbps!, ping: _s.pingMs ?? 0),
          ],
          ListenableBuilder(
            listenable: Storage.instance,
            builder: (context, _) {
              final h = Storage.instance.speedHistory;
              if (h.isEmpty) return const SizedBox.shrink();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SectionHeader('History'),
                  AppCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                    child: Column(
                      children: [
                        for (final (i, r) in h.take(10).indexed) _HistoryRow(r, last: i == math.min(h.length, 10) - 1),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class _GoButton extends StatelessWidget {
  const _GoButton({required this.onTap, required this.error});

  final VoidCallback onTap;
  final bool error;

  @override
  Widget build(BuildContext context) {
    return PulseRings(
      color: AppColors.primary,
      size: 190,
      child: Material(
        shape: const CircleBorder(),
        clipBehavior: Clip.antiAlias,
        child: Ink(
          width: 116,
          height: 116,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: AppColors.brandGradient,
            boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.5), blurRadius: 30)],
          ),
          child: InkWell(
            onTap: onTap,
            child: Center(
              child: Text(
                error ? 'RETRY' : 'GO',
                style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: Colors.white, letterSpacing: 1),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _GaugeReadout extends StatelessWidget {
  const _GaugeReadout({required this.value, required this.unit, this.loading = false});

  final double value;
  final String unit;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const SizedBox.square(dimension: 36, child: CircularProgressIndicator(strokeWidth: 3));
    }
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TweenAnimationBuilder<double>(
          tween: Tween(end: value),
          duration: const Duration(milliseconds: 300),
          builder: (context, v, _) => Text(
            v >= 100 ? v.toStringAsFixed(0) : v.toStringAsFixed(1),
            style: const TextStyle(
              fontSize: 54,
              fontWeight: FontWeight.w800,
              letterSpacing: -2,
              fontFeatures: [FontFeature.tabularFigures()],
            ),
          ),
        ),
        Text(unit, style: const TextStyle(color: AppColors.textMuted, fontSize: 15)),
      ],
    );
  }
}

class _ResultTile extends StatelessWidget {
  const _ResultTile({
    required this.icon,
    required this.label,
    required this.value,
    required this.unit,
    required this.color,
    required this.active,
  });

  final IconData icon;
  final String label;
  final double? value;
  final String unit;
  final Color color;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final v = value;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(22),
        boxShadow: active ? [BoxShadow(color: color.withValues(alpha: 0.25), blurRadius: 18)] : const [],
      ),
      child: StatTile(
        icon: icon,
        label: label,
        value: v == null ? '–' : (v >= 100 ? v.toStringAsFixed(0) : v.toStringAsFixed(1)),
        unit: unit,
        color: color,
      ),
    );
  }
}

class _Verdict extends StatelessWidget {
  const _Verdict({required this.down, required this.ping});

  final double down;
  final double ping;

  @override
  Widget build(BuildContext context) {
    final (title, body, color) = switch (down) {
      >= 100 => ('Excellent connection', 'Handles 4K streaming on several devices, large downloads and gaming.', AppColors.excellent),
      >= 25 => ('Good connection', 'Great for HD streaming, video calls and everyday use.', AppColors.good),
      >= 8 => ('Fair connection', 'Fine for browsing and SD video. HD may buffer on busy networks.', AppColors.fair),
      _ => ('Slow connection', 'Try moving closer to your router or testing at a different time.', AppColors.bad),
    };
    return AppCard(
      child: Row(
        children: [
          Icon(Icons.verified_rounded, color: color, size: 30),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontWeight: FontWeight.w700, color: color, fontSize: 16)),
                const SizedBox(height: 4),
                Text(
                  ping > 80 ? '$body High ping may cause lag in games and calls.' : body,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textMuted),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HistoryRow extends StatelessWidget {
  const _HistoryRow(this.r, {required this.last});

  final SpeedResult r;
  final bool last;

  @override
  Widget build(BuildContext context) {
    final l = MaterialLocalizations.of(context);
    final when = '${l.formatShortMonthDay(r.at)}, ${l.formatTimeOfDay(TimeOfDay.fromDateTime(r.at))}';
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        border: last ? null : const Border(bottom: BorderSide(color: AppColors.stroke, width: 0.6)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(r.ssid ?? 'Mobile / unknown', maxLines: 1, overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 2),
                Text(when, style: const TextStyle(color: AppColors.textFaint, fontSize: 12)),
              ],
            ),
          ),
          _num(Icons.south_rounded, r.download, AppColors.primary),
          const SizedBox(width: 14),
          _num(Icons.north_rounded, r.upload, AppColors.accent),
          const SizedBox(width: 14),
          SizedBox(
            width: 52,
            child: Text('${r.ping.round()} ms',
                textAlign: TextAlign.right, style: const TextStyle(color: AppColors.textMuted, fontSize: 13)),
          ),
        ],
      ),
    );
  }

  Widget _num(IconData icon, double v, Color c) => Row(
        children: [
          Icon(icon, size: 14, color: c),
          const SizedBox(width: 2),
          Text(v >= 100 ? v.toStringAsFixed(0) : v.toStringAsFixed(1),
              style: const TextStyle(fontWeight: FontWeight.w600, fontFeatures: [FontFeature.tabularFigures()])),
        ],
      );
}
