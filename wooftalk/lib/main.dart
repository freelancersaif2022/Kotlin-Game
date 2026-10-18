import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'screens/splash_screen.dart';
import 'services/app_state.dart';
import 'services/voice_engine.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: Color(0xFFEDE7FF),
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  final prefs = await SharedPreferences.getInstance();
  runApp(WoofTalkApp(state: AppState(prefs), engine: VoiceEngine()));
}

class WoofTalkApp extends StatelessWidget {
  const WoofTalkApp({super.key, required this.state, required this.engine});

  final AppState state;
  final VoiceEngine engine;

  @override
  Widget build(BuildContext context) {
    return AppScope(
      state: state,
      engine: engine,
      child: MaterialApp(
        title: 'WoofTalk',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        home: const SplashScreen(),
      ),
    );
  }
}

/// Gives every screen access to the app state and the voice engine.
class AppScope extends InheritedNotifier<AppState> {
  const AppScope({
    super.key,
    required AppState state,
    required this.engine,
    required super.child,
  }) : super(notifier: state);

  final VoiceEngine engine;

  static AppState stateOf(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AppScope>()!.notifier!;

  static AppState read(BuildContext context) =>
      context.getInheritedWidgetOfExactType<AppScope>()!.notifier!;

  static VoiceEngine engineOf(BuildContext context) =>
      context.getInheritedWidgetOfExactType<AppScope>()!.engine;
}
