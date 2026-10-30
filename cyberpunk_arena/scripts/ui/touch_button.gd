class_name TouchActionButton
extends Control
## Round multitouch action button (vector-drawn).
##
## Claims the first finger that lands inside its circle (plus padding) and
## tracks it by index, so it can be held together with the joystick, other
## buttons and the camera drag. Works as a momentary or toggle button.

signal pressed
signal released
signal toggled(enabled: bool)

enum Icon { SWORD, RUN }

@export var icon := Icon.SWORD
@export var label := "ATTACK"
@export var radius := 82.0
## Extra touch tolerance outside the visible circle.
@export var hit_padding := 22.0
@export var toggle_mode := false
@export var accent := Color(0.0, 0.82, 1.0)

var is_down := false
var toggled_on := false
## 0..1, drawn as a ring sweep (used for the attack cooldown).
var cooldown_ratio := 0.0:
	set(value):
		cooldown_ratio = value
		queue_redraw()

var _touch_index := -1
var _press_anim := 0.0


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	custom_minimum_size = Vector2.ONE * radius * 2.0


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and _touch_index == -1 and _hit_test(event.position):
			_touch_index = event.index
			_set_down(true)
			get_viewport().set_input_as_handled()
		elif not event.pressed and event.index == _touch_index:
			_touch_index = -1
			_set_down(false)
			get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index == _touch_index:
		# Keep the finger's drags away from the camera.
		get_viewport().set_input_as_handled()


func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT or what == NOTIFICATION_WM_WINDOW_FOCUS_OUT \
			or what == NOTIFICATION_APPLICATION_PAUSED:
		if _touch_index != -1:
			_touch_index = -1
			_set_down(false)


func _process(delta: float) -> void:
	var target := 1.0 if is_down else 0.0
	var prev := _press_anim
	_press_anim = lerpf(_press_anim, target, 1.0 - exp(-25.0 * delta))
	if absf(prev - _press_anim) > 0.001:
		queue_redraw()


func set_toggled(enabled: bool) -> void:
	if toggled_on == enabled:
		return
	toggled_on = enabled
	queue_redraw()


func _set_down(down: bool) -> void:
	is_down = down
	if down:
		pressed.emit()
		if toggle_mode:
			toggled_on = not toggled_on
			toggled.emit(toggled_on)
	else:
		released.emit()
	queue_redraw()


func _hit_test(screen_pos: Vector2) -> bool:
	if not is_visible_in_tree():
		return false
	var local := get_global_transform_with_canvas().affine_inverse() * screen_pos
	return local.distance_to(size * 0.5) <= radius + hit_padding


func _draw() -> void:
	var c := size * 0.5
	var r := radius * (1.0 - 0.07 * _press_anim)
	var lit := is_down or toggled_on
	var fill := Color(accent, 0.85) if lit else Color(0.06, 0.09, 0.14, 0.42)
	draw_circle(c + Vector2(0, 5), r, Color(0, 0, 0, 0.16), true, -1.0, true)
	draw_circle(c, r, fill, true, -1.0, true)
	draw_arc(c, r, 0.0, TAU, 64, Color(accent, 0.95), 3.5, true)
	draw_arc(c, r - 9.0, 0.0, TAU, 64, Color(1, 1, 1, 0.14), 1.5, true)
	if cooldown_ratio > 0.001:
		draw_arc(c, r - 5.0, -PI * 0.5, -PI * 0.5 + TAU * cooldown_ratio, 48, Color(1, 1, 1, 0.75), 6.0, true)
	var ink := Color(0.04, 0.06, 0.1) if lit else Color(1, 1, 1, 0.95)
	var s := r / 82.0
	match icon:
		Icon.SWORD:
			_draw_sword(c + Vector2(0, -10) * s, s, ink)
		Icon.RUN:
			_draw_run(c + Vector2(0, -8) * s, s, ink)
	var font := ThemeDB.fallback_font
	var font_size := int(round(15.0 * s))
	var text_w := font.get_string_size(label, HORIZONTAL_ALIGNMENT_CENTER, -1, font_size).x
	draw_string(font, c + Vector2(-text_w * 0.5, r * 0.62), label, HORIZONTAL_ALIGNMENT_CENTER, -1, font_size, ink)


func _draw_sword(c: Vector2, s: float, ink: Color) -> void:
	var tip := c + Vector2(26, -26) * s
	var hilt := c + Vector2(-14, 14) * s
	draw_line(hilt, tip, ink, 7.0 * s, true)
	draw_line(c + Vector2(-24, 4) * s, c + Vector2(-4, 24) * s, ink, 6.0 * s, true)
	draw_line(hilt, c + Vector2(-26, 26) * s, ink, 6.0 * s, true)


func _draw_run(c: Vector2, s: float, ink: Color) -> void:
	for i in 2:
		var o := Vector2(-14 + i * 18, 0) * s
		draw_polyline(PackedVector2Array([
			c + o + Vector2(-8, -18) * s, c + o + Vector2(8, 0) * s, c + o + Vector2(-8, 18) * s]),
			ink, 6.0 * s, true)
