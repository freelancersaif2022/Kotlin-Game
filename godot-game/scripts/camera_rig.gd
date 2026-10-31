class_name CameraRig
extends Node3D
## Third-person orbit camera on a spring arm, with screen shake.
## Mouse look on desktop, touch-drag look on mobile (fed by TouchControls).

const MOUSE_SENSITIVITY := 0.0035
const TOUCH_SENSITIVITY := 0.006
const MIN_PITCH := -1.15
const MAX_PITCH := -0.12

var target: Node3D
var yaw := 0.0
var pitch := -0.42
var camera: Camera3D
var orbit_menu := false   # slow cinematic orbit behind the menus

var _arm: SpringArm3D
var _shake := 0.0


func _ready() -> void:
	_arm = SpringArm3D.new()
	_arm.spring_length = 7.0
	_arm.margin = 0.3
	_arm.collision_mask = 1 | 16
	var probe := SphereShape3D.new()
	probe.radius = 0.25
	_arm.shape = probe
	add_child(_arm)
	camera = Camera3D.new()
	camera.fov = 62.0
	camera.far = 160.0
	_arm.add_child(camera)
	camera.current = true


func look(delta_pixels: Vector2, sensitivity: float) -> void:
	yaw -= delta_pixels.x * sensitivity
	pitch = clampf(pitch - delta_pixels.y * sensitivity, MIN_PITCH, MAX_PITCH)


func shake(amount: float) -> void:
	_shake = minf(_shake + amount, 0.8)


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		look(event.relative, MOUSE_SENSITIVITY)


func _process(delta: float) -> void:
	if orbit_menu:
		yaw += delta * 0.08
		pitch = lerpf(pitch, -0.3, delta)
	if target:
		var goal := target.global_position + Vector3.UP * 1.7
		global_position = global_position.lerp(goal, clampf(delta * 10.0, 0, 1))
	rotation = Vector3(pitch, yaw, 0)
	_arm.spring_length = lerpf(_arm.spring_length, 11.0 if orbit_menu else 7.0, delta * 2.0)
	if _shake > 0.0:
		_shake = maxf(0.0, _shake - delta * 1.8)
		var s := _shake * _shake
		camera.h_offset = randf_range(-1, 1) * s * 0.6
		camera.v_offset = randf_range(-1, 1) * s * 0.6
	else:
		camera.h_offset = 0.0
		camera.v_offset = 0.0
