import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../screens/paywall_screen.dart';
import '../services/app_state.dart';
import '../services/services.dart';
import '../theme.dart';
import '../util/format.dart';

/// Frosted glass panel with a fine light border (like real glass edges).
class Glass extends StatelessWidget {
  const Glass({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(18),
    this.radius = 24,
    this.blur = 22,
    this.tint,
    this.opacity = 0.10,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final double radius;
  final double blur;
  final Color? tint;
  final double opacity;

  @override
  Widget build(BuildContext context) {
    final r = BorderRadius.circular(radius);
    return ClipRRect(
      borderRadius: r,
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: r,
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                (tint ?? Colors.white).withValues(alpha: opacity + 0.05),
                (tint ?? Colors.white).withValues(alpha: opacity * 0.4),
              ],
            ),
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.14),
              width: 0.8,
            ),
          ),
          child: Padding(padding: padding, child: child),
        ),
      ),
    );
  }
}

/// Scales down slightly while pressed, with a haptic tick.
class Pressable extends StatefulWidget {
  const Pressable({
    super.key,
    required this.child,
    this.onTap,
    this.scale = 0.96,
  });
  final Widget child;
  final VoidCallback? onTap;
  final double scale;

  @override
  State<Pressable> createState() => _PressableState();
}

class _PressableState extends State<Pressable> {
  bool _down = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: widget.onTap == null
          ? null
          : (_) => setState(() => _down = true),
      onTapCancel: () => setState(() => _down = false),
      onTapUp: (_) => setState(() => _down = false),
      onTap: widget.onTap == null
          ? null
          : () {
              HapticFeedback.selectionClick();
              widget.onTap!();
            },
      child: AnimatedScale(
        scale: _down ? widget.scale : 1,
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}

/// Gold gradient call-to-action with a moving light sheen.
class GoldButton extends StatefulWidget {
  const GoldButton({
    super.key,
    required this.label,
    this.onTap,
    this.icon,
    this.busy = false,
  });
  final String label;
  final IconData? icon;
  final VoidCallback? onTap;
  final bool busy;

  @override
  State<GoldButton> createState() => _GoldButtonState();
}

class _GoldButtonState extends State<GoldButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 2600),
  )..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Pressable(
      onTap: widget.busy ? null : widget.onTap,
      child: Container(
        height: 58,
        decoration: BoxDecoration(
          gradient: GH.goldGradient,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: GH.amber.withValues(alpha: 0.45),
              blurRadius: 28,
              offset: const Offset(0, 10),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: Stack(
            fit: StackFit.expand,
            children: [
              AnimatedBuilder(
                animation: _c,
                builder: (_, _) => FractionalTranslation(
                  translation: Offset(_c.value * 3 - 1.5, 0),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          Colors.white.withValues(alpha: 0),
                          Colors.white.withValues(alpha: 0.35),
                          Colors.white.withValues(alpha: 0),
                        ],
                        stops: const [0.35, 0.5, 0.65],
                      ),
                    ),
                  ),
                ),
              ),
              Center(
                child: widget.busy
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.4,
                          color: GH.ink,
                        ),
                      )
                    : Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (widget.icon != null) ...[
                            Icon(widget.icon, color: GH.ink, size: 20),
                            const SizedBox(width: 8),
                          ],
                          Text(
                            widget.label,
                            style: const TextStyle(
                              color: GH.ink,
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 0.2,
                            ),
                          ),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class SectionLabel extends StatelessWidget {
  const SectionLabel(this.text, {super.key, this.trailing});
  final String text;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(4, 26, 4, 12),
    child: Row(
      children: [
        Text(text.toUpperCase(), style: GH.label),
        const Spacer(),
        ?trailing,
      ],
    ),
  );
}

class ProBadge extends StatelessWidget {
  const ProBadge({super.key, this.small = false});
  final bool small;

  @override
  Widget build(BuildContext context) => Container(
    padding: EdgeInsets.symmetric(
      horizontal: small ? 6 : 8,
      vertical: small ? 2 : 3,
    ),
    decoration: BoxDecoration(
      gradient: GH.goldGradient,
      borderRadius: BorderRadius.circular(6),
    ),
    child: Text(
      'PRO',
      style: TextStyle(
        color: GH.ink,
        fontSize: small ? 9 : 10,
        fontWeight: FontWeight.w800,
        letterSpacing: 1.2,
      ),
    ),
  );
}

Future<void> openPaywall(BuildContext context) => Navigator.of(context).push(
  PageRouteBuilder(
    opaque: false,
    transitionDuration: const Duration(milliseconds: 480),
    reverseTransitionDuration: const Duration(milliseconds: 320),
    pageBuilder: (_, _, _) => const PaywallScreen(),
    transitionsBuilder: (_, a, _, child) => FadeTransition(
      opacity: a,
      child: SlideTransition(
        position: Tween(
          begin: const Offset(0, 0.06),
          end: Offset.zero,
        ).animate(CurvedAnimation(parent: a, curve: Curves.easeOutCubic)),
        child: child,
      ),
    ),
  ),
);

/// Shows a rewarded ad and grants [feature] for 24 hours.
Future<bool> unlockWithAd(BuildContext context, RewardFeature feature) async {
  final s = ServicesScope.of(context);
  final messenger = ScaffoldMessenger.of(context);
  final ok = await s.ads.showRewarded();
  if (ok) {
    await s.state.grantReward(feature);
    if (feature == RewardFeature.extraAlert) await s.notifications.reschedule();
    messenger.showSnackBar(
      SnackBar(content: Text('✨ ${feature.label} unlocked for 24 hours')),
    );
  } else {
    messenger.showSnackBar(
      const SnackBar(
        content: Text('Ad not ready yet — please try again in a moment'),
      ),
    );
  }
  return ok;
}

