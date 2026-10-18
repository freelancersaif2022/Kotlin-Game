import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:in_app_purchase/in_app_purchase.dart';

import 'app_state.dart';

/// Subscription product IDs. Create these in Google Play Console
/// (Monetize > Subscriptions) and App Store Connect with exactly these IDs.
/// Give the yearly plan a 3-day free-trial offer.
abstract final class ProductIds {
  static const monthly = 'golden_pro_monthly';
  static const yearly = 'golden_pro_yearly';
  static const all = {monthly, yearly};
}

/// Google Play / App Store subscriptions without a backend.
/// Entitlement is re-checked on every launch with restorePurchases();
/// if the store hasn't confirmed it for 7 days, Pro is switched off.
class PurchaseService extends ChangeNotifier {
  PurchaseService(this._state);

  static const _offlineGrace = Duration(days: 7);

  final AppState _state;
  late final InAppPurchase _iap = InAppPurchase.instance;
  StreamSubscription<List<PurchaseDetails>>? _sub;

  bool available = false;
  bool busy = false;
  String? error;
  Map<String, ProductDetails> products = {};
  bool _confirmedThisSession = false;

  ProductDetails? get monthly => products[ProductIds.monthly];
  ProductDetails? get yearly => products[ProductIds.yearly];

  Future<void> init() async {
    try {
      available = await _iap.isAvailable();
    } catch (_) {
      available = false;
    }
    if (!available) {
      notifyListeners();
      return;
    }
    _sub = _iap.purchaseStream.listen(
      _onPurchases,
      onError: (Object e) {
        error = '$e';
        busy = false;
        notifyListeners();
      },
    );
    final resp = await _iap.queryProductDetails(ProductIds.all);
    for (final p in resp.productDetails) {
      products.putIfAbsent(p.id, () => p);
    }
    notifyListeners();

    // Silent re-validation of an existing subscription.
    await _iap.restorePurchases();
    Future.delayed(const Duration(seconds: 10), () {
      if (_confirmedThisSession || !_state.isPro) return;
      final v = _state.proVerifiedAt;
      if (v == null || DateTime.now().difference(v) > _offlineGrace) {
        _state.setPro(false);
      }
    });
  }

  Future<void> _onPurchases(List<PurchaseDetails> list) async {
    for (final p in list) {
      if (!ProductIds.all.contains(p.productID)) continue;
      switch (p.status) {
        case PurchaseStatus.pending:
          busy = true;
        case PurchaseStatus.purchased:
        case PurchaseStatus.restored:
          _confirmedThisSession = true;
          busy = false;
          error = null;
          await _state.setPro(true, verified: true);
        case PurchaseStatus.error:
          busy = false;
          error = p.error?.message ?? 'Purchase failed';
        case PurchaseStatus.canceled:
          busy = false;
      }
      if (p.pendingCompletePurchase) {
        await _iap.completePurchase(p);
      }
    }
    notifyListeners();
  }

  Future<void> buy(ProductDetails product) async {
    error = null;
    busy = true;
    notifyListeners();
    try {
      await _iap.buyNonConsumable(
        purchaseParam: PurchaseParam(productDetails: product),
      );
    } catch (e) {
      busy = false;
      error = '$e';
      notifyListeners();
    }
  }

  Future<void> restore() async {
    busy = true;
    notifyListeners();
    await _iap.restorePurchases();
    Future.delayed(const Duration(seconds: 6), () {
      busy = false;
      notifyListeners();
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    super.dispose();
  }
}
