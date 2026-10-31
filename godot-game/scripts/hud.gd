class_name HUD
extends CanvasLayer
## All 2D interface: in-game HUD, touch controls and the menu screens
## (title, pause, game over, victory). Built in code so it scales to any phone.

signal play_pressed
signal pause_pressed
signal resume_pressed
signal restart_pressed
signal menu_pressed

const GOLD := Color(0.98, 0.82, 0.42)
const PANEL_BG := Color(0.05, 0.07, 0.06, 0.78)

var touch: TouchControls
var _theme := Theme.new()
var _hud_root: Control
var _health_bar: ProgressBar
var _health_label: Label
var _special_bar: ProgressBar
var _wave_label: Label
var _enemies_label: Label
var _score_label: Label
var _combo_label: Label
var _boss_panel: Control
var _boss_bar: ProgressBar
var _banner: Label
var _vignette: ColorRect
var _loading: Label
var _screens := {}


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	layer = 10
	_build_theme()
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.theme = _theme
	add_child(root)

	_vignette = ColorRect.new()
	_vignette.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_vignette.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var vs := Shader.new()
	vs.code = """
shader_type canvas_item;
uniform float strength = 0.0;
void fragment() {
	float d = distance(UV, vec2(0.5));
	COLOR = vec4(0.75, 0.0, 0.0, smoothstep(0.35, 0.8, d) * strength);
}
"""
	var vm := ShaderMaterial.new()
	vm.shader = vs
	_vignette.material = vm
	root.add_child(_vignette)

	touch = TouchControls.new()
	root.add_child(touch)
	touch.visible = false

	_build_hud(root)
	_build_screens(root)

	_loading = _label("Growing the forest...", 30, GOLD)
	_loading.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	_loading.grow_horizontal = Control.GROW_DIRECTION_BOTH
	_loading.grow_vertical = Control.GROW_DIRECTION_BOTH
	root.add_child(_loading)


# --- Theme ------------------------------------------------------------------

func _build_theme() -> void:
	_theme.default_font_size = 22
	var normal := _box(Color(0.12, 0.16, 0.12, 0.92), GOLD.darkened(0.35))
	var hover := _box(Color(0.2, 0.27, 0.18, 0.95), GOLD)
	var pressed := _box(Color(0.32, 0.25, 0.1, 0.95), GOLD)
	for state in [["normal", normal], ["hover", hover], ["pressed", pressed], ["focus", hover]]:
		_theme.set_stylebox(state[0], "Button", state[1])
	_theme.set_color("font_color", "Button", Color(0.96, 0.93, 0.85))
	_theme.set_color("font_hover_color", "Button", GOLD)
	_theme.set_font_size("font_size", "Button", 28)
	_theme.set_color("font_outline_color", "Label", Color(0, 0, 0, 0.85))
	_theme.set_constant("outline_size", "Label", 6)
	_theme.set_stylebox("panel", "PanelContainer", _box(PANEL_BG, GOLD.darkened(0.5), 16, 2))


func _box(bg: Color, border: Color, radius := 12, border_w := 2) -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = bg
	sb.border_color = border
	sb.set_border_width_all(border_w)
	sb.set_corner_radius_all(radius)
	sb.content_margin_left = 22
	sb.content_margin_right = 22
	sb.content_margin_top = 12
	sb.content_margin_bottom = 12
	return sb


func _label(text: String, size := 22, color := Color.WHITE) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return l


func _bar(color: Color, height: float) -> ProgressBar:
	var bar := ProgressBar.new()
	bar.show_percentage = false
	bar.custom_minimum_size = Vector2(0, height)
	bar.add_theme_stylebox_override("background", _box(Color(0, 0, 0, 0.6), Color(1, 1, 1, 0.25), 6, 1))
	var fill := _box(color, color.lightened(0.3), 6, 0)
	fill.content_margin_top = 0
	fill.content_margin_bottom = 0
	bar.add_theme_stylebox_override("fill", fill)
	bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
	return bar


