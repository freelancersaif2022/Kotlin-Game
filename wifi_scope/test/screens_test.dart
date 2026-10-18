import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wifi_scope/core/theme.dart';
import 'package:wifi_scope/screens/home_shell.dart';
import 'package:wifi_scope/screens/onboarding_screen.dart';
import 'package:wifi_scope/screens/survey_screen.dart';
import 'package:wifi_scope/services/storage.dart';

Future<void> _pump(WidgetTester tester, Widget home) async {
  tester.view.physicalSize = const Size(1080, 2340);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(MaterialApp(theme: AppTheme.dark, home: home));
  await tester.pump(const Duration(seconds: 1));
}

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await Storage.instance.init();
  });

  testWidgets('onboarding pages lay out', (tester) async {
    await _pump(tester, const OnboardingScreen());
    for (var i = 0; i < 3; i++) {
      await tester.tap(find.text('Continue'));
      await tester.pumpAndSettle();
    }
    expect(find.text('Allow location access'), findsOneWidget);
  });

  testWidgets('every tab renders', (tester) async {
    await _pump(tester, const HomeShell());
    for (final tab in ['Speed', 'Heatmap', 'Networks', 'Signal']) {
      await tester.tap(find.text(tab).last);
      await tester.pump(const Duration(milliseconds: 500));
    }
    expect(find.text('Signal data not available'), findsWidgets);
  });

  testWidgets('heatmap list shows saved surveys', (tester) async {
    await Storage.instance.saveSurvey(Survey(
      id: 'x',
      name: 'Living room',
      createdAt: DateTime(2026),
      widthMeters: 10,
      heightMeters: 8,
      points: [
        SurveyPoint(x: 0.2, y: 0.2, rssi: -48, linkSpeed: 400),
        SurveyPoint(x: 0.6, y: 0.5, rssi: -66, linkSpeed: 150),
        SurveyPoint(x: 0.9, y: 0.9, rssi: -84, linkSpeed: 12),
      ],
    ));
    await _pump(tester, const HomeShell());
    await tester.tap(find.text('Heatmap'));
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('Living room'), findsOneWidget);
  });

  testWidgets('survey screen with new survey renders', (tester) async {
    final s = Survey(id: 'y', name: 'Office', createdAt: DateTime(2026), widthMeters: 14, heightMeters: 9);
    await _pump(tester, SurveyScreen(survey: s));
    expect(find.text('Office'), findsOneWidget);
  });
}
