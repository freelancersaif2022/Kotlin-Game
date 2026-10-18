import 'package:flutter/material.dart';
import 'package:wifi_scan/wifi_scan.dart';

import '../core/signal.dart';
import '../core/theme.dart';
import '../services/ad_service.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import '../widgets/access_gate.dart';
import '../widgets/common.dart';
import 'settings_screen.dart';

enum _Band { b24, b5, b6 }

extension on _Band {
  String get label => switch (this) { _Band.b24 => '2.4 GHz', _Band.b5 => '5 GHz', _Band.b6 => '6 GHz' };
  bool contains(int f) => switch (this) {
        _Band.b24 => f < 3000,
        _Band.b5 => f >= 4900 && f < 5925,
        _Band.b6 => f >= 5925,
      };
}

class NetworksScreen extends StatefulWidget {
  const NetworksScreen({super.key});

  @override
  State<NetworksScreen> createState() => _NetworksScreenState();
}

class _NetworksScreenState extends State<NetworksScreen> {
  List<WiFiAccessPoint> _all = [];
  bool _loading = false;
  bool _throttled = false;
  bool _scannedOnce = false;
  _Band _band = _Band.b24;

  @override
  void initState() {
    super.initState();
    WifiService.instance.addListener(_maybeFirstScan);
    _maybeFirstScan();
  }

  @override
  void dispose() {
    WifiService.instance.removeListener(_maybeFirstScan);
    super.dispose();
  }

  void _maybeFirstScan() {
    if (!_scannedOnce && WifiService.instance.access == WifiAccess.ready) {
      _scannedOnce = true;
      _scan();
    }
  }

  Future<void> _scan() async {
    if (_loading) return;
    setState(() => _loading = true);
    final r = await WifiService.instance.scan();
    if (!mounted) return;
    setState(() {
      _all = r.results;
      _throttled = r.throttled;
      _loading = false;
    });
  }

  Future<void> _unlockAdvisor() async {
    final earned = await AdService.instance.showRewarded();
    if (earned) await Storage.instance.unlockChannelAdvisor();
  }

