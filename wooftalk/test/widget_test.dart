import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wooftalk/models/voice.dart';
import 'package:wooftalk/services/app_state.dart';
import 'package:wooftalk/widgets/animal_character.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('only the dog voice is free, five voices are locked', () async {
    final state = AppState(await SharedPreferences.getInstance());
    expect(voices.length, 6);
    expect(voices.where(state.isUnlocked).map((v) => v.id), ['dog']);
  });

  test('a voice unlocks after two rewarded ads and stays unlocked', () async {
    final prefs = await SharedPreferences.getInstance();
    final state = AppState(prefs);
    final cat = voiceById('cat');
    expect(state.addRewardFor(cat), isFalse);
    expect(state.isUnlocked(cat), isFalse);
    expect(state.addRewardFor(cat), isTrue);
    expect(state.isUnlocked(cat), isTrue);
    expect(state.selectedId, 'cat');
    expect(AppState(prefs).isUnlocked(cat), isTrue);
  });

  test('interstitial is due on every second translation', () async {
    final state = AppState(await SharedPreferences.getInstance());
    expect(
      [for (var i = 0; i < 4; i++) state.registerTranslation()],
      [false, true, false, true],
    );
  });

  test('premium unlocks everything and disables interstitials', () async {
    final prefs = await SharedPreferences.getInstance();
    final state = AppState(prefs)..activatePremium(PremiumPlan.yearly);
    expect(voices.every(state.isUnlocked), isTrue);
    expect(state.registerTranslation(), isFalse);
    expect(state.registerTranslation(), isFalse);
    expect(AppState(prefs).isPremium, isTrue);
    state.cancelPremium();
    expect(state.isPremium, isFalse);
    expect(state.isUnlocked(voiceById('wolf')), isFalse);
  });

  testWidgets('every character paints in every mood', (tester) async {
    for (final v in voices) {
      for (final mood in CharacterMood.values) {
        await tester.pumpWidget(
          MaterialApp(
            home: AnimalCharacter(voiceId: v.id, mood: mood, mouth: 0.6),
          ),
        );
        await tester.pump(const Duration(milliseconds: 100));
        expect(tester.takeException(), isNull);
      }
    }
  });
}
