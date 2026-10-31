class_name TouchControls
extends Control
## Multi-touch mobile controls drawn in one Control:
## left side = floating virtual joystick, right side = camera drag,
## round buttons for Attack, Jump and Special. Buttons drive InputMap actions
## so the player code is identical for touch, keyboard and gamepad.

var camera_rig: CameraRig
var special_ratio := 1.0

var _stick_touch := -1
var _stick_origin := Vector2.ZERO
var _stick_pos := Vector2.ZERO
var _look_touch := -1
var _look_last := Vector2.ZERO
var _button_touch := {}   # touch index -> action
var _buttons := []        # [{action, label, center, radius}]
var _font: Font


func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_font = ThemeDB.fallback_font
	resized.connect(_layout)
	_layout()


func _layout() -> void:
	var s := size
	var unit := clampf(s.y * 0.11, 52.0, 110.0)
	_buttons = [
		{"action": "attack", "label": "ATTACK", "center": Vector2(s.x - unit * 1.55, s.y - unit * 1.55), "radius": unit * 0.95},
		{"action": "jump", "label": "JUMP", "center": Vector2(s.x - unit * 3.35, s.y - unit * 0.85), "radius": unit * 0.6},
		{"action": "special", "label": "SLAM", "center": Vector2(s.x - unit * 0.95, s.y - unit * 3.4), "radius": unit * 0.6},
	]
	queue_redraw()


func _stick_radius() -> float:
	return clampf(size.y * 0.13, 60.0, 130.0)


func _input(event: InputEvent) -> void:
	if not visible:
		return
	if event is InputEventScreenTouch:
		if event.pressed:
			_touch_down(event.index, event.position)
		else:
			_touch_up(event.index)
	elif event is InputEventScreenDrag:
		if event.index == _stick_touch:
			_stick_pos = event.position
			_update_stick()
		elif event.index == _look_touch and camera_rig:
			camera_rig.look(event.position - _look_last, CameraRig.TOUCH_SENSITIVITY)
			_look_last = event.position
		queue_redraw()


func _touch_down(index: int, pos: Vector2) -> void:
	for b in _buttons:
		if pos.distance_to(b["center"]) <= b["radius"] * 1.15:
			_button_touch[index] = b["action"]
			Input.action_press(b["action"])
			queue_redraw()
			return
	if pos.x < size.x * 0.45 and _stick_touch == -1:
		_stick_touch = index
		_stick_origin = pos
		_stick_pos = pos
	elif _look_touch == -1:
		_look_touch = index
		_look_last = pos
	queue_redraw()


func _touch_up(index: int) -> void:
	if _button_touch.has(index):
		Input.action_release(_button_touch[index])
		_button_touch.erase(index)
	if index == _stick_touch:
		_stick_touch = -1
		_update_stick()
	if index == _look_touch:
		_look_touch = -1
	queue_redraw()


func _update_stick() -> void:
	var v := Vector2.ZERO
	if _stick_touch != -1:
		v = (_stick_pos - _stick_origin) / _stick_radius()
		if v.length() > 1.0:
			v = v.normalized()
	_set_axis("move_left", "move_right", v.x)
	_set_axis("move_forward", "move_back", v.y)


func _set_axis(neg: String, pos: String, value: float) -> void:
	if value < -0.05:
		Input.action_press(neg, -value)
		Input.action_release(pos)
	elif value > 0.05:
		Input.action_press(pos, value)
		Input.action_release(neg)
	else:
		Input.action_release(neg)
		Input.action_release(pos)


func release_all() -> void:
	for index in _button_touch:
		Input.action_release(_button_touch[index])
	_button_touch.clear()
	_stick_touch = -1
	_look_touch = -1
	_update_stick()
	queue_redraw()


func _draw() -> void:
	var r := _stick_radius()
	var base := _stick_origin if _stick_touch != -1 else Vector2(r * 1.5, size.y - r * 1.5)
	draw_circle(base, r, Color(0, 0, 0, 0.28))
	draw_arc(base, r, 0, TAU, 48, Color(1, 1, 1, 0.35), 3.0, true)
	var knob := base
	if _stick_touch != -1:
		var off := _stick_pos - _stick_origin
		knob = base + off.limit_length(r)
	draw_circle(knob, r * 0.42, Color(0.95, 0.92, 0.8, 0.55))

	for b in _buttons:
		var pressed: bool = b["action"] in _button_touch.values()
		var center: Vector2 = b["center"]
		var radius: float = b["radius"]
		var fill := Color(0.75, 0.2, 0.15, 0.55) if b["action"] == "attack" else Color(0.1, 0.1, 0.1, 0.45)
		if pressed:
			fill = fill.lightened(0.35)
		draw_circle(center, radius, fill)
		draw_arc(center, radius, 0, TAU, 48, Color(1, 0.9, 0.7, 0.6), 3.0, true)
		if b["action"] == "special" and special_ratio < 1.0:
			draw_circle(center, radius, Color(0, 0, 0, 0.45))
			draw_arc(center, radius - 4, -PI / 2, -PI / 2 + TAU * special_ratio, 48, Color(0.5, 0.9, 1.0, 0.9), 6.0, true)
		var fs := int(radius * (0.34 if b["action"] == "attack" else 0.42))
		var text_size := _font.get_string_size(b["label"], HORIZONTAL_ALIGNMENT_CENTER, -1, fs)
		draw_string(_font, center + Vector2(-text_size.x / 2, fs * 0.35), b["label"],
			HORIZONTAL_ALIGNMENT_CENTER, -1, fs, Color(1, 1, 1, 0.92))
