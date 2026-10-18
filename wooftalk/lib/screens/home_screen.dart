import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:share_plus/share_plus.dart';

import '../main.dart';
import '../models/voice.dart';
import '../services/ad_service.dart';
import '../services/app_state.dart';
import '../services/voice_engine.dart';
import '../theme/app_theme.dart';
import '../widgets/animal_character.dart';
import '../widgets/common.dart';
import '../widgets/home_widgets.dart';
import '../widgets/popups.dart';
import '../widgets/synced_character.dart';
import 'premium_screen.dart';
import 'unlock_dialog.dart';

enum _Phase { idle, recording, processing, result }

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with TickerProviderStateMixin {
  _Phase _phase = _Phase.idle;
  final _levels = List<double>.filled(24, 0);
  StreamSubscription<double>? _levelSub;
  late final Ticker _timer = createTicker((_) {
    if (!mounted || _phase != _Phase.recording) return;
    final secs = _engine.recordedSeconds;
    if (secs >= VoiceEngine.maxSeconds) {
      _stop();
    } else if ((secs * 10).floor() != (_elapsed * 10).floor()) {
      setState(() => _elapsed = secs);
    }
  });
  double _elapsed = 0;
  String? _caption;
  final _rng = math.Random();
  bool _busy = false;

  VoiceEngine get _engine => AppScope.engineOf(context);

  @override
  void dispose() {
    _timer.dispose();
    _levelSub?.cancel();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Recording flow
  // ---------------------------------------------------------------------------

  Future<void> _onMic() async {
    if (_busy) return;
    if (_phase == _Phase.recording) return _stop();
    _busy = true;
    try {
      final allowed = await _engine.ensurePermission();
      if (!mounted) return;
      if (!allowed) {
        _showPermissionPopup();
        return;
      }
      await _engine.startRecording();
      HapticFeedback.mediumImpact();
      _levelSub?.cancel();
      _levelSub = _engine.levels.listen((l) {
        if (!mounted) return;
        setState(() {
          _levels.removeAt(0);
          _levels.add(l);
        });
      });
      setState(() {
        _phase = _Phase.recording;
        _elapsed = 0;
        _caption = null;
      });
      _timer.start();
    } catch (e) {
      if (mounted) {
        showAppSnack(
          context,
          'Microphone is busy. Please try again.',
          type: SnackType.error,
          title: 'Oops!',
        );
      }
    } finally {
      _busy = false;
    }
  }

  Future<void> _stop() async {
    if (_phase != _Phase.recording || _busy) return;
    _busy = true;
    _timer.stop();
    await _levelSub?.cancel();
    _levelSub = null;
    _levels.fillRange(0, _levels.length, 0);
    HapticFeedback.mediumImpact();
    final secs = _engine.recordedSeconds;
    final pcm = await _engine.stopRecording();
    if (!mounted) return;
    if (secs < 1.0) {
      setState(() => _phase = _Phase.idle);
      _busy = false;
      showAppSnack(
        context,
        'Hold on! Say a few words before tapping stop.',
        type: SnackType.info,
        title: 'Too short',
      );
      return;
    }

    final state = AppScope.read(context);
    final voice = state.selected;
    setState(() => _phase = _Phase.processing);

    final translating = _engine.translateSpeech(pcm, voice.id);
    await Future.delayed(Duration(milliseconds: 2300 + _rng.nextInt(500)));
    final result = await translating;
    if (!mounted) return;
    if (result == null) {
      setState(() => _phase = _Phase.idle);
      _busy = false;
      showAppSnack(
        context,
        "We couldn't hear you. Speak a little louder and closer to the mic.",
        type: SnackType.error,
        title: 'Nothing heard',
      );
      return;
    }

    final showAd = state.registerTranslation();
    if (showAd) await AdService.instance.showInterstitial();
    if (!mounted) return;
    setState(() {
      _phase = _Phase.result;
      _caption = voice.caption(result.words, _rng);
    });
    _busy = false;
    await _engine.play();
  }

  // ---------------------------------------------------------------------------
  // Popups
  // ---------------------------------------------------------------------------

  void _showPermissionPopup() {
    showFancyDialog(
      context,
      builder: (ctx) => AppDialog(
        heroBackground: AppColors.purpleGradient,
        hero: const Icon(Icons.mic_rounded, color: Colors.white, size: 52),
        title: 'Mic access needed',
        message:
            'We need the microphone to hear what you say. Your voice is processed on your phone only. '
            'If no prompt appears, enable it in Settings › Apps › WoofTalk › Permissions.',
        actions: [
          GradientButton(
            label: 'Allow microphone',
            icon: Icons.mic_rounded,
            gradient: AppColors.purpleGradient,
            onTap: () {
              Navigator.pop(ctx);
              _onMic();
            },
          ),
          TextLinkButton('Not now', onTap: () => Navigator.pop(ctx)),
        ],
      ),
    );
  }

  void _showInfo() {
    showFancyDialog(
      context,
      builder: (ctx) => AppDialog(
        hero: const AnimalCharacter(voiceId: 'dog', size: 96, shadow: false),
        title: 'How it works',
        body: Column(
          children: const [
            _InfoRow(
              Icons.mic_rounded,
              'Tap the mic and talk in any language.',
            ),
            _InfoRow(Icons.stop_circle_rounded, 'Tap stop when you are done.'),
            _InfoRow(
              Icons.auto_awesome_rounded,
              'Your rhythm turns into a brand-new animal voice every time.',
            ),
            _InfoRow(
              Icons.lock_rounded,
              'Everything happens on your phone. Nothing is saved or uploaded.',
            ),
            _InfoRow(
              Icons.celebration_rounded,
              'Made for fun & pranks: pets don\'t really understand it!',
            ),
          ],
        ),
        actions: [
          GradientButton(label: 'Got it!', onTap: () => Navigator.pop(ctx)),
        ],
      ),
    );
  }

  Future<void> _onVoiceTap(AnimalVoice v) async {
    final state = AppScope.read(context);
    if (_phase == _Phase.recording || _phase == _Phase.processing) return;
    if (state.isUnlocked(v)) {
      if (state.selectedId == v.id) return;
      await _engine.stopPlayback();
      state.select(v);
      setState(() {
        _phase = _Phase.idle;
        _caption = null;
      });
      return;
    }
    await _engine.stopPlayback();
    if (!mounted) return;
    setState(() {
      _phase = _Phase.idle;
      _caption = null;
    });
    final unlocked = await showUnlockDialog(context, v);
    if (!mounted) return;
    if (unlocked == true) {
      setState(() {});
      await showCelebration(
        context,
        gradient: LinearGradient(colors: [v.accent, v.color]),
        hero: AnimalCharacter(voiceId: v.id, size: 96, shadow: false),
        title: 'Voice unlocked!',
        message: '${v.name} is yours forever. Tap the mic and give it a try!',
      );
      if (mounted) {
        showAppSnack(
          context,
          '${v.name} is now your active voice.',
          type: SnackType.success,
        );
      }
    }
  }

  void _openPremium() async {
    await _engine.stopPlayback();
    if (!mounted) return;
    await Navigator.of(context).push(premiumRoute());
  }

  Future<void> _share() async {
    final path = _engine.lastFile;
    if (path == null) return;
    final voice = AppScope.read(context).selected;
    try {
      await SharePlus.instance.share(
        ShareParams(
          files: [
            XFile(
              path,
              mimeType: 'audio/wav',
              name: 'WoofTalk_${voice.id}.wav',
            ),
          ],
          text: '${_caption ?? ''} 🐾 Translated with WoofTalk',
        ),
      );
    } catch (_) {
      if (mounted) {
        showAppSnack(
          context,
          'Sharing is not available right now.',
          type: SnackType.error,
        );
      }
    }
  }

  // ---------------------------------------------------------------------------
  // UI
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    final state = AppScope.stateOf(context);
    final voice = state.selected;
    final width = MediaQuery.sizeOf(context).width;
    return Scaffold(
      body: PlayfulBackground(
        child: Stack(
          children: [
            SafeArea(
              child: Column(
                children: [
                  _topBar(state.isPremium),
                  Expanded(child: _stage(voice, width)),
                  _controls(voice),
                  const SizedBox(height: 6),
                  _voicePicker(state),
                  const SizedBox(height: 12),
                ],
              ),
            ),
            if (_phase == _Phase.processing)
              Positioned.fill(
                child: TweenAnimationBuilder<double>(
                  tween: Tween(begin: 0, end: 1),
                  duration: const Duration(milliseconds: 300),
                  builder: (_, v, child) => Opacity(opacity: v, child: child),
                  child: ProcessingOverlay(voice: voice),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _topBar(bool premium) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 10, 18, 0),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              gradient: AppColors.primaryGradient,
              borderRadius: BorderRadius.circular(14),
              boxShadow: AppTheme.softShadow(AppColors.coral),
            ),
            child: const Icon(
              Icons.pets_rounded,
              color: Colors.white,
              size: 26,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('WoofTalk', style: AppTheme.title(24)),
                Text(
                  'Animal voice translator',
                  style: AppTheme.label(12.5, color: AppColors.inkSoft),
                ),
              ],
            ),
          ),
          CircleIconButton(
            icon: Icons.help_outline_rounded,
            onTap: _showInfo,
            size: 42,
          ),
          const SizedBox(width: 10),
          Pressable(
            onTap: _openPremium,
            child: Container(
              height: 42,
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(
                gradient: AppColors.goldGradient,
                borderRadius: BorderRadius.circular(21),
                boxShadow: AppTheme.softShadow(AppColors.goldDeep),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.workspace_premium_rounded,
                    color: Colors.white,
                    size: 20,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    premium ? 'PRO ✓' : 'PRO',
                    style: AppTheme.title(16, color: Colors.white),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _stage(AnimalVoice voice, double width) {
    final charSize = math.min(width * 0.6, 250.0);
    final mood = switch (_phase) {
      _Phase.recording => CharacterMood.listening,
      _Phase.processing => CharacterMood.thinking,
      _ => CharacterMood.idle,
    };
    final bubble = switch (_phase) {
      _Phase.recording => 'I\'m listening… 👂',
      _Phase.result => _caption ?? '',
      _ => 'Say anything in any language!',
    };
    return LayoutBuilder(
      builder: (context, c) {
        final size = math.min(charSize, c.maxHeight - 90).clamp(120.0, 260.0);
        return Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 400),
              transitionBuilder: (child, a) => ScaleTransition(
                scale: CurvedAnimation(parent: a, curve: Curves.elasticOut),
                child: FadeTransition(opacity: a, child: child),
              ),
              child: ConstrainedBox(
                key: ValueKey(bubble),
                constraints: BoxConstraints(maxWidth: width - 60),
                child: SpeechBubble(text: bubble, color: voice.color),
              ),
            ),
            const SizedBox(height: 8),
            Stack(
              alignment: Alignment.center,
              children: [
                Container(
                  width: size * 0.92,
                  height: size * 0.92,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: RadialGradient(
                      colors: [voice.accent, voice.accent.withValues(alpha: 0)],
                    ),
                  ),
                ),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 450),
                  transitionBuilder: (child, a) => ScaleTransition(
                    scale: CurvedAnimation(parent: a, curve: Curves.elasticOut),
                    child: child,
                  ),
                  child: SyncedCharacter(
                    key: ValueKey(voice.id),
                    engine: _engine,
                    voiceId: voice.id,
                    size: size,
                    mood: mood,
                  ),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  Widget _controls(AnimalVoice voice) {
    final recording = _phase == _Phase.recording;
    return Column(
      children: [
        SizedBox(
          height: 64,
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            child: recording
                ? Column(
                    key: const ValueKey('rec'),
                    children: [
                      LiveWaveform(levels: _levels),
                      Text(
                        '0:${_elapsed.floor().toString().padLeft(2, '0')} / 0:${VoiceEngine.maxSeconds}',
                        style: AppTheme.label(
                          13,
                          color: AppColors.coral,
                          weight: FontWeight.w800,
                        ),
                      ),
                    ],
                  )
                : _phase == _Phase.result
                ? Row(
                    key: const ValueKey('result'),
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _pillButton(
                        Icons.replay_rounded,
                        'Replay',
                        voice.color,
                        () {
                          _engine.play();
                        },
                      ),
                      const SizedBox(width: 12),
                      _pillButton(
                        Icons.ios_share_rounded,
                        'Share',
                        AppColors.purple,
                        _share,
                      ),
                    ],
                  )
                : Center(
                    key: const ValueKey('hint'),
                    child: Text(
                      'Tap the mic and start talking',
                      style: AppTheme.label(15, color: AppColors.inkSoft),
                    ),
                  ),
          ),
        ),
        MicButton(
          recording: recording,
          level: _levels.last,
          enabled: _phase != _Phase.processing,
          onTap: _onMic,
        ),
      ],
    );
  }

  Widget _pillButton(
    IconData icon,
    String label,
    Color color,
    VoidCallback onTap,
  ) {
    return Pressable(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 11),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(30),
          boxShadow: AppTheme.softShadow(color),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 20),
            const SizedBox(width: 6),
            Text(label, style: AppTheme.title(16, color: color)),
          ],
        ),
      ),
    );
  }

  Widget _voicePicker(AppState state) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Row(
            children: [
              Text('Choose a voice', style: AppTheme.title(18)),
              const Spacer(),
              if (!state.isPremium)
                Pressable(
                  onTap: _openPremium,
                  child: Text(
                    'Unlock all',
                    style: AppTheme.label(
                      14,
                      color: AppColors.goldDeep,
                      weight: FontWeight.w800,
                    ),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 6),
        SizedBox(
          height: 150,
          child: ListView.separated(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 14),
            scrollDirection: Axis.horizontal,
            itemCount: voices.length,
            separatorBuilder: (_, _) => const SizedBox(width: 12),
            itemBuilder: (_, i) {
              final v = voices[i];
              return VoiceCard(
                voice: v,
                selected: state.selectedId == v.id,
                locked: !state.isUnlocked(v),
                adProgress: state.adProgress(v.id),
                onTap: () => _onVoiceTap(v),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.icon, this.text);

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(7),
            decoration: BoxDecoration(
              color: AppColors.orange.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 18, color: AppColors.orange),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: AppTheme.label(
                14,
                color: AppColors.ink,
                weight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
