extends SceneTree
# Dev tool: prints the imported GLB node tree, animation clips and track paths.
# Usage: godot --headless --path . -s res://tools/inspect_glb.gd

func _init() -> void:
	var scene: PackedScene = load("res://assets/characters/cyberpunk_warrior.glb")
	var root := scene.instantiate()
	_dump(root, 0)
	var player := root.find_child("AnimationPlayer", true, false) as AnimationPlayer
	if player:
		print("root_node=", player.root_node)
		for anim_name in player.get_animation_list():
			var anim := player.get_animation(anim_name)
			print("ANIM ", anim_name, " len=", anim.length, " loop=", anim.loop_mode, " tracks=", anim.get_track_count())
			for t in anim.get_track_count():
				print("   ", anim.track_get_type(t), " ", anim.track_get_path(t), " keys=", anim.track_get_key_count(t))
	for mi in root.find_children("*", "MeshInstance3D", true, false):
		var m: Mesh = mi.mesh
		var arr := m.surface_get_arrays(0)
		print("MESH ", mi.name, " mat=", m.surface_get_material(0), " override=", mi.material_override, " has_color=", arr[Mesh.ARRAY_COLOR] != null)
	root.free()
	quit()

func _dump(n: Node, d: int) -> void:
	var extra := ""
	if n is Node3D:
		extra = " pos=%s rot=%s" % [n.position, n.rotation]
	print("  ".repeat(d), n.name, " <", n.get_class(), ">", extra)
	for c in n.get_children():
		_dump(c, d + 1)
