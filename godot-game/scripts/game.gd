extends Node3D
## Bone Forest - main game controller.
## Owns the world, player, camera and HUD; runs the wave spawner, scoring,
## menus, pause/resume, saving the best score and juice effects.

const FINAL_WAVE := 10
const BOSS_WAVES := [5, 10]
const MAX_ALIVE := 10
const SAVE_PATH := "user://save.cfg"

enum State { LOADING, MENU, PLAYING, PAUSED, OVER }

var state := State.LOADING
var world: World
var player: Player
var rig: CameraRig
var hud: HUD
var wave := 0
var score := 0
var best_score := 0
var kills := 0
var combo := 1
var use_touch := false

var _spawn_queue: Array = []
var _spawn_timer := 0.0
var _between_waves := -1.0
var _combo_timer := 0.0
var _alive := 0
var _boss: Enemy
var _hit_stop_active := false
var _fx_material: StandardMaterial3D


func _ready() -> void:
	_setup_input()
	use_touch = DisplayServer.is_touchscreen_available() or OS.has_feature("mobile")
	_load_save()

	hud = HUD.new()
	add_child(hud)
	hud.play_pressed.connect(start_game)
	hud.restart_pressed.connect(start_game)
	hud.resume_pressed.connect(resume_game)
	hud.pause_pressed.connect(pause_game)
	hud.menu_pressed.connect(show_menu)
	hud.set_loading(true)

	world = World.new()
	add_child(world)
	world.build()

	player = Player.new()
	player.game = self
	add_child(player)
	player.health_changed.connect(hud.set_health)
	player.special_changed.connect(hud.set_special)
	player.died.connect(_on_player_died)

	rig = CameraRig.new()
	add_child(rig)
	rig.target = player
	rig.yaw = PI
	rig.orbit_menu = true
	hud.touch.camera_rig = rig

	_fx_material = StandardMaterial3D.new()
	_fx_material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_fx_material.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
	_fx_material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	_fx_material.vertex_color_use_as_albedo = true
	_fx_material.albedo_texture = FX.soft_circle()

	# Let the first frame render the loading text before baking the navmesh.
	await get_tree().process_frame
	await get_tree().process_frame
	world.navigation_ready.connect(_on_world_ready)
	world.bake_navigation()


func _on_world_ready() -> void:
	hud.set_loading(false)
	Sfx.start_loops()
	show_menu()


func _setup_input() -> void:
	var keys := {
		"move_forward": [KEY_W, KEY_UP], "move_back": [KEY_S, KEY_DOWN],
		"move_left": [KEY_A, KEY_LEFT], "move_right": [KEY_D, KEY_RIGHT],
		"jump": [KEY_SPACE], "attack": [KEY_J, KEY_ENTER], "special": [KEY_E, KEY_K, KEY_SHIFT],
		"pause": [KEY_ESCAPE, KEY_P],
	}
	var pads := {"jump": JOY_BUTTON_A, "attack": JOY_BUTTON_X, "special": JOY_BUTTON_Y, "pause": JOY_BUTTON_START}
	for action in keys:
		if not InputMap.has_action(action):
			InputMap.add_action(action, 0.2)
		for key in keys[action]:
			var ev := InputEventKey.new()
			ev.physical_keycode = key
			InputMap.action_add_event(action, ev)
		if pads.has(action):
			var jb := InputEventJoypadButton.new()
			jb.button_index = pads[action]
			InputMap.action_add_event(action, jb)
	var axes := {"move_left": [JOY_AXIS_LEFT_X, -1.0], "move_right": [JOY_AXIS_LEFT_X, 1.0],
		"move_forward": [JOY_AXIS_LEFT_Y, -1.0], "move_back": [JOY_AXIS_LEFT_Y, 1.0]}
	for action in axes:
		var jm := InputEventJoypadMotion.new()
		jm.axis = axes[action][0]
		jm.axis_value = axes[action][1]
		InputMap.action_add_event(action, jm)
	if not OS.has_feature("mobile"):
		var lmb := InputEventMouseButton.new()
		lmb.button_index = MOUSE_BUTTON_LEFT
		InputMap.action_add_event("attack", lmb)
		var rmb := InputEventMouseButton.new()
		rmb.button_index = MOUSE_BUTTON_RIGHT
		InputMap.action_add_event("special", rmb)


