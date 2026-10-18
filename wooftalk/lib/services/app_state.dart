import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/voice.dart';

enum PremiumPlan { monthly, yearly }

/// Everything the app remembers between launches. Stored on the device only.
class AppState extends ChangeNotifier {
  AppState(this._prefs) {
    _load();
  }

  static const adsToUnlock = 2;

  final SharedPreferences _prefs;

  String _selected = 'dog';
  final Set<String> _unlocked = {};
  final Map<String, int> _adProgress = {};
  int _translations = 0;
  PremiumPlan? _plan;
  DateTime? _premiumUntil;

  String get selectedId => _selected;
  AnimalVoice get selected => voiceById(_selected);
  int get translations => _translations;
  PremiumPlan? get plan => isPremium ? _plan : null;
  DateTime? get premiumUntil => isPremium ? _premiumUntil : null;

  bool get isPremium =>
      _premiumUntil != null && _premiumUntil!.isAfter(DateTime.now());

  bool isUnlocked(AnimalVoice v) =>
      v.free || isPremium || _unlocked.contains(v.id);

  int adProgress(String id) => _adProgress[id] ?? 0;

  void _load() {
    _selected = _prefs.getString('selected') ?? 'dog';
    _unlocked.addAll(_prefs.getStringList('unlocked') ?? const []);
    for (final v in voices) {
      final p = _prefs.getInt('ads_${v.id}');
      if (p != null) _adProgress[v.id] = p;
    }
    _translations = _prefs.getInt('translations') ?? 0;
    final until = _prefs.getInt('premium_until');
    if (until != null) {
      _premiumUntil = DateTime.fromMillisecondsSinceEpoch(until);
    }
    final plan = _prefs.getString('premium_plan');
    _plan = PremiumPlan.values.where((p) => p.name == plan).firstOrNull;
    if (!isUnlocked(selected)) _selected = 'dog';
  }

  void select(AnimalVoice v) {
    if (!isUnlocked(v) || v.id == _selected) return;
    _selected = v.id;
    _prefs.setString('selected', v.id);
    notifyListeners();
  }

  /// Records one watched rewarded ad. Returns true when the voice unlocks.
  bool addRewardFor(AnimalVoice v) {
    final p = adProgress(v.id) + 1;
    _adProgress[v.id] = p;
    _prefs.setInt('ads_${v.id}', p);
    var unlockedNow = false;
    if (p >= adsToUnlock) {
      _unlocked.add(v.id);
      _prefs.setStringList('unlocked', _unlocked.toList());
      _selected = v.id;
      _prefs.setString('selected', v.id);
      unlockedNow = true;
    }
    notifyListeners();
    return unlockedNow;
  }

  /// Counts a finished translation. Returns true when an interstitial is due
  /// (every 2nd translation for free users).
  bool registerTranslation() {
    _translations++;
    _prefs.setInt('translations', _translations);
    return !isPremium && _translations % 2 == 0;
  }

  /// Mock subscription purchase – stores an expiry date locally.
  /// Replace with Google Play Billing (in_app_purchase) before publishing.
  void activatePremium(PremiumPlan plan) {
    final now = DateTime.now();
    _plan = plan;
    _premiumUntil = plan == PremiumPlan.monthly
        ? DateTime(now.year, now.month + 1, now.day, now.hour, now.minute)
        : DateTime(now.year + 1, now.month, now.day, now.hour, now.minute);
    _prefs.setString('premium_plan', plan.name);
    _prefs.setInt('premium_until', _premiumUntil!.millisecondsSinceEpoch);
    notifyListeners();
  }

  void cancelPremium() {
    _plan = null;
    _premiumUntil = null;
    _prefs.remove('premium_plan');
    _prefs.remove('premium_until');
    if (!isUnlocked(selected)) {
      _selected = 'dog';
      _prefs.setString('selected', 'dog');
    }
    notifyListeners();
  }
}
