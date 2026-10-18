import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../core/heatmap_engine.dart';
import '../core/signal.dart';
import '../core/theme.dart';
import '../services/storage.dart';
import '../services/wifi_service.dart';
import '../widgets/common.dart';
import 'settings_screen.dart';
import 'survey_screen.dart';

class HeatmapListScreen extends StatelessWidget {
  const HeatmapListScreen({super.key});

  Future<void> _create(BuildContext context) async {
    final survey = await showModalBottomSheet<Survey>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _NewSurveySheet(),
    );
    if (survey == null || !context.mounted) return;
    await Storage.instance.saveSurvey(survey);
    if (!context.mounted) return;
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => SurveyScreen(survey: survey)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Heatmaps'),
        actions: [SettingsScreen.button(context), const SizedBox(width: 4)],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _create(context),
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.bg,
        icon: const Icon(Icons.add_rounded),
        label: const Text('New survey', style: TextStyle(fontWeight: FontWeight.w700)),
      ),
      body: ListenableBuilder(
        listenable: Storage.instance,
        builder: (context, _) {
          final surveys = Storage.instance.surveys;
          if (surveys.isEmpty) {
            return ListView(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
              children: const [_HowItWorks()],
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
            itemCount: surveys.length + 1,
            separatorBuilder: (_, _) => const SizedBox(height: 12),
            itemBuilder: (context, i) {
              if (i == surveys.length) return const _HowItWorks(compact: true);
              return _SurveyCard(surveys[i]);
            },
          );
        },
      ),
    );
  }
}

class _SurveyCard extends StatelessWidget {
  const _SurveyCard(this.s);
  final Survey s;

  @override
  Widget build(BuildContext context) {
    final insights = SurveyInsights.analyze(s);
    final avg = s.points.isEmpty ? null : s.points.map((p) => p.rssi).reduce((a, b) => a + b) ~/ s.points.length;
    final color = avg == null ? AppColors.textMuted : Signal.color(avg);
    final l = MaterialLocalizations.of(context);
    return Dismissible(
      key: ValueKey(s.id),
      direction: DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 24),
        decoration: BoxDecoration(color: AppColors.bad.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(22)),
        child: const Icon(Icons.delete_outline_rounded, color: AppColors.bad),
      ),
      confirmDismiss: (_) => showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
          title: const Text('Delete survey?'),
          content: Text('"${s.name}" and its measurements will be removed.'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Cancel')),
            TextButton(
              onPressed: () => Navigator.pop(c, true),
              child: const Text('Delete', style: TextStyle(color: AppColors.bad)),
            ),
          ],
        ),
      ),
      onDismissed: (_) => Storage.instance.deleteSurvey(s),
      child: AppCard(
        onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => SurveyScreen(survey: s))),
        child: Row(
          children: [
            Container(
              width: 54,
              height: 54,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                gradient: LinearGradient(colors: [color.withValues(alpha: 0.35), color.withValues(alpha: 0.08)]),
              ),
              child: Icon(Icons.grid_on_rounded, color: color),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(s.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 4),
                  Text(
                    '${s.points.length} points · ${s.widthMeters.round()}×${s.heightMeters.round()} m · ${l.formatShortMonthDay(s.createdAt)}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.textMuted),
                  ),
                ],
              ),
            ),
            if (insights != null)
              Column(
                children: [
                  Text('${insights.score}', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: color)),
                  const Text('SCORE', style: TextStyle(fontSize: 9, color: AppColors.textFaint, letterSpacing: 1)),
                ],
              )
            else
              const Icon(Icons.chevron_right_rounded, color: AppColors.textFaint),
          ],
        ),
      ),
    );
  }
}

