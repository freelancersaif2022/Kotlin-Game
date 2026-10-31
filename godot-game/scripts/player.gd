class_name Player
extends CharacterBody3D
## The Skeleton Knight: third-person movement relative to the camera, a two-hit
## sword combo with soft lock-on, a ground-slam special and jumping.

signal health_changed(current: float, maximum: float)
signal special_changed(ratio: float)
signal died

const SPEED := 6.5
const ACCEL := 12.0
const JUMP_VELOCITY := 7.5
const GRAVITY := 22.0
const TURN_SPEED := 12.0
const ATTACK_RANGE := 2.8
const LOCK_ON_RANGE := 5.0
const SPECIAL_RANGE := 5.5
const SPECIAL_COOLDOWN := 7.0

# Attack combo: animation, playback speed, hit time (s), lock time (s), damage multiplier.
const COMBO := [
	{"anim": "Throw", "speed": 1.9, "hit": 0.26, "lock": 0.5, "mult": 1.0},
	{"anim": "Use_Item", "speed": 2.2, "hit": 0.28, "lock": 0.52, "mult": 1.0},
	{"anim": "Throw", "speed": 1.6, "hit": 0.32, "lock": 0.62, "mult": 1.6},
]

var max_health := 100.0
var health := 100.0
var damage := 26.0
var is_dead := false
var input_enabled := false
var camera_yaw := 0.0
var model: CharacterModel
var game: Node  # Game, for effects and hit-stop

var _action_time := 0.0      # time left before the player may act again
var _pending_hit := -1.0     # countdown to the active attack's hit frame
var _pending_mult := 1.0
var _combo_index := 0
var _combo_window := 0.0
var _queued_attack := false
var _special_cd := 0.0
var _special_pending := -1.0
var _invulnerable := 0.0
var _airborne := false


func _ready() -> void:
	add_to_group("player")
	collision_layer = 2
	collision_mask = 1 | 4
	var shape := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.45
	capsule.height = 1.9
	shape.shape = capsule
	shape.position.y = 0.95
	add_child(shape)
	model = CharacterModel.new()
	add_child(model)
	model.setup("res://assets/characters/Skeleton_Warrior.glb",
		"res://assets/weapons/Skeleton_Blade.gltf",
		"res://assets/weapons/Skeleton_Shield_Large_A.gltf")
	model.play("Idle_A")


func reset(at: Vector3) -> void:
	global_position = at
	velocity = Vector3.ZERO
	health = max_health
	is_dead = false
	_action_time = 0.0
	_pending_hit = -1.0
	_special_cd = 0.0
	_special_pending = -1.0
	_invulnerable = 0.0
	model.restart("Idle_A")
	health_changed.emit(health, max_health)
	special_changed.emit(1.0)


func _physics_process(delta: float) -> void:
	if is_dead:
		velocity.x = move_toward(velocity.x, 0, ACCEL * delta)
		velocity.z = move_toward(velocity.z, 0, ACCEL * delta)
		velocity.y -= GRAVITY * delta
		move_and_slide()
		return

	_action_time = maxf(0.0, _action_time - delta)
	_combo_window = maxf(0.0, _combo_window - delta)
	_invulnerable = maxf(0.0, _invulnerable - delta)
	if _special_cd > 0.0:
		_special_cd = maxf(0.0, _special_cd - delta)
		special_changed.emit(1.0 - _special_cd / SPECIAL_COOLDOWN)

	if _pending_hit >= 0.0:
		_pending_hit -= delta
		if _pending_hit < 0.0:
			_resolve_attack(_pending_mult)
	if _special_pending >= 0.0:
		_special_pending -= delta
		if _special_pending < 0.0:
			_resolve_special()

	var input := Vector2.ZERO
	if input_enabled:
		input = Input.get_vector("move_left", "move_right", "move_forward", "move_back")
		if Input.is_action_just_pressed("attack"):
			_queued_attack = true
		if Input.is_action_just_pressed("special"):
			_try_special()
		if Input.is_action_just_pressed("jump") and is_on_floor() and _action_time <= 0.0:
			velocity.y = JUMP_VELOCITY
			model.restart("Jump_Start", 0.05, 1.4)
			Sfx.play("jump", -6.0)
	if _queued_attack and _action_time <= 0.1:
		_queued_attack = false
		_start_attack()

	var dir := Vector3(input.x, 0, input.y).rotated(Vector3.UP, camera_yaw)
	var speed := SPEED
	if _action_time > 0.0:
		speed *= 0.25  # commit to attacks
	velocity.x = move_toward(velocity.x, dir.x * speed, ACCEL * speed * delta)
	velocity.z = move_toward(velocity.z, dir.z * speed, ACCEL * speed * delta)
	velocity.y -= GRAVITY * delta
	move_and_slide()

	if dir.length() > 0.1 and _action_time <= 0.0:
		_face(dir, delta)

	_update_animation(input.length())


