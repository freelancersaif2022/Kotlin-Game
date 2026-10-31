class_name MagicBolt
extends Area3D
## A slow homing ball of necromantic fire cast by skeleton mages.

const SPEED := 9.0
const TURN := 1.6

var damage := 12.0
var _velocity := Vector3.ZERO
var _target: Node3D
var _life := 4.0


func launch(from: Vector3, target: Node3D, dmg: float) -> void:
	global_position = from
	_target = target
	damage = dmg
	_velocity = (target.global_position + Vector3.UP - from).normalized() * SPEED


func _ready() -> void:
	collision_layer = 0
	collision_mask = 1 | 2
	var shape := CollisionShape3D.new()
	var sphere := SphereShape3D.new()
	sphere.radius = 0.3
	shape.shape = sphere
	add_child(shape)

	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.albedo_color = Color(0.55, 1.0, 0.45)
	mat.emission_enabled = true
	mat.emission = Color(0.4, 1.0, 0.3)
	mat.emission_energy_multiplier = 3.0
	var mesh := SphereMesh.new()
	mesh.radius = 0.22
	mesh.height = 0.44
	mesh.material = mat
	var mi := MeshInstance3D.new()
	mi.mesh = mesh
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mi)

	var trail_mat := StandardMaterial3D.new()
	trail_mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	trail_mat.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
	trail_mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	trail_mat.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	trail_mat.vertex_color_use_as_albedo = true
	trail_mat.albedo_texture = FX.soft_circle()
	var quad := QuadMesh.new()
	quad.size = Vector2(0.3, 0.3)
	quad.material = trail_mat
	var trail := CPUParticles3D.new()
	trail.mesh = quad
	trail.amount = 24
	trail.lifetime = 0.4
	trail.local_coords = false
	trail.gravity = Vector3(0, 0.6, 0)
	trail.initial_velocity_max = 0.3
	trail.spread = 180
	trail.scale_amount_min = 0.5
	trail.scale_amount_max = 1.0
	var ramp := Gradient.new()
	ramp.set_color(0, Color(0.6, 1.0, 0.5, 0.9))
	ramp.set_color(1, Color(0.1, 0.5, 0.1, 0.0))
	trail.color_ramp = ramp
	add_child(trail)
	body_entered.connect(_on_body_entered)


func _physics_process(delta: float) -> void:
	_life -= delta
	if _life <= 0.0:
		queue_free()
		return
	if is_instance_valid(_target):
		var want := (_target.global_position + Vector3.UP - global_position).normalized() * SPEED
		_velocity = _velocity.lerp(want, clampf(TURN * delta, 0, 1))
	global_position += _velocity * delta


func _on_body_entered(body: Node3D) -> void:
	if body is Player:
		body.take_damage(damage, _velocity.normalized() * 3.0)
	queue_free()