  @override
  Widget build(BuildContext context) {
    final list = _all.where((a) => _band.contains(a.frequency)).toList();
    final connected = WifiService.instance.connection;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Networks'),
        actions: [
          IconButton(
            tooltip: 'Scan again',
            onPressed: _loading ? null : _scan,
            icon: _loading
                ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2.4))
                : const Icon(Icons.refresh_rounded),
          ),
          SettingsScreen.button(context),
          const SizedBox(width: 4),
        ],
      ),
      body: AccessGate(
        child: RefreshIndicator(
          onRefresh: _scan,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
            children: [
              SegmentedButton<_Band>(
                showSelectedIcon: false,
                style: SegmentedButton.styleFrom(
                  backgroundColor: AppColors.surface,
                  selectedBackgroundColor: AppColors.primary.withValues(alpha: 0.18),
                  selectedForegroundColor: AppColors.primary,
                  side: const BorderSide(color: AppColors.stroke),
                ),
                segments: [
                  for (final b in _Band.values)
                    ButtonSegment(
                      value: b,
                      label: Text('${b.label} (${_all.where((a) => b.contains(a.frequency)).length})'),
                    ),
                ],
                selected: {_band},
                onSelectionChanged: (v) => setState(() => _band = v.first),
              ),
              if (_throttled)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: AppCard(
                    padding: const EdgeInsets.all(14),
                    child: Row(
                      children: [
                        const Icon(Icons.info_outline_rounded, color: AppColors.fair, size: 20),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            'Android limits Wi-Fi scans to 4 every 2 minutes. Showing the latest available results.',
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.textMuted),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              if (_band != _Band.b6 && list.isNotEmpty) ...[
                const SectionHeader('Channel usage'),
                AppCard(
                  padding: const EdgeInsets.fromLTRB(12, 16, 12, 10),
                  child: _ChannelChart(aps: list, band: _band, connectedFreq: connected.frequency),
                ),
                const SizedBox(height: 12),
                ListenableBuilder(
                  listenable: Storage.instance,
                  builder: (context, _) => _ChannelAdvisor(
                    aps: list,
                    band: _band,
                    unlocked: Storage.instance.channelAdvisorUnlocked,
                    onUnlock: _unlockAdvisor,
                    currentChannel: _band.contains(connected.frequency) ? connected.channel : null,
                  ),
                ),
              ],
              SectionHeader('Nearby networks', trailing: Text('${list.length}', style: const TextStyle(color: AppColors.textMuted))),
              if (list.isEmpty && !_loading)
                const StateMessage(
                  icon: Icons.wifi_find_rounded,
                  title: 'No networks found',
                  message: 'Pull down to scan again, or try another band.',
                )
              else
                for (final ap in list)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _ApTile(ap, isConnected: ap.bssid == connected.bssid),
                  ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ApTile extends StatelessWidget {
  const _ApTile(this.ap, {required this.isConnected});

  final WiFiAccessPoint ap;
  final bool isConnected;

  String get _security {
    final c = ap.capabilities;
    if (c.contains('SAE')) return 'WPA3';
    if (c.contains('WPA2') || c.contains('RSN')) return 'WPA2';
    if (c.contains('WPA')) return 'WPA';
    if (c.contains('WEP')) return 'WEP';
    return 'Open';
  }

  @override
  Widget build(BuildContext context) {
    final color = Signal.color(ap.level);
    final width = switch (ap.channelWidth) {
      WiFiChannelWidth.mhz20 => '20',
      WiFiChannelWidth.mhz40 => '40',
      WiFiChannelWidth.mhz80 => '80',
      WiFiChannelWidth.mhz160 || WiFiChannelWidth.mhz80Plus80 => '160',
      WiFiChannelWidth.mhz320 => '320',
      _ => null,
    };
    return AppCard(
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(14)),
            child: SignalBars(quality: Signal.quality(ap.level), color: color, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(
                        ap.ssid.isEmpty ? 'Hidden network' : ap.ssid,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontWeight: FontWeight.w600,
                          color: ap.ssid.isEmpty ? AppColors.textMuted : AppColors.text,
                        ),
                      ),
                    ),
                    if (isConnected) ...[
                      const SizedBox(width: 8),
                      const Pill(text: 'Connected', color: AppColors.primary),
                    ],
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  'Ch ${Signal.channel(ap.frequency)}${width != null ? ' · $width MHz' : ''} · $_security',
                  style: const TextStyle(color: AppColors.textMuted, fontSize: 12.5),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('${ap.level}', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: color)),
              const Text('dBm', style: TextStyle(fontSize: 10, color: AppColors.textFaint)),
            ],
          ),
        ],
      ),
    );
  }
}

/// Interference score per channel: each AP contributes its signal strength
/// to the channels its bandwidth overlaps.
Map<int, double> _channelLoad(List<WiFiAccessPoint> aps, _Band band) {
  final channels = band == _Band.b24
      ? List.generate(13, (i) => i + 1)
      : [36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165];
  final load = {for (final c in channels) c: 0.0};
  for (final ap in aps) {
    final ch = Signal.channel(ap.frequency);
    final weight = Signal.quality(ap.level) / 100;
    if (band == _Band.b24) {
      // A 20 MHz 2.4 GHz signal spreads ±2 channels.
      for (final c in channels) {
        final d = (c - ch).abs();
        if (d <= 2) load[c] = load[c]! + weight * (1 - d * 0.3);
      }
    } else if (load.containsKey(ch)) {
      load[ch] = load[ch]! + weight;
    }
  }
  return load;
}

class _ChannelChart extends StatelessWidget {
  const _ChannelChart({required this.aps, required this.band, required this.connectedFreq});

  final List<WiFiAccessPoint> aps;
  final _Band band;
  final int connectedFreq;

