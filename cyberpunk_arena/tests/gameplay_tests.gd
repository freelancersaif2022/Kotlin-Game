extends Node
## Automated gameplay verification for Cyberpunk Arena.
##
## Loads the real main scene and drives it through synthetic multitouch
## events (exactly like a phone would send them), then checks GLB import,
## rig, grounding, movement, animations, combat, camera and configuration.
##
## Started by tests/test_runner.gd (see that file for the command line).

var _failures := 0
var _passes := 0
var _shot_dir := ""
var main: Node3D
var player: Player
var rig: CameraRig
var model: WarriorModel
var anim: AnimationPlayer
var joystick: VirtualJoystick
var attack_btn: TouchActionButton
var run_btn: TouchActionButton


var root: Window


func _ready() -> void:
	root = get_tree().root
	_run.call_deferred()


func _run() -> void:
	var args := OS.get_cmdline_user_args()
	_shot_dir = args[0] if args.size() > 0 else ""
	main = load("res://scenes/main.tscn").instantiate()
	root.add_child(main)
	get_tree().current_scene = main
	player = main.get_node("Player")
	rig = main.get_node("CameraRig")
	model = player.get_node("Visual/WarriorModel")
	anim = model.find_child("AnimationPlayer", true, false)
	joystick = main.get_node("UI/MobileControls/SafeArea/Joystick")
	attack_btn = main.get_node("UI/MobileControls/SafeArea/AttackButton")
	run_btn = main.get_node("UI/MobileControls/SafeArea/RunButton")
	await _wait(0.8)

	await _test_config()
	await _test_glb_and_rig()
	await _test_grounding_and_idle()
	await _test_multitouch_movement()
	await _test_attack()
	await _test_camera_clipping()
	await _test_ramp()

	print("\n==== RESULT: %d passed, %d failed ====" % [_passes, _failures])
	get_tree().quit(1 if _failures > 0 else 0)


# ---------------------------------------------------------------- tests ----

func _test_config() -> void:
	_section("Configuration")
	_check(ProjectSettings.get_setting("display/window/handheld/orientation") == 4, "orientation = sensor landscape")
	_check(ProjectSettings.get_setting("rendering/renderer/rendering_method.mobile") == "gl_compatibility", "mobile renderer = gl_compatibility")
	_check(ProjectSettings.get_setting("input_devices/pointing/emulate_mouse_from_touch") == false, "touch is not duplicated as mouse")
	var cfg := ConfigFile.new()
	_check(cfg.load("res://export_presets.cfg") == OK, "export_presets.cfg loads")
	_check(cfg.get_value("preset.0.options", "screen/immersive_mode", false) == true, "Android immersive fullscreen enabled")
	_check(cfg.get_value("preset.0.options", "architectures/arm64-v8a", false) == true, "arm64-v8a enabled")
	var vp := root.get_visible_rect().size
	_check(vp.x > vp.y, "viewport is landscape (%s)" % vp)


func _test_glb_and_rig() -> void:
	_section("GLB import, clips and rig")
	_check(anim != null, "AnimationPlayer found in GLB")
	for clip in [WarriorModel.Clip.IDLE, WarriorModel.Clip.WALK, WarriorModel.Clip.RUN, WarriorModel.Clip.ATTACK]:
		_check(model.has_clip(clip), "clip detected: %s" % WarriorModel.CLIP_NAMES[clip])
	_check(anim.get_animation("Walk").loop_mode == Animation.LOOP_LINEAR, "Walk loops")
	_check(anim.get_animation("Attack").loop_mode == Animation.LOOP_NONE, "Attack does not loop")
	var parts := {}
	for mi in model.find_children("*", "MeshInstance3D", true, false):
		parts[String(mi.name)] = mi
	_check(parts.size() == 27, "all 27 GLB mesh parts present (%d)" % parts.size())
	_check(parts["R_forearm"].get_parent().name == "R_upper_arm", "forearm follows upper arm")
	_check(parts["energy_blade"].get_parent().name == "R_upper_arm", "blade follows arm")
	_check(parts["L_shin"].get_parent().name == "L_thigh", "shin follows thigh")
	_check(parts["R_upper_arm"].get_parent().name == "torso", "arms follow torso")
	var unresolved := 0
	var anim_root := anim.get_node(anim.root_node)
	for anim_name in anim.get_animation_list():
		var a := anim.get_animation(anim_name)
		for t in a.get_track_count():
			if anim_root.get_node_or_null(NodePath(a.track_get_path(t).get_concatenated_names())) == null:
				unresolved += 1
	_check(unresolved == 0, "all animation tracks resolve after retarget (%d unresolved)" % unresolved)
	var mat := (parts["torso"] as MeshInstance3D).material_override as StandardMaterial3D
	_check(mat != null and mat.vertex_color_use_as_albedo, "model uses its vertex-colour materials")


