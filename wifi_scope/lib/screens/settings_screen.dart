import 'package:flutter/material.dart';

import '../core/theme.dart';
import '../services/ad_service.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import '../widgets/common.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  static Widget button(BuildContext context) => IconButton(
        tooltip: 'Settings',
        icon: const Icon(Icons.tune_rounded),
        onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const SettingsScreen())),
      );

  @override
  Widget build(BuildContext context) {
    Widget tile(IconData icon, String title, String subtitle, VoidCallback onTap) => ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 4),
          leading: Icon(icon, color: AppColors.primary),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
          subtitle: Text(subtitle, style: const TextStyle(color: AppColors.textMuted)),
          trailing: const Icon(Icons.chevron_right_rounded, color: AppColors.textFaint),
          onTap: onTap,
        );
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 32),
        children: [
          AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            child: Column(
              children: [
                tile(Icons.wifi_rounded, 'Wi-Fi settings', 'Connect to or switch networks',
                    WifiService.instance.openWifiSettings),
                tile(Icons.location_on_outlined, 'Location permission', 'Needed to read Wi-Fi details',
                    () => WifiService.instance.checkAccess(request: true)),
                ValueListenableBuilder<bool>(
                  valueListenable: AdService.instance.privacyOptionsRequired,
                  builder: (context, needed, _) => needed
                      ? tile(Icons.privacy_tip_outlined, 'Ad privacy choices', 'Review your ad consent',
                          AdService.instance.showPrivacyOptions)
                      : const SizedBox.shrink(),
                ),
                tile(Icons.delete_outline_rounded, 'Clear speed history', 'Remove all saved speed tests', () async {
                  await Storage.instance.clearSpeedHistory();
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Speed history cleared')));
                  }
                }),
              ],
            ),
          ),
          const SectionHeader('About'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('WiFi Scope', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
                const SizedBox(height: 4),
                Text('Version 1.0.0', style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textMuted)),
                const SizedBox(height: 14),
                Text(
                  'Signal readings come straight from your phone\'s Wi-Fi chip. Distances and placement '
                  'suggestions are estimates: walls, floors and interference change real results. '
                  'Speed tests use Cloudflare\'s public test servers.',
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