# --- Flow ---------------------------------------------------------------------

func show_menu() -> void:
	get_tree().paused = false
	Engine.time_scale = 1.0
	state = State.MENU
	_clear_arena()
	player.reset(Vector3.ZERO)
	player.input_enabled = false
	rig.orbit_menu = true
	hud.set_playing(false, use_touch)
	var controls := "Move: WASD / Stick    Attack: Mouse / J    Slam: Right-click / E    Jump: Space" \
		if not use_touch else "Left: move    Right: look    ATTACK / SLAM / JUMP buttons"
	hud.show_screen("title", "", "Best score: %d\n%s\nArt: KayKit by Kay Lousberg (CC0)" % [best_score, controls])
	_set_mouse_captured(false)


func start_game() -> void:
	get_tree().paused = false
	Engine.time_scale = 1.0
	_clear_arena()
	wave = 0
	score = 0
	kills = 0
	combo = 1
	player.reset(Vector3.ZERO)
	player.input_enabled = true
	rig.orbit_menu = false
	rig.yaw = PI
	rig.pitch = -0.42
	hud.show_screen("")
	hud.set_playing(true, use_touch)
	hud.set_score(0, 1)
	hud.set_boss(false)
	state = State.PLAYING
	_set_mouse_captured(true)
	_between_waves = 1.2


func pause_game() -> void:
	if state != State.PLAYING:
		return
	state = State.PAUSED
	get_tree().paused = true
	hud.touch.release_all()
	hud.show_screen("pause", "", "Wave %d   -   Score %d" % [wave, score])
	_set_mouse_captured(false)


func resume_game() -> void:
	if state != State.PAUSED:
		return
	hud.show_screen("")
	get_tree().paused = false
	state = State.PLAYING
	_set_mouse_captured(true)


func _on_player_died() -> void:
	state = State.OVER
	player.input_enabled = false
	hud.set_boss(false)
	Sfx.play("game_over", -2.0, 0.0)
	_save_best()
	await get_tree().create_timer(2.2).timeout
	if state != State.OVER:
		return
	hud.set_playing(false, use_touch)
	hud.show_screen("game_over", "You reached wave %d and destroyed %d skeletons." % [wave, kills],
		"Score: %d      Best: %d" % [score, best_score])
	_set_mouse_captured(false)


func _victory() -> void:
	state = State.OVER
	player.input_enabled = false
	Sfx.play("victory", 0.0, 0.0)
	_save_best()
	hud.banner("THE FOREST IS SAFE", HUD.GOLD, 2.0)
	await get_tree().create_timer(3.0).timeout
	if state != State.OVER:
		return
	hud.set_playing(false, use_touch)
	hud.show_screen("victory", "The Bone King has fallen.\n%d skeletons returned to dust." % kills,
		"Score: %d      Best: %d" % [score, best_score])
	_set_mouse_captured(false)


func _set_mouse_captured(captured: bool) -> void:
	if use_touch or DisplayServer.get_name() == "headless":
		return
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED if captured else Input.MOUSE_MODE_VISIBLE


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		if state == State.PLAYING:
			pause_game()
		elif state == State.PAUSED:
			resume_game()
	elif state == State.PLAYING and event is InputEventMouseButton and event.pressed \
			and Input.mouse_mode != Input.MOUSE_MODE_CAPTURED and not use_touch:
		_set_mouse_captured(true)


func _notification(what: int) -> void:
	match what:
		NOTIFICATION_WM_GO_BACK_REQUEST:
			if state == State.PLAYING:
				pause_game()
			elif state == State.PAUSED:
				resume_game()
			elif state == State.MENU:
				get_tree().quit()
		NOTIFICATION_APPLICATION_PAUSED, NOTIFICATION_APPLICATION_FOCUS_OUT:
			pause_game()


# --- Waves ------------------------------------------------------------------

