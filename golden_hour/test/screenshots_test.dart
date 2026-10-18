// Renders real screenshots of the app into /screenshots (run with TZ=UTC):
//   TZ=UTC flutter test test/screenshots_test.dart
import 'dart:convert';
import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:golden_hour/screens/paywall_screen.dart';
import 'package:golden_hour/screens/shell.dart';
import 'package:golden_hour/services/ads_service.dart';
import 'package:golden_hour/services/app_state.dart';
import 'package:golden_hour/services/notification_service.dart';
import 'package:golden_hour/services/purchase_service.dart';
import 'package:golden_hour/services/services.dart';
import 'package:golden_hour/theme.dart';
import 'package:golden_hour/widgets/sky/shaders.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<void> _loadFonts() async {
  final root =
      '${Platform.environment['FLUTTER_ROOT'] ?? '/root/flutter'}/bin/cache/artifacts/material_fonts';
  // Load Roboto under the test-default family too, so every label renders.
  for (final family in ['Roboto', 'FlutterTest']) {
    await _loadRoboto(root, family);
  }
  final icons = FontLoader('MaterialIcons')
    ..addFont(
      Future.value(
        ByteData.sublistView(
          File('$root/MaterialIcons-Regular.otf').readAsBytesSync(),
        ),
      ),
    );
  await icons.load();
}

Future<void> _loadRoboto(String root, String family) async {
  final roboto = FontLoader(family);
  for (final f in [
    'Roboto-Light.ttf',
    'Roboto-Regular.ttf',
    'Roboto-Medium.ttf',
    'Roboto-Bold.ttf',
    'Roboto-Thin.ttf',
    'Roboto-Black.ttf',
  ]) {
    final file = File('$root/$f');
    if (file.existsSync()) {
      roboto.addFont(
        Future.value(ByteData.sublistView(file.readAsBytesSync())),
      );
    }
  }
  await roboto.load();
}

void main() {
  const place = Place(
    name: 'Brighton Beach',
    lat: 50.819,
    lng: -0.137,
    isGps: true,
  );
  final boundaryKey = GlobalKey();

  setUpAll(() async {
    await _loadFonts();
  });

  Future<void> shoot(
    WidgetTester tester,
    String name,
    DateTime time, {
    int tab = 0,
    bool paywall = false,
  }) async {
    tester.view.physicalSize = const Size(1080, 2340);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);

    SharedPreferences.setMockInitialValues({
      'onboarded': true,
      'gps_place': jsonEncode(place.toJson()),
      'launch_count': 3,
    });
    final messenger = tester.binding.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(
      const MethodChannel('dev.fluttercommunity.plus/sensors/method'),
      (_) async => null,
    );
    // Phone lying flat, top edge pointing to 300° (WNW).
    final sensors = {
      'accelerometer': [0.0, 0.0, 9.8, 0.0],
      'magnetometer': [17.3, 10.0, -40.0, 0.0],
    };
    for (final e in sensors.entries) {
      messenger.setMockStreamHandler(
        EventChannel('dev.fluttercommunity.plus/sensors/${e.key}'),
        MockStreamHandler.inline(onListen: (_, sink) => sink.success(e.value)),
      );
    }
    final state = await AppState.load();
    await tester.runAsync(Shaders.load);
    final services = Services(
      state: state,
      ads: AdsService(state),
      purchases: PurchaseService(state),
      notifications: NotificationService(state),
      clock: Clock(fixed: time),
    );

    await tester.pumpWidget(
      RepaintBoundary(
        key: boundaryKey,
        child: ServicesScope(
          services: services,
          child: AppScope(
            state: state,
            child: MaterialApp(
              debugShowCheckedModeBanner: false,
              theme: GH.theme(),
              home: paywall ? const PaywallScreen() : Shell(initialTab: tab),
            ),
          ),
        ),
      ),
    );
    for (var i = 0; i < 12; i++) {
      await tester.pump(const Duration(milliseconds: 200));
    }

    final boundary =
        boundaryKey.currentContext!.findRenderObject()!
            as RenderRepaintBoundary;
    final bytes = await tester.runAsync(() async {
      final img = await boundary.toImage(pixelRatio: 3);
      final data = await img.toByteData(format: ui.ImageByteFormat.png);
      return data!.buffer.asUint8List();
    });
    final dir = Directory('screenshots')..createSync(recursive: true);
    File('${dir.path}/$name.png').writeAsBytesSync(bytes!);
    services.clock.dispose();
  }

  testWidgets(
    'home golden hour',
    (t) => shoot(t, '1_home_golden_hour', DateTime(2024, 10, 18, 16, 38)),
  );
  testWidgets(
    'home blue hour',
    (t) => shoot(t, '2_home_blue_hour', DateTime(2024, 10, 18, 17, 30)),
  );
  testWidgets(
    'home night',
    (t) => shoot(t, '3_home_night', DateTime(2024, 10, 18, 21, 40)),
  );
  testWidgets(
    'home day',
    (t) => shoot(t, '4_home_day', DateTime(2024, 10, 18, 11, 5)),
  );
  testWidgets(
    'compass',
    (t) => shoot(t, '5_compass', DateTime(2024, 10, 18, 16, 38), tab: 2),
  );
  testWidgets(
    'planner',
    (t) => shoot(t, '6_planner', DateTime(2024, 10, 18, 16, 38), tab: 3),
  );
  testWidgets(
    'moon',
    (t) => shoot(t, '7_moon', DateTime(2024, 10, 18, 21, 40), tab: 4),
  );
  testWidgets(
    'paywall',
    (t) => shoot(t, '8_paywall', DateTime(2024, 10, 18, 16, 38), paywall: true),
  );
}
