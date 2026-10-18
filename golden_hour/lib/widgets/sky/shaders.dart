import 'dart:ui' as ui;

/// Loads the GLSL fragment shaders once. Widgets fall back to gradients if
/// a device can't compile them.
class Shaders {
  static ui.FragmentProgram? sky;
  static ui.FragmentProgram? clouds;
  static ui.FragmentProgram? moon;

  static Future<void> load() async {
    Future<ui.FragmentProgram?> get(String path) async {
      try {
        return await ui.FragmentProgram.fromAsset(path);
      } catch (_) {
        return null;
      }
    }

    final r = await Future.wait([
      get('shaders/sky.frag'),
      get('shaders/clouds.frag'),
      get('shaders/moon.frag'),
    ]);
    sky = r[0];
    clouds = r[1];
    moon = r[2];
  }
}
