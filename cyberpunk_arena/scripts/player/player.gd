class_name Player
extends CharacterBody3D
## Third-person player: camera-relative movement with acceleration and
## deceleration, gravity, natural turning, walk/run, and a melee attack with
## cooldown, buffered input, soft target assist and a timed hitbox.

signal attack_started
signal hit_landed(total_hits: int)

@export_group("Movement")
@export var walk_speed := 2.6
@export var run_speed := 6.0
@export var acceleration := 16.0
@export var deceleration := 20.0
@export_range(0.0, 1.0) var air_control := 0.3
## Higher = snappier turning toward the movement direction.
@export var turn_smoothing := 11.0
@export var max_fall_speed := 40.0
## Run toggle switches off after standing still this long.
@export var run_auto_cancel_time := 0.6

@export_group("Combat")
@export var attack_anim_speed := 1.5
## Delay after an attack ends before the next one may start.
@export var attack_cooldown := 0.15
## Fraction of the attack (0..1) during which the hitbox is live.
@export var hit_window_start := 0.3
@export var hit_window_end := 0.62
## Movement speed multiplier while attacking.
@export_range(0.0, 1.0) var attack_move_factor := 0.2
@export var attack_lunge_speed := 3.2
## Auto-face the nearest enemy within this range when attacking.
@export var target_assist_range := 3.5
@export var target_assist_angle := 110.0

@export_group("Nodes")
@export var camera_rig: CameraRig

@onready var visual: Node3D = $Visual
@onready var model: WarriorModel = $Visual/WarriorModel
@onready var hitbox: AttackHitbox = $Visual/AttackHitbox
@onready var slash_fx: SlashEffect = $Visual/SlashEffect

var gravity: float = ProjectSettings.get_setting("physics/3d/default_gravity")
var total_hits := 0

var _facing_yaw := 0.0
var _attack_time := 0.0
var _attack_duration := 0.0
var _cooldown_left := 0.0
var _attacking := false
var _idle_time := 0.0


func _ready() -> void:
	_facing_yaw = visual.rotation.y
	hitbox.source = self
	hitbox.hit_landed.connect(_on_hit_landed)
	floor_snap_length = 0.35
	floor_max_angle = deg_to_rad(46.0)
	floor_constant_speed = true


func _physics_process(delta: float) -> void:
	var input := GameInput.get_move_vector()
	var move_dir := _camera_relative(input)
	var running := GameInput.is_running()

	_update_attack(delta)
	if not _attacking and _cooldown_left <= 0.0 and GameInput.consume_attack() and is_on_floor():
		_start_attack(move_dir)

	# --- Horizontal movement with acceleration / deceleration -------------
	var max_speed := run_speed if running else walk_speed
	var target := move_dir * max_speed
	if _attacking:
		target *= attack_move_factor
	var horizontal := Vector3(velocity.x, 0.0, velocity.z)
	var accelerating := target.length_squared() > 0.0001 and target.dot(horizontal) >= 0.0
	var rate := acceleration if accelerating else deceleration
	if not is_on_floor():
		rate *= air_control
	horizontal = horizontal.move_toward(target, rate * delta)
	velocity.x = horizontal.x
	velocity.z = horizontal.z

	# --- Gravity -----------------------------------------------------------
	if is_on_floor():
		velocity.y = 0.0
	else:
		velocity.y = maxf(velocity.y - gravity * delta, -max_fall_speed)

	move_and_slide()

	# --- Facing ------------------------------------------------------------
	var ground_speed := Vector2(velocity.x, velocity.z).length()
	if not _attacking and move_dir.length_squared() > 0.0025:
		_facing_yaw = atan2(-move_dir.x, -move_dir.z)
	visual.rotation.y = lerp_angle(visual.rotation.y, _facing_yaw, 1.0 - exp(-turn_smoothing * delta))

	# --- Animation ---------------------------------------------------------
	var state := WarriorModel.State.IDLE
	if ground_speed > 0.15 and input.length_squared() > 0.0001:
		var run_threshold := lerpf(walk_speed, run_speed, 0.35)
		state = WarriorModel.State.RUN if ground_speed > run_threshold else WarriorModel.State.WALK
	elif ground_speed > 0.6:
		state = WarriorModel.State.WALK
	var lean := clampf((ground_speed - walk_speed) / maxf(run_speed - walk_speed, 0.01), 0.0, 1.0)
	model.set_locomotion(state, ground_speed, lean)

	# --- Auto cancel run toggle when standing still --------------------------
	if input.length_squared() < 0.0001 and not _attacking:
		_idle_time += delta
		if _idle_time > run_auto_cancel_time:
			GameInput.set_run_enabled(false)
	else:
		_idle_time = 0.0


