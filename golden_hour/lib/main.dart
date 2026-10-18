import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'screens/onboarding_screen.dart';
import 'screens/shell.dart';
import 'services/ads_service.dart';
import 'services/app_state.dart';
import 'services/location_service.dart';
import 'services/notification_service.dart';
import 'services/purchase_service.dart';
import 'services/services.dart';
import 'theme.dart';
import 'widgets/sky/shaders.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
    ),
  );
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  final state = await AppState.load();
  await Shaders.load();

  final services = Services(
    state: state,
    ads: AdsService(state),
    purchases: PurchaseService(state),
    notifications: NotificationService(state),
    clock: Clock(),
  );
  runApp(GoldenHourApp(services: services));

  // Non-blocking startup work.
  await services.notifications.init();
  services.purchases.init();
  if (state.onboarded) {
    _refreshLocation(services);
    services.ads.init();
  }
}

Future<void> _refreshLocation(Services s) async {
  final (_, place) = await LocationService.current();
  if (place != null) await s.state.setGpsPlace(place);
  await s.notifications.reschedule();
}

class GoldenHourApp extends StatefulWidget {
  const GoldenHourApp({super.key, required this.services});
  final Services services;

  @override
  State<GoldenHourApp> createState() => _GoldenHourAppState();
}

class _GoldenHourAppState extends State<GoldenHourApp>
    with WidgetsBindingObserver {
  bool _wasPro = false;

  Services get s => widget.services;

  @override
  void initState() {
    super.initState();
    _wasPro = s.state.isPro;
    s.state.addListener(_onState);
    WidgetsBinding.instance.addObserver(this);
  }

  void _onState() {
    if (_wasPro != s.state.isPro) {
      _wasPro = s.state.isPro;
      s.ads.onProChanged();
      s.notifications.reschedule();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState lifecycle) {
    if (lifecycle == AppLifecycleState.resumed && s.state.onboarded) {
      _refreshLocation(s);
    }
  }

  @override
  void dispose() {
    s.state.removeListener(_onState);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ServicesScope(
      services: s,
      child: AppScope(
        state: s.state,
        child: MaterialApp(
          title: 'Golden Hour',
          debugShowCheckedModeBanner: false,
          theme: GH.theme(),
          home: Builder(
            builder: (context) {
              final state = AppScope.of(context);
              return AnimatedSwitcher(
                duration: const Duration(milliseconds: 700),
                child: state.onboarded
                    ? const Shell(key: ValueKey('shell'))
                    : OnboardingScreen(
                        key: const ValueKey('onboarding'),
                        onDone: () async {
                          await state.completeOnboarding();
                          s.ads.init();
                          await s.notifications.reschedule();
                        },
                      ),
              );
            },
          ),
        ),
      ),
    );
  }
}
