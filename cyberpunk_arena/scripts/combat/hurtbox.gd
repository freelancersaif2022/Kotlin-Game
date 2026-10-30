class_name Hurtbox
extends Area3D
## Receives hits from an AttackHitbox. Put this on enemies / destructibles
## (collision layer "enemy_hurtbox") and connect to the `hurt` signal.

signal hurt(damage: int, knockback: Vector3, source: Node3D)

@export var enabled := true


func _ready() -> void:
	monitoring = false
	monitorable = true


func receive_hit(damage: int, knockback: Vector3, source: Node3D) -> bool:
	if not enabled:
		return false
	hurt.emit(damage, knockback, source)
	return true
