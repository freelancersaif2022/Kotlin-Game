class_name MobileControls
extends Control
## On-screen control layer + minimal HUD.
## Wires the joystick / buttons to GameInput, keeps everything inside the
## device safe area (notches / rounded corners) and shows FPS + hit counter.

@export var player: Player

@onready var _safe_area: Control = $SafeArea
@onready var _joystick: VirtualJoystick = $SafeArea/Joystick
@onready var _attack_button: TouchActionButton = $SafeArea/AttackButton
@onready var _run_button: TouchActionButton = $SafeArea/RunButton
@onready var _fps_label: Label = $SafeArea/TopBar/FpsLabel
@onready var _hits_label: Label = $SafeArea/TopBar/HitsLabel
@onready var _hint_label: Label = $SafeArea/HintLabel

var _fps_timer := 0.0
var _hint_time := 6.0


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_attack_button.pressed.connect(_on_attack_pressed)
	_attack_button.released.connect(_on_attack_released)
	_run_button.toggled.connect(_on_run_toggled)
	GameInput.run_toggled.connect(_run_button.set_toggled)
	if player:
		player.hit_landed.connect(_on_hit_landed)
	get_viewport().size_changed.connect(_apply_safe_area)
	_apply_safe_area()
	_hint_label.visible = true


func _process(delta: float) -> void:
	_fps_timer -= delta
	if _fps_timer <= 0.0:
		_fps_timer = 0.5
		_fps_label.text = "%d FPS" % Engine.get_frames_per_second()
	if player:
		_attack_button.cooldown_ratio = player.get_attack_cooldown_ratio()
	if _hint_time > 0.0:
		_hint_time -= delta
		_hint_label.modulate.a = clampf(_hint_time, 0.0, 1.0)
		if _hint_time <= 0.0:
			_hint_label.visible = false


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("toggle_debug"):
		_fps_label.visible = not _fps_label.visible


func _on_attack_pressed() -> void:
	GameInput.attack_held = true
	GameInput.request_attack()


func _on_attack_released() -> void:
	GameInput.attack_held = false


func _on_run_toggled(enabled: bool) -> void:
	GameInput.set_run_enabled(enabled)


func _on_hit_landed(total: int) -> void:
	_hits_label.text = "HITS  %d" % total


## Insets the controls by the display cutout / safe area, converted from
## window pixels to the (stretched) viewport coordinate space.
func _apply_safe_area() -> void:
	var window_size := Vector2(DisplayServer.window_get_size())
	var viewport_size := get_viewport().get_visible_rect().size
	if window_size.x <= 0.0 or window_size.y <= 0.0:
		return
	var safe := Rect2(DisplayServer.get_display_safe_area())
	var scale := viewport_size / window_size
	var left := 0.0
	var right := 0.0
	var top := 0.0
	var bottom := 0.0
	if OS.has_feature("mobile") and safe.size.x > 0.0 and safe.size.y > 0.0:
		left = maxf(safe.position.x, 0.0) * scale.x
		top = maxf(safe.position.y, 0.0) * scale.y
		right = maxf(window_size.x - safe.end.x, 0.0) * scale.x
		bottom = maxf(window_size.y - safe.end.y, 0.0) * scale.y
	_safe_area.offset_left = left
	_safe_area.offset_top = top
	_safe_area.offset_right = -right
	_safe_area.offset_bottom = -bottom
