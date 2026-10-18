import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart' hide AppState;
import 'package:shared_preferences/shared_preferences.dart';

import 'app_state.dart';

/// AdMob unit IDs. These are Google's official TEST IDs.
/// Replace them with your own IDs from the AdMob console before release
/// (also replace the App IDs in AndroidManifest.xml and Info.plist).
abstract final class AdIds {
  static String get interstitial => Platform.isAndroid
      ? 'ca-app-pub-3940256099942544/1033173712'
      : 'ca-app-pub-3940256099942544/4411468910';

  static String get rewarded => Platform.isAndroid
      ? 'ca-app-pub-3940256099942544/5224354917'
      : 'ca-app-pub-3940256099942544/1712485313';
}

/// Interstitial + rewarded ads with user-friendly frequency rules:
/// * never during the first app session (good first impression),
/// * never for Pro users,
/// * at most one interstitial every 3 minutes and 5 per day,
/// * only at natural breaks (leaving AR / Planner), never over the camera.
class AdsService {
  AdsService(this._state);

  static const _minGap = Duration(minutes: 3);
  static const _maxPerDay = 5;
  static const _kDay = 'ads_day';
  static const _kCount = 'ads_count';

  final AppState _state;
  final DateTime _sessionStart = DateTime.now();
  bool _ready = false;
  InterstitialAd? _interstitial;
  RewardedAd? _rewarded;
  bool _loadingRewarded = false;
  DateTime? _lastInterstitial;

  bool get _supported => !kIsWeb && (Platform.isAndroid || Platform.isIOS);

  Future<void> init() async {
    if (!_supported) return;
    final consent = Completer<void>();
    ConsentInformation.instance.requestConsentInfoUpdate(
      ConsentRequestParameters(),
      () => ConsentForm.loadAndShowConsentFormIfRequired(
        (_) => consent.complete(),
      ),
      (_) => consent.complete(),
    );
    await consent.future.timeout(const Duration(seconds: 15), onTimeout: () {});
    if (!await ConsentInformation.instance.canRequestAds()) return;
    await MobileAds.instance.initialize();
    _ready = true;
    if (!_state.isPro) {
      _loadInterstitial();
      _loadRewarded();
    }
  }

  void _loadInterstitial() {
    if (!_ready || _state.isPro) return;
    InterstitialAd.load(
      adUnitId: AdIds.interstitial,
      request: const AdRequest(),
      adLoadCallback: InterstitialAdLoadCallback(
        onAdLoaded: (ad) => _interstitial = ad,
        onAdFailedToLoad: (_) =>
            Future.delayed(const Duration(minutes: 1), _loadInterstitial),
      ),
    );
  }

  void _loadRewarded() {
    if (!_ready || _loadingRewarded || _rewarded != null) return;
    _loadingRewarded = true;
    RewardedAd.load(
      adUnitId: AdIds.rewarded,
      request: const AdRequest(),
      rewardedAdLoadCallback: RewardedAdLoadCallback(
        onAdLoaded: (ad) {
          _loadingRewarded = false;
          _rewarded = ad;
        },
        onAdFailedToLoad: (_) {
          _loadingRewarded = false;
          Future.delayed(const Duration(seconds: 45), _loadRewarded);
        },
      ),
    );
  }

  /// Call at a natural break (e.g. leaving the AR or Planner screen).
  Future<void> maybeShowInterstitial() async {
    final ad = _interstitial;
    if (ad == null || _state.isPro) return;
    if (_state.launchCount <= 1) return;
    final now = DateTime.now();
    if (now.difference(_sessionStart) < const Duration(seconds: 60)) return;
    if (_lastInterstitial != null &&
        now.difference(_lastInterstitial!) < _minGap) {
      return;
    }

    final prefs = await SharedPreferences.getInstance();
    final today = '${now.year}-${now.month}-${now.day}';
    var count = prefs.getString(_kDay) == today
        ? (prefs.getInt(_kCount) ?? 0)
        : 0;
    if (count >= _maxPerDay) return;

    _interstitial = null;
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        _loadInterstitial();
      },
      onAdFailedToShowFullScreenContent: (ad, _) {
        ad.dispose();
        _loadInterstitial();
      },
    );
    _lastInterstitial = now;
    count++;
    await prefs.setString(_kDay, today);
    await prefs.setInt(_kCount, count);
    await ad.show();
  }

  bool get rewardedReady => _rewarded != null;

  /// Shows a rewarded ad. Returns true only if the user earned the reward.
  Future<bool> showRewarded() async {
    if (!_supported) return false;
    final ad = _rewarded;
    if (ad == null) {
      _loadRewarded();
      return false;
    }
    _rewarded = null;
    final done = Completer<bool>();
    var earned = false;
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        _loadRewarded();
        if (!done.isCompleted) done.complete(earned);
      },
      onAdFailedToShowFullScreenContent: (ad, _) {
        ad.dispose();
        _loadRewarded();
        if (!done.isCompleted) done.complete(false);
      },
    );
    await ad.show(onUserEarnedReward: (_, _) => earned = true);
    return done.future;
  }

  void onProChanged() {
    if (_state.isPro) {
      _interstitial?.dispose();
      _interstitial = null;
    } else {
      _loadInterstitial();
      _loadRewarded();
    }
  }

  Future<void> showPrivacyOptions() async {
    if (!_supported) return;
    await ConsentForm.showPrivacyOptionsForm((_) {});
  }
}