## 0 = ready, 1 = just attacked. Used by the HUD cooldown ring.
func get_attack_cooldown_ratio() -> float:
	if _attacking:
		return 1.0
	return clampf(_cooldown_left / maxf(attack_cooldown, 0.001), 0.0, 1.0)


func get_facing_yaw() -> float:
	return visual.rotation.y


func is_attacking() -> bool:
	return _attacking


func _camera_relative(input: Vector2) -> Vector3:
	var yaw := camera_rig.get_yaw() if camera_rig else 0.0
	var forward := Vector3(-sin(yaw), 0.0, -cos(yaw))
	var right := Vector3(cos(yaw), 0.0, -sin(yaw))
	return right * input.x - forward * input.y


func _start_attack(move_dir: Vector3) -> void:
	var target := _find_assist_target()
	if target:
		var to_target := target.global_position - global_position
		_facing_yaw = atan2(-to_target.x, -to_target.z)
	elif move_dir.length_squared() > 0.0025:
		_facing_yaw = atan2(-move_dir.x, -move_dir.z)
	visual.rotation.y = _facing_yaw
	_attacking = true
	_attack_time = 0.0
	_attack_duration = model.play_attack(attack_anim_speed)
	var forward := Vector3(-sin(_facing_yaw), 0.0, -cos(_facing_yaw))
	velocity += forward * attack_lunge_speed
	attack_started.emit()


func _update_attack(delta: float) -> void:
	if not _attacking:
		_cooldown_left = maxf(_cooldown_left - delta, 0.0)
		return
	_attack_time += delta
	var progress := _attack_time / maxf(_attack_duration, 0.001)
	var in_window := progress >= hit_window_start and progress <= hit_window_end
	if in_window and not hitbox.is_active():
		hitbox.begin_swing()
		slash_fx.play(_attack_duration * (hit_window_end - hit_window_start))
	elif not in_window and hitbox.is_active():
		hitbox.end_swing()
	if progress >= 1.0:
		_attacking = false
		_cooldown_left = attack_cooldown
		if hitbox.is_active():
			hitbox.end_swing()
		model.finish_attack()


func _find_assist_target() -> Node3D:
	var best: Node3D = null
	var best_dist := target_assist_range
	var forward := Vector3(-sin(visual.rotation.y), 0.0, -cos(visual.rotation.y))
	var input := _camera_relative(GameInput.get_move_vector())
	if input.length_squared() > 0.01:
		forward = input.normalized()
	for node in get_tree().get_nodes_in_group("enemies"):
		var enemy := node as Node3D
		if enemy == null:
			continue
		var to_enemy := enemy.global_position - global_position
		to_enemy.y = 0.0
		var dist := to_enemy.length()
		if dist > best_dist or dist < 0.01:
			continue
		if rad_to_deg(forward.angle_to(to_enemy)) > target_assist_angle:
			continue
		best = enemy
		best_dist = dist
	return best


func _on_hit_landed(_hurtbox: Hurtbox, _damage: int) -> void:
	total_hits += 1
	hit_landed.emit(total_hits)
	if camera_rig:
		camera_rig.add_shake(0.12)
	if OS.has_feature("mobile"):
		Input.vibrate_handheld(25)