func _button(text: String, callback: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.custom_minimum_size = Vector2(300, 66)
	b.pressed.connect(func():
		Sfx.play("click", -4.0, 0.0)
		callback.call())
	return b


# --- In-game HUD --------------------------------------------------------------

func _build_hud(root: Control) -> void:
	_hud_root = Control.new()
	_hud_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_hud_root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(_hud_root)

	var margin := MarginContainer.new()
	margin.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	margin.mouse_filter = Control.MOUSE_FILTER_IGNORE
	for side in ["left", "right", "top", "bottom"]:
		margin.add_theme_constant_override("margin_" + side, 18)
	_hud_root.add_child(margin)

	var top := HBoxContainer.new()
	top.size_flags_vertical = Control.SIZE_SHRINK_BEGIN
	top.mouse_filter = Control.MOUSE_FILTER_IGNORE
	margin.add_child(top)

	# Health + special (top-left)
	var left := VBoxContainer.new()
	left.custom_minimum_size = Vector2(330, 0)
	left.add_theme_constant_override("separation", 6)
	top.add_child(left)
	var name_row := HBoxContainer.new()
	left.add_child(name_row)
	var title := _label("SKELETON KNIGHT", 18, GOLD)
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	name_row.add_child(title)
	_health_label = _label("100 / 100", 18)
	name_row.add_child(_health_label)
	_health_bar = _bar(Color(0.78, 0.16, 0.14), 24)
	left.add_child(_health_bar)
	_special_bar = _bar(Color(0.35, 0.75, 1.0), 10)
	_special_bar.max_value = 1.0
	_special_bar.value = 1.0
	left.add_child(_special_bar)

	# Wave info (top-center)
	var center := VBoxContainer.new()
	center.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	center.mouse_filter = Control.MOUSE_FILTER_IGNORE
	top.add_child(center)
	_wave_label = _label("WAVE 1 / 10", 30, GOLD)
	center.add_child(_wave_label)
	_enemies_label = _label("", 18)
	center.add_child(_enemies_label)

	# Score + pause (top-right)
	var right := HBoxContainer.new()
	right.add_theme_constant_override("separation", 14)
	top.add_child(right)
	var score_box := VBoxContainer.new()
	right.add_child(score_box)
	_score_label = _label("SCORE 0", 26)
	_score_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	score_box.add_child(_score_label)
	_combo_label = _label("", 20, GOLD)
	_combo_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	score_box.add_child(_combo_label)
	var pause := Button.new()
	pause.text = "II"
	pause.custom_minimum_size = Vector2(64, 64)
	pause.focus_mode = Control.FOCUS_NONE
	pause.pressed.connect(func(): pause_pressed.emit())
	right.add_child(pause)

	# Boss bar (under the wave label)
	_boss_panel = VBoxContainer.new()
	_boss_panel.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
	_boss_panel.custom_minimum_size = Vector2(520, 0)
	_boss_panel.offset_left = -260
	_boss_panel.offset_right = 260
	_boss_panel.offset_top = 100
	_boss_panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_boss_panel.add_child(_label("THE BONE KING", 22, Color(1, 0.6, 0.3)))
	_boss_bar = _bar(Color(0.9, 0.45, 0.1), 18)
	_boss_panel.add_child(_boss_bar)
	_boss_panel.visible = false
	_hud_root.add_child(_boss_panel)

	_banner = _label("", 64, GOLD)
	_banner.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	_banner.grow_horizontal = Control.GROW_DIRECTION_BOTH
	_banner.grow_vertical = Control.GROW_DIRECTION_BOTH
	_banner.position.y -= 120
	_banner.add_theme_constant_override("outline_size", 12)
	_banner.modulate.a = 0.0
	_hud_root.add_child(_banner)
	_hud_root.visible = false


func set_health(current: float, maximum: float) -> void:
	_health_bar.max_value = maximum
	create_tween().tween_property(_health_bar, "value", current, 0.2)
	_health_label.text = "%d / %d" % [ceili(current), int(maximum)]


func set_special(ratio: float) -> void:
	_special_bar.value = ratio
	touch.special_ratio = ratio
	touch.queue_redraw()


func set_wave(wave: int, total: int) -> void:
	_wave_label.text = "WAVE %d / %d" % [wave, total]


func set_enemies(remaining: int) -> void:
	_enemies_label.text = "Skeletons left: %d" % remaining if remaining > 0 else ""


func set_score(score: int, combo: int) -> void:
	_score_label.text = "SCORE %d" % score
	_combo_label.text = "COMBO x%d" % combo if combo > 1 else ""


func set_boss(visible_bar: bool, ratio := 1.0) -> void:
	_boss_panel.visible = visible_bar
	_boss_bar.max_value = 1.0
	_boss_bar.value = ratio


func banner(text: String, color := GOLD, hold := 1.6) -> void:
	_banner.text = text
	_banner.add_theme_color_override("font_color", color)
	_banner.pivot_offset = _banner.size / 2
	var t := create_tween()
	_banner.scale = Vector2(1.4, 1.4)
	t.tween_property(_banner, "modulate:a", 1.0, 0.2)
	t.parallel().tween_property(_banner, "scale", Vector2.ONE, 0.25).set_trans(Tween.TRANS_BACK)
	t.tween_interval(hold)
	t.tween_property(_banner, "modulate:a", 0.0, 0.4)


func hurt_flash() -> void:
	var mat: ShaderMaterial = _vignette.material
	var t := create_tween()
	t.tween_method(func(v): mat.set_shader_parameter("strength", v), 0.85, 0.0, 0.45)


# --- Menu screens --------------------------------------------------------------

func _build_screens(root: Control) -> void:
	_screens["title"] = _screen(root, "BONE FOREST", "The dead have risen in the ancient woods.\nSurvive 10 waves and defeat the Bone King.",
		[["PLAY", func(): play_pressed.emit()]], true)
	_screens["pause"] = _screen(root, "PAUSED", "",
		[["RESUME", func(): resume_pressed.emit()], ["RESTART", func(): restart_pressed.emit()],
		["MAIN MENU", func(): menu_pressed.emit()]])
	_screens["game_over"] = _screen(root, "YOU HAVE FALLEN", "",
		[["TRY AGAIN", func(): restart_pressed.emit()], ["MAIN MENU", func(): menu_pressed.emit()]])
	_screens["victory"] = _screen(root, "VICTORY!", "",
		[["PLAY AGAIN", func(): restart_pressed.emit()], ["MAIN MENU", func(): menu_pressed.emit()]])


func _screen(root: Control, title: String, body: String, buttons: Array, is_title := false) -> Control:
	var screen := Control.new()
	screen.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	screen.visible = false
	root.add_child(screen)
	var dim := ColorRect.new()
	dim.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	dim.color = Color(0, 0, 0, 0.25 if is_title else 0.5)
	screen.add_child(dim)

	var center := CenterContainer.new()
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	screen.add_child(center)
	var panel := PanelContainer.new()
	center.add_child(panel)
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 14)
	box.alignment = BoxContainer.ALIGNMENT_CENTER
	panel.add_child(box)

	var t := _label(title, 72 if is_title else 56, GOLD)
	t.add_theme_constant_override("outline_size", 12)
	box.add_child(t)
	var b := _label(body, 22, Color(0.9, 0.9, 0.85))
	b.name = "Body"
	box.add_child(b)
	box.add_child(Control.new())
	for entry in buttons:
		var btn := _button(entry[0], entry[1])
		btn.size_flags_horizontal = Control.SIZE_SHRINK_CENTER
		box.add_child(btn)
	var footer := _label("", 16, Color(0.75, 0.8, 0.75))
	footer.name = "Footer"
	box.add_child(footer)
	return screen


func show_screen(id: String, body := "", footer := "") -> void:
	for key in _screens:
		_screens[key].visible = key == id
	if id == "":
		return
	var screen: Control = _screens[id]
	var box := screen.get_child(1).get_child(0).get_child(0)
	if body != "":
		box.get_node("Body").text = body
	box.get_node("Footer").text = footer
	screen.modulate.a = 0.0
	create_tween().tween_property(screen, "modulate:a", 1.0, 0.3)
	# Give keyboard/gamepad users a focused button.
	for child in box.get_children():
		if child is Button:
			child.grab_focus()
			break


func screen_visible(id: String) -> bool:
	return _screens[id].visible


func set_loading(is_loading: bool) -> void:
	_loading.visible = is_loading


func set_playing(playing: bool, use_touch: bool) -> void:
	_hud_root.visible = playing
	touch.visible = playing and use_touch
	if not playing:
		touch.release_all()
