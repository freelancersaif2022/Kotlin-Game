class_name WarriorModel
extends Node3D
## Visual + animation controller for the imported cyberpunk_warrior.glb.
##
## What the GLB actually contains (verified on import):
##   * 27 rigid MeshInstance3D parts, all FLAT siblings under "world".
##   * No Skeleton3D / skin - animations are node-rotation tracks.
##   * 4 clips: "Idle", "Walk", "Run", "Attack" (imported without looping).
##   * No textures; colours are stored as per-vertex COLOR_0.
##
## Because the hierarchy is flat, rotating "R_upper_arm" would leave the
## forearm, glove and blade floating in place. On load we rebuild a proper
## part hierarchy (forearm -> upper arm -> torso, shin -> thigh ...) keeping
## every global transform, then retarget the animation track paths so the
## original clips drive the whole limb. Clips are detected by name; any role
## that is missing or whose tracks do not resolve falls back to a procedural
## animation so the game never breaks with a different GLB.

signal clips_detected(report: String)

enum State { IDLE, WALK, RUN }
enum Clip { IDLE, WALK, RUN, ATTACK }

const CLIP_KEYWORDS := {
	Clip.IDLE: ["idle", "stand", "breath"],
	Clip.WALK: ["walk"],
	Clip.RUN: ["run", "sprint", "jog"],
	Clip.ATTACK: ["attack", "slash", "swing", "strike", "punch", "hit"],
}
const CLIP_NAMES := {Clip.IDLE: "Idle", Clip.WALK: "Walk", Clip.RUN: "Run", Clip.ATTACK: "Attack"}

## child part -> part it should follow. Order matters only for readability;
## reparenting keeps global transforms so any order produces the same result.
const RIG_HIERARCHY := {
	"R_forearm": "R_upper_arm", "R_glove": "R_upper_arm",
	"R_arm_light": "R_upper_arm", "energy_blade": "R_upper_arm",
	"L_forearm": "L_upper_arm", "L_glove": "L_upper_arm", "L_arm_light": "L_upper_arm",
	"R_shin": "R_thigh", "R_boot": "R_thigh", "R_knee_light": "R_thigh",
	"L_shin": "L_thigh", "L_boot": "L_thigh", "L_knee_light": "L_thigh",
	"chest_plate": "torso", "reactor": "torso", "helmet": "torso", "visor": "torso",
	"helmet_crest": "torso", "L_shoulder": "torso", "R_shoulder": "torso",
	"L_upper_arm": "torso", "R_upper_arm": "torso",
}
const GLOW_PARTS := ["visor", "reactor", "energy_blade", "L_knee_light", "R_knee_light", "L_arm_light", "R_arm_light"]
const ACCENT_PARTS := ["helmet_crest", "L_shoulder", "R_shoulder"]

## Final standing height of the character in metres (model is auto-scaled).
@export var target_height := 1.8
## Ground speed (m/s) at which the walk / run clips play at 1x.
@export var walk_reference_speed := 1.7
@export var run_reference_speed := 3.9
@export var min_anim_speed := 0.7
@export var max_anim_speed := 1.6
@export var locomotion_blend := 0.2
@export var attack_blend := 0.06
@export var glow_energy := 2.4
## Max forward lean (degrees) applied while running.
@export var max_lean_degrees := 7.0

@onready var _lean_pivot: Node3D = $LeanPivot
@onready var _mesh_root: Node3D = $LeanPivot/Mesh

var _anim: AnimationPlayer
var _parts: Dictionary = {}
var _clips: Dictionary = {}
var _report := ""
var _state := State.IDLE
var _attacking := false
var _lean := 0.0
var _lean_target := 0.0
var _procedural_phase := 0.0
var _procedural_speed := 0.0
var _attack_tween: Tween
var _mesh_base_y := 0.0


