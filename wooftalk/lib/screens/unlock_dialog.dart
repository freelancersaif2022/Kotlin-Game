import 'package:flutter/material.dart';

import '../main.dart';
import '../models/voice.dart';
import '../services/ad_service.dart';
import '../services/app_state.dart';
import '../theme/app_theme.dart';
import '../widgets/common.dart';
import '../widgets/home_widgets.dart';
import '../widgets/popups.dart';
import '../widgets/synced_character.dart';
import 'premium_screen.dart';

/// "Watch 2 ads to unlock" popup. Resolves to true when the voice unlocked.
Future<bool?> showUnlockDialog(BuildContext context, AnimalVoice voice) {
  return showFancyDialog<bool>(
    context,
    builder: (_) => _UnlockDialog(voice: voice),
  );
}

class _UnlockDialog extends StatefulWidget {
  const _UnlockDialog({required this.voice});

  final AnimalVoice voice;

  @override
  State<_UnlockDialog> createState() => _UnlockDialogState();
}

class _UnlockDialogState extends State<_UnlockDialog> {
  bool _loadingAd = false;
  bool _previewing = false;

  AnimalVoice get v => widget.voice;

  Future<void> _preview() async {
    if (_previewing) return;
    setState(() => _previewing = true);
    final engine = AppScope.engineOf(context);
    await engine.preview(v.id);
    await engine.play();
    if (mounted) setState(() => _previewing = false);
  }

  Future<void> _watchAd() async {
    if (_loadingAd) return;
    final engine = AppScope.engineOf(context);
    await engine.stopPlayback();
    setState(() => _loadingAd = true);
    final ready = await AdService.instance.ensureRewarded();
    if (!mounted) return;
    if (!ready) {
      setState(() => _loadingAd = false);
      showAppSnack(
        context,
        'No video available right now. Check your internet and try again.',
        type: SnackType.error,
        title: 'Ad not ready',
      );
      return;
    }
    final earned = await AdService.instance.showRewarded();
    if (!mounted) return;
    setState(() => _loadingAd = false);
    if (!earned) {
      showAppSnack(
        context,
        'Watch the full video to earn your reward.',
        type: SnackType.info,
        title: 'Almost!',
      );
      return;
    }
    final state = AppScope.read(context);
    final unlocked = state.addRewardFor(v);
    if (unlocked) {
      Navigator.pop(context, true);
      return;
    }
    final left = AppState.adsToUnlock - state.adProgress(v.id);
    showAppSnack(
      context,
      '$left more video to unlock ${v.name}!',
      type: SnackType.success,
      title: 'Nice! 1 of 2 done',
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.stateOf(context);
    final progress = state.adProgress(v.id).clamp(0, AppState.adsToUnlock);
    final engine = AppScope.engineOf(context);
    return AppDialog(
      heroBackground: LinearGradient(
        colors: [v.accent, v.color],
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      ),
      hero: SyncedCharacter(
        engine: engine,
        voiceId: v.id,
        size: 96,
        shadow: false,
      ),
      title: 'Unlock ${v.name}',
      message: 'Watch 2 short videos to unlock this voice forever.',
      body: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var i = 0; i < AppState.adsToUnlock; i++) ...[
                if (i > 0)
                  Container(
                    width: 34,
                    height: 4,
                    margin: const EdgeInsets.symmetric(horizontal: 6),
                    decoration: BoxDecoration(
                      color: i < progress
                          ? AppColors.mint
                          : AppColors.inkSoft.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                _Step(index: i + 1, done: i < progress, color: v.color),
              ],
            ],
          ),
          const SizedBox(height: 14),
          Pressable(
            onTap: _preview,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
              decoration: BoxDecoration(
                color: v.accent,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _previewing
                      ? SizedBox.square(
                          dimension: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            color: v.color,
                          ),
                        )
                      : Icon(Icons.play_arrow_rounded, color: v.color),
                  const SizedBox(width: 6),
                  Text(
                    'Preview voice',
                    style: AppTheme.title(15, color: v.color),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      actions: [
        GradientButton(
          label: 'Watch video (${progress + 1} of ${AppState.adsToUnlock})',
          icon: Icons.play_circle_fill_rounded,
          loading: _loadingAd,
          onTap: _watchAd,
        ),
        GradientButton(
          label: 'Unlock all with PRO',
          icon: Icons.workspace_premium_rounded,
          gradient: AppColors.goldGradient,
          height: 50,
          onTap: () async {
            final nav = Navigator.of(context);
            final state = AppScope.read(context);
            await nav.push(premiumRoute());
            if (mounted && state.isUnlocked(v)) nav.pop(false);
          },
        ),
        TextLinkButton('Maybe later', onTap: () => Navigator.pop(context)),
      ],
    );
  }
}

class _Step extends StatelessWidget {
  const _Step({required this.index, required this.done, required this.color});

  final int index;
  final bool done;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 400),
      curve: Curves.elasticOut,
      width: done ? 44 : 40,
      height: done ? 44 : 40,
      decoration: BoxDecoration(
        color: done ? AppColors.mint : Colors.white,
        shape: BoxShape.circle,
        border: Border.all(
          color: done
              ? AppColors.mint
              : AppColors.inkSoft.withValues(alpha: 0.25),
          width: 2.5,
        ),
        boxShadow: done ? AppTheme.softShadow(AppColors.mint) : null,
      ),
      child: Center(
        child: done
            ? const Icon(Icons.check_rounded, color: Colors.white)
            : Text(
                '$index',
                style: AppTheme.title(17, color: AppColors.inkSoft),
              ),
      ),
    );
  }
}

/// Celebration popup after a voice or PRO gets unlocked.
Future<void> showCelebration(
  BuildContext context, {
  required Widget hero,
  required String title,
  required String message,
  Gradient? gradient,
}) {
  return showFancyDialog(
    context,
    builder: (ctx) => Stack(
      children: [
        AppDialog(
          heroBackground: gradient ?? AppColors.goldGradient,
          hero: hero,
          title: title,
          message: message,
          actions: [
            GradientButton(label: 'Awesome!', onTap: () => Navigator.pop(ctx)),
          ],
        ),
        const Positioned.fill(child: ConfettiBurst()),
      ],
    ),
  );
}