class _HowItWorks extends StatelessWidget {
  const _HowItWorks({this.compact = false});
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    Widget step(int n, String title, String body) => Padding(
          padding: const EdgeInsets.only(bottom: 16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 28,
                height: 28,
                alignment: Alignment.center,
                decoration: BoxDecoration(shape: BoxShape.circle, color: AppColors.primary.withValues(alpha: 0.15)),
                child: Text('$n', style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 2),
                    Text(body, style: t.bodyMedium?.copyWith(color: AppColors.textMuted)),
                  ],
                ),
              ),
            ],
          ),
        );
    if (compact) {
      return Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Text('Swipe a survey left to delete it.', textAlign: TextAlign.center, style: t.bodySmall?.copyWith(color: AppColors.textFaint)),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AppCard(
          gradient: LinearGradient(
            colors: [AppColors.primary.withValues(alpha: 0.18), AppColors.accent.withValues(alpha: 0.12)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.map_rounded, color: AppColors.primary, size: 34),
              const SizedBox(height: 14),
              Text('Find your Wi-Fi dead zones', style: t.titleLarge),
              const SizedBox(height: 6),
              Text(
                'Build a real coverage map of your home from actual measurements, the same way professional installers do.',
                style: t.bodyMedium?.copyWith(color: AppColors.textMuted),
              ),
            ],
          ),
        ),
        const SectionHeader('How it works'),
        AppCard(
          child: Column(
            children: [
              step(1, 'Set up your space', 'Enter the room or floor size, and add a photo of your floor plan if you have one.'),
              step(2, 'Mark your router', 'Tap the router tool and place it where your router is.'),
              step(3, 'Walk and tap', 'Stand still at each spot and tap where you are. Each point averages several readings.'),
              step(4, 'Read the map', 'Green is strong, red is weak. Unlock insights to see the best router and extender spots.'),
            ],
          ),
        ),
      ],
    );
  }
}

class _NewSurveySheet extends StatefulWidget {
  const _NewSurveySheet();

  @override
  State<_NewSurveySheet> createState() => _NewSurveySheetState();
}

class _NewSurveySheetState extends State<_NewSurveySheet> {
  final _name = TextEditingController(text: 'My home');
  double _w = 12;
  double _h = 9;
  XFile? _plan;
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _pick(ImageSource source) async {
    final f = await ImagePicker().pickImage(source: source, maxWidth: 2400, imageQuality: 88);
    if (f != null) setState(() => _plan = f);
  }

  Future<void> _create() async {
    setState(() => _saving = true);
    final id = DateTime.now().microsecondsSinceEpoch.toRadixString(36);
    String? planPath;
    if (_plan != null) planPath = await Storage.instance.importFloorPlan(_plan!.path, id);
    final conn = WifiService.instance.connection;
    final survey = Survey(
      id: id,
      name: _name.text.trim().isEmpty ? 'Untitled survey' : _name.text.trim(),
      createdAt: DateTime.now(),
      widthMeters: _w,
      heightMeters: _h,
      floorPlanPath: planPath,
      ssid: conn.connected ? conn.ssid : null,
    );
    if (mounted) Navigator.pop(context, survey);
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    Widget slider(String label, double v, ValueChanged<double> onChanged) => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
                const Spacer(),
                Text('${v.round()} m', style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700)),
              ],
            ),
            Slider(value: v, min: 3, max: 40, divisions: 37, onChanged: onChanged),
          ],
        );
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 0, 20, 20 + MediaQuery.viewInsetsOf(context).bottom),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('New survey', style: t.titleLarge),
            const SizedBox(height: 18),
            TextField(
              controller: _name,
              textCapitalization: TextCapitalization.sentences,
              decoration: InputDecoration(
                labelText: 'Name',
                filled: true,
                fillColor: AppColors.surfaceHigh,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 18),
            slider('Width', _w, (v) => setState(() => _w = v)),
            slider('Length', _h, (v) => setState(() => _h = v)),
            const SizedBox(height: 4),
            Text('Floor plan (optional)', style: t.labelSmall),
            const SizedBox(height: 8),
            if (_plan != null)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.image_rounded, color: AppColors.excellent),
                title: const Text('Floor plan added'),
                trailing: IconButton(icon: const Icon(Icons.close_rounded), onPressed: () => setState(() => _plan = null)),
              )
            else
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _pick(ImageSource.gallery),
                      icon: const Icon(Icons.photo_library_outlined),
                      label: const Text('Gallery'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _pick(ImageSource.camera),
                      icon: const Icon(Icons.photo_camera_outlined),
                      label: const Text('Camera'),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _saving ? null : _create,
              child: _saving
                  ? const SizedBox.square(dimension: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
                  : const Text('Start survey'),
            ),
          ],
        ),
      ),
    );
  }
}
