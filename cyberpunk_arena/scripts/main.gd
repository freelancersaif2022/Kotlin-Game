extends Node3D
## Root of the game scene: wires player, camera and UI together and applies
## platform-specific runtime settings.

@onready var player: Player = $Player
@onready var camera_rig: CameraRig = $CameraRig
@onready var controls: MobileControls = $UI/MobileControls


func _ready() -> void:
	# Keep the screen awake while playing on phones.
	DisplayServer.screen_set_keep_on(true)
	print("Cyberpunk Arena ready | renderer: %s | window: %s | viewport: %s" % [
		RenderingServer.get_current_rendering_method(),
		DisplayServer.window_get_size(),
		get_viewport().get_visible_rect().size])


func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT or what == NOTIFICATION_APPLICATION_PAUSED:
		GameInput.reset_touch_state()
