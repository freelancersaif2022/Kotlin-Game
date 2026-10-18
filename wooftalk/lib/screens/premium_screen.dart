import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../main.dart';
import '../models/voice.dart';
import '../services/app_state.dart';
import '../theme/app_theme.dart';
import '../widgets/animal_character.dart';
import '../widgets/common.dart';
import '../widgets/popups.dart';
import 'unlock_dialog.dart';

Route<void> premiumRoute() => PageRouteBuilder(
  transitionDuration: const Duration(milliseconds: 500),
  reverseTransitionDuration: const Duration(milliseconds: 350),
  pageBuilder: (_, _, _) => const PremiumScreen(),
  transitionsBuilder: (_, a, _, child) => SlideTransition(
    position: Tween(
      begin: const Offset(0, 1),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: a, curve: Curves.easeOutCubic)),
    child: child,
  ),
);

class _PlanInfo {
  const _PlanInfo(
    this.plan,
    this.title,
    this.price,
    this.period,
    this.sub, {
    this.badge,
  });

  final PremiumPlan plan;
  final String title, price, period, sub;
  final String? badge;
}

const _plans = [
  _PlanInfo(
    PremiumPlan.yearly,
    'Yearly',
    '\$19.99',
    '/year',
    'Just \$1.67 per month',
    badge: 'SAVE 44%',
  ),
  _PlanInfo(
    PremiumPlan.monthly,
    'Monthly',
    '\$2.99',
    '/month',
    'Flexible, cancel anytime',
  ),
];

class PremiumScreen extends StatefulWidget {
  const PremiumScreen({super.key});

  @override
  State<PremiumScreen> createState() => _PremiumScreenState();
}

