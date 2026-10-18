import 'package:flutter/material.dart';
import 'package:in_app_purchase/in_app_purchase.dart';

import '../services/app_state.dart';
import '../services/services.dart';
import '../theme.dart';
import '../widgets/common.dart';
import '../widgets/sky/sky_view.dart';

class PaywallScreen extends StatefulWidget {
  const PaywallScreen({super.key});

  @override
  State<PaywallScreen> createState() => _PaywallScreenState();
}

class _PaywallScreenState extends State<PaywallScreen>
    with SingleTickerProviderStateMixin {
  bool _yearly = true;
  late final AnimationController _intro = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..forward();

  static const _features = [
    (
      Icons.view_in_ar_rounded,
      'AR sun path for any date',
      'Plan weddings, trips & shoots months ahead',
    ),
    (
      Icons.dark_mode_rounded,
      'AR moon path',
      'See exactly where the moon rises over your scene',
    ),
    (
      Icons.notifications_active_rounded,
      'Unlimited smart alerts',
      'Morning, evening & blue hour, any lead time',
    ),
    (
      Icons.alt_route_rounded,
      'Sun alignment finder',
      'Dates the sun sets right down your street',
    ),
    (
      Icons.blur_on_rounded,
      'Milky Way finder',
      'Galactic core times, direction & moon conditions',
    ),
    (
      Icons.camera_rounded,
      'Pro camera settings',
      'ISO, shutter, aperture & WB for the current light',
    ),
    (Icons.place_rounded, 'Unlimited places', 'Save every viewpoint you love'),
    (
      Icons.block_rounded,
      'No ads, ever',
      'A calm, distraction-free experience',
    ),
  ];

  @override
  void dispose() {
    _intro.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = ServicesScope.of(context);
    final state = AppScope.of(context);
    return ListenableBuilder(
      listenable: s.purchases,
      builder: (context, _) {
        final p = s.purchases;
        final yearly = p.yearly;
        final monthly = p.monthly;
        if (state.isPro) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (context.mounted && Navigator.of(context).canPop()) {
              Navigator.of(context).pop();
            }
          });
        }
        return Scaffold(
          backgroundColor: GH.ink,
          body: Stack(
            fit: StackFit.expand,
            children: [
              const SkyView(
                sunBearing: 262,
                sunAltitude: 1.2,
                viewBearing: 262,
                horizon: 0.42,
                cloudCover: 0.55,
              ),
              const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color(0x00000000),
                      Color(0xB305060B),
                      Color(0xF705060B),
                    ],
                    stops: [0.0, 0.38, 0.55],
                  ),
                ),
              ),
              SafeArea(
                child: Column(
                  children: [
                    Align(
                      alignment: Alignment.topRight,
                      child: IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.close_rounded, color: GH.textHi),
                      ),
                    ),
                    Expanded(
                      child: ListView(
                        padding: const EdgeInsets.symmetric(horizontal: 22),
                        children: [
                          const SizedBox(height: 40),
                          ShaderMask(
                            shaderCallback: (r) =>
                                GH.goldGradient.createShader(r),
                            child: const Text(
                              'Golden Hour Pro',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 36,
                                fontWeight: FontWeight.w300,
                                color: Colors.white,
                                letterSpacing: -1,
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'Never miss perfect light again.',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 16, color: GH.textMid),
                          ),
                          const SizedBox(height: 28),
                          for (var i = 0; i < _features.length; i++)
                            _FeatureRow(
                              animation: CurvedAnimation(
                                parent: _intro,
                                curve: Interval(
                                  0.08 * i,
                                  (0.08 * i + 0.45).clamp(0, 1),
                                  curve: Curves.easeOutCubic,
                                ),
                              ),
                              icon: _features[i].$1,
                              title: _features[i].$2,
                              subtitle: _features[i].$3,
                            ),
                          const SizedBox(height: 16),
                        ],
                      ),
                    ),
                    // Plans + CTA stay pinned so they're always visible.
                    Container(
                      padding: const EdgeInsets.fromLTRB(20, 14, 20, 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0B0B12),
                        borderRadius: const BorderRadius.vertical(
                          top: Radius.circular(28),
                        ),
                        border: Border(
                          top: BorderSide(
                            color: Colors.white.withValues(alpha: 0.08),
                          ),
                        ),
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: _PlanCard(
                                  selected: _yearly,
                                  title: 'Yearly',
                                  price: yearly?.price ?? r'$14.99',
                                  per: '/ year',
                                  note:
                                      'Only ${_perMonth(yearly) ?? r'$1.25'}/mo',
                                  badge: 'SAVE 58%',
                                  onTap: () => setState(() => _yearly = true),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: _PlanCard(
                                  selected: !_yearly,
                                  title: 'Monthly',
                                  price: monthly?.price ?? r'$2.99',
                                  per: '/ month',
                                  note: 'Cancel anytime',
                                  onTap: () => setState(() => _yearly = false),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          GoldButton(
                            label: _yearly ? 'Start free trial' : 'Continue',
                            busy: p.busy,
                            onTap: () {
                              final product = _yearly ? yearly : monthly;
                              if (product == null) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'Store not available right now. Check your connection and Play Store account.',
                                    ),
                                  ),
                                );
                                return;
                              }
                              p.buy(product);
                            },
                          ),
                          if (p.error != null) ...[
                            const SizedBox(height: 10),
                            Text(
                              p.error!,
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                color: GH.rose,
                                fontSize: 13,
                              ),
                            ),
                          ],
                          const SizedBox(height: 10),
                          Text(
                            _yearly
                                ? 'Free for 3 days, then ${yearly?.price ?? r'$14.99'}/year. Cancel anytime in Google Play / App Store — you won\'t be charged during the trial.'
                                : 'Renews monthly. Cancel anytime in Google Play / App Store.',
                            textAlign: TextAlign.center,
                            style: GH.body.copyWith(fontSize: 12),
                          ),
                          TextButton(
                            onPressed: p.busy ? null : p.restore,
                            child: const Text(
                              'Restore purchases',
                              style: TextStyle(color: GH.textHi),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  String? _perMonth(ProductDetails? p) {
    if (p == null) return null;
    final m = p.rawPrice / 12;
    return '${p.currencySymbol}${m.toStringAsFixed(2)}';
  }
}

class _FeatureRow extends StatelessWidget {
  const _FeatureRow({
    required this.animation,
    required this.icon,
    required this.title,
    required this.subtitle,
  });
  final Animation<double> animation;
  final IconData icon;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: animation,
    builder: (_, child) => Opacity(
      opacity: animation.value,
      child: Transform.translate(
        offset: Offset(0, 16 * (1 - animation.value)),
        child: child,
      ),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(13),
              gradient: LinearGradient(
                colors: [
                  GH.gold.withValues(alpha: 0.25),
                  GH.amber.withValues(alpha: 0.08),
                ],
              ),
              border: Border.all(color: GH.gold.withValues(alpha: 0.3)),
            ),
            child: Icon(icon, color: GH.goldLight, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
                Text(subtitle, style: GH.body.copyWith(fontSize: 12.5)),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({
    required this.selected,
    required this.title,
    required this.price,
    required this.per,
    required this.note,
    required this.onTap,
    this.badge,
  });
  final bool selected;
  final String title;
  final String price;
  final String per;
  final String note;
  final String? badge;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Pressable(
    onTap: onTap,
    child: Stack(
      clipBehavior: Clip.none,
      children: [
        AnimatedContainer(
          duration: const Duration(milliseconds: 260),
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            color: selected
                ? GH.gold.withValues(alpha: 0.12)
                : Colors.white.withValues(alpha: 0.05),
            border: Border.all(
              color: selected ? GH.gold : Colors.white12,
              width: selected ? 1.6 : 1,
            ),
            boxShadow: selected
                ? [
                    BoxShadow(
                      color: GH.gold.withValues(alpha: 0.25),
                      blurRadius: 24,
                    ),
                  ]
                : null,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                    ),
                  ),
                  const Spacer(),
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 260),
                    width: 20,
                    height: 20,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: selected ? GH.goldGradient : null,
                      border: Border.all(
                        color: selected ? Colors.transparent : Colors.white38,
                        width: 1.5,
                      ),
                    ),
                    child: selected
                        ? const Icon(
                            Icons.check_rounded,
                            size: 14,
                            color: GH.ink,
                          )
                        : null,
                  ),
                ],
              ),
              const SizedBox(height: 10),
              FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Text.rich(
                  TextSpan(
                    children: [
                      TextSpan(
                        text: price,
                        style: GH.value.copyWith(fontSize: 22),
                      ),
                      TextSpan(
                        text: ' $per',
                        style: GH.body.copyWith(fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                note,
                style: GH.body.copyWith(fontSize: 12),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
        if (badge != null)
          Positioned(
            top: -10,
            left: 14,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                gradient: GH.goldGradient,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                badge!,
                style: const TextStyle(
                  color: GH.ink,
                  fontSize: 9.5,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.8,
                ),
              ),
            ),
          ),
      ],
    ),
  );
}
