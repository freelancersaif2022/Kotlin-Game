extends Node
## Global input hub (autoload "GameInput").
##
## Merges the on-screen touch controls with keyboard / gamepad actions so the
## player only reads one source of truth. Touch widgets write into this node;
## gameplay code reads from it.

signal run_toggled(enabled: bool)
signal attack_requested

## Seconds an attack request stays buffered if the player is still busy.
const ATTACK_BUFFER_TIME := 0.3

## Analog vector written by the virtual joystick (x = right, y = back).
var joystick_vector := Vector2.ZERO
## True while the on-screen attack button is held (enables auto-repeat).
var attack_held := false

var _run_enabled := false
var _attack_buffer := 0.0


func _process(delta: float) -> void:
	_attack_buffer = maxf(_attack_buffer - delta, 0.0)


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("attack", false):
		request_attack()


## Combined movement input, length clamped to 1.0 (analog magnitude preserved).
func get_move_vector() -> Vector2:
	var keyboard := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	return (joystick_vector + keyboard).limit_length(1.0)


## Camera rotation from keyboard / right gamepad stick (touch drag is handled by the camera).
func get_camera_vector() -> Vector2:
	return Input.get_vector("camera_left", "camera_right", "camera_up", "camera_down")


func is_running() -> bool:
	return _run_enabled or Input.is_action_pressed("run")


func is_run_toggled() -> bool:
	return _run_enabled


func set_run_enabled(enabled: bool) -> void:
	if _run_enabled == enabled:
		return
	_run_enabled = enabled
	run_toggled.emit(enabled)


func request_attack() -> void:
	_attack_buffer = ATTACK_BUFFER_TIME
	attack_requested.emit()


## Returns true once per buffered attack request (or continuously while held).
func consume_attack() -> bool:
	if _attack_buffer > 0.0:
		_attack_buffer = 0.0
		return true
	return attack_held


func reset_touch_state() -> void:
	joystick_vector = Vector2.ZERO
	attack_held = false
