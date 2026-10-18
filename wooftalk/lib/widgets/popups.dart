import 'dart:ui';

import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import 'common.dart';

enum SnackType { success, error, info, premium }

/// Floating, rounded, gradient snackbar with an icon bubble.
void showAppSnack(
  BuildContext context,
  String message, {
  SnackType type = SnackType.info,
  String? title,
}) {
  final (Gradient g, IconData icon) = switch (type) {
    SnackType.success => (
      const LinearGradient(colors: [Color(0xFF2BD9A6), Color(0xFF14B07F)]),
      Icons.check_rounded,
    ),
    SnackType.error => (
      const LinearGradient(colors: [Color(0xFFFF6B7D), Color(0xFFE8335A)]),
      Icons.priority_high_rounded,
    ),
    SnackType.premium => (
      AppColors.goldGradient,
      Icons.workspace_premium_rounded,
    ),
    SnackType.info => (AppColors.purpleGradient, Icons.pets_rounded),
  };
  final messenger = ScaffoldMessenger.of(context);
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(
    SnackBar(
      behavior: SnackBarBehavior.floating,
      backgroundColor: Colors.transparent,
      elevation: 0,
      padding: EdgeInsets.zero,
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 18),
      duration: const Duration(milliseconds: 2800),
      content: TweenAnimationBuilder<double>(
        tween: Tween(begin: 0.85, end: 1),
        duration: const Duration(milliseconds: 450),
        curve: Curves.elasticOut,
        builder: (_, s, child) => Transform.scale(scale: s, child: child),
        child: Container(
          padding: const EdgeInsets.fromLTRB(10, 10, 16, 10),
          decoration: BoxDecoration(
            gradient: g,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: g.colors.last.withValues(alpha: 0.4),
                blurRadius: 18,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.25),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: Colors.white, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (title != null)
                      Text(
                        title,
                        style: AppTheme.title(15, color: Colors.white),
                      ),
                    Text(
                      message,
                      style: AppTheme.label(
                        13.5,
                        color: Colors.white.withValues(alpha: 0.95),
                        weight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

/// Opens a popup with a blurred backdrop and a bouncy scale-in.
Future<T?> showFancyDialog<T>(
  BuildContext context, {
  required WidgetBuilder builder,
  bool dismissible = true,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: dismissible,
    barrierLabel: 'dialog',
    barrierColor: AppColors.ink.withValues(alpha: 0.35),
    transitionDuration: const Duration(milliseconds: 420),
    pageBuilder: (ctx, _, _) => builder(ctx),
    transitionBuilder: (ctx, anim, _, child) {
      final curved = CurvedAnimation(
        parent: anim,
        curve: Curves.elasticOut,
        reverseCurve: Curves.easeIn,
      );
      return BackdropFilter(
        filter: ImageFilter.blur(
          sigmaX: 6 * anim.value,
          sigmaY: 6 * anim.value,
        ),
        child: FadeTransition(
          opacity: CurvedAnimation(parent: anim, curve: Curves.easeOut),
          child: ScaleTransition(
            scale: Tween(begin: 0.7, end: 1.0).animate(curved),
            child: child,
          ),
        ),
      );
    },
  );
}

/// Card used by every popup: a floating "hero" on top, title, body, actions.
class AppDialog extends StatelessWidget {
  const AppDialog({
    super.key,
    required this.hero,
    required this.title,
    this.message,
    this.body,
    this.actions = const [],
    this.heroBackground = AppColors.primaryGradient,
  });

  final Widget hero;
  final String title;
  final String? message;
  final Widget? body;
  final List<Widget> actions;
  final Gradient heroBackground;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 26),
        child: Material(
          color: Colors.transparent,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Stack(
              clipBehavior: Clip.none,
              alignment: Alignment.topCenter,
              children: [
                Container(
                  margin: const EdgeInsets.only(top: 56),
                  padding: const EdgeInsets.fromLTRB(22, 70, 22, 20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(32),
                    boxShadow: AppTheme.softShadow(),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: AppTheme.title(24),
                      ),
                      if (message != null) ...[
                        const SizedBox(height: 8),
                        Text(
                          message!,
                          textAlign: TextAlign.center,
                          style: AppTheme.label(
                            15,
                            color: AppColors.inkSoft,
                            weight: FontWeight.w600,
                          ),
                        ),
                      ],
                      if (body != null) ...[const SizedBox(height: 16), body!],
                      if (actions.isNotEmpty) ...[
                        const SizedBox(height: 20),
                        for (var i = 0; i < actions.length; i++) ...[
                          if (i > 0) const SizedBox(height: 10),
                          actions[i],
                        ],
                      ],
                    ],
                  ),
                ),
                Container(
                  width: 112,
                  height: 112,
                  decoration: BoxDecoration(
                    gradient: heroBackground,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 6),
                    boxShadow: AppTheme.softShadow(heroBackground.colors.last),
                  ),
                  child: Center(child: hero),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class TextLinkButton extends StatelessWidget {
  const TextLinkButton(
    this.label, {
    super.key,
    required this.onTap,
    this.color = AppColors.inkSoft,
  });

  final String label;
  final VoidCallback onTap;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Pressable(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
        child: Text(
          label,
          style: AppTheme.label(15, color: color, weight: FontWeight.w800),
        ),
      ),
    );
  }
}

/// Small blocking "loading" popup (e.g. while an ad or purchase loads).
Future<void> showLoadingPopup(BuildContext context, String text) {
  return showFancyDialog(
    context,
    dismissible: false,
    builder: (_) => PopScope(
      canPop: false,
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(26),
            boxShadow: AppTheme.softShadow(),
          ),
          child: Material(
            color: Colors.transparent,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox.square(
                  dimension: 42,
                  child: CircularProgressIndicator(
                    strokeWidth: 4.5,
                    strokeCap: StrokeCap.round,
                    color: AppColors.orange,
                  ),
                ),
                const SizedBox(height: 16),
                Text(text, style: AppTheme.title(17)),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}
