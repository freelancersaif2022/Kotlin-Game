import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../services/app_state.dart';
import '../services/services.dart';
import '../theme.dart';
import '../widgets/common.dart';
import '../widgets/places_sheet.dart';

/// Replace with your hosted privacy policy (required by Google Play).
const privacyPolicyUrl = 'https://example.com/golden-hour/privacy';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final s = ServicesScope.of(context);
    final a = state.alerts;

    Future<void> update(AlertSettings next) async {
      await state.setAlerts(next);
      if (next.enabled) await s.notifications.requestPermission();
      await s.notifications.reschedule();
    }

    Future<void> proToggle(bool value, AlertSettings Function() build) async {
      if (!await ensureAccess(context, title: 'Custom alerts')) return;
      await update(build());
    }

    return Scaffold(
      backgroundColor: GH.ink,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 40),
        children: [
          if (!state.isPro)
            Pressable(
              onTap: () => openPaywall(context),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(24),
                  gradient: const LinearGradient(
                    colors: [Color(0xFF3A2414), Color(0xFF1A1220)],
                  ),
                  border: Border.all(color: GH.gold.withValues(alpha: 0.4)),
                ),
                child: const Row(
                  children: [
                    Icon(
                      Icons.workspace_premium_rounded,
                      color: GH.goldLight,
                      size: 34,
                    ),
                    SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Upgrade to Pro',
                            style: TextStyle(
                              fontWeight: FontWeight.w700,
                              fontSize: 17,
                            ),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'No ads, unlimited alerts & every planning tool',
                            style: GH.body,
                          ),
                        ],
                      ),
                    ),
                    Icon(Icons.chevron_right_rounded, color: GH.textMid),
                  ],
                ),
              ),
            )
          else
            const Glass(
              child: Row(
                children: [
                  Icon(Icons.verified_rounded, color: GH.goldLight),
                  SizedBox(width: 12),
                  Text(
                    'Golden Hour Pro is active',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
          const SectionLabel('Alerts'),
          Glass(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            child: Column(
              children: [
                SwitchListTile(
                  value: a.enabled,
                  onChanged: (v) => update(a.copyWith(enabled: v)),
                  title: const Text('Golden hour alerts'),
                  subtitle: Text(
                    state.isPro ? 'Before each window you choose' : 'Free: 1 alert a day, 20 min before evening golden hour',
                    style: GH.body.copyWith(fontSize: 12.5),
                  ),
                ),
                _ProSwitch(
                  title: 'Evening golden hour',
                  value: state.isPro ? a.eveningGolden : true,
                  isPro: state.isPro,
                  onChanged: (v) =>
                      proToggle(v, () => a.copyWith(eveningGolden: v)),
                ),
                _ProSwitch(
                  title: 'Morning golden hour',
                  value: state.isPro && a.morningGolden,
                  isPro: state.isPro,
                  onChanged: (v) =>
                      proToggle(v, () => a.copyWith(morningGolden: v)),
                ),
                _ProSwitch(
                  title: 'Evening blue hour',
                  value: state.isPro && a.blueHour,
                  isPro: state.isPro,
                  onChanged: (v) => proToggle(v, () => a.copyWith(blueHour: v)),
                ),
                ListTile(
                  title: const Text('Alert me before'),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '${state.isPro ? a.minutesBefore : 20} min',
                        style: GH.value.copyWith(fontSize: 15),
                      ),
                      if (!state.isPro) ...[
                        const SizedBox(width: 8),
                        const ProBadge(small: true),
                      ],
                    ],
                  ),
                  onTap: () async {
                    if (!await ensureAccess(
                      context,
                      title: 'Custom alert timing',
                    )) {
                      return;
                    }
                    if (!context.mounted) return;
                    final v = await showModalBottomSheet<int>(
                      context: context,
                      backgroundColor: const Color(0xFF151827),
                      builder: (ctx) => SafeArea(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            for (final m in [5, 10, 15, 20, 30, 45, 60, 90])
                              ListTile(
                                title: Text('$m minutes'),
                                trailing: m == a.minutesBefore
                                    ? const Icon(
                                        Icons.check_rounded,
                                        color: GH.gold,
                                      )
                                    : null,
                                onTap: () => Navigator.pop(ctx, m),
                              ),
                          ],
                        ),
                      ),
                    );
                    if (v != null) await update(a.copyWith(minutesBefore: v));
                  },
                ),
                if (!state.isPro)
                  ListTile(
                    leading: const Icon(
                      Icons.play_circle_fill_rounded,
                      color: GH.goldLight,
                    ),
                    title: const Text('Extra morning alert today'),
                    subtitle: Text(
                      state.hasAccess(RewardFeature.extraAlert)
                          ? 'Active for today'
                          : 'Watch a short ad',
                      style: GH.body.copyWith(fontSize: 12.5),
                    ),
                    onTap: state.hasAccess(RewardFeature.extraAlert)
                        ? null
                        : () => unlockWithAd(context, RewardFeature.extraAlert),
                  ),
              ],
            ),
          ),
          const SectionLabel('Places'),
          Glass(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            child: ListTile(
              leading: const Icon(Icons.place_rounded, color: GH.goldLight),
              title: const Text('Manage places'),
              subtitle: Text(
                '${state.savedPlaces.length} saved${state.isPro ? '' : ' · ${AppState.freePlaceLimit} free'}',
                style: GH.body.copyWith(fontSize: 12.5),
              ),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () => showPlacesSheet(context),
            ),
          ),
          const SectionLabel('Purchases & privacy'),
          Glass(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.restore_rounded),
                  title: const Text('Restore purchases'),
                  onTap: s.purchases.restore,
                ),
                ListTile(
                  leading: const Icon(Icons.privacy_tip_outlined),
                  title: const Text('Ad privacy choices'),
                  onTap: s.ads.showPrivacyOptions,
                ),
                ListTile(
                  leading: const Icon(Icons.description_outlined),
                  title: const Text('Privacy policy'),
                  onTap: () => launchUrl(
                    Uri.parse(privacyPolicyUrl),
                    mode: LaunchMode.externalApplication,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 26),
          Text(
            'All calculations run offline on your phone. Your location never leaves the device.\n'
            'Times are accurate to about a minute. Weather and clouds are not forecast.',
            textAlign: TextAlign.center,
            style: GH.body.copyWith(fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _ProSwitch extends StatelessWidget {
  const _ProSwitch({
    required this.title,
    required this.value,
    required this.isPro,
    required this.onChanged,
  });
  final String title;
  final bool value;
  final bool isPro;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => SwitchListTile(
    value: value,
    onChanged: onChanged,
    title: Row(
      children: [
        Text(title),
        if (!isPro) ...[const SizedBox(width: 8), const ProBadge(small: true)],
      ],
    ),
  );
}