func _face(dir: Vector3, delta: float) -> void:
	var target := atan2(dir.x, dir.z)
	model.rotation.y = lerp_angle(model.rotation.y, target, clampf(TURN_SPEED * delta, 0, 1))


func _update_animation(input_strength: float) -> void:
	if _action_time > 0.0:
		return
	if not is_on_floor():
		_airborne = true
		if model.current != "Jump_Start" or not model.anim.is_playing():
			model.play("Jump_Idle", 0.2)
		return
	if _airborne:
		_airborne = false
		model.restart("Jump_Land", 0.05, 1.5)
		_action_time = 0.12
		return
	var horizontal := Vector2(velocity.x, velocity.z).length()
	if horizontal > SPEED * 0.6:
		model.play("Running_A", 0.15, horizontal / SPEED)
	elif horizontal > 0.4:
		model.play("Walking_A", 0.15, clampf(horizontal / 3.0, 0.8, 1.4))
	else:
		model.play("Idle_A", 0.25)


func _start_attack() -> void:
	if _combo_window <= 0.0:
		_combo_index = 0
	var step: Dictionary = COMBO[_combo_index]
	_combo_index = (_combo_index + 1) % COMBO.size()
	_combo_window = step["lock"] + 0.45
	var target := _nearest_enemy(LOCK_ON_RANGE)
	if target:
		var to := target.global_position - global_position
		model.rotation.y = atan2(to.x, to.z)
		# Small lunge toward the target so attacks feel connected.
		var lunge := Vector3(to.x, 0, to.z).normalized() * 4.0
		velocity.x = lunge.x
		velocity.z = lunge.z
	model.restart(step["anim"], 0.05, step["speed"])
	_action_time = step["lock"]
	_pending_hit = step["hit"]
	_pending_mult = step["mult"]
	Sfx.play("swing", -4.0, 0.15)


func _resolve_attack(mult: float) -> void:
	var forward := Vector3(sin(model.rotation.y), 0, cos(model.rotation.y))
	var hit_any := false
	for e in get_tree().get_nodes_in_group("enemies"):
		if e.is_dead:
			continue
		var to: Vector3 = e.global_position - global_position
		to.y = 0
		var reach: float = ATTACK_RANGE + e.radius
		if to.length() > reach:
			continue
		if to.length() > 0.8 and forward.dot(to.normalized()) < 0.25:
			continue
		e.take_damage(damage * mult * randf_range(0.9, 1.1), to.normalized() * (6.0 * mult))
		hit_any = true
	if hit_any:
		Sfx.play("hit", -2.0)
		if game:
			game.hit_stop(0.06 if mult < 1.5 else 0.1)
			game.shake(0.15 * mult)


func _try_special() -> void:
	if _special_cd > 0.0 or _action_time > 0.0 or not is_on_floor():
		return
	_special_cd = SPECIAL_COOLDOWN
	special_changed.emit(0.0)
	model.restart("PickUp", 0.05, 1.8)
	_action_time = 0.65
	_special_pending = 0.32
	_invulnerable = 0.7


func _resolve_special() -> void:
	Sfx.play("bone_break", 0.0, 0.05)
	Sfx.play("hit", 0.0)
	if game:
		game.shockwave(global_position, SPECIAL_RANGE)
		game.shake(0.45)
	for e in get_tree().get_nodes_in_group("enemies"):
		if e.is_dead:
			continue
		var to: Vector3 = e.global_position - global_position
		to.y = 0
		if to.length() <= SPECIAL_RANGE + e.radius:
			var push := to.normalized() * 11.0 + Vector3.UP * 4.0
			e.take_damage(damage * 1.8, push)


func _nearest_enemy(max_dist: float) -> Node3D:
	var best: Node3D = null
	var best_d := max_dist
	for e in get_tree().get_nodes_in_group("enemies"):
		if e.is_dead:
			continue
		var d: float = global_position.distance_to(e.global_position) - e.radius
		if d < best_d:
			best_d = d
			best = e
	return best


func take_damage(amount: float, knockback := Vector3.ZERO) -> void:
	if is_dead or _invulnerable > 0.0:
		return
	health = maxf(0.0, health - amount)
	health_changed.emit(health, max_health)
	model.flash()
	velocity += knockback
	_invulnerable = 0.45
	if game:
		game.player_hurt_feedback()
		game.spawn_damage_number(global_position + Vector3.UP * 2.2, amount, Color(1, 0.35, 0.3))
	Sfx.play("hurt", -2.0)
	if health <= 0.0:
		is_dead = true
		_pending_hit = -1.0
		_special_pending = -1.0
		model.restart("Death_A", 0.1)
		died.emit()
	elif _action_time <= 0.0:
		model.restart("Hit_A", 0.05, 1.6)
		_action_time = 0.3


func heal(amount: float) -> void:
	health = minf(max_health, health + amount)
	health_changed.emit(health, max_health)
	model.flash(Color(0.4, 1.0, 0.5, 0.5), 0.25)
	Sfx.play("heal", -4.0, 0.0)
