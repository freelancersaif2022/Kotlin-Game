import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

import '../services/wifi_service.dart';
import 'common.dart';

/// Shows [child] only when Wi-Fi data can be read; otherwise explains what
/// is missing and offers the one-tap fix.
class AccessGate extends StatelessWidget {
  const AccessGate({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final wifi = WifiService.instance;
    return ListenableBuilder(
      listenable: wifi,
      builder: (context, _) {
        final msg = switch (wifi.access) {
          WifiAccess.ready => null,
          WifiAccess.unsupported => const StateMessage(
              icon: Icons.phone_iphone_rounded,
              title: 'Signal data not available',
              message: 'iOS does not let apps read Wi-Fi signal strength. The speed test works on every device.',
            ),
          WifiAccess.permissionDenied => StateMessage(
              icon: Icons.location_on_rounded,
              title: 'Location permission needed',
              message: 'Android only shares Wi-Fi details with apps that have location access. '
                  'WiFi Scope never stores your location.',
              actionLabel: 'Allow access',
              onAction: () => wifi.checkAccess(request: true),
            ),
          WifiAccess.permissionPermanentlyDenied => StateMessage(
              icon: Icons.location_disabled_rounded,
              title: 'Permission turned off',
              message: 'Enable location for WiFi Scope in system settings to read Wi-Fi details.',
              actionLabel: 'Open settings',
              onAction: openAppSettings,
            ),
          WifiAccess.locationOff => StateMessage(
              icon: Icons.location_off_rounded,
              title: 'Turn on location',
              message: 'Android needs location services on to share Wi-Fi details with apps.',
              actionLabel: 'Turn on',
              onAction: wifi.openLocationSettings,
            ),
        };
        if (msg == null) return child;
        return Center(child: SingleChildScrollView(child: msg));
      },
    );
  }
}