func _test_grounding_and_idle() -> void:
	_section("Grounding and idle")
	_check(player.is_on_floor(), "player is on floor")
	_check(absf(player.global_position.y) < 0.03, "player y = %.3f" % player.global_position.y)
	var min_y := INF
	var max_y := -INF
	for mi: MeshInstance3D in model.find_children("*", "MeshInstance3D", true, false):
		var box: AABB = mi.global_transform * mi.get_aabb()
		min_y = minf(min_y, box.position.y)
		max_y = maxf(max_y, box.end.y)
	_check(absf(min_y) < 0.04, "feet touch floor (lowest vertex y = %.3f)" % min_y)
	_check(absf((max_y - min_y) - 1.8) < 0.08, "character height = %.2f m" % (max_y - min_y))
	_check(anim.current_animation == "Idle", "Idle playing (%s)" % anim.current_animation)
	_shot("01_idle")


func _test_multitouch_movement() -> void:
	_section("Multitouch: joystick + run button + camera drag simultaneously")
	# Start in open space so nothing blocks the run.
	player.global_position = Vector3(-6, 0, 22)
	await _wait(0.5)
	var start := player.global_position
	var forward := Vector3(-sin(rig.get_yaw()), 0, -cos(rig.get_yaw()))
	# Finger 0: joystick, pushed fully up.
	var stick_center := _global_center(joystick) + Vector2(-joystick.size.x * 0.15, joystick.size.y * 0.2)
	_touch(0, stick_center, true)
	await _frames(2)
	_drag(0, stick_center + Vector2(0, -joystick.base_radius * 1.3), Vector2(0, -joystick.base_radius * 1.3))
	await _wait(1.3)
	_check(GameInput.joystick_vector.y < -0.95, "joystick full forward (%s)" % GameInput.joystick_vector)
	var speed := _ground_speed()
	_check(absf(speed - player.walk_speed) < 0.25, "walk speed %.2f ~ %.2f" % [speed, player.walk_speed])
	var dir := (player.global_position - start)
	dir.y = 0
	_check(dir.normalized().dot(forward) > 0.97, "moves in camera forward direction")
	_check(anim.current_animation == "Walk", "Walk playing (%s)" % anim.current_animation)
	_shot("02_walk")

	# Finger 1: run button (joystick still held).
	_touch(1, _global_center(run_btn), true)
	await _frames(2)
	_touch(1, _global_center(run_btn), false)
	await _wait(1.0)
	_check(GameInput.is_run_toggled(), "run toggled on while joystick held")
	speed = _ground_speed()
	_check(absf(speed - player.run_speed) < 0.3, "run speed %.2f ~ %.2f" % [speed, player.run_speed])
	_check(anim.current_animation == "Run", "Run playing (%s)" % anim.current_animation)
	_shot("03_run")

	# Finger 2: camera drag on the right half (joystick + run still active).
	var yaw_before := rig.get_yaw()
	var cam_pos := root.get_visible_rect().size * Vector2(0.62, 0.35)
	_touch(2, cam_pos, true)
	await _frames(1)
	for i in 10:
		cam_pos += Vector2(30, 0)
		_drag(2, cam_pos, Vector2(30, 0))
		await _frames(1)
	await _wait(0.4)
	var expected := -300.0 / root.get_visible_rect().size.y * rig.touch_sensitivity
	var turned := angle_difference(yaw_before, rig.get_yaw())
	_check(absf(turned - expected) < 0.35, "camera yaw changed by %.2f rad (expected ~%.2f)" % [turned, expected])
	_check(GameInput.joystick_vector.length() > 0.95, "joystick unaffected by camera finger")
	_check(not attack_btn.is_down, "attack button not triggered by other fingers")
	_touch(2, cam_pos, false)

	# Release everything: deceleration, idle, run auto-cancel.
	_touch(0, stick_center, false)
	await _wait(1.2)
	_check(GameInput.joystick_vector == Vector2.ZERO, "joystick released")
	_check(_ground_speed() < 0.05, "decelerated to stop (%.3f)" % _ground_speed())
	_check(anim.current_animation == "Idle", "back to Idle (%s)" % anim.current_animation)
	_check(not GameInput.is_run_toggled(), "run toggle auto-cancelled when stopped")
	_check(rig.camera.global_position.y > 0.25, "camera above floor")