/// Returns true if the user has (or just got) access. [reward] null = Pro only.
Future<bool> ensureAccess(
  BuildContext context, {
  RewardFeature? reward,
  required String title,
}) async {
  final state = AppScope.read(context);
  if (reward == null ? state.isPro : state.hasAccess(reward)) return true;
  final choice = await showModalBottomSheet<String>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (ctx) => Padding(
      padding: const EdgeInsets.all(12),
      child: Glass(
        radius: 28,
        tint: const Color(0xFF10131F),
        opacity: 0.8,
        padding: const EdgeInsets.fromLTRB(22, 24, 22, 18),
        child: SafeArea(
          top: false,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.lock_open_rounded,
                color: GH.goldLight,
                size: 30,
              ),
              const SizedBox(height: 10),
              Text(title, style: GH.title, textAlign: TextAlign.center),
              const SizedBox(height: 6),
              Text(
                reward == null ? 'This is a Pro feature.' : 'Watch a short ad to unlock it for 24 hours, or go Pro for unlimited access.',
                style: GH.body,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 20),
              if (reward != null) ...[
                Pressable(
                  onTap: () => Navigator.pop(ctx, 'ad'),
                  child: Container(
                    height: 54,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: Colors.white24),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.play_circle_fill_rounded, color: GH.textHi),
                        SizedBox(width: 8),
                        Text(
                          'Watch ad · unlock 24h',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 10),
              ],
              GoldButton(
                label: 'Go Pro',
                icon: Icons.workspace_premium_rounded,
                onTap: () => Navigator.pop(ctx, 'pro'),
              ),
            ],
          ),
        ),
      ),
    ),
  );
  if (!context.mounted) return false;
  if (choice == 'ad' && reward != null) return unlockWithAd(context, reward);
  if (choice == 'pro') {
    await openPaywall(context);
    if (!context.mounted) return false;
    return AppScope.read(context).isPro;
  }
  return false;
}

/// Shows [child] when the user has access; otherwise a blurred preview with
/// "Watch ad (24h)" and/or "Go Pro" actions.
class FeatureGate extends StatelessWidget {
  const FeatureGate({
    super.key,
    required this.child,
    required this.title,
    this.reward,
    this.description,
    this.minHeight = 160,
  });

  final Widget child;
  final String title;
  final String? description;

  /// If null, the feature is Pro-only.
  final RewardFeature? reward;
  final double minHeight;

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final open = reward == null ? state.isPro : state.hasAccess(reward!);
    if (open) {
      final left = reward == null || state.isPro
          ? null
          : state.unlockRemaining(reward!);
      return Stack(
        children: [
          child,
          if (left != null)
            Positioned(
              top: 10,
              right: 12,
              child: Text(
                'Unlocked · ${fmtDuration(left)} left',
                style: GH.label.copyWith(fontSize: 9, color: GH.goldLight),
              ),
            ),
        ],
      );
    }
    return ConstrainedBox(
      constraints: BoxConstraints(minHeight: minHeight),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Stack(
          children: [
            IgnorePointer(
              child: ImageFiltered(
                imageFilter: ImageFilter.blur(sigmaX: 9, sigmaY: 9),
                child: child,
              ),
            ),
            Positioned.fill(
              child: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      GH.ink.withValues(alpha: 0.25),
                      GH.ink.withValues(alpha: 0.75),
                    ],
                  ),
                ),
                padding: const EdgeInsets.all(18),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(
                      Icons.lock_rounded,
                      color: GH.goldLight,
                      size: 26,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      title,
                      style: GH.title.copyWith(fontSize: 18),
                      textAlign: TextAlign.center,
                    ),
                    if (description != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        description!,
                        style: GH.body.copyWith(fontSize: 13),
                        textAlign: TextAlign.center,
                      ),
                    ],
                    const SizedBox(height: 14),
                    Wrap(
                      spacing: 10,
                      runSpacing: 10,
                      alignment: WrapAlignment.center,
                      children: [
                        if (reward != null)
                          _ChipButton(
                            icon: Icons.play_circle_fill_rounded,
                            label: 'Watch ad · 24h',
                            onTap: () => unlockWithAd(context, reward!),
                          ),
                        _ChipButton(
                          icon: Icons.workspace_premium_rounded,
                          label: 'Go Pro',
                          gold: true,
                          onTap: () => openPaywall(context),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChipButton extends StatelessWidget {
  const _ChipButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.gold = false,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool gold;

  @override
  Widget build(BuildContext context) => Pressable(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
      decoration: BoxDecoration(
        gradient: gold ? GH.goldGradient : null,
        color: gold ? null : Colors.white.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(14),
        border: gold ? null : Border.all(color: Colors.white24, width: 0.8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 18, color: gold ? GH.ink : GH.textHi),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              color: gold ? GH.ink : GH.textHi,
              fontWeight: FontWeight.w700,
              fontSize: 13.5,
            ),
          ),
        ],
      ),
    ),
  );
}

/// Small stat with label + value.
class Stat extends StatelessWidget {
  const Stat({
    super.key,
    required this.label,
    required this.value,
    this.icon,
    this.color,
  });
  final String label;
  final String value;
  final IconData? icon;
  final Color? color;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    mainAxisSize: MainAxisSize.min,
    children: [
      Row(
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: color ?? GH.textLo),
            const SizedBox(width: 5),
          ],
          Flexible(
            child: Text(
              label.toUpperCase(),
              style: GH.label.copyWith(fontSize: 9.5),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
      const SizedBox(height: 6),
      Text(value, style: GH.value.copyWith(fontSize: 17)),
    ],
  );
}
