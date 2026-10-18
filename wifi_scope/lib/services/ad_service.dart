import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

/// AdMob ad unit IDs.
///
/// These are Google's official TEST IDs, which are safe to use while
/// developing. Before publishing, replace them with the IDs from your AdMob
/// account (and the app IDs in AndroidManifest.xml and Info.plist).
abstract final class AdIds {
  static String get interstitial => Platform.isAndroid
      ? 'ca-app-pub-3940256099942544/1033173712'
      : 'ca-app-pub-3940256099942544/4411468910';

  static String get rewarded => Platform.isAndroid
      ? 'ca-app-pub-3940256099942544/5224354917'
      : 'ca-app-pub-3940256099942544/1712485313';
}

/// Owns ad loading and showing so screens only call [showInterstitial] or
/// [showRewarded].
///
/// Interstitials are frequency-capped so they never interrupt the user too
/// often: they appear only at natural breaks, at most once every
/// [_interstitialCooldown], and never on the first eligible break of a
/// session.
class AdService {
  AdService._();
  static final instance = AdService._();

  static const _interstitialCooldown = Duration(seconds: 90);
  static const _maxLoadRetries = 3;

  bool _canRequestAds = false;
  bool _initialized = false;

  InterstitialAd? _interstitial;
  RewardedAd? _rewarded;
  bool _loadingInterstitial = false;
  bool _loadingRewarded = false;
  int _interstitialRetries = 0;
  int _rewardedRetries = 0;

  DateTime _lastInterstitial = DateTime.fromMillisecondsSinceEpoch(0);
  int _breaks = 0;
  bool _showing = false;

  final privacyOptionsRequired = ValueNotifier<bool>(false);

  /// Gathers consent (GDPR / US states) through Google's UMP SDK and then
  /// starts the Mobile Ads SDK. Safe to call more than once.
  Future<void> init() async {
    if (_initialized || !(Platform.isAndroid || Platform.isIOS)) return;
    _initialized = true;

    final consent = Completer<void>();
    ConsentInformation.instance.requestConsentInfoUpdate(
      ConsentRequestParameters(),
      () async {
        await ConsentForm.loadAndShowConsentFormIfRequired((_) {});
        if (!consent.isCompleted) consent.complete();
      },
      (_) {
        if (!consent.isCompleted) consent.complete();
      },
    );
    await consent.future.timeout(const Duration(seconds: 12), onTimeout: () {});

    privacyOptionsRequired.value = await ConsentInformation.instance.getPrivacyOptionsRequirementStatus() ==
        PrivacyOptionsRequirementStatus.required;
    _canRequestAds = await ConsentInformation.instance.canRequestAds();
    if (!_canRequestAds) return;

    await MobileAds.instance.initialize();
    _loadInterstitial();
    _loadRewarded();
  }

  Future<void> showPrivacyOptions() async {
    final done = Completer<void>();
    await ConsentForm.showPrivacyOptionsForm((_) => done.complete());
    await done.future;
    _canRequestAds = await ConsentInformation.instance.canRequestAds();
  }

  // ---------------------------------------------------------------- loading

  void _loadInterstitial() {
    if (!_canRequestAds || _loadingInterstitial || _interstitial != null) return;
    _loadingInterstitial = true;
    InterstitialAd.load(
      adUnitId: AdIds.interstitial,
      request: const AdRequest(),
      adLoadCallback: InterstitialAdLoadCallback(
        onAdLoaded: (ad) {
          _interstitial = ad;
          _loadingInterstitial = false;
          _interstitialRetries = 0;
        },
        onAdFailedToLoad: (error) {
          _loadingInterstitial = false;
          if (_interstitialRetries++ < _maxLoadRetries) {
            Timer(Duration(seconds: 5 * _interstitialRetries), _loadInterstitial);
          }
        },
      ),
    );
  }

  void _loadRewarded() {
    if (!_canRequestAds || _loadingRewarded || _rewarded != null) return;
    _loadingRewarded = true;
    RewardedAd.load(
      adUnitId: AdIds.rewarded,
      request: const AdRequest(),
      rewardedAdLoadCallback: RewardedAdLoadCallback(
        onAdLoaded: (ad) {
          _rewarded = ad;
          _loadingRewarded = false;
          _rewardedRetries = 0;
        },
        onAdFailedToLoad: (error) {
          _loadingRewarded = false;
          if (_rewardedRetries++ < _maxLoadRetries) {
            Timer(Duration(seconds: 5 * _rewardedRetries), _loadRewarded);
          }
        },
      ),
    );
  }

  // ---------------------------------------------------------------- showing

  /// Call at a natural break (a finished test, leaving a survey). Completes
  /// once the ad is closed, or immediately when no ad is shown, so callers
  /// can simply `await` it before continuing.
  Future<void> showInterstitial() async {
    _breaks++;
    final ad = _interstitial;
    final cooledDown = DateTime.now().difference(_lastInterstitial) >= _interstitialCooldown;
    if (ad == null || _showing || !cooledDown || _breaks < 2) {
      _loadInterstitial();
      return;
    }
    _interstitial = null;
    _showing = true;
    final closed = Completer<void>();
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        _finishInterstitial(closed);
      },
      onAdFailedToShowFullScreenContent: (ad, _) {
        ad.dispose();
        _finishInterstitial(closed);
      },
    );
    _lastInterstitial = DateTime.now();
    await ad.show();
    return closed.future;
  }

  void _finishInterstitial(Completer<void> closed) {
    _showing = false;
    _loadInterstitial();
    if (!closed.isCompleted) closed.complete();
  }

  bool get rewardedReady => _rewarded != null;

  /// Shows a rewarded ad and completes with `true` when the user earned the
  /// reward. If no ad can be loaded (no fill, offline, no consent) the user
  /// is not punished for it: the reward is granted anyway.
  Future<bool> showRewarded() async {
    if (_showing) return false;
    if (_rewarded == null) {
      _loadRewarded();
      final deadline = DateTime.now().add(const Duration(seconds: 6));
      while (_rewarded == null && (_loadingRewarded || !_canRequestAds) && DateTime.now().isBefore(deadline)) {
        if (!_canRequestAds) break;
        await Future<void>.delayed(const Duration(milliseconds: 200));
      }
    }
    final ad = _rewarded;
    if (ad == null) {
      _loadRewarded();
      return true;
    }
    _rewarded = null;
    _showing = true;
    var earned = false;
    final closed = Completer<bool>();
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        _showing = false;
        _loadRewarded();
        if (!closed.isCompleted) closed.complete(earned);
      },
      onAdFailedToShowFullScreenContent: (ad, _) {
        ad.dispose();
        _showing = false;
        _loadRewarded();
        if (!closed.isCompleted) closed.complete(true);
      },
    );
    await ad.show(onUserEarnedReward: (_, _) => earned = true);
    return closed.future;
  }
}
