import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/app_state.dart';
import '../services/location_service.dart';
import '../services/services.dart';
import '../theme.dart';
import 'common.dart';

Future<void> showPlacesSheet(BuildContext context) => showModalBottomSheet(
  context: context,
  isScrollControlled: true,
  backgroundColor: Colors.transparent,
  barrierColor: Colors.black54,
  builder: (_) => const _PlacesSheet(),
);

class _PlacesSheet extends StatelessWidget {
  const _PlacesSheet();

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final s = ServicesScope.of(context);
    void select(int i) {
      state.selectPlace(i);
      s.notifications.reschedule();
      Navigator.pop(context);
    }

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: Glass(
        radius: 30,
        tint: const Color(0xFF10131F),
        opacity: 0.75,
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        child: SafeArea(
          top: false,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white24,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 18),
              Row(
                children: [
                  const Text('Places', style: GH.title),
                  const Spacer(),
                  if (!state.isPro)
                    Text(
                      '${state.savedPlaces.length}/${AppState.freePlaceLimit} free',
                      style: GH.label.copyWith(fontSize: 10),
                    ),
                ],
              ),
              const SizedBox(height: 14),
              _PlaceRow(
                icon: Icons.near_me_rounded,
                title: 'Current location',
                subtitle: state.gpsPlace?.coords ?? 'Tap to locate',
                selected: state.selectedIndex == -1,
                onTap: () async {
                  if (state.gpsPlace == null) {
                    final (_, p) = await LocationService.current();
                    if (p != null) await state.setGpsPlace(p);
                  }
                  if (context.mounted) select(-1);
                },
              ),
              for (var i = 0; i < state.savedPlaces.length; i++)
                Dismissible(
                  key: ValueKey('${state.savedPlaces[i].name}$i'),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.only(right: 20),
                    child: const Icon(
                      Icons.delete_outline_rounded,
                      color: GH.rose,
                    ),
                  ),
                  onDismissed: (_) {
                    state.removePlace(i);
                    s.notifications.reschedule();
                  },
                  child: _PlaceRow(
                    icon: Icons.place_rounded,
                    title: state.savedPlaces[i].name,
                    subtitle: state.savedPlaces[i].coords,
                    selected: state.selectedIndex == i,
                    onTap: () => select(i),
                  ),
                ),
              const SizedBox(height: 14),
              if (state.canAddPlace)
                GoldButton(
                  label: 'Add a place',
                  icon: Icons.add_location_alt_rounded,
                  onTap: () => _addPlace(context),
                )
              else
                Glass(
                  child: Row(
                    children: [
                      const Icon(Icons.lock_rounded, color: GH.goldLight),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Text(
                          'Unlimited places with Pro',
                          style: TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                      TextButton(
                        onPressed: () => openPaywall(context),
                        child: const Text('Go Pro'),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _addPlace(BuildContext context) async {
    final state = AppScope.read(context);
    final name = TextEditingController();
    final lat = TextEditingController(
      text: state.gpsPlace?.lat.toStringAsFixed(5) ?? '',
    );
    final lng = TextEditingController(
      text: state.gpsPlace?.lng.toStringAsFixed(5) ?? '',
    );
    final place = await showDialog<Place>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF151827),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
        title: const Text('New place'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: name,
              decoration: const InputDecoration(
                labelText: 'Name (e.g. Beach viewpoint)',
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: lat,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                      signed: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[-0-9.]')),
                    ],
                    decoration: const InputDecoration(labelText: 'Latitude'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: lng,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                      signed: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[-0-9.]')),
                    ],
                    decoration: const InputDecoration(labelText: 'Longitude'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              'Tip: copy coordinates from any map app. Prefilled with your current GPS spot.',
              style: GH.body.copyWith(fontSize: 12),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              final la = double.tryParse(lat.text),
                  lo = double.tryParse(lng.text);
              if (la == null || lo == null || la.abs() > 90 || lo.abs() > 180) {
                return;
              }
              Navigator.pop(
                ctx,
                Place(
                  name: name.text.trim().isEmpty ? 'My spot' : name.text.trim(),
                  lat: la,
                  lng: lo,
                ),
              );
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
    if (place != null) {
      await state.addPlace(place);
      if (context.mounted) {
        await ServicesScope.of(context).notifications.reschedule();
      }
    }
  }
}

class _PlaceRow extends StatelessWidget {
  const _PlaceRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.selected,
    required this.onTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Pressable(
    onTap: onTap,
    child: AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        color: selected
            ? GH.gold.withValues(alpha: 0.14)
            : Colors.white.withValues(alpha: 0.05),
        border: Border.all(
          color: selected ? GH.gold.withValues(alpha: 0.6) : Colors.white10,
        ),
      ),
      child: Row(
        children: [
          Icon(icon, color: selected ? GH.goldLight : GH.textMid),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: GH.body.copyWith(
                    fontSize: 12,
                    fontFeatures: GH.tabular,
                  ),
                ),
              ],
            ),
          ),
          if (selected) const Icon(Icons.check_circle_rounded, color: GH.gold),
        ],
      ),
    ),
  );
}
