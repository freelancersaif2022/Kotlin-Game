import 'dart:math';

import 'package:flutter/material.dart';

class AnimalVoice {
  const AnimalVoice({
    required this.id,
    required this.name,
    required this.tagline,
    required this.color,
    required this.accent,
    required this.words,
    this.free = false,
  });

  final String id;
  final String name;
  final String tagline;
  final Color color;
  final Color accent;
  final List<String> words;
  final bool free;

  /// Funny "translated" caption shown in the speech bubble.
  String caption(int wordCount, Random rng) {
    final n = wordCount.clamp(1, 8);
    return [
      for (var i = 0; i < n; i++)
        '${words[rng.nextInt(words.length)]}${rng.nextDouble() < 0.2 ? '!!' : '!'}',
    ].join(' ');
  }
}

const voices = <AnimalVoice>[
  AnimalVoice(
    id: 'dog',
    name: 'Buddy Dog',
    tagline: 'Classic happy woofs',
    color: Color(0xFFFFA23A),
    accent: Color(0xFFFFE2B8),
    words: ['Woof', 'Arf', 'Ruff', 'Bork', 'Woof woof'],
    free: true,
  ),
  AnimalVoice(
    id: 'puppy',
    name: 'Tiny Puppy',
    tagline: 'Squeaky little yips',
    color: Color(0xFFFF7EB3),
    accent: Color(0xFFFFDDEB),
    words: ['Yip', 'Yap', 'Arf', 'Yip yip'],
  ),
  AnimalVoice(
    id: 'cat',
    name: 'Kitty Cat',
    tagline: 'Sweet meows & trills',
    color: Color(0xFF8C7BFF),
    accent: Color(0xFFE5E0FF),
    words: ['Meow', 'Mew', 'Mrrp', 'Meeow', 'Prrr'],
  ),
  AnimalVoice(
    id: 'wolf',
    name: 'Night Wolf',
    tagline: 'Epic moon howls',
    color: Color(0xFF4F8DFF),
    accent: Color(0xFFDDE9FF),
    words: ['Awoooo', 'Ahwooo', 'Owooo'],
  ),
  AnimalVoice(
    id: 'duck',
    name: 'Silly Duck',
    tagline: 'Quacky chatter',
    color: Color(0xFFFFC21A),
    accent: Color(0xFFFFF1BF),
    words: ['Quack', 'Quack quack', 'Kwak'],
  ),
  AnimalVoice(
    id: 'cow',
    name: 'Moo Cow',
    tagline: 'Big farm moos',
    color: Color(0xFF26C6A0),
    accent: Color(0xFFD3F7EC),
    words: ['Mooo', 'Moooo', 'Mmmoo'],
  ),
];

AnimalVoice voiceById(String id) =>
    voices.firstWhere((v) => v.id == id, orElse: () => voices.first);