func _process(delta: float) -> void:
	player.camera_yaw = rig.yaw
	if state != State.PLAYING:
		return
	if _combo_timer > 0.0:
		_combo_timer -= delta
		if _combo_timer <= 0.0:
			combo = 1
			hud.set_score(score, combo)

	if _between_waves >= 0.0:
		_between_waves -= delta
		if _between_waves < 0.0:
			_start_wave(wave + 1)
		return

	if _spawn_queue.size() > 0:
		_spawn_timer -= delta
		if _spawn_timer <= 0.0 and _alive < MAX_ALIVE:
			_spawn_timer = 0.75
			_spawn(_spawn_queue.pop_front())
	elif _alive == 0:
		_wave_cleared()

	if is_instance_valid(_boss) and not _boss.is_dead:
		hud.set_boss(true, _boss.health / _boss.max_health)


func _start_wave(n: int) -> void:
	wave = n
	hud.set_wave(wave, FINAL_WAVE)
	_spawn_queue = _compose_wave(n)
	_spawn_timer = 0.5
	hud.set_enemies(_spawn_queue.size())
	if n in BOSS_WAVES:
		hud.banner("THE BONE KING RISES", Color(1, 0.45, 0.25), 2.0)
	else:
		hud.banner("WAVE %d" % n)
	Sfx.play("wave_horn", -3.0, 0.0)


func _compose_wave(n: int) -> Array:
	var list := []
	var count := 3 + n * 2
	if n in BOSS_WAVES:
		count = 2 + n
		list.append({"kind": "boss", "elite": false})
	var elite_chance := clampf((n - 3) * 0.08, 0.0, 0.4)
	for i in count:
		var r := randf()
		var kind := "minion"
		if n >= 3 and r < 0.22:
			kind = "mage"
		elif n >= 2 and r < 0.55:
			kind = "rogue"
		list.append({"kind": kind, "elite": randf() < elite_chance})
	return list


func _spawn(entry: Dictionary) -> void:
	var enemy := Enemy.new()
	enemy.configure(entry["kind"], entry["elite"], wave)
	enemy.target = player
	enemy.game = self
	enemy.position = _spawn_point()
	add_child(enemy)
	enemy.died.connect(_on_enemy_died)
	_alive += 1
	if entry["kind"] == "boss":
		_boss = enemy
		shake(0.5)


func _spawn_point() -> Vector3:
	var map := world.nav_region.get_navigation_map()
	for attempt in 12:
		var a := randf() * TAU
		var d := randf_range(13.0, 22.0)
		var p := player.global_position + Vector3(cos(a) * d, 0, sin(a) * d)
		p = p.limit_length(World.ARENA_RADIUS - 3.0)
		p.y = 0
		var snapped_p := NavigationServer3D.map_get_closest_point(map, p)
		if snapped_p.distance_to(player.global_position) > 9.0:
			return Vector3(snapped_p.x, 0.05, snapped_p.z)
	return Vector3(0, 0.05, -20)


func _on_enemy_died(enemy: Enemy) -> void:
	_alive -= 1
	kills += 1
	combo = mini(combo + 1, 8) if _combo_timer > 0.0 else 1
	_combo_timer = 3.0
	score += int(enemy.stats["score"] * (2 if enemy.elite else 1)) * combo
	hud.set_score(score, combo)
	hud.set_enemies(_spawn_queue.size() + _alive)
	if enemy == _boss:
		hud.set_boss(false)
		hud.banner("BONE KING DEFEATED!", HUD.GOLD)
		shake(0.7)
		for i in 3:
			_drop_orb(enemy.global_position + Vector3(randf_range(-2, 2), 0, randf_range(-2, 2)))
	elif randf() < 0.22:
		_drop_orb(enemy.global_position)


func _drop_orb(at: Vector3) -> void:
	var orb := HealOrb.new()
	orb.position = Vector3(at.x, 0, at.z)
	add_child(orb)


func _wave_cleared() -> void:
	if wave >= FINAL_WAVE:
		_victory()
		return
	hud.banner("WAVE CLEARED", Color(0.6, 1, 0.6))
	player.heal(player.max_health * 0.25)
	_between_waves = 3.5


func _clear_arena() -> void:
	for n in get_children():
		if n is Enemy or n is HealOrb or n is MagicBolt:
			n.queue_free()
	_spawn_queue.clear()
	_alive = 0
	_boss = null
	_between_waves = -1.0
	_combo_timer = 0.0


# --- Juice ------------------------------------------------------------------

