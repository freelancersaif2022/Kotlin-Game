import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../services/voice_engine.dart';
import 'animal_character.dart';

/// [AnimalCharacter] whose mouth follows the audio that is currently playing.
class SyncedCharacter extends StatefulWidget {
  const SyncedCharacter({
    super.key,
    required this.engine,
    required this.voiceId,
    required this.size,
    this.mood = CharacterMood.idle,
    this.shadow = true,
  });

  final VoiceEngine engine;
  final String voiceId;
  final double size;
  final CharacterMood mood;
  final bool shadow;

  @override
  State<SyncedCharacter> createState() => _SyncedCharacterState();
}

class _SyncedCharacterState extends State<SyncedCharacter>
    with SingleTickerProviderStateMixin {
  late final Ticker _ticker;
  double _mouth = 0;

  @override
  void initState() {
    super.initState();
    _ticker = createTicker(_tick)..start();
  }

  void _tick(Duration _) {
    final player = widget.engine.player;
    final result = widget.engine.lastResult;
    var target = 0.0;
    if (player.playing &&
        result != null &&
        result.voiceId == widget.voiceId &&
        result.mouth.isNotEmpty) {
      final i = player.position.inMilliseconds ~/ 30;
      if (i < result.mouth.length) target = result.mouth[i];
    }
    // smooth so the jaw doesn't jitter
    final next = _mouth + (target - _mouth) * 0.45;
    if ((next - _mouth).abs() > 0.004) setState(() => _mouth = next);
  }

  @override
  void dispose() {
    _ticker.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final talking = _mouth > 0.02;
    return AnimalCharacter(
      voiceId: widget.voiceId,
      size: widget.size,
      mouth: _mouth,
      shadow: widget.shadow,
      mood: talking ? CharacterMood.talking : widget.mood,
    );
  }
}
