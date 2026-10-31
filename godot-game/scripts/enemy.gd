class_name Enemy
extends CharacterBody3D
## An undead skeleton. Chases the player over the navigation mesh, attacks in
## melee (minion, rogue, boss) or casts magic bolts from range (mage).

signal died(enemy: Enemy)

const GRAVITY := 22.0
const TYPES := {
	"minion": {
		"model": "Skeleton_Minion", "right": "Skeleton_Axe", "left": "",
		"health": 60.0, "speed": 3.4, "damage": 10.0, "range": 1.9, "cooldown": 1.5,
		"windup": 0.45, "attack_anim": "Throw", "attack_speed": 1.4, "score": 10, "scale": 1.0,
	},
	"rogue": {
		"model": "Skeleton_Rogue", "right": "Skeleton_Blade", "left": "",
		"health": 45.0, "speed": 5.2, "damage": 8.0, "range": 1.8, "cooldown": 1.0,
		"windup": 0.32, "attack_anim": "Use_Item", "attack_speed": 2.0, "score": 15, "scale": 0.95,
	},
	"mage": {
		"model": "Skeleton_Mage", "right": "Skeleton_Staff", "left": "",
		"health": 40.0, "speed": 3.0, "damage": 12.0, "range": 11.0, "cooldown": 2.6,
		"windup": 0.55, "attack_anim": "Throw", "attack_speed": 1.2, "score": 20, "scale": 1.0,
	},
	"boss": {
		"model": "Skeleton_Warrior", "right": "Skeleton_Axe", "left": "Skeleton_Shield_Large_B",
		"health": 650.0, "speed": 3.3, "damage": 24.0, "range": 3.4, "cooldown": 1.8,
		"windup": 0.6, "attack_anim": "Throw", "attack_speed": 1.1, "score": 250, "scale": 1.9,
	},
}

var kind := "minion"
var stats: Dictionary
var max_health := 60.0
var health := 60.0
var radius := 0.45
var is_dead := false
var elite := false
var target: Player
var game: Node
var model: CharacterModel

var _agent: NavigationAgent3D
var _repath := 0.0
var _cooldown := 1.0
var _windup := -1.0
var _busy := 0.0          # spawn / attack / stagger lock
var _knockback := Vector3.ZERO
var _bar_fill: QuadMesh
var _bar_width := 1.2
var _bar_root: Node3D
var _bar_show := 0.0


func configure(enemy_kind: String, is_elite := false, wave := 1) -> void:
	kind = enemy_kind
	elite = is_elite
	stats = TYPES[kind].duplicate()
	var hp_scale := 1.0 + (wave - 1) * 0.08
	max_health = stats["health"] * hp_scale * (1.8 if elite else 1.0)
	stats["damage"] *= 1.0 + (wave - 1) * 0.05 + (0.3 if elite else 0.0)
	health = max_health


func _ready() -> void:
	add_to_group("enemies")
	collision_layer = 4
	if kind == "boss":
		collision_layer |= 16  # the camera spring arm avoids the huge boss
	collision_mask = 1 | 2 | 4
	var s: float = stats["scale"] * (1.2 if elite else 1.0)
	radius = 0.45 * s
	var shape := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = radius
	capsule.height = 1.9 * s
	shape.shape = capsule
	shape.position.y = 0.95 * s
	add_child(shape)

	model = CharacterModel.new()
	model.scale = Vector3.ONE * s
	add_child(model)
	var w := "res://assets/weapons/%s.gltf"
	model.setup("res://assets/characters/%s.glb" % stats["model"],
		w % stats["right"] if stats["right"] != "" else "",
		w % stats["left"] if stats["left"] != "" else "")
	if kind == "boss":
		model.set_tint(Color(0.32, 0.04, 0.0, 0.5))
	elif elite:
		model.set_tint(Color(0.22, 0.05, 0.38, 0.5))

	_agent = NavigationAgent3D.new()
	_agent.path_desired_distance = 0.8
	_agent.target_desired_distance = 1.0
	_agent.radius = radius
	add_child(_agent)
	_build_health_bar(2.5 * s)

	# Rise out of the ground.
	model.restart("Spawn_Ground", 0.0, 1.2)
	_busy = model.anim_length("Spawn_Ground") / 1.2
	_cooldown = randf_range(0.6, 1.4)
	if game:
		game.dust(global_position, 1.0 * s)


