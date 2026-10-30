class_name SlashEffect
extends MeshInstance3D
## Lightweight energy-blade swoosh: a procedurally built arc ribbon that
## sweeps and fades out. No textures, one draw call, additive blending.

@export var color := Color(0.0, 0.82, 1.0)
@export var inner_radius := 0.45
@export var outer_radius := 1.15
## Arc in degrees measured in the local YZ plane (0 = forward, -90 = down).
@export var start_angle := -110.0
@export var end_angle := 25.0
@export var segments := 20

var _material: StandardMaterial3D
var _tween: Tween


func _ready() -> void:
	mesh = _build_arc()
	_material = StandardMaterial3D.new()
	_material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	_material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	_material.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	_material.cull_mode = BaseMaterial3D.CULL_DISABLED
	_material.vertex_color_use_as_albedo = true
	_material.albedo_color = Color(color, 0.0)
	_material.no_depth_test = false
	material_override = _material
	cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	visible = false


func play(duration: float) -> void:
	if _tween:
		_tween.kill()
	visible = true
	rotation.x = deg_to_rad(25.0)
	_material.albedo_color = Color(color, 0.0)
	_tween = create_tween().set_process_mode(Tween.TWEEN_PROCESS_PHYSICS).set_parallel(true)
	_tween.tween_property(_material, "albedo_color:a", 1.0, duration * 0.25)
	_tween.tween_property(self, "rotation:x", deg_to_rad(-15.0), duration).set_ease(Tween.EASE_OUT)
	_tween.chain().tween_property(_material, "albedo_color:a", 0.0, duration * 0.9)
	_tween.chain().tween_callback(hide)


func _build_arc() -> ArrayMesh:
	var verts := PackedVector3Array()
	var colors := PackedColorArray()
	var indices := PackedInt32Array()
	for i in segments + 1:
		var t := float(i) / segments
		var a := deg_to_rad(lerpf(start_angle, end_angle, t))
		var dir := Vector3(0.0, sin(a), -cos(a))
		# Trail fades in from the tail and is brightest at the leading edge.
		var strength := pow(t, 1.6)
		verts.append(dir * inner_radius)
		colors.append(Color(1, 1, 1, 0.0))
		verts.append(dir * outer_radius)
		colors.append(Color(1, 1, 1, strength))
		if i < segments:
			var b := i * 2
			indices.append_array([b, b + 1, b + 2, b + 1, b + 3, b + 2])
	var arrays := []
	arrays.resize(Mesh.ARRAY_MAX)
	arrays[Mesh.ARRAY_VERTEX] = verts
	arrays[Mesh.ARRAY_COLOR] = colors
	arrays[Mesh.ARRAY_INDEX] = indices
	var arc := ArrayMesh.new()
	arc.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)
	return arc
