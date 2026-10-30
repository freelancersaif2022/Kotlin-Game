class_name VirtualJoystick
extends Control
## Floating multitouch virtual joystick.
##
## The Control's rect is the activation zone (bottom-left of the screen).
## A finger that lands inside the zone is claimed by its touch index; the base
## re-centres under that finger and only that finger drives the stick, so the
## joystick works at the same time as the buttons and the camera drag.
## All drawing is vector-based (_draw), so it is crisp on every resolution.

signal stick_changed(vector: Vector2)

@export var base_radius := 105.0
@export var knob_radius := 44.0
@export_range(0.0, 0.9) var dead_zone := 0.12
## Re-centre the base under the finger on touch (floating joystick).
@export var floating := true
## Rest position of the base relative to the zone's bottom-left corner.
@export var rest_offset := Vector2(190.0, -175.0)
@export var accent := Color(0.0, 0.82, 1.0)

var output := Vector2.ZERO

var _touch_index := -1
var _center := Vector2.ZERO
var _knob := Vector2.ZERO
var _alpha := 0.55


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	resized.connect(_reset_center)
	_reset_center()


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		var local := _to_local(event.position)
		if event.pressed and _touch_index == -1 and Rect2(Vector2.ZERO, size).has_point(local):
			_touch_index = event.index
			if floating:
				_center = _clamp_center(local)
			_update_knob(local)
			get_viewport().set_input_as_handled()
		elif not event.pressed and event.index == _touch_index:
			_release()
			get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index == _touch_index:
		_update_knob(_to_local(event.position))
		get_viewport().set_input_as_handled()


func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT or what == NOTIFICATION_WM_WINDOW_FOCUS_OUT \
			or what == NOTIFICATION_APPLICATION_PAUSED:
		_release()


func _process(delta: float) -> void:
	var active := _touch_index != -1
	var target_alpha := 1.0 if active else 0.55
	_alpha = lerpf(_alpha, target_alpha, 1.0 - exp(-12.0 * delta))
	if not active:
		# Glide back to the rest position when released.
		var rest := _rest_center()
		_center = _center.lerp(rest, 1.0 - exp(-14.0 * delta))
		_knob = _knob.lerp(Vector2.ZERO, 1.0 - exp(-20.0 * delta))
	queue_redraw()


func _draw() -> void:
	var a := _alpha
	# Base: soft dark disc, accent ring and direction arc.
	draw_circle(_center, base_radius, Color(0.06, 0.09, 0.14, 0.32 * a), true, -1.0, true)
	draw_arc(_center, base_radius, 0.0, TAU, 64, Color(accent, 0.55 * a), 3.0, true)
	draw_arc(_center, base_radius * 0.55, 0.0, TAU, 48, Color(1, 1, 1, 0.12 * a), 2.0, true)
	if output.length() > 0.05:
		var ang := output.angle()
		draw_arc(_center, base_radius - 6.0, ang - 0.5, ang + 0.5, 24, Color(accent, 0.9 * a), 6.0, true)
	# Knob.
	var knob_pos := _center + _knob
	draw_circle(knob_pos + Vector2(0, 4), knob_radius, Color(0, 0, 0, 0.18 * a), true, -1.0, true)
	draw_circle(knob_pos, knob_radius, Color(0.96, 0.98, 1.0, 0.92 * a), true, -1.0, true)
	draw_arc(knob_pos, knob_radius, 0.0, TAU, 48, Color(accent, a), 3.0, true)
	draw_circle(knob_pos, knob_radius * 0.35, Color(accent, 0.75 * a), true, -1.0, true)


func is_active() -> bool:
	return _touch_index != -1


func _update_knob(local: Vector2) -> void:
	_knob = (local - _center).limit_length(base_radius)
	var raw := _knob / base_radius
	var length := raw.length()
	if length < dead_zone:
		output = Vector2.ZERO
	else:
		# Rescale so output ramps smoothly from 0 at the dead zone to 1 at the rim.
		output = raw.normalized() * minf((length - dead_zone) / (1.0 - dead_zone), 1.0)
	GameInput.joystick_vector = output
	stick_changed.emit(output)


func _release() -> void:
	_touch_index = -1
	output = Vector2.ZERO
	GameInput.joystick_vector = Vector2.ZERO
	stick_changed.emit(output)


func _to_local(screen_pos: Vector2) -> Vector2:
	return get_global_transform_with_canvas().affine_inverse() * screen_pos


func _rest_center() -> Vector2:
	return _clamp_center(Vector2(rest_offset.x, size.y + rest_offset.y))


func _reset_center() -> void:
	_center = _rest_center()


## Keeps the whole base inside the zone so it is never drawn off-screen.
func _clamp_center(p: Vector2) -> Vector2:
	var margin := base_radius + 8.0
	return Vector2(
		clampf(p.x, margin, maxf(size.x - margin, margin)),
		clampf(p.y, margin, maxf(size.y - margin, margin)))