func _ready() -> void:
	_anim = _mesh_root.find_child("AnimationPlayer", true, false) as AnimationPlayer
	if _anim:
		# Pose updates happen on physics ticks so 3D physics interpolation can
		# smooth them on high refresh-rate displays together with the body.
		_anim.callback_mode_process = AnimationMixer.ANIMATION_CALLBACK_MODE_PROCESS_PHYSICS
	for mi in _mesh_root.find_children("*", "MeshInstance3D", true, false):
		_parts[String(mi.name)] = mi
	_normalize_scale_and_ground()
	_apply_materials()
	_build_rig_hierarchy()
	_detect_clips()
	set_locomotion(State.IDLE, 0.0)


func _physics_process(delta: float) -> void:
	_lean = lerpf(_lean, _lean_target, 1.0 - exp(-8.0 * delta))
	_lean_pivot.rotation.x = -deg_to_rad(max_lean_degrees) * _lean
	if not _attacking and not _clips.has(_clip_for_state(_state)):
		_update_procedural_locomotion(delta)


# --------------------------------------------------------------------------
# Public API
# --------------------------------------------------------------------------

func get_clip_report() -> String:
	return _report


func has_clip(clip: Clip) -> bool:
	return _clips.has(clip)


func is_attacking() -> bool:
	return _attacking


## Requests a locomotion state. Ignored while an attack is playing so the
## attack can never be interrupted by movement.
func set_locomotion(state: State, ground_speed: float, lean_amount := 0.0) -> void:
	_lean_target = clampf(lean_amount, 0.0, 1.0)
	_procedural_speed = ground_speed
	if _attacking:
		return
	_state = state
	var clip := _clip_for_state(state)
	if _clips.has(clip):
		var anim_name: StringName = _clips[clip]
		var speed := 1.0
		if state == State.WALK:
			speed = clampf(ground_speed / walk_reference_speed, min_anim_speed, max_anim_speed)
		elif state == State.RUN:
			speed = clampf(ground_speed / run_reference_speed, min_anim_speed, max_anim_speed)
		# Locomotion playback rate follows ground speed to limit foot sliding.
		_anim.speed_scale = speed
		if _anim.current_animation != anim_name:
			_anim.play(anim_name, locomotion_blend)
	elif _anim and _anim.is_playing():
		_anim.stop(true)


## Starts the attack. Returns its duration in seconds (at the given speed).
func play_attack(speed_scale := 1.0) -> float:
	_attacking = true
	if _clips.has(Clip.ATTACK):
		var anim_name: StringName = _clips[Clip.ATTACK]
		_anim.speed_scale = 1.0
		_anim.play(anim_name, attack_blend, speed_scale)
		_anim.seek(0.0, true)
		return _anim.get_animation(anim_name).length / maxf(speed_scale, 0.01)
	return _play_procedural_attack(speed_scale)


## Called by the owner when the attack window is over; locomotion resumes.
func finish_attack() -> void:
	if not _attacking:
		return
	_attacking = false
	set_locomotion(_state, _procedural_speed, _lean_target)


# --------------------------------------------------------------------------
# Setup
# --------------------------------------------------------------------------

func _normalize_scale_and_ground() -> void:
	var bounds := AABB()
	var first := true
	var to_local := global_transform.affine_inverse()
	for mi: MeshInstance3D in _parts.values():
		var box: AABB = to_local * mi.global_transform * mi.get_aabb()
		bounds = box if first else bounds.merge(box)
		first = false
	if first or bounds.size.y <= 0.001:
		push_warning("WarriorModel: no mesh geometry found in GLB.")
		return
	var factor := target_height / bounds.size.y
	_mesh_root.scale *= factor
	# Feet exactly on the floor (y = 0 of this node).
	_mesh_root.position.y -= bounds.position.y * factor
	_mesh_base_y = _mesh_root.position.y


