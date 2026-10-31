class_name CharacterModel
extends Node3D
## Wraps a KayKit skeleton .glb: adds an AnimationPlayer fed by the shared
## Rig_Medium library, attaches weapons to the hand slots and offers a hit flash.

var anim: AnimationPlayer
var skeleton: Skeleton3D
var current := ""
var _meshes: Array[MeshInstance3D] = []
var _flash_mat: StandardMaterial3D
var _flash_time := 0.0
var _tint: Material = null


func setup(model_path: String, right_hand := "", left_hand := "") -> CharacterModel:
	var model: Node3D = load(model_path).instantiate()
	add_child(model)
	skeleton = model.find_children("*", "Skeleton3D", true, false)[0]
	for m in model.find_children("*", "MeshInstance3D", true, false):
		_meshes.append(m)
	anim = AnimationPlayer.new()
	model.add_child(anim)
	anim.root_node = NodePath("..")
	anim.add_animation_library("", Anims.library)
	if right_hand != "":
		_attach(right_hand, "handslot.r")
	if left_hand != "":
		_attach(left_hand, "handslot.l")
	_flash_mat = StandardMaterial3D.new()
	_flash_mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_flash_mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	_flash_mat.albedo_color = Color(1, 0.25, 0.2, 0.55)
	return self


func _attach(scene_path: String, bone: String) -> void:
	var attachment := BoneAttachment3D.new()
	attachment.bone_name = bone
	skeleton.add_child(attachment)
	var item: Node3D = load(scene_path).instantiate()
	attachment.add_child(item)
	for m in item.find_children("*", "MeshInstance3D", true, false):
		_meshes.append(m)


## Plays a (usually looping) animation; does nothing if it is already playing.
func play(anim_name: String, blend := 0.15, speed := 1.0) -> void:
	if current == anim_name and anim.is_playing():
		anim.speed_scale = speed
		return
	current = anim_name
	anim.speed_scale = speed
	anim.play(anim_name, blend)


## Plays an animation from its first frame, even if it is the current one.
func restart(anim_name: String, blend := 0.08, speed := 1.0) -> void:
	current = anim_name
	anim.speed_scale = speed
	anim.play(anim_name, blend)
	anim.seek(0.0, true)


func anim_length(anim_name: String) -> float:
	return Anims.library.get_animation(anim_name).length


func flash(color := Color(1, 0.25, 0.2, 0.55), duration := 0.12) -> void:
	_flash_mat.albedo_color = color
	for m in _meshes:
		m.material_overlay = _flash_mat
	_flash_time = duration


func set_tint(color: Color) -> void:
	## Permanent additive tint, used to make elite and boss skeletons stand out.
	_tint = StandardMaterial3D.new()
	_tint.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_tint.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	_tint.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	_tint.albedo_color = color
	for m in _meshes:
		m.material_overlay = _tint


func _process(delta: float) -> void:
	if _flash_time > 0.0:
		_flash_time -= delta
		if _flash_time <= 0.0:
			for m in _meshes:
				m.material_overlay = _tint
