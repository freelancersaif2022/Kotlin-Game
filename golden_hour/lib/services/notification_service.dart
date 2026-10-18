import 'dart:io';
import 'dart:ui' show Color;

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../astro/day_light.dart';
import 'app_state.dart';

/// Local, offline golden-hour alerts. Scheduled 7 days ahead and refreshed
/// whenever the app opens or settings / location change.
class NotificationService {
  NotificationService(this._state);

  static const _days = 7;
  static const _details = NotificationDetails(
    android: AndroidNotificationDetails(
      'golden_hour_alerts',
      'Golden hour alerts',
      channelDescription: 'Reminders before golden hour and blue hour',
      importance: Importance.high,
      priority: Priority.high,
      color: Color(0xFFFFB347),
    ),
    iOS: DarwinNotificationDetails(),
  );

  final AppState _state;
  final _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false;

  Future<void> init() async {
    if (kIsWeb || !(Platform.isAndroid || Platform.isIOS)) return;
    tzdata.initializeTimeZones();
    try {
      final info = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(info.identifier));
    } catch (_) {
      tz.setLocalLocation(tz.UTC);
    }
    await _plugin.initialize(
      settings: const InitializationSettings(
        android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        iOS: DarwinInitializationSettings(
          requestAlertPermission: false,
          requestBadgePermission: false,
          requestSoundPermission: false,
        ),
      ),
    );
    _ready = true;
  }

  Future<bool> requestPermission() async {
    if (!_ready) return false;
    if (Platform.isAndroid) {
      return await _plugin
              .resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin
              >()
              ?.requestNotificationsPermission() ??
          false;
    }
    return await _plugin
            .resolvePlatformSpecificImplementation<
              IOSFlutterLocalNotificationsPlugin
            >()
            ?.requestPermissions(alert: true, sound: true) ??
        false;
  }

  /// Free: 1 alert a day (evening golden hour, 20 min before).
  /// Rewarded "extra alert": also tomorrow's/today's morning golden hour.
  /// Pro: every window, any lead time.
  Future<void> reschedule() async {
    if (!_ready) return;
    await _plugin.cancelAll();
    final place = _state.place;
    final a = _state.alerts;
    if (place == null || !a.enabled) return;

    final pro = _state.isPro;
    final lead = Duration(minutes: pro ? a.minutesBefore : 20);
    final now = DateTime.now();
    var id = 0;

    Future<void> schedule(DateTime eventTime, String title, String body) async {
      final at = eventTime.subtract(lead);
      if (!at.isAfter(now)) return;
      await _plugin.zonedSchedule(
        id: id++,
        title: title,
        body: body,
        scheduledDate: tz.TZDateTime.from(at, tz.local),
        notificationDetails: _details,
        androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      );
    }

    final m = lead.inMinutes;
    for (var i = 0; i < _days; i++) {
      final day = DayLight.forDate(
        now.add(Duration(days: i)),
        place.lat,
        place.lng,
      );
      final eveningGolden = day.goldenHourEvening;
      final morningGolden = day.goldenHourMorning;

      if ((pro ? a.eveningGolden : true) && eveningGolden != null) {
        await schedule(
          eveningGolden.start,
          '🌅 Golden hour in $m min',
          'Warm light at ${place.name}. Sunset follows soon.',
        );
      }
      final extraMorning =
          !pro && i == 0 && _state.hasAccess(RewardFeature.extraAlert);
      if (((pro && a.morningGolden) || extraMorning) && morningGolden != null) {
        await schedule(
          morningGolden.start,
          '🌄 Morning golden hour in $m min',
          'Soft sunrise light at ${place.name}.',
        );
      }
      if (pro && a.blueHour) {
        final blue = day.blueHourEvening;
        if (blue != null) {
          await schedule(
            blue.start,
            '🌌 Blue hour in $m min',
            'Deep blue sky — perfect for city lights.',
          );
        }
      }
    }
  }
}