func _apply_materials() -> void:
	var cache: Dictionary = {}
	for part_name: String in _parts:
		var mi: MeshInstance3D = _parts[part_name]
		var base := _first_vertex_color(mi.mesh)
		var kind := "armor"
		if part_name in GLOW_PARTS:
			kind = "glow"
		elif part_name in ACCENT_PARTS:
			kind = "accent"
		var key := "%s_%s" % [kind, base.to_html()]
		if not cache.has(key):
			cache[key] = _make_material(kind, base)
		mi.material_override = cache[key]
		mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF if kind == "glow" else GeometryInstance3D.SHADOW_CASTING_SETTING_ON


func _make_material(kind: String, base: Color) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.vertex_color_use_as_albedo = true
	mat.vertex_color_is_srgb = true
	match kind:
		"glow":
			mat.roughness = 0.25
			mat.emission_enabled = true
			mat.emission = base
			mat.emission_energy_multiplier = glow_energy
		"accent":
			mat.metallic = 0.35
			mat.roughness = 0.32
			mat.emission_enabled = true
			mat.emission = base
			mat.emission_energy_multiplier = 0.35
		_:
			mat.metallic = 0.45
			mat.roughness = 0.4
			mat.metallic_specular = 0.6
	return mat


func _first_vertex_color(mesh: Mesh) -> Color:
	if mesh == null or mesh.get_surface_count() == 0:
		return Color.WHITE
	var arrays := mesh.surface_get_arrays(0)
	var colors = arrays[Mesh.ARRAY_COLOR]
	if colors is PackedColorArray and colors.size() > 0:
		return colors[0]
	return Color.WHITE


func _build_rig_hierarchy() -> void:
	var changed := false
	for child_name: String in RIG_HIERARCHY:
		var child: Node3D = _parts.get(child_name)
		var parent: Node3D = _parts.get(RIG_HIERARCHY[child_name])
		if child and parent and child.get_parent() != parent:
			child.reparent(parent, true)
			changed = true
	if changed and _anim:
		_retarget_tracks()


## Rewrites animation track paths so they point at the reparented parts.
func _retarget_tracks() -> void:
	var anim_root := _anim.get_node(_anim.root_node)
	for lib_name in _anim.get_animation_library_list():
		var lib := _anim.get_animation_library(lib_name)
		for anim_name in lib.get_animation_list():
			var anim := lib.get_animation(anim_name)
			for t in anim.get_track_count():
				var path := anim.track_get_path(t)
				var node_path := NodePath(path.get_concatenated_names())
				if anim_root.get_node_or_null(node_path) != null:
					continue
				var part_name := String(path.get_name(path.get_name_count() - 1))
				var target: Node = _parts.get(part_name)
				if target == null:
					continue
				var new_path := String(anim_root.get_path_to(target))
				if path.get_subname_count() > 0:
					new_path += ":" + String(path.get_concatenated_subnames())
				anim.track_set_path(t, NodePath(new_path))
	_anim.clear_caches()


func _detect_clips() -> void:
	var lines: PackedStringArray = []
	var available: PackedStringArray = []
	if _anim:
		for anim_name in _anim.get_animation_list():
			if String(anim_name).to_lower() not in ["reset", "_reset"]:
				available.append(anim_name)
	lines.append("GLB animation clips: %s" % (", ".join(available) if available.size() > 0 else "none"))
	for clip: Clip in CLIP_KEYWORDS:
		var found := _find_clip(available, CLIP_KEYWORDS[clip])
		var role: String = CLIP_NAMES[clip]
		if found == "":
			lines.append("  %s: MISSING -> procedural fallback" % role)
			continue
		var anim := _anim.get_animation(found)
		var unresolved := _count_unresolved_tracks(anim)
		if anim.get_track_count() == 0 or unresolved == anim.get_track_count():
			lines.append("  %s: '%s' INCOMPATIBLE (tracks do not match model nodes) -> procedural fallback" % [role, found])
			continue
		anim.loop_mode = Animation.LOOP_NONE if clip == Clip.ATTACK else Animation.LOOP_LINEAR
		_clips[clip] = StringName(found)
		lines.append("  %s: '%s' (%.2fs, %d tracks%s)" % [role, found, anim.length, anim.get_track_count(),
				", %d unresolved" % unresolved if unresolved > 0 else ""])
	# Locomotion fallbacks that reuse a real clip are better than procedural ones.
	if not _clips.has(Clip.RUN) and _clips.has(Clip.WALK):
		_clips[Clip.RUN] = _clips[Clip.WALK]
		lines.append("  Run fallback: using Walk clip at higher speed")
	elif not _clips.has(Clip.WALK) and _clips.has(Clip.RUN):
		_clips[Clip.WALK] = _clips[Clip.RUN]
		lines.append("  Walk fallback: using Run clip at lower speed")
	lines.append("  Rig: %s" % ("flat GLB hierarchy rebuilt into limb chains" if _parts.has("torso") else "unchanged"))
	_report = "\n".join(lines)
	print(_report)
	clips_detected.emit(_report)


