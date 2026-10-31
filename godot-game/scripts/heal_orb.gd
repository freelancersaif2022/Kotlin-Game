class_name HealOrb
extends Area3D
## Glowing soul orb dropped by fallen skeletons. Restores health on touch.

var amount := 20.0
var _time := 0.0
var _life := 14.0
var _visual: Node3D


func _ready() -> void:
	collision_layer = 0
	collision_mask = 2
	var shape := CollisionShape3D.new()
	var sphere := SphereShape3D.new()
	sphere.radius = 0.9
	shape.shape = sphere
	shape.position.y = 0.6
	add_child(shape)
	_visual = Node3D.new()
	add_child(_visual)
	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.albedo_color = Color(0.45, 1.0, 0.6)
	mat.emission_enabled = true
	mat.emission = Color(0.3, 1.0, 0.5)
	mat.emission_energy_multiplier = 2.5
	var mesh := SphereMesh.new()
	mesh.radius = 0.2
	mesh.height = 0.4
	mesh.material = mat
	var mi := MeshInstance3D.new()
	mi.mesh = mesh
	_visual.add_child(mi)
	var halo_mat := mat.duplicate()
	halo_mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	halo_mat.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	halo_mat.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	halo_mat.albedo_color = Color(0.3, 1.0, 0.5, 0.6)
	halo_mat.albedo_texture = FX.soft_circle()
	halo_mat.emission_enabled = false
	var halo_mesh := QuadMesh.new()
	halo_mesh.size = Vector2(1.3, 1.3)
	halo_mesh.material = halo_mat
	var halo := MeshInstance3D.new()
	halo.mesh = halo_mesh
	halo.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	_visual.add_child(halo)
	body_entered.connect(_on_body_entered)


func _process(delta: float) -> void:
	_time += delta
	_life -= delta
	_visual.position.y = 0.7 + sin(_time * 3.0) * 0.15
	_visual.rotation.y += delta * 2.0
	if _life < 3.0:
		_visual.visible = fmod(_life, 0.3) > 0.12
	if _life <= 0.0:
		queue_free()


func _on_body_entered(body: Node3D) -> void:
	if body is Player and not body.is_dead:
		body.heal(amount)
		queue_free()
