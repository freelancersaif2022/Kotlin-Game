import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

/// AdMob ad unit IDs.
///
/// These are Google's official TEST ids, safe to use while developing.
/// Before publishing replace them with your own ids from admob.google.com
/// (and the APPLICATION_ID meta-data in android/app/src/main/AndroidManifest.xml).
class AdIds {
  static const interstitial = 'ca-app-pub-3940256099942544/1033173712';
  static const rewarded = 'ca-app-pub-3940256099942544/5224354917';
}

class AdService {
  AdService._();
  static final instance = AdService._();

  InterstitialAd? _interstitial;
  RewardedAd? _rewarded;
  bool _loadingInterstitial = false;
  bool _loadingRewarded = false;
  Completer<bool>? _rewardedWaiter;
  bool _ready = false;

  bool get _supported => !kIsWeb && (Platform.isAndroid || Platform.isIOS);

  Future<void> init() async {
    if (!_supported) return;
    try {
      await MobileAds.instance.initialize();
      _ready = true;
      loadInterstitial();
      loadRewarded();
    } catch (e) {
      debugPrint('Ads init failed: $e');
    }
  }

  void loadInterstitial() {
    if (!_ready || _interstitial != null || _loadingInterstitial) return;
    _loadingInterstitial = true;
    InterstitialAd.load(
      adUnitId: AdIds.interstitial,
      request: const AdRequest(),
      adLoadCallback: InterstitialAdLoadCallback(
        onAdLoaded: (ad) {
          _interstitial = ad;
          _loadingInterstitial = false;
        },
        onAdFailedToLoad: (e) {
          _loadingInterstitial = false;
          debugPrint('Interstitial failed: $e');
        },
      ),
    );
  }

  void loadRewarded() {
    if (!_ready || _rewarded != null || _loadingRewarded) return;
    _loadingRewarded = true;
    RewardedAd.load(
      adUnitId: AdIds.rewarded,
      request: const AdRequest(),
      rewardedAdLoadCallback: RewardedAdLoadCallback(
        onAdLoaded: (ad) {
          _rewarded = ad;
          _loadingRewarded = false;
          _rewardedWaiter?.complete(true);
          _rewardedWaiter = null;
        },
        onAdFailedToLoad: (e) {
          _loadingRewarded = false;
          _rewardedWaiter?.complete(false);
          _rewardedWaiter = null;
          debugPrint('Rewarded failed: $e');
        },
      ),
    );
  }

  /// Shows an interstitial if one is ready. Completes when it is closed
  /// (or immediately when no ad is available).
  Future<void> showInterstitial() async {
    final ad = _interstitial;
    if (ad == null) {
      loadInterstitial();
      return;
    }
    _interstitial = null;
    final done = Completer<void>();
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        if (!done.isCompleted) done.complete();
        loadInterstitial();
      },
      onAdFailedToShowFullScreenContent: (ad, e) {
        ad.dispose();
        if (!done.isCompleted) done.complete();
        loadInterstitial();
      },
    );
    await ad.show();
    return done.future;
  }

  /// Makes sure a rewarded ad is loaded, waiting up to [timeout].
  Future<bool> ensureRewarded({
    Duration timeout = const Duration(seconds: 10),
  }) {
    if (_rewarded != null) return Future.value(true);
    if (!_ready) return Future.value(false);
    _rewardedWaiter ??= Completer<bool>();
    final waiter = _rewardedWaiter!;
    loadRewarded();
    return waiter.future.timeout(timeout, onTimeout: () => false);
  }

  /// Shows a rewarded ad. Resolves to true only when the reward was earned.
  Future<bool> showRewarded() async {
    final ad = _rewarded;
    if (ad == null) return false;
    _rewarded = null;
    var earned = false;
    final done = Completer<bool>();
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        if (!done.isCompleted) done.complete(earned);
        loadRewarded();
      },
      onAdFailedToShowFullScreenContent: (ad, e) {
        ad.dispose();
        if (!done.isCompleted) done.complete(false);
        loadRewarded();
      },
    );
    await ad.show(onUserEarnedReward: (_, _) => earned = true);
    return done.future;
  }
}