  @override
  Widget build(BuildContext context) {
    final load = _channelLoad(aps, band);
    final entries = band == _Band.b24 ? load.entries.toList() : load.entries.where((e) => e.value > 0).toList();
    final maxLoad = entries.fold<double>(0.5, (m, e) => e.value > m ? e.value : m);
    final mine = band.contains(connectedFreq) ? Signal.channel(connectedFreq) : null;
    if (entries.isEmpty) return const SizedBox.shrink();
    return SizedBox(
      height: 150,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          for (final e in entries)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2.5),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Flexible(
                      child: FractionallySizedBox(
                        heightFactor: (e.value / maxLoad).clamp(0.03, 1.0),
                        child: TweenAnimationBuilder<double>(
                          tween: Tween(begin: 0, end: 1),
                          duration: const Duration(milliseconds: 600),
                          curve: Curves.easeOutCubic,
                          builder: (context, v, _) => FractionallySizedBox(
                            heightFactor: v,
                            alignment: Alignment.bottomCenter,
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(5),
                                gradient: LinearGradient(
                                  begin: Alignment.bottomCenter,
                                  end: Alignment.topCenter,
                                  colors: e.key == mine
                                      ? [AppColors.primary.withValues(alpha: 0.5), AppColors.primary]
                                      : [AppColors.accent.withValues(alpha: 0.25), AppColors.accent.withValues(alpha: 0.8)],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    FittedBox(
                      child: Text(
                        '${e.key}',
                        style: TextStyle(
                          fontSize: 10,
                          color: e.key == mine ? AppColors.primary : AppColors.textFaint,
                          fontWeight: e.key == mine ? FontWeight.w700 : FontWeight.w500,
                        ),
                      ),
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

class _ChannelAdvisor extends StatelessWidget {
  const _ChannelAdvisor({
    required this.aps,
    required this.band,
    required this.unlocked,
    required this.onUnlock,
    required this.currentChannel,
  });

  final List<WiFiAccessPoint> aps;
  final _Band band;
  final bool unlocked;
  final VoidCallback onUnlock;
  final int? currentChannel;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    if (!unlocked) {
      return AppCard(
        onTap: onUnlock,
        gradient: LinearGradient(colors: [AppColors.accent.withValues(alpha: 0.2), AppColors.primary.withValues(alpha: 0.1)]),
        child: Row(
          children: [
            const Icon(Icons.auto_awesome_rounded, color: AppColors.accent),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Best channel advisor', style: TextStyle(fontWeight: FontWeight.w700)),
                  const SizedBox(height: 2),
                  Text('Watch a short video to unlock for 24 hours', style: t.bodySmall?.copyWith(color: AppColors.textMuted)),
                ],
              ),
            ),
            const Icon(Icons.play_circle_fill_rounded, color: AppColors.accent, size: 30),
          ],
        ),
      );
    }
    final load = _channelLoad(aps, band);
    // On 2.4 GHz only 1, 6 and 11 don't overlap, so only recommend those.
    final candidates = band == _Band.b24 ? [1, 6, 11] : load.keys.toList();
    candidates.sort((a, b) => load[a]!.compareTo(load[b]!));
    final best = candidates.first;
    final current = currentChannel;
    final alreadyBest = current != null && (current == best || (load[current] ?? 99) <= load[best]! + 0.05);
    return AppCard(
      child: Row(
        children: [
          Container(
            width: 52,
            height: 52,
            alignment: Alignment.center,
            decoration: BoxDecoration(gradient: AppColors.brandGradient, borderRadius: BorderRadius.circular(16)),
            child: Text('$best', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: Colors.white)),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Best ${band.label} channel: $best', style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 3),
                Text(
                  alreadyBest
                      ? 'Your router is already on the least crowded channel.'
                      : current != null
                          ? 'Switch from channel $current to $best in your router settings to reduce interference.'
                          : 'Set your router to channel $best for the least interference.',
                  style: t.bodyMedium?.copyWith(color: AppColors.textMuted),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
