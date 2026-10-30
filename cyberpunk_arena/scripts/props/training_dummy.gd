class_name TrainingDummy
extends StaticBody3D
## Hittable target used to exercise the combat system (and a template for
## future enemies). Listens to its Hurtbox, wobbles away from the hit,
## flashes, pops a damage number and regenerates.

@export var max_health := 100
@export var regen_delay := 2.0
@export var regen_per_second := 40.0

@onready var _pivot: Node3D = $Pivot
@onready var _hurtbox: Hurtbox = $Hurtbox
@onready var _core: MeshInstance3D = $Pivot/Core
@onready var _health_bar: MeshInstance3D = $Pivot/HealthBar

var health := 0.0
var _core_material: StandardMaterial3D
var _base_emission := Color(0.0, 0.82, 1.0)
var _wobble := Vector2.ZERO
var _wobble_velocity := Vector2.ZERO
var _since_hit := 100.0
var _flash := 0.0


func _ready() -> void:
	add_to_group("enemies")
	health = max_health
	_hurtbox.hurt.connect(_on_hurt)
	_core_material = (_core.get_active_material(0) as StandardMaterial3D).duplicate()
	_core.material_override = _core_material
	_base_emission = _core_material.emission
	_update_health_bar()


func _physics_process(delta: float) -> void:
	# Damped spring wobble.
	_wobble_velocity += (-_wobble * 90.0 - _wobble_velocity * 9.0) * delta
	_wobble += _wobble_velocity * delta
	_pivot.rotation = Vector3(_wobble.y, 0.0, -_wobble.x)
	_since_hit += delta
	if _since_hit > regen_delay and health < max_health:
		health = minf(health + regen_per_second * delta, max_health)
		_update_health_bar()


func _process(delta: float) -> void:
	if _flash > 0.0:
		_flash = maxf(_flash - delta * 4.0, 0.0)
		_core_material.emission = _base_emission.lerp(Color(1.0, 0.25, 0.35), _flash)
		_core_material.emission_energy_multiplier = 1.5 + _flash * 4.0


func _on_hurt(damage: int, knockback: Vector3, _source: Node3D) -> void:
	var local := global_basis.inverse() * knockback
	_wobble_velocity += Vector2(local.x, local.z) * 1.4
	_flash = 1.0
	_since_hit = 0.0
	health = maxf(health - damage, 0.0)
	if health <= 0.0:
		health = max_health
	_update_health_bar()
	_spawn_damage_number(damage)


func _update_health_bar() -> void:
	var ratio := health / float(max_health)
	_health_bar.scale.x = maxf(ratio, 0.001)
	_health_bar.position.x = -0.4 * (1.0 - ratio)


func _spawn_damage_number(damage: int) -> void:
	var label := Label3D.new()
	label.text = str(damage)
	label.font_size = 96
	label.pixel_size = 0.004
	label.outline_size = 18
	label.modulate = Color(1.0, 0.3, 0.42)
	label.outline_modulate = Color(1, 1, 1, 0.9)
	label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	label.no_depth_test = true
	label.fixed_size = false
	label.position = Vector3(randf_range(-0.25, 0.25), 2.1, 0.0)
	add_child(label)
	var tween := label.create_tween().set_process_mode(Tween.TWEEN_PROCESS_PHYSICS).set_parallel(true)
	tween.tween_property(label, "position:y", 2.8, 0.7).set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_CUBIC)
	tween.tween_property(label, "modulate:a", 0.0, 0.7).set_delay(0.25)
	tween.chain().tween_callback(label.queue_free)
