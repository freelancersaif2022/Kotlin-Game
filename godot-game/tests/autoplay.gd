extends SceneTree
## Automated smoke test: boots the game, plays a few waves with a simple bot and
## saves screenshots. Run: godot --rendering-driver opengl3 --script res://tests/autoplay.gd -- <out_dir>

var out_dir := "user://shots"
var game: Node
var t := 0.0
var shots := 0
var next_shot := 0.0


func _initialize() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() > 0:
		out_dir = args[0]
	DirAccess.make_dir_recursive_absolute(out_dir)
	change_scene_to_file("res://scenes/main.tscn")


func _process(delta: float) -> bool:
	t += delta
	if game == null:
		game = current_scene
		return false
	if t > 4.0 and t < 4.1:
		_shot("menu")
	if t > 5.0 and game.state == game.State.MENU and shots < 3:
		game.start_game()
		game.use_touch = true
		game.hud.set_playing(true, true)
		next_shot = t + 3.0
	if t > 14.0 and game.wave < 5 and game.state == game.State.PLAYING:
		game._clear_arena()
		game._start_wave(5)
	if t > 30.0 and t < 30.1 and game.state == game.State.PLAYING:
		game.pause_game()
		_shot("pause")
		game.resume_game()
	if t > 34.0 and game.state == game.State.PLAYING:
		game.player.take_damage(999)
	if game.state == game.State.PLAYING:
		_bot()
	if next_shot > 0.0 and t >= next_shot:
		next_shot = t + 4.0
		_shot("play")
	if t > 40.0:
		print("RESULT wave=%d kills=%d score=%d hp=%d state=%d fps=%d" % [game.wave, game.kills, game.score, game.player.health, game.state, Engine.get_frames_per_second()])
		_shot("end")
		quit()
	return false


func _bot() -> void:
	var p: Player = game.player
	var target: Node3D = null
	var best := 1e9
	for e in get_nodes_in_group("enemies"):
		var d: float = p.global_position.distance_to(e.global_position)
		if d < best:
			best = d
			target = e
	for a in ["move_left", "move_right", "move_forward", "move_back"]:
		Input.action_release(a)
	Input.action_release("attack")
	Input.action_release("special")
	if p.health < 40 and t < 34.0:
		p.heal(60)  # keep the bot alive long enough to see later waves
	if target:
		var to := target.global_position - p.global_position
		to = to.rotated(Vector3.UP, -p.camera_yaw)
		if best > 2.2:
			if to.x < -0.2: Input.action_press("move_left", clampf(-to.x / to.length(), 0, 1))
			if to.x > 0.2: Input.action_press("move_right", clampf(to.x / to.length(), 0, 1))
			if to.z < -0.2: Input.action_press("move_forward", clampf(-to.z / to.length(), 0, 1))
			if to.z > 0.2: Input.action_press("move_back", clampf(to.z / to.length(), 0, 1))
		if best < 3.5:
			Input.action_press("attack")
		if best < 4.0 and get_nodes_in_group("enemies").size() >= 3:
			Input.action_press("special")
	# Slowly orbit the camera so screenshots show different angles.
	game.rig.yaw += 0.004


func _shot(tag: String) -> void:
	var img := root.get_viewport().get_texture().get_image()
	var path := "%s/%02d_%s.png" % [out_dir, shots, tag]
	img.save_png(path)
	shots += 1
	print("shot ", path, " t=", snapped(t, 0.1))
