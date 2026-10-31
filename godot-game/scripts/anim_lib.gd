extends Node
## Autoload "Anims": loads the KayKit Rig_Medium animation files once and
## shares a single AnimationLibrary with every skeleton character.

const SOURCES := [
	"res://assets/animations/Rig_Medium_General.glb",
	"res://assets/animations/Rig_Medium_MovementBasic.glb",
]
const LOOPING := [
	"Idle_A", "Idle_B", "Walking_A", "Walking_B", "Walking_C",
	"Running_A", "Running_B", "Jump_Idle",
]

var library := AnimationLibrary.new()


func _ready() -> void:
	for path in SOURCES:
		var scene: Node = load(path).instantiate()
		var src: AnimationPlayer = scene.find_children("*", "AnimationPlayer", true, false)[0]
		for anim_name in src.get_animation_list():
			if library.has_animation(anim_name):
				continue
			var anim: Animation = src.get_animation(anim_name).duplicate(true)
			anim.loop_mode = Animation.LOOP_LINEAR if anim_name in LOOPING else Animation.LOOP_NONE
			library.add_animation(anim_name, anim)
		scene.free()
