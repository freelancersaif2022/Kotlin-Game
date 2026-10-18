import 'package:flutter/material.dart';

import '../main.dart';
import '../services/wifi_service.dart';
import 'dashboard_screen.dart';
import 'heatmap_list_screen.dart';
import 'networks_screen.dart';
import 'speed_test_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;
  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    startAdsAfterFirstFrame();
    WifiService.instance.checkAccess();
    WifiService.instance.startPolling();
    // Stop polling in the background and re-check permissions on return,
    // since the user may have changed them in system settings.
    _lifecycle = AppLifecycleListener(
      onHide: WifiService.instance.stopPolling,
      onShow: () {
        WifiService.instance.checkAccess();
        WifiService.instance.startPolling();
      },
    );
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    WifiService.instance.stopPolling();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _index,
        children: const [DashboardScreen(), SpeedTestScreen(), HeatmapListScreen(), NetworksScreen()],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.wifi_rounded), label: 'Signal'),
          NavigationDestination(icon: Icon(Icons.speed_rounded), label: 'Speed'),
          NavigationDestination(icon: Icon(Icons.map_outlined), selectedIcon: Icon(Icons.map_rounded), label: 'Heatmap'),
          NavigationDestination(icon: Icon(Icons.radar_rounded), label: 'Networks'),
        ],
      ),
    );
  }
}
