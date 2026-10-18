import 'dart:async';

import 'package:flutter/widgets.dart';

import 'ads_service.dart';
import 'app_state.dart';
import 'notification_service.dart';
import 'purchase_service.dart';

/// Ticks once per second so countdowns stay live.
class Clock extends ValueNotifier<DateTime> {
  Clock({DateTime? fixed})
    : _fixed = fixed != null,
      super(fixed ?? DateTime.now()) {
    if (!_fixed) {
      _timer = Timer.periodic(
        const Duration(seconds: 1),
        (_) => value = DateTime.now(),
      );
    }
  }

  final bool _fixed;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

class Services {
  Services({
    required this.state,
    required this.ads,
    required this.purchases,
    required this.notifications,
    required this.clock,
  });

  final AppState state;
  final AdsService ads;
  final PurchaseService purchases;
  final NotificationService notifications;
  final Clock clock;
}

class ServicesScope extends InheritedWidget {
  const ServicesScope({
    super.key,
    required this.services,
    required super.child,
  });
  final Services services;

  static Services of(BuildContext context) =>
      context.getInheritedWidgetOfExactType<ServicesScope>()!.services;

  @override
  bool updateShouldNotify(ServicesScope oldWidget) =>
      services != oldWidget.services;
}
