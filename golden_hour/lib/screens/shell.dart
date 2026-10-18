import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/services.dart';
import '../theme.dart';
import '../widgets/common.dart';
import 'ar_screen.dart';
import 'compass_screen.dart';
import 'home_screen.dart';
import 'moon_screen.dart';
import 'planner_screen.dart';

class Shell extends StatefulWidget {
  const Shell({super.key, this.initialTab = 0});
  final int initialTab;

  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> {
  static const _arTab = 1;
  static const _plannerTab = 3;

  late int _index = widget.initialTab;

  void _select(int i) {
    if (i == _index) return;
    final leaving = _index;
    HapticFeedback.selectionClick();
    setState(() => _index = i);
    // Interstitial only at a natural break: leaving AR or the Planner.
    if (leaving == _arTab || leaving == _plannerTab) {
      ServicesScope.of(context).ads.maybeShowInterstitial();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      backgroundColor: GH.ink,
      body: IndexedStack(
        index: _index,
        children: [
          const HomeScreen(),
          ArScreen(active: _index == _arTab),
          CompassScreen(active: _index == 2),
          const PlannerScreen(),
          const MoonScreen(),
        ],
      ),
      bottomNavigationBar: _NavBar(index: _index, onSelect: _select),
    );
  }
}

class _NavBar extends StatelessWidget {
  const _NavBar({required this.index, required this.onSelect});
  final int index;
  final ValueChanged<int> onSelect;

  static const _items = [
    (Icons.wb_twilight_rounded, 'Today'),
    (Icons.view_in_ar_rounded, 'AR Sun'),
    (Icons.explore_rounded, 'Compass'),
    (Icons.calendar_month_rounded, 'Planner'),
    (Icons.dark_mode_rounded, 'Moon'),
  ];

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.paddingOf(context).bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(14, 0, 14, bottom > 0 ? bottom : 12),
      child: Glass(
        radius: 28,
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
        tint: const Color(0xFF0E1020),
        opacity: 0.45,
        child: Row(
          children: [
            for (var i = 0; i < _items.length; i++)
              Expanded(
                child: Pressable(
                  onTap: () => onSelect(i),
                  child: _NavItem(
                    icon: _items[i].$1,
                    label: _items[i].$2,
                    selected: i == index,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.selected,
  });
  final IconData icon;
  final String label;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        color: selected ? GH.gold.withValues(alpha: 0.16) : Colors.transparent,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedScale(
            scale: selected ? 1.12 : 1,
            duration: const Duration(milliseconds: 320),
            curve: Curves.easeOutBack,
            child: ShaderMask(
              shaderCallback: (r) =>
                  (selected
                          ? GH.goldGradient
                          : const LinearGradient(
                              colors: [GH.textLo, GH.textLo],
                            ))
                      .createShader(r),
              child: Icon(icon, color: Colors.white, size: 24),
            ),
          ),
          const SizedBox(height: 4),
          AnimatedDefaultTextStyle(
            duration: const Duration(milliseconds: 320),
            style: TextStyle(
              fontSize: 10.5,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              color: selected ? GH.goldLight : GH.textLo,
              letterSpacing: 0.2,
            ),
            child: Text(label, maxLines: 1),
          ),
        ],
      ),
    );
  }
}
