import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models/voice.dart';
import '../theme/app_theme.dart';
import 'animal_character.dart';
import 'common.dart';

/// Big round record button with pulsing rings.
class MicButton extends StatefulWidget {
  const MicButton({
    super.key,
    required this.recording,
    required this.level,
    required this.onTap,
    this.enabled = true,
  });

  final bool recording;
  final double level;
  final VoidCallback onTap;
  final bool enabled;

  @override
  State<MicButton> createState() => _MicButtonState();
}

class _MicButtonState extends State<MicButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1800),
  )..repeat();

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final g = widget.recording
        ? AppColors.recordGradient
        : AppColors.primaryGradient;
    const size = 92.0;
    return Opacity(
      opacity: widget.enabled ? 1 : 0.5,
      child: Pressable(
        onTap: widget.enabled ? widget.onTap : null,
        scale: 0.9,
        child: SizedBox.square(
          dimension: 150,
          child: AnimatedBuilder(
            animation: _pulse,
            builder: (_, child) {
              return Stack(
                alignment: Alignment.center,
                children: [
                  for (var i = 0; i < 3; i++)
                    Builder(
                      builder: (_) {
                        final p = (_pulse.value + i / 3) % 1;
                        final boost = widget.recording ? widget.level * 22 : 0;
                        return Container(
                          width: size + p * 56 + boost,
                          height: size + p * 56 + boost,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: g.colors.last.withValues(
                              alpha: (1 - p) * (widget.recording ? 0.28 : 0.16),
                            ),
                          ),
                        );
                      },
                    ),
                  child!,
                ],
              );
            },
            child: Container(
              width: size,
              height: size,
              decoration: BoxDecoration(
                gradient: g,
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 5),
                boxShadow: [
                  BoxShadow(
                    color: g.colors.last.withValues(alpha: 0.5),
                    blurRadius: 24,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 250),
                transitionBuilder: (c, a) =>
                    ScaleTransition(scale: a, child: c),
                child: widget.recording
                    ? Container(
                        key: const ValueKey('stop'),
                        width: 28,
                        height: 28,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(8),
                        ),
                      )
                    : const Icon(
                        Icons.mic_rounded,
                        key: ValueKey('mic'),
                        color: Colors.white,
                        size: 44,
                      ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Bars that dance with the microphone level.
class LiveWaveform extends StatelessWidget {
  const LiveWaveform({
    super.key,
    required this.levels,
    this.color = AppColors.coral,
  });

  final List<double> levels;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 46,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          for (var i = 0; i < levels.length; i++)
            AnimatedContainer(
              duration: const Duration(milliseconds: 90),
              margin: const EdgeInsets.symmetric(horizontal: 2),
              width: 5,
              height: 6 + levels[i] * 40,
              decoration: BoxDecoration(
                color: Color.lerp(color, AppColors.purple, i / levels.length),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
        ],
      ),
    );
  }
}

/// Selectable voice tile with a lock & ad-progress indicator.
class VoiceCard extends StatelessWidget {
  const VoiceCard({
    super.key,
    required this.voice,
    required this.selected,
    required this.locked,
    required this.adProgress,
    required this.onTap,
  });

  final AnimalVoice voice;
  final bool selected;
  final bool locked;
  final int adProgress;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Pressable(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOutBack,
        width: 98,
        margin: EdgeInsets.only(
          top: selected ? 0 : 8,
          bottom: selected ? 8 : 0,
        ),
        padding: const EdgeInsets.fromLTRB(6, 8, 6, 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: selected ? voice.color : Colors.transparent,
            width: 3,
          ),
          boxShadow: AppTheme.softShadow(
            selected ? voice.color : AppColors.purple,
          ),
        ),
        child: Stack(
          clipBehavior: Clip.none,
          children: [
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 66,
                  height: 66,
                  decoration: BoxDecoration(
                    color: voice.accent,
                    shape: BoxShape.circle,
                  ),
                  child: Opacity(
                    opacity: locked ? 0.55 : 1,
                    child: Center(
                      child: AnimalCharacter(
                        key: ValueKey('${voice.id}$selected'),
                        voiceId: voice.id,
                        size: 66,
                        shadow: false,
                        alive: selected,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  voice.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTheme.title(13.5),
                ),
                const SizedBox(height: 3),
                if (locked)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      for (var i = 0; i < 2; i++)
                        Container(
                          width: 16,
                          height: 5,
                          margin: const EdgeInsets.symmetric(horizontal: 2),
                          decoration: BoxDecoration(
                            color: i < adProgress
                                ? AppColors.mint
                                : AppColors.inkSoft.withValues(alpha: 0.18),
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                    ],
                  )
                else
                  Text(
                    selected ? 'Selected' : (voice.free ? 'Free' : 'Unlocked'),
                    style: AppTheme.label(
                      11,
                      color: selected ? voice.color : AppColors.inkSoft,
                    ),
                  ),
              ],
            ),
            if (locked)
              Positioned(
                right: -2,
                top: -2,
                child: Container(
                  padding: const EdgeInsets.all(5),
                  decoration: BoxDecoration(
                    gradient: AppColors.goldGradient,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: const Icon(
                    Icons.lock_rounded,
                    size: 13,
                    color: Colors.white,
                  ),
                ),
              ),
            if (selected)
              Positioned(
                right: -2,
                top: -2,
                child: Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    color: voice.color,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: const Icon(
                    Icons.check_rounded,
                    size: 13,
                    color: Colors.white,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Cartoon speech bubble with the funny "translation".
class SpeechBubble extends StatelessWidget {
  const SpeechBubble({super.key, required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _BubblePainter(color),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 22),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: AppTheme.title(19, color: AppColors.ink),
        ),
      ),
    );
  }
}

class _BubblePainter extends CustomPainter {
  _BubblePainter(this.color);

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final body = RRect.fromRectAndRadius(
      Rect.fromLTWH(0, 0, size.width, size.height - 10),
      const Radius.circular(22),
    );
    final path = Path()
      ..addRRect(body)
      ..moveTo(size.width / 2 - 12, size.height - 11)
      ..lineTo(size.width / 2, size.height)
      ..lineTo(size.width / 2 + 12, size.height - 11)
      ..close();
    canvas.drawShadow(path, color, 8, false);
    canvas.drawPath(path, Paint()..color = Colors.white);
    canvas.drawPath(
      path,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3,
    );
  }

  @override
  bool shouldRepaint(_BubblePainter old) => old.color != color;
}

/// Full-screen "translating…" overlay shown for 2-3 seconds.
class ProcessingOverlay extends StatefulWidget {
  const ProcessingOverlay({super.key, required this.voice});

  final AnimalVoice voice;

  @override
  State<ProcessingOverlay> createState() => _ProcessingOverlayState();
}

class _ProcessingOverlayState extends State<ProcessingOverlay>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 2600),
  )..forward();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final steps = [
      'Listening to your voice…',
      'Decoding human language…',
      'Translating to ${widget.voice.name}…',
    ];
    return Container(
      color: AppColors.ink.withValues(alpha: 0.4),
      alignment: Alignment.center,
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, _) {
          final step = (_c.value * steps.length).floor().clamp(
            0,
            steps.length - 1,
          );
          return Container(
            margin: const EdgeInsets.symmetric(horizontal: 36),
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 26),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(32),
              boxShadow: AppTheme.softShadow(widget.voice.color),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox.square(
                      dimension: 150,
                      child: CustomPaint(
                        painter: _OrbitPainter(_c.value, widget.voice.color),
                      ),
                    ),
                    AnimalCharacter(
                      voiceId: widget.voice.id,
                      size: 120,
                      mood: CharacterMood.thinking,
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 300),
                  transitionBuilder: (c, a) => FadeTransition(
                    opacity: a,
                    child: SlideTransition(
                      position: Tween(
                        begin: const Offset(0, 0.4),
                        end: Offset.zero,
                      ).animate(a),
                      child: c,
                    ),
                  ),
                  child: Text(
                    steps[step],
                    key: ValueKey(step),
                    textAlign: TextAlign.center,
                    style: AppTheme.title(19),
                  ),
                ),
                const SizedBox(height: 16),
                ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: SizedBox(
                    height: 12,
                    child: Stack(
                      children: [
                        Container(color: widget.voice.accent),
                        FractionallySizedBox(
                          widthFactor: Curves.easeInOut.transform(_c.value),
                          child: Container(
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: [
                                  widget.voice.color,
                                  Color.lerp(
                                    widget.voice.color,
                                    AppColors.coral,
                                    0.5,
                                  )!,
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '${(_c.value * 100).round()}%',
                  style: AppTheme.label(13, color: AppColors.inkSoft),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _OrbitPainter extends CustomPainter {
  _OrbitPainter(this.t, this.color);

  final double t;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    canvas.drawCircle(
      c,
      size.width / 2 - 6,
      Paint()..color = color.withValues(alpha: 0.12),
    );
    final notes = ['♪', '♫', '♪', '♬'];
    for (var i = 0; i < 4; i++) {
      final a = t * 2 * math.pi * 1.5 + i * math.pi / 2;
      final o = c + Offset(math.cos(a), math.sin(a)) * (size.width / 2 - 8);
      final tp = TextPainter(
        text: TextSpan(
          text: notes[i],
          style: TextStyle(
            fontSize: 20,
            color: color,
            fontWeight: FontWeight.w900,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, o - Offset(tp.width / 2, tp.height / 2));
    }
  }

  @override
  bool shouldRepaint(_OrbitPainter old) => old.t != t;
}

/// Burst of confetti for celebrations.
class ConfettiBurst extends StatefulWidget {
  const ConfettiBurst({super.key});

  @override
  State<ConfettiBurst> createState() => _ConfettiBurstState();
}

class _ConfettiBurstState extends State<ConfettiBurst>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1800),
  )..forward();
  final _seed = math.Random().nextInt(1 << 20);

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, _) => CustomPaint(
          size: Size.infinite,
          painter: _ConfettiPainter(_c.value, _seed),
        ),
      ),
    );
  }
}

class _ConfettiPainter extends CustomPainter {
  _ConfettiPainter(this.t, this.seed);

  final double t;
  final int seed;

  static const colors = [
    AppColors.orange,
    AppColors.coral,
    AppColors.purple,
    AppColors.mint,
    AppColors.gold,
    Color(0xFF4F8DFF),
  ];

  @override
  void paint(Canvas canvas, Size size) {
    final rng = math.Random(seed);
    final origin = Offset(size.width / 2, size.height * 0.35);
    for (var i = 0; i < 70; i++) {
      final ang = rng.nextDouble() * math.pi * 2;
      final speed = 180 + rng.nextDouble() * 320;
      final e = Curves.easeOut.transform(t);
      final p =
          origin +
          Offset(math.cos(ang), math.sin(ang)) * speed * e +
          Offset(0, 260 * t * t);
      final paint = Paint()
        ..color = colors[i % colors.length].withValues(
          alpha: (1 - t).clamp(0, 1),
        );
      canvas.save();
      canvas.translate(p.dx, p.dy);
      canvas.rotate(t * 10 * (rng.nextDouble() - 0.5));
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          const Rect.fromLTWH(-5, -3, 10, 6),
          const Radius.circular(2),
        ),
        paint,
      );
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(_ConfettiPainter old) => old.t != t;
}
