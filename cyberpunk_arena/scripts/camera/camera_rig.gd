class_name CameraRig
extends Node3D
## Smooth third-person orbit camera.
##
## Hierarchy: CameraRig (yaw) -> Pitch (pitch) -> SpringArm3D -> Camera3D.
## * Follows the target smoothly (exponential smoothing, frame-rate independent).
## * Touch-drag anywhere not used by the on-screen controls rotates the view.
##   Each finger is tracked by index, so dragging works while the joystick
##   and buttons are held (multitouch).
## * The SpringArm3D sphere-casts against world geometry so the camera never
##   clips through the floor, walls or pillars; the pitch is also clamped so
##   the camera cannot dip below floor level.
## * Optionally eases back behind the player while moving forward.

@export var target: Player
@export var follow_height := 1.4
@export var distance := 4.3
@export var min_pitch_degrees := -62.0
@export var max_pitch_degrees := 12.0
@export var start_pitch_degrees := -16.0
## Radians of rotation per full screen-height of finger drag.
@export var touch_sensitivity := 3.2
## Radians per second for keyboard / gamepad camera input.
@export var stick_speed := 2.6
@export var follow_smoothing := 14.0
@export var rotation_smoothing := 16.0
@export var min_height_above_floor := 0.3
@export_group("Auto follow")
@export var auto_follow := true
@export var auto_follow_delay := 1.0
@export var auto_follow_strength := 1.4

@onready var _pitch_node: Node3D = $Pitch
@onready var _spring: SpringArm3D = $Pitch/SpringArm3D
@onready var camera: Camera3D = $Pitch/SpringArm3D/Camera3D

var _yaw := 0.0
var _pitch := 0.0
var _target_yaw := 0.0
var _target_pitch := 0.0
var _drag_index := -1
var _since_manual := 100.0
var _shake := 0.0


func _ready() -> void:
	# Moved every rendered frame, so it must not be physics-interpolated.
	physics_interpolation_mode = Node.PHYSICS_INTERPOLATION_MODE_OFF
	_spring.spring_length = distance
	_pitch = deg_to_rad(start_pitch_degrees)
	_target_pitch = _pitch
	if target:
		_spring.add_excluded_object(target.get_rid())
		_yaw = target.get_facing_yaw()
		_target_yaw = _yaw
		global_position = _focus_point()
	_apply_rotation()
	camera.make_current()


func get_yaw() -> float:
	return _yaw


func add_shake(amount: float) -> void:
	_shake = minf(_shake + amount, 0.35)


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and _drag_index == -1:
			_drag_index = event.index
		elif not event.pressed and event.index == _drag_index:
			_drag_index = -1
	elif event is InputEventScreenDrag and event.index == _drag_index:
		var screen_h := maxf(get_viewport().get_visible_rect().size.y, 1.0)
		var rel: Vector2 = event.relative / screen_h
		_target_yaw -= rel.x * touch_sensitivity
		_target_pitch -= rel.y * touch_sensitivity * 0.7
		_clamp_pitch()
		_since_manual = 0.0


func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT or what == NOTIFICATION_WM_WINDOW_FOCUS_OUT:
		_drag_index = -1


func _process(delta: float) -> void:
	var stick := GameInput.get_camera_vector()
	if stick.length_squared() > 0.0:
		_target_yaw -= stick.x * stick_speed * delta
		_target_pitch -= stick.y * stick_speed * 0.7 * delta
		_clamp_pitch()
		_since_manual = 0.0
	_since_manual += delta

	if target:
		_update_auto_follow(delta)
		var follow := 1.0 - exp(-follow_smoothing * delta)
		global_position = global_position.lerp(_focus_point(), follow)

	var turn := 1.0 - exp(-rotation_smoothing * delta)
	_yaw = lerp_angle(_yaw, _target_yaw, turn)
	_pitch = lerpf(_pitch, _target_pitch, turn)
	_apply_rotation()
	_update_shake(delta)


func _focus_point() -> Vector3:
	return target.get_global_transform_interpolated().origin + Vector3.UP * follow_height


func _update_auto_follow(delta: float) -> void:
	if not auto_follow or _drag_index != -1 or _since_manual < auto_follow_delay:
		return
	var speed := Vector2(target.velocity.x, target.velocity.z).length()
	if speed < 0.5 or target.is_attacking():
		return
	var behind := target.get_facing_yaw()
	# Only recenter when running roughly away from the camera; never swing
	# around when the player deliberately walks toward or across the view.
	if absf(angle_difference(_target_yaw, behind)) > deg_to_rad(70.0):
		return
	var ratio := clampf(speed / target.run_speed, 0.0, 1.0)
	_target_yaw = lerp_angle(_target_yaw, behind, 1.0 - exp(-auto_follow_strength * ratio * delta))


func _apply_rotation() -> void:
	rotation = Vector3(0.0, _yaw, 0.0)
	_pitch_node.rotation = Vector3(_pitch, 0.0, 0.0)


## Positive pitch lowers the camera. Besides the designer limits, the upward
## look is capped analytically so the camera stays min_height_above_floor
## above the ground the player stands on (the spring arm handles walls).
func _clamp_pitch() -> void:
	var floor_limit := asin(clampf((follow_height - min_height_above_floor) / distance, -1.0, 1.0))
	var max_pitch := minf(deg_to_rad(max_pitch_degrees), floor_limit)
	_target_pitch = clampf(_target_pitch, deg_to_rad(min_pitch_degrees), max_pitch)


func _update_shake(delta: float) -> void:
	if _shake <= 0.0:
		camera.h_offset = 0.0
		camera.v_offset = 0.0
		return
	_shake = maxf(_shake - delta * 1.6, 0.0)
	var strength := _shake * _shake
	camera.h_offset = randf_range(-1.0, 1.0) * strength
	camera.v_offset = randf_range(-1.0, 1.0) * strength