func shake(amount: float) -> void:
	rig.shake(amount)


func player_hurt_feedback() -> void:
	hud.hurt_flash()
	shake(0.25)


func hit_stop(duration: float) -> void:
	if _hit_stop_active:
		return
	_hit_stop_active = true
	Engine.time_scale = 0.08
	await get_tree().create_timer(duration, true, false, true).timeout
	if state == State.PLAYING or state == State.OVER:
		Engine.time_scale = 1.0
	_hit_stop_active = false


func spawn_damage_number(at: Vector3, amount: float, color: Color) -> void:
	var label := Label3D.new()
	label.text = str(roundi(amount))
	label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	label.no_depth_test = true
	label.fixed_size = true
	label.pixel_size = 0.0018
	label.font_size = 40
	label.outline_size = 10
	label.modulate = color
	label.position = at + Vector3(randf_range(-0.3, 0.3), 0, randf_range(-0.3, 0.3))
	add_child(label)
	var t := label.create_tween()
	t.tween_property(label, "position:y", at.y + 1.2, 0.7).set_ease(Tween.EASE_OUT)
	t.parallel().tween_property(label, "modulate:a", 0.0, 0.7).set_delay(0.3)
	t.tween_callback(label.queue_free)


func _burst(at: Vector3, amount: int, color_a: Color, color_b: Color, speed: float, gravity: float, size: float, life: float) -> void:
	var p := CPUParticles3D.new()
	var quad := QuadMesh.new()
	quad.size = Vector2(size, size)
	quad.material = _fx_material
	p.mesh = quad
	p.amount = amount
	p.lifetime = life
	p.one_shot = true
	p.explosiveness = 0.95
	p.spread = 180
	p.direction = Vector3.UP
	p.initial_velocity_min = speed * 0.4
	p.initial_velocity_max = speed
	p.gravity = Vector3(0, -gravity, 0)
	p.scale_amount_min = 0.5
	p.scale_amount_max = 1.2
	var ramp := Gradient.new()
	ramp.set_color(0, color_a)
	ramp.set_color(1, color_b)
	p.color_ramp = ramp
	p.position = at
	add_child(p)
	p.emitting = true
	get_tree().create_timer(life + 0.3).timeout.connect(p.queue_free)


func spark(at: Vector3) -> void:
	_burst(at, 14, Color(1, 0.95, 0.7, 1), Color(1, 0.6, 0.2, 0), 6.0, 9.0, 0.12, 0.35)


func dust(at: Vector3, size_scale: float) -> void:
	_burst(at + Vector3.UP * 0.2, 22, Color(0.55, 0.45, 0.32, 0.8), Color(0.45, 0.38, 0.3, 0), 2.5 * size_scale, 1.0, 0.5 * size_scale, 1.0)


func shockwave(at: Vector3, radius: float) -> void:
	_burst(at + Vector3.UP * 0.2, 40, Color(0.7, 0.9, 1.0, 1), Color(0.3, 0.5, 1.0, 0), 9.0, 4.0, 0.3, 0.6)
	dust(at, 1.6)
	var ring := MeshInstance3D.new()
	var torus := TorusMesh.new()
	torus.inner_radius = 0.85
	torus.outer_radius = 1.0
	torus.rings = 48
	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mat.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	mat.albedo_color = Color(0.5, 0.8, 1.0, 0.9)
	torus.material = mat
	ring.mesh = torus
	ring.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	ring.position = at + Vector3.UP * 0.15
	ring.scale = Vector3(0.5, 0.2, 0.5)
	add_child(ring)
	var t := ring.create_tween()
	t.tween_property(ring, "scale", Vector3(radius, 0.4, radius), 0.35).set_ease(Tween.EASE_OUT)
	t.parallel().tween_property(mat, "albedo_color:a", 0.0, 0.45)
	t.tween_callback(ring.queue_free)


# --- Save -------------------------------------------------------------------

func _load_save() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(SAVE_PATH) == OK:
		best_score = cfg.get_value("stats", "best_score", 0)


func _save_best() -> void:
	if score <= best_score:
		return
	best_score = score
	var cfg := ConfigFile.new()
	cfg.set_value("stats", "best_score", best_score)
	cfg.save(SAVE_PATH)
