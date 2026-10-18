import 'package:flutter/material.dart';

import '../core/signal.dart';
import '../core/theme.dart';
import '../services/wifi_service.dart';
import '../widgets/access_gate.dart';
import '../widgets/common.dart';
import '../widgets/gauges.dart';
import 'settings_screen.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Signal'),
        actions: [SettingsScreen.button(context), const SizedBox(width: 4)],
      ),
      body: AccessGate(
        child: ListenableBuilder(
          listenable: WifiService.instance,
          builder: (context, _) {
            final c = WifiService.instance.connection;
            if (!c.wifiEnabled || !c.connected) {
              return Center(
                child: StateMessage(
                  icon: Icons.wifi_off_rounded,
                  title: c.wifiEnabled ? 'Not connected' : 'Wi-Fi is off',
                  message: c.wifiEnabled
                      ? 'Connect to a Wi-Fi network to see live signal details.'
                      : 'Turn on Wi-Fi and connect to a network to get started.',
                  actionLabel: 'Open Wi-Fi',
                  onAction: WifiService.instance.openWifiSettings,
                ),
              );
            }
            return _Connected(c, WifiService.instance.history);
          },
        ),
      ),
    );
  }
}

class _Connected extends StatelessWidget {
  const _Connected(this.c, this.history);

  final WifiConnection c;
  final List<int> history;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final color = Signal.color(c.rssi);
    final speeds = [
      if (c.rxLinkSpeed != null && c.rxLinkSpeed! > 0) c.rxLinkSpeed!,
      if (c.txLinkSpeed != null && c.txLinkSpeed! > 0) c.txLinkSpeed!,
    ];
    final details = [
      ('IP address', c.ip ?? '–'),
      ('BSSID', c.bssid ?? 'Hidden'),
      if (c.rxLinkSpeed != null && c.rxLinkSpeed! > 0) ('Receive link', '${c.rxLinkSpeed} Mbps'),
      if (c.txLinkSpeed != null && c.txLinkSpeed! > 0) ('Transmit link', '${c.txLinkSpeed} Mbps'),
      if (c.maxTxSpeed != null && c.maxTxSpeed! > 0) ('Max supported', '${c.maxTxSpeed} Mbps'),
    ];
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
      children: [
        AppCard(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 22),
          child: Column(
            children: [
              Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(gradient: AppColors.brandGradient, borderRadius: BorderRadius.circular(14)),
                    child: const Icon(Icons.wifi_rounded, color: Colors.white),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(c.ssid ?? 'Hidden network',
                            maxLines: 1, overflow: TextOverflow.ellipsis, style: t.titleMedium),
                        const SizedBox(height: 2),
                        Text('${c.band} · Channel ${c.channel}', style: t.bodySmall?.copyWith(color: AppColors.textMuted)),
                      ],
                    ),
                  ),
                  TextButton.icon(
                    onPressed: WifiService.instance.openWifiSettings,
                    icon: const Icon(Icons.swap_horiz_rounded, size: 18),
                    label: const Text('Switch'),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              ArcGauge(
                fraction: c.quality / 100,
                color: color,
                size: 250,
                ticks: const ['-100', '-85', '-70', '-50'],
                center: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text.rich(TextSpan(children: [
                      TextSpan(
                        text: '${c.rssi}',
                        style: const TextStyle(fontSize: 58, fontWeight: FontWeight.w800, letterSpacing: -2),
                      ),
                      const TextSpan(text: ' dBm', style: TextStyle(fontSize: 16, color: AppColors.textMuted)),
                    ])),
                    const SizedBox(height: 6),
                    Pill(text: '${Signal.label(c.rssi)} · ${c.quality}%', color: color),
                  ],
                ),
              ),
              const SizedBox(height: 6),
              Text(Signal.usage(c.rssi), textAlign: TextAlign.center, style: t.bodyMedium?.copyWith(color: AppColors.textMuted)),
            ],
          ),
        ),
        const SectionHeader('Live signal', trailing: Text('last 60 s', style: TextStyle(color: AppColors.textFaint, fontSize: 12))),
        AppCard(
          padding: const EdgeInsets.fromLTRB(14, 16, 14, 12),
          child: Column(
            children: [
              Sparkline(values: history, color: color),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _miniStat('Min', history.isEmpty ? '–' : '${history.reduce((a, b) => a < b ? a : b)}'),
                  _miniStat('Avg', history.isEmpty ? '–' : '${(history.reduce((a, b) => a + b) / history.length).round()}'),
                  _miniStat('Max', history.isEmpty ? '–' : '${history.reduce((a, b) => a > b ? a : b)}'),
                ],
              ),
            ],
          ),
        ),
        const SectionHeader('Connection'),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 1.75,
          children: [
            StatTile(
              icon: Icons.bolt_rounded,
              label: 'Link speed',
              value: '${speeds.isEmpty ? c.linkSpeed : speeds.reduce((a, b) => a > b ? a : b)}',
              unit: 'Mbps',
              color: AppColors.primary,
            ),
            StatTile(
              icon: Icons.social_distance_rounded,
              label: 'Distance',
              value: '~${c.distance < 10 ? c.distance.toStringAsFixed(1) : c.distance.round()}',
              unit: 'm',
              color: AppColors.accent,
            ),
            StatTile(icon: Icons.waves_rounded, label: 'Frequency', value: '${c.frequency}', unit: 'MHz'),
            StatTile(icon: Icons.router_rounded, label: 'Standard', value: Signal.standardName(c.standard).split(' (').first),
          ],
        ),
        const SizedBox(height: 12),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 6),
          child: Column(
            children: [
              for (final (i, (k, v)) in details.indexed) _row(k, v, last: i == details.length - 1),
            ],
          ),
        ),
        const SizedBox(height: 14),
        Text(
          'Distance is estimated from signal loss and is affected by walls and floors.',
          textAlign: TextAlign.center,
          style: t.bodySmall?.copyWith(color: AppColors.textFaint),
        ),
      ],
    );
  }

  Widget _miniStat(String label, String value) => Column(
        children: [
          Text(label.toUpperCase(), style: const TextStyle(fontSize: 10, color: AppColors.textFaint, letterSpacing: 0.8)),
          const SizedBox(height: 2),
          Text('$value dBm', style: const TextStyle(fontWeight: FontWeight.w600)),
        ],
      );

  Widget _row(String k, String v, {bool last = false}) => Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          border: last ? null : const Border(bottom: BorderSide(color: AppColors.stroke, width: 0.6)),
        ),
        child: Row(
          children: [
            Text(k, style: const TextStyle(color: AppColors.textMuted)),
            const Spacer(),
            Text(v, style: const TextStyle(fontWeight: FontWeight.w600, fontFeatures: [FontFeature.tabularFigures()])),
          ],
        ),
      );
}
