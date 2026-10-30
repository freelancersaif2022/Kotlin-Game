class_name AttackHitbox
extends Area3D
## Melee hitbox. Inactive by default; the owner opens it for the strike window
## of an attack with begin_swing() / end_swing(). Each Hurtbox is hit at most
## once per swing.

signal hit_landed(hurtbox: Hurtbox, damage: int)

@export var damage := 25
@export var knockback_force := 5.0
## Node that "owns" the attack (used for knockback direction and as hit source).
@export var source: Node3D

var _active := false
var _already_hit: Dictionary = {}


func _ready() -> void:
	monitorable = false
	monitoring = false
	area_entered.connect(_try_hit)


func is_active() -> bool:
	return _active


func begin_swing() -> void:
	_already_hit.clear()
	_active = true
	monitoring = true


func end_swing() -> void:
	_active = false
	set_deferred("monitoring", false)


func _physics_process(_delta: float) -> void:
	if not _active:
		return
	for area in get_overlapping_areas():
		_try_hit(area)


func _try_hit(area: Area3D) -> void:
	if not _active or not (area is Hurtbox):
		return
	var id := area.get_instance_id()
	if _already_hit.has(id):
		return
	_already_hit[id] = true
	var origin := source.global_position if source else global_position
	var dir := area.global_position - origin
	dir.y = 0.0
	dir = dir.normalized() if dir.length_squared() > 0.0001 else -global_basis.z
	if (area as Hurtbox).receive_hit(damage, dir * knockback_force, source):
		hit_landed.emit(area, damage)