func _test_attack() -> void:
	_section("Combat")
	# Stand in front of the centre dummy, facing it.
	var dummy: Node3D = main.get_node("Dummies/DummyCenter")
	player.global_position = dummy.global_position + Vector3(0.3, 0, 1.5)
	player.velocity = Vector3.ZERO
	await _wait(0.4)
	var hits_before := player.total_hits
	var blade: Node3D = model.find_child("energy_blade", true, false)
	var forearm: Node3D = model.find_child("R_forearm", true, false)
	var upper: Node3D = model.find_child("R_upper_arm", true, false)
	var link := forearm.global_position.distance_to(upper.global_position)
	var blade_start := blade.global_position

	_touch(3, _global_center(attack_btn), true)
	await _frames(3)
	_touch(3, _global_center(attack_btn), false)
	await _frames(2)
	_check(player.is_attacking(), "attack started from touch button")
	_check(anim.current_animation == "Attack", "Attack playing (%s)" % anim.current_animation)
	var to_dummy := dummy.global_position - player.global_position
	var facing := Vector3(-sin(player.get_facing_yaw()), 0, -cos(player.get_facing_yaw()))
	to_dummy.y = 0
	_check(facing.dot(to_dummy.normalized()) > 0.95, "target assist faces the dummy")

	# Try to interrupt: move + spam attack mid-swing.
	GameInput.joystick_vector = Vector2(1, 0)
	var max_blade_move := 0.0
	var max_link_err := 0.0
	var last_pos := 0.0
	var restarted := false
	for i in 30:
		GameInput.request_attack()
		await _frames(1)
		if anim.current_animation == "Attack":
			if anim.current_animation_position + 0.001 < last_pos:
				restarted = true
			last_pos = anim.current_animation_position
		max_blade_move = maxf(max_blade_move, blade.global_position.distance_to(blade_start))
		max_link_err = maxf(max_link_err, absf(forearm.global_position.distance_to(upper.global_position) - link))
		if i == 12:
			_shot("04_attack")
	_check(not restarted, "attack not restarted/interrupted mid-swing")
	_check(max_blade_move > 0.3, "blade swings with the arm (%.2f m)" % max_blade_move)
	_check(max_link_err < 0.01, "forearm stays attached to upper arm (err %.4f)" % max_link_err)
	GameInput.joystick_vector = Vector2.ZERO
	GameInput.consume_attack()
	await _wait(1.0)
	_check(player.total_hits > hits_before, "hitbox hit the dummy (hits %d)" % player.total_hits)
	_check(not player.is_attacking(), "attack finished")
	_check(anim.current_animation == "Idle", "returns to Idle after attack (%s)" % anim.current_animation)

	# Cooldown: two immediate requests produce only one new attack start.
	var starts := [0]
	var counter := func(): starts[0] += 1
	player.attack_started.connect(counter)
	GameInput.request_attack()
	await _frames(2)
	GameInput.request_attack()
	await _frames(2)
	GameInput.consume_attack()
	_check(starts[0] == 1, "cooldown/lock prevents double attack (%d starts)" % starts[0])
	await _wait(1.0)
	player.attack_started.disconnect(counter)