func _build_health_bar(height: float) -> void:
	_bar_root = Node3D.new()
	_bar_root.position.y = height
	add_child(_bar_root)
	_bar_root.add_child(_bar_quad(_bar_width + 0.06, 0.18, Color(0, 0, 0, 0.65), 0))
	var fill := _bar_quad(_bar_width, 0.12, Color(0.85, 0.2, 0.15), 1)
	_bar_fill = fill.mesh
	_bar_root.add_child(fill)
	_bar_root.visible = false


func _bar_quad(w: float, h: float, color: Color, priority: int) -> MeshInstance3D:
	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	mat.billboard_keep_scale = true
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mat.no_depth_test = true
	mat.render_priority = priority
	mat.albedo_color = color
	var quad := QuadMesh.new()
	quad.size = Vector2(w, h)
	quad.material = mat
	var mi := MeshInstance3D.new()
	mi.mesh = quad
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	return mi


func _physics_process(delta: float) -> void:
	velocity.y -= GRAVITY * delta
	_knockback = _knockback.move_toward(Vector3.ZERO, 30.0 * delta)
	if is_dead:
		velocity.x = _knockback.x
		velocity.z = _knockback.z
		move_and_slide()
		return

	_busy = maxf(0.0, _busy - delta)
	_cooldown = maxf(0.0, _cooldown - delta)
	if _bar_show > 0.0 and kind != "boss":
		_bar_show -= delta
		_bar_root.visible = _bar_show > 0.0

	if _windup >= 0.0:
		_windup -= delta
		if _windup < 0.0:
			_perform_attack()

	var desired := Vector3.ZERO
	if target and not target.is_dead and _busy <= 0.0:
		desired = _think(delta)
	elif _busy <= 0.0:
		model.play("Idle_B", 0.3)

	desired += _separation() * 2.0
	velocity.x = lerpf(velocity.x, desired.x, clampf(8.0 * delta, 0, 1)) + _knockback.x
	velocity.z = lerpf(velocity.z, desired.z, clampf(8.0 * delta, 0, 1)) + _knockback.z
	move_and_slide()
	velocity.x -= _knockback.x
	velocity.z -= _knockback.z


func _think(delta: float) -> Vector3:
	var to_player := target.global_position - global_position
	to_player.y = 0
	var dist := to_player.length()
	_face(to_player, delta)
	var speed: float = stats["speed"] * (1.1 if elite else 1.0)
	var attack_range: float = stats["range"] + target_radius()

	if kind == "mage":
		if dist < 5.0:
			# Back away from the player while keeping line of fire.
			model.play("Walking_C", 0.2, 1.3)
			return -to_player.normalized() * speed * 0.8
		if dist <= attack_range and _cooldown <= 0.0:
			_begin_attack()
			return Vector3.ZERO
		if dist <= attack_range * 0.8:
			model.play("Idle_B", 0.3)
			return Vector3.ZERO
	elif dist <= attack_range:
		if _cooldown <= 0.0:
			_begin_attack()
		else:
			model.play("Idle_A", 0.25)
		return Vector3.ZERO

	model.play("Running_B" if speed > 4.0 else "Walking_B", 0.2, clampf(speed / 3.5, 0.9, 1.4))
	return _steer_towards(target.global_position) * speed


func target_radius() -> float:
	return 0.45


func _steer_towards(goal: Vector3) -> Vector3:
	_repath -= get_physics_process_delta_time()
	if _repath <= 0.0:
		_repath = 0.3
		_agent.target_position = goal
	var next := goal
	if NavigationServer3D.map_get_iteration_id(_agent.get_navigation_map()) > 0 and not _agent.is_navigation_finished():
		next = _agent.get_next_path_position()
	var dir := next - global_position
	dir.y = 0
	return dir.normalized() if dir.length() > 0.05 else Vector3.ZERO