class _PremiumScreenState extends State<PremiumScreen>
    with SingleTickerProviderStateMixin {
  PremiumPlan _selected = PremiumPlan.yearly;
  late final AnimationController _shine = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 3),
  )..repeat();

  @override
  void dispose() {
    _shine.dispose();
    super.dispose();
  }

  Future<void> _buy() async {
    final state = AppScope.read(context);
    final nav = Navigator.of(context);
    // Mock checkout. Swap for Google Play Billing (in_app_purchase) to go live.
    showLoadingPopup(context, 'Processing payment…');
    await Future.delayed(const Duration(milliseconds: 1700));
    if (!mounted) return;
    nav.pop();
    state.activatePremium(_selected);
    await showCelebration(
      context,
      hero: const Icon(
        Icons.workspace_premium_rounded,
        color: Colors.white,
        size: 58,
      ),
      title: 'Welcome to PRO!',
      message: 'All voices unlocked and ads removed. Have fun pranking! 🐾',
    );
    if (mounted) nav.pop();
  }

  void _restore() async {
    final state = AppScope.read(context);
    showLoadingPopup(context, 'Restoring…');
    await Future.delayed(const Duration(milliseconds: 1200));
    if (!mounted) return;
    Navigator.pop(context);
    if (state.isPremium) {
      showAppSnack(
        context,
        'Your PRO subscription is active.',
        type: SnackType.premium,
        title: 'Restored',
      );
    } else {
      showAppSnack(
        context,
        'No previous purchases were found on this account.',
        type: SnackType.info,
        title: 'Nothing to restore',
      );
    }
  }

  void _cancel() {
    showFancyDialog(
      context,
      builder: (ctx) => AppDialog(
        heroBackground: AppColors.purpleGradient,
        hero: const AnimalCharacter(voiceId: 'puppy', size: 96, shadow: false),
        title: 'Cancel PRO?',
        message: 'You will lose premium voices and ads will come back.',
        actions: [
          GradientButton(
            label: 'Keep PRO',
            gradient: AppColors.goldGradient,
            onTap: () => Navigator.pop(ctx),
          ),
          TextLinkButton(
            'Cancel subscription',
            color: AppColors.danger,
            onTap: () {
              Navigator.pop(ctx);
              AppScope.read(context).cancelPremium();
              showAppSnack(
                context,
                'Your subscription was cancelled.',
                type: SnackType.info,
                title: 'Done',
              );
            },
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.stateOf(context);
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF3B1E8F), Color(0xFF6A3DE8), Color(0xFFFF6F91)],
            stops: [0, 0.55, 1],
          ),
        ),
        child: Stack(
          children: [
            Positioned.fill(
              child: AnimatedBuilder(
                animation: _shine,
                builder: (_, _) =>
                    CustomPaint(painter: _StarsPainter(_shine.value)),
              ),
            ),
            SafeArea(
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(14, 8, 14, 0),
                    child: Row(
                      children: [
                        CircleIconButton(
                          icon: Icons.close_rounded,
                          onTap: () => Navigator.pop(context),
                          size: 42,
                          color: Colors.white,
                          background: Colors.white.withValues(alpha: 0.18),
                        ),
                        const Spacer(),
                        TextLinkButton(
                          'Restore',
                          color: Colors.white,
                          onTap: _restore,
                        ),
                      ],
                    ),
                  ),
                  Expanded(
                    child: SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(22, 0, 22, 12),
                      child: Column(
                        children: [
                          _hero(),
                          const SizedBox(height: 10),
                          Text(
                            'WoofTalk PRO',
                            style: AppTheme.title(34, color: Colors.white),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Every voice. Zero ads. Endless laughs.',
                            textAlign: TextAlign.center,
                            style: AppTheme.label(
                              15.5,
                              color: Colors.white.withValues(alpha: 0.85),
                            ),
                          ),
                          const SizedBox(height: 16),
                          _voiceRow(),
                          const SizedBox(height: 16),
                          _features(),
                          const SizedBox(height: 20),
                          if (state.isPremium)
                            _activeCard(state)
                          else
                            for (final p in _plans) ...[
                              _planCard(p),
                              const SizedBox(height: 12),
                            ],
                        ],
                      ),
                    ),
                  ),
                  if (!state.isPremium)
                    Padding(
                      padding: const EdgeInsets.fromLTRB(22, 4, 22, 6),
                      child: GradientButton(
                        label: _selected == PremiumPlan.yearly
                            ? 'Start Yearly PRO'
                            : 'Start Monthly PRO',
                        icon: Icons.workspace_premium_rounded,
                        gradient: AppColors.goldGradient,
                        onTap: _buy,
                      ),
                    ),
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10, top: 4),
                    child: Text(
                      'Auto-renews until cancelled · Cancel anytime',
                      style: AppTheme.label(
                        12,
                        color: Colors.white.withValues(alpha: 0.75),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _hero() {
    return SizedBox(
      height: 160,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            width: 132,
            height: 132,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: AppColors.goldGradient,
              border: Border.all(color: Colors.white, width: 5),
              boxShadow: [
                BoxShadow(
                  color: AppColors.gold.withValues(alpha: 0.6),
                  blurRadius: 30,
                ),
              ],
            ),
            child: const Center(
              child: AnimalCharacter(voiceId: 'dog', size: 116, shadow: false),
            ),
          ),
          Positioned(
            top: 0,
            child: Transform.rotate(
              angle: -0.15,
              child: const Icon(
                Icons.workspace_premium_rounded,
                color: AppColors.gold,
                size: 44,
                shadows: [
                  Shadow(
                    color: Colors.black26,
                    blurRadius: 8,
                    offset: Offset(0, 3),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Row of the five premium voices.
  Widget _voiceRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        for (final v in voices.where((v) => !v.free))
          Container(
            width: 54,
            height: 54,
            margin: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(
              color: v.accent,
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white, width: 3),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.15),
                  blurRadius: 8,
                ),
              ],
            ),
            child: Center(
              child: AnimalCharacter(voiceId: v.id, size: 50, shadow: false),
            ),
          ),
      ],
    );
  }

  Widget _features() {
    const items = [
      (Icons.lock_open_rounded, 'Unlock all 5 premium voices'),
      (Icons.block_rounded, '100% ad-free, no videos ever'),
      (Icons.auto_awesome_rounded, 'New voices first, free forever'),
      (Icons.favorite_rounded, 'Support future updates'),
    ];
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
      ),
      child: Column(
        children: [
          for (final (icon, text) in items)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(7),
                    decoration: const BoxDecoration(
                      gradient: AppColors.goldGradient,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(icon, color: Colors.white, size: 18),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      text,
                      style: AppTheme.label(15.5, color: Colors.white),
                    ),
                  ),
                  const Icon(
                    Icons.check_circle_rounded,
                    color: AppColors.mint,
                    size: 22,
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _planCard(_PlanInfo p) {
    final sel = _selected == p.plan;
    return Pressable(
      onTap: () => setState(() => _selected = p.plan),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        padding: const EdgeInsets.fromLTRB(16, 16, 18, 16),
        decoration: BoxDecoration(
          color: sel ? Colors.white : Colors.white.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: sel ? AppColors.gold : Colors.white.withValues(alpha: 0.3),
            width: sel ? 3 : 1.5,
          ),
          boxShadow: sel
              ? [
                  BoxShadow(
                    color: AppColors.gold.withValues(alpha: 0.45),
                    blurRadius: 22,
                  ),
                ]
              : null,
        ),
        child: Row(
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 250),
              width: 26,
              height: 26,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: sel ? AppColors.goldGradient : null,
                border: sel
                    ? null
                    : Border.all(color: Colors.white70, width: 2),
              ),
              child: sel
                  ? const Icon(
                      Icons.check_rounded,
                      color: Colors.white,
                      size: 18,
                    )
                  : null,
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        p.title,
                        style: AppTheme.title(
                          19,
                          color: sel ? AppColors.ink : Colors.white,
                        ),
                      ),
                      if (p.badge != null) ...[
                        const SizedBox(width: 8),
                        Badge2(
                          p.badge!,
                          icon: Icons.local_fire_department_rounded,
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    p.sub,
                    style: AppTheme.label(
                      13,
                      color: sel ? AppColors.inkSoft : Colors.white70,
                    ),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  p.price,
                  style: AppTheme.title(
                    21,
                    color: sel ? AppColors.ink : Colors.white,
                  ),
                ),
                Text(
                  p.period,
                  style: AppTheme.label(
                    12,
                    color: sel ? AppColors.inkSoft : Colors.white70,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _activeCard(AppState state) {
    final until = state.premiumUntil!;
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    final date = '${months[until.month - 1]} ${until.day}, ${until.year}';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: AppColors.gold.withValues(alpha: 0.5),
            blurRadius: 24,
          ),
        ],
      ),
      child: Column(
        children: [
          const Badge2('ACTIVE', icon: Icons.verified_rounded),
          const SizedBox(height: 10),
          Text("You're PRO! 🎉", style: AppTheme.title(24)),
          const SizedBox(height: 4),
          Text(
            '${state.plan == PremiumPlan.yearly ? 'Yearly' : 'Monthly'} plan · renews $date',
            style: AppTheme.label(14, color: AppColors.inkSoft),
          ),
          const SizedBox(height: 6),
          TextLinkButton(
            'Manage subscription',
            color: AppColors.purple,
            onTap: _cancel,
          ),
        ],
      ),
    );
  }
}

class _StarsPainter extends CustomPainter {
  _StarsPainter(this.t);

  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    final rng = math.Random(7);
    for (var i = 0; i < 40; i++) {
      final x = rng.nextDouble() * size.width;
      final y = rng.nextDouble() * size.height * 0.7;
      final tw = 0.5 + 0.5 * math.sin((t + rng.nextDouble()) * 2 * math.pi);
      canvas.drawCircle(
        Offset(x, y),
        1 + rng.nextDouble() * 1.8,
        Paint()..color = Colors.white.withValues(alpha: 0.15 + tw * 0.5),
      );
    }
  }

  @override
  bool shouldRepaint(_StarsPainter old) => old.t != t;
}
