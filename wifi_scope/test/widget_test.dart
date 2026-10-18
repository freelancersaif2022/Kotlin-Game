import 'package:flutter_test/flutter_test.dart';
import 'package:wifi_scope/core/heatmap_engine.dart';
import 'package:wifi_scope/core/signal.dart';
import 'package:wifi_scope/services/storage.dart';

void main() {
  test('signal quality maps dBm to 0-100', () {
    expect(Signal.quality(-50), 100);
    expect(Signal.quality(-75), 50);
    expect(Signal.quality(-110), 0);
  });

  test('channel from frequency', () {
    expect(Signal.channel(2412), 1);
    expect(Signal.channel(2437), 6);
    expect(Signal.channel(5180), 36);
    expect(Signal.band(5180), '5 GHz');
  });

  test('distance grows as signal weakens', () {
    expect(Signal.estimateDistanceMeters(-70, 2437), greaterThan(Signal.estimateDistanceMeters(-50, 2437)));
  });

  test('insights find best and weakest spots', () {
    final s = Survey(id: 'a', name: 'Test', createdAt: DateTime(2026), widthMeters: 10, heightMeters: 8, points: [
      SurveyPoint(x: 0.1, y: 0.1, rssi: -45, linkSpeed: 400),
      SurveyPoint(x: 0.5, y: 0.5, rssi: -62, linkSpeed: 200),
      SurveyPoint(x: 0.9, y: 0.9, rssi: -82, linkSpeed: 20),
    ]);
    final i = SurveyInsights.analyze(s)!;
    expect(i.best.rssi, -45);
    expect(i.worst.rssi, -82);
    expect(i.deadZones, hasLength(1));
    expect(i.suggestedExtender, isNotNull);
  });

  test('survey round-trips through JSON', () {
    final s = Survey(id: 'b', name: 'Flat', createdAt: DateTime(2026), widthMeters: 6, heightMeters: 4,
        points: [SurveyPoint(x: 0.2, y: 0.3, rssi: -60, linkSpeed: 300)]);
    final back = Survey.fromJson(s.toJson());
    expect(back.points.single.rssi, -60);
    expect(back.aspect, 1.5);
  });
}
