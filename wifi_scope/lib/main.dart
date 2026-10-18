import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'core/theme.dart';
import 'screens/home_shell.dart';
import 'screens/onboarding_screen.dart';
import 'services/ad_service.dart';
import 'services/storage.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    systemNavigationBarColor: AppColors.surface,
    systemNavigationBarIconBrightness: Brightness.light,
  ));
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  await Storage.instance.init();
  runApp(const WifiScopeApp());
}

class WifiScopeApp extends StatelessWidget {
  const WifiScopeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WiFi Scope',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.dark,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.dark,
      home: Storage.instance.onboarded ? const HomeShell() : const OnboardingScreen(),
    );
  }
}

/// Starts consent + ads once the first screen is on screen, so the app
/// opens instantly instead of waiting on the network.
void startAdsAfterFirstFrame() {
  WidgetsBinding.instance.addPostFrameCallback((_) => AdService.instance.init());
}