func _test_camera_clipping() -> void:
	_section("Camera collision")
	# Look up as far as allowed: camera must stay above the floor.
	_touch(4, root.get_visible_rect().size * Vector2(0.62, 0.3), true)
	for i in 20:
		_drag(4, root.get_visible_rect().size * Vector2(0.62, 0.3), Vector2(0, -60))
		await _frames(1)
	_touch(4, Vector2.ZERO, false)
	await _wait(0.6)
	_check(rig.camera.global_position.y > 0.25, "max look-up: camera y = %.2f" % rig.camera.global_position.y)
	_shot("05_look_up")
	# Back the player against the north wall with the camera behind the wall.
	player.global_position = Vector3(0, 0, -34.2)
	rig.set("_target_yaw", PI)
	rig.set("_target_pitch", deg_to_rad(-10))
	await _wait(1.0)
	var cam := rig.camera.global_position
	_check(cam.z > -35.5, "spring arm keeps camera inside the wall (z = %.2f)" % cam.z)
	_shot("06_wall")
	player.global_position = Vector3(0, 0, 5)
	rig.set("_target_yaw", 0.0)
	rig.set("_target_pitch", deg_to_rad(-16))
	await _wait(0.8)


func _test_ramp() -> void:
	_section("Slopes / floor collision")
	player.global_position = Vector3(16, 0, -3.5)
	rig.set("_target_yaw", 0.0)
	await _wait(0.6)
	GameInput.joystick_vector = Vector2(0, -1)
	await _wait(3.2)
	GameInput.joystick_vector = Vector2.ZERO
	await _wait(0.5)
	_check(player.is_on_floor(), "on floor after climbing ramp")
	_check(absf(player.global_position.y - 1.0) < 0.05, "standing on platform (y = %.2f)" % player.global_position.y)
	_shot("07_platform")


# -------------------------------------------------------------- helpers ----

func _global_center(c: Control) -> Vector2:
	return c.get_global_rect().get_center()


## Positions are in viewport (canvas) coordinates; converted to window pixels
## because parse_input_event expects raw device coordinates.
func _touch(index: int, pos: Vector2, pressed: bool) -> void:
	var e := InputEventScreenTouch.new()
	e.index = index
	e.position = root.get_final_transform() * pos
	e.pressed = pressed
	Input.parse_input_event(e)


func _drag(index: int, pos: Vector2, relative: Vector2) -> void:
	var e := InputEventScreenDrag.new()
	e.index = index
	e.position = root.get_final_transform() * pos
	e.relative = root.get_final_transform().basis_xform(relative)
	e.screen_relative = e.relative
	Input.parse_input_event(e)


func _ground_speed() -> float:
	return Vector2(player.velocity.x, player.velocity.z).length()


func _frames(n: int) -> void:
	for i in n:
		await get_tree().physics_frame
	await get_tree().process_frame


func _wait(seconds: float) -> void:
	await get_tree().create_timer(seconds, true, true).timeout
	await get_tree().process_frame


func _section(title: String) -> void:
	print("\n-- ", title)


func _check(ok: bool, what: String) -> void:
	if ok:
		_passes += 1
		print("  PASS  ", what)
	else:
		_failures += 1
		print("  FAIL  ", what)


func _shot(shot_name: String) -> void:
	if _shot_dir == "" or DisplayServer.get_name() == "headless":
		return
	var img := root.get_texture().get_image()
	if img:
		img.save_png(_shot_dir.path_join(shot_name + ".png"))