func _separation() -> Vector3:
	var push := Vector3.ZERO
	for other in get_tree().get_nodes_in_group("enemies"):
		if other == self:
			continue
		var d: Vector3 = global_position - other.global_position
		d.y = 0
		var min_d: float = radius + other.radius + 0.3
		if d.length() < min_d and d.length() > 0.001:
			push += d.normalized() * (min_d - d.length())
	return push


func _face(dir: Vector3, delta: float) -> void:
	if dir.length() < 0.01:
		return
	model.rotation.y = lerp_angle(model.rotation.y, atan2(dir.x, dir.z), clampf(8.0 * delta, 0, 1))


func _begin_attack() -> void:
	var anim_speed: float = stats["attack_speed"]
	model.restart(stats["attack_anim"], 0.08, anim_speed)
	_windup = stats["windup"]
	_busy = stats["windup"] + 0.45
	_cooldown = stats["cooldown"] * randf_range(0.85, 1.2)
	if kind == "mage":
		Sfx.play("magic", -6.0)


func _perform_attack() -> void:
	if not target or target.is_dead:
		return
	if kind == "mage":
		var bolt := MagicBolt.new()
		get_parent().add_child(bolt)
		bolt.launch(global_position + Vector3.UP * 1.6, target, stats["damage"])
		return
	Sfx.play("swing", -8.0, 0.2)
	var to := target.global_position - global_position
	to.y = 0
	var forward := Vector3(sin(model.rotation.y), 0, cos(model.rotation.y))
	if to.length() <= stats["range"] + target_radius() + 0.4 and forward.dot(to.normalized()) > 0.3:
		target.take_damage(stats["damage"], to.normalized() * (8.0 if kind == "boss" else 4.0))
		if kind == "boss" and game:
			game.shake(0.35)


func take_damage(amount: float, knockback := Vector3.ZERO) -> void:
	if is_dead:
		return
	health -= amount
	model.flash(Color(1, 1, 1, 0.6), 0.1)
	_bar_show = 2.5
	_bar_root.visible = kind != "boss"  # the boss uses the big HUD bar
	# Billboards ignore node offsets, so shrink the fill quad toward its left edge.
	var ratio := maxf(0.0, health / max_health)
	_bar_fill.size.x = maxf(0.001, _bar_width * ratio)
	_bar_fill.center_offset.x = -_bar_width * 0.5 + _bar_width * ratio * 0.5
	var resist := 0.25 if kind == "boss" else 1.0
	_knockback = Vector3(knockback.x, 0, knockback.z) * resist
	if knockback.y > 0.0 and kind != "boss":
		velocity.y = knockback.y
	if game:
		game.spawn_damage_number(global_position + Vector3.UP * (2.2 * stats["scale"]), amount, Color(1, 0.9, 0.4))
		game.spark(global_position + Vector3.UP * 1.1 * stats["scale"])
	if health <= 0.0:
		_die()
	elif kind != "boss" and _windup < 0.0:
		model.restart("Hit_B" if randf() < 0.5 else "Hit_A", 0.05, 1.5)
		_busy = 0.35
	elif kind != "boss":
		# Heavy hits interrupt a wind-up.
		if amount > 30.0:
			_windup = -1.0
			model.restart("Hit_A", 0.05, 1.5)
			_busy = 0.35


func _die() -> void:
	is_dead = true
	_windup = -1.0
	remove_from_group("enemies")
	collision_layer = 0
	collision_mask = 1
	_bar_root.visible = false
	model.restart("Death_A" if randf() < 0.5 else "Death_B", 0.05, 1.2)
	Sfx.play("bone_break", -1.0)
	died.emit(self)
	var tween := create_tween()
	tween.tween_interval(2.6)
	tween.tween_property(self, "position:y", position.y - 1.2, 1.2)
	tween.tween_callback(queue_free)
