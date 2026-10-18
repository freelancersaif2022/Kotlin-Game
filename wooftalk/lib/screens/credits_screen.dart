import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/voice.dart';
import '../theme/app_theme.dart';
import '../widgets/animal_character.dart';
import '../widgets/common.dart';

/// Lists the recordists and licenses of every bundled animal recording
/// (required by the Creative Commons licenses).
class CreditsScreen extends StatelessWidget {
  const CreditsScreen({super.key});

  Future<Map<String, dynamic>> _load() async =>
      jsonDecode(await rootBundle.loadString('assets/sounds/credits.json'))
          as Map<String, dynamic>;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PlayfulBackground(
        child: SafeArea(
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
                child: Row(
                  children: [
                    CircleIconButton(
                      icon: Icons.arrow_back_rounded,
                      size: 42,
                      onTap: () => Navigator.pop(context),
                    ),
                    const SizedBox(width: 14),
                    Text('Sound credits', style: AppTheme.title(24)),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 22),
                child: Text(
                  'All animal voices are real recordings from Wikimedia Commons. '
                  'Thank you to these recordists!',
                  style: AppTheme.label(14, color: AppColors.inkSoft),
                ),
              ),
              Expanded(
                child: FutureBuilder<Map<String, dynamic>>(
                  future: _load(),
                  builder: (context, snap) {
                    if (!snap.hasData) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    final data = snap.data!;
                    return ListView(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      children: [
                        for (final v in voices)
                          if (data[v.id] is List &&
                              (data[v.id] as List).isNotEmpty)
                            _AnimalCredits(voice: v, items: data[v.id] as List),
                      ],
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AnimalCredits extends StatelessWidget {
  const _AnimalCredits({required this.voice, required this.items});

  final AnimalVoice voice;
  final List items;

  @override
  Widget build(BuildContext context) {
    final seen = <String>{};
    final sources = [
      for (final it in items)
        if (seen.add(it['title'] as String)) it as Map,
    ];
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: AppTheme.softShadow(voice.color),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              AnimalCharacter(
                voiceId: voice.id,
                size: 40,
                shadow: false,
                alive: false,
              ),
              const SizedBox(width: 8),
              Text(voice.name, style: AppTheme.title(17)),
            ],
          ),
          const SizedBox(height: 6),
          for (final s in sources)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                '• ${(s['title'] as String).replaceFirst('File:', '')}\n'
                '   by ${s['author']} · ${s['license']}',
                style: AppTheme.label(
                  12.5,
                  color: AppColors.inkSoft,
                  weight: FontWeight.w600,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