func _find_clip(available: PackedStringArray, keywords: Array) -> String:
	for keyword: String in keywords:
		for anim_name in available:
			if anim_name.to_lower() == keyword:
				return anim_name
	for keyword: String in keywords:
		for anim_name in available:
			if anim_name.to_lower().contains(keyword):
				return anim_name
	return ""


func _count_unresolved_tracks(anim: Animation) -> int:
	var anim_root := _anim.get_node(_anim.root_node)
	var count := 0
	for t in anim.get_track_count():
		if anim_root.get_node_or_null(NodePath(anim.track_get_path(t).get_concatenated_names())) == null:
			count += 1
	return count


func _clip_for_state(state: State) -> Clip:
	match state:
		State.WALK:
			return Clip.WALK
		State.RUN:
			return Clip.RUN
	return Clip.IDLE


# --------------------------------------------------------------------------
# Procedural fallbacks (only used when the GLB lacks a usable clip)
# --------------------------------------------------------------------------

func _update_procedural_locomotion(delta: float) -> void:
	var moving := _state != State.IDLE
	var freq := 1.2 if not moving else clampf(_procedural_speed * 1.1, 4.0, 10.0)
	_procedural_phase = fmod(_procedural_phase + delta * freq, TAU)
	var swing := 0.0 if not moving else (0.35 if _state == State.WALK else 0.6)
	_set_part_rotation_x("L_thigh", sin(_procedural_phase) * swing)
	_set_part_rotation_x("R_thigh", -sin(_procedural_phase) * swing)
	_set_part_rotation_x("L_upper_arm", -sin(_procedural_phase) * swing * 0.7)
	_set_part_rotation_x("R_upper_arm", sin(_procedural_phase) * swing * 0.7)
	var bob := absf(sin(_procedural_phase)) * 0.04 if moving else sin(_procedural_phase) * 0.008
	_mesh_root.position.y = _mesh_base_y + bob


func _play_procedural_attack(speed_scale: float) -> float:
	var duration := 0.6 / maxf(speed_scale, 0.01)
	if _anim:
		_anim.stop(true)
	if _attack_tween:
		_attack_tween.kill()
	_attack_tween = create_tween().set_process_mode(Tween.TWEEN_PROCESS_PHYSICS)
	var arm: Node3D = _parts.get("R_upper_arm")
	if arm:
		_attack_tween.tween_property(arm, "rotation:x", deg_to_rad(-100.0), duration * 0.35).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
		_attack_tween.parallel().tween_property(_mesh_root, "rotation:y", PI + deg_to_rad(25.0), duration * 0.35)
		_attack_tween.tween_property(arm, "rotation:x", 0.0, duration * 0.65).set_trans(Tween.TRANS_SINE)
		_attack_tween.parallel().tween_property(_mesh_root, "rotation:y", PI, duration * 0.65)
	else:
		_attack_tween.tween_property(_lean_pivot, "position:z", -0.25, duration * 0.35)
		_attack_tween.tween_property(_lean_pivot, "position:z", 0.0, duration * 0.65)
	return duration


func _set_part_rotation_x(part_name: String, angle: float) -> void:
	var part: Node3D = _parts.get(part_name)
	if part:
		part.rotation.x = angle
