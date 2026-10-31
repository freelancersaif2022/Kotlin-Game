class_name World
extends Node3D
## Builds the forest arena from the KayKit Forest Nature Pack: sky, lighting,
## ground, trees, rocks, bushes, grass (as MultiMeshes for mobile speed),
## collision, a navigation mesh for the enemies, and atmosphere particles.

signal navigation_ready

const ARENA_RADIUS := 34.0
const FOREST := "res://assets/forest/%s.gltf"
const WEAPONS := "res://assets/weapons/%s.gltf"

const TREES := ["Tree_1_A_Color1", "Tree_1_B_Color1", "Tree_1_C_Color1", "Tree_2_A_Color1",
	"Tree_2_B_Color1", "Tree_2_C_Color1", "Tree_2_D_Color1", "Tree_2_E_Color1",
	"Tree_3_A_Color1", "Tree_3_B_Color1", "Tree_3_C_Color1", "Tree_4_A_Color1",
	"Tree_4_B_Color1", "Tree_4_C_Color1"]
const BARE_TREES := ["Tree_Bare_1_A_Color1", "Tree_Bare_1_B_Color1", "Tree_Bare_1_C_Color1",
	"Tree_Bare_2_A_Color1", "Tree_Bare_2_B_Color1", "Tree_Bare_2_C_Color1"]
const BIG_ROCKS := ["Rock_1_G_Color1", "Rock_1_J_Color1", "Rock_1_K_Color1", "Rock_1_M_Color1",
	"Rock_1_P_Color1", "Rock_2_E_Color1", "Rock_2_F_Color1", "Rock_2_H_Color1"]
const SMALL_ROCKS := ["Rock_1_A_Color1", "Rock_1_B_Color1", "Rock_1_C_Color1", "Rock_1_D_Color1",
	"Rock_2_A_Color1", "Rock_2_B_Color1", "Rock_2_C_Color1", "Rock_3_A_Color1",
	"Rock_3_C_Color1", "Rock_3_E_Color1", "Rock_3_F_Color1"]
const BUSHES := ["Bush_1_A_Color1", "Bush_1_C_Color1", "Bush_1_D_Color1", "Bush_1_G_Color1",
	"Bush_2_A_Color1", "Bush_2_C_Color1", "Bush_2_F_Color1", "Bush_3_A_Color1",
	"Bush_3_C_Color1", "Bush_4_A_Color1", "Bush_4_C_Color1", "Bush_4_F_Color1"]
const GRASS := ["Grass_1_A_Color1", "Grass_1_B_Color1", "Grass_1_C_Color1", "Grass_1_D_Color1",
	"Grass_2_A_Color1", "Grass_2_B_Color1", "Grass_2_C_Color1", "Grass_2_D_Color1"]

var nav_region: NavigationRegion3D
var sun: DirectionalLight3D
var _rng := RandomNumberGenerator.new()
var _mesh_cache := {}        # name -> [Mesh, Transform3D local to scene]
var _batches := {}           # name -> Array[Transform3D]
var _no_shadow := {}         # names whose MultiMesh should not cast shadows
var _occupied: Array[Vector3] = []  # x,z + radius in y, for spacing


func build(seed_value := 1337) -> void:
	_rng.seed = seed_value
	_build_environment()
	nav_region = NavigationRegion3D.new()
	nav_region.name = "Navigation"
	add_child(nav_region)
	_build_ground()
	_build_boundary()
	_scatter()
	_flush_batches()
	_build_particles()


func bake_navigation() -> void:
	var nm := NavigationMesh.new()
	nm.geometry_parsed_geometry_type = NavigationMesh.PARSED_GEOMETRY_STATIC_COLLIDERS
	nm.geometry_collision_mask = 1
	nm.agent_radius = 0.6
	nm.agent_height = 1.8
	nm.agent_max_climb = 0.3
	nm.cell_size = 0.25
	nm.cell_height = 0.25
	nm.filter_baking_aabb = AABB(Vector3(-ARENA_RADIUS - 4, -1, -ARENA_RADIUS - 4),
		Vector3(ARENA_RADIUS * 2 + 8, 5, ARENA_RADIUS * 2 + 8))
	nav_region.navigation_mesh = nm
	nav_region.bake_finished.connect(func(): navigation_ready.emit(), CONNECT_ONE_SHOT)
	nav_region.bake_navigation_mesh(true)


# --- Sky, light, fog -------------------------------------------------------

func _build_environment() -> void:
	var sky_mat := ProceduralSkyMaterial.new()
	sky_mat.sky_top_color = Color(0.26, 0.48, 0.78)
	sky_mat.sky_horizon_color = Color(0.78, 0.84, 0.80)
	sky_mat.ground_horizon_color = Color(0.45, 0.55, 0.45)
	sky_mat.ground_bottom_color = Color(0.12, 0.18, 0.12)
	sky_mat.sun_angle_max = 20.0
	var sky := Sky.new()
	sky.sky_material = sky_mat

	var env := Environment.new()
	env.background_mode = Environment.BG_SKY
	env.sky = sky
	env.ambient_light_source = Environment.AMBIENT_SOURCE_SKY
	env.ambient_light_energy = 0.55
	env.tonemap_mode = Environment.TONE_MAPPER_ACES
	env.tonemap_exposure = 0.95
	env.adjustment_enabled = true
	env.adjustment_saturation = 1.15
	env.adjustment_contrast = 1.08
	env.fog_enabled = true
	env.fog_light_color = Color(0.5, 0.62, 0.6)
	env.fog_density = 0.007
	env.fog_sky_affect = 0.25
	env.glow_enabled = true
	env.glow_intensity = 0.6
	env.glow_bloom = 0.05
	var world_env := WorldEnvironment.new()
	world_env.environment = env
	add_child(world_env)

	sun = DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-52, -38, 0)
	sun.light_color = Color(1.0, 0.94, 0.82)
	sun.light_energy = 1.35
	sun.shadow_enabled = true
	sun.shadow_bias = 0.04
	sun.shadow_blur = 1.5
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_ORTHOGONAL
	sun.directional_shadow_max_distance = 45.0
	add_child(sun)


# --- Ground and borders ------------------------------------------------------

func _build_ground() -> void:
	var shader := Shader.new()
	shader.code = """
shader_type spatial;
uniform sampler2D noise_tex : repeat_enable, filter_linear_mipmap;
uniform vec3 grass_a : source_color = vec3(0.27, 0.50, 0.17);
uniform vec3 grass_b : source_color = vec3(0.15, 0.36, 0.14);
uniform vec3 dirt : source_color = vec3(0.47, 0.36, 0.22);
uniform float clearing_radius = 7.0;
varying vec3 world_pos;
void vertex() { world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz; }
void fragment() {
	float n = texture(noise_tex, world_pos.xz * 0.035).r;
	float fine = texture(noise_tex, world_pos.xz * 0.25).r;
	vec3 col = mix(grass_b, grass_a, smoothstep(0.3, 0.7, n));
	col *= 0.9 + fine * 0.2;
	float d = length(world_pos.xz) + (n - 0.5) * 4.0;
	float dirt_mask = 1.0 - smoothstep(clearing_radius - 1.5, clearing_radius + 1.5, d);
	col = mix(col, dirt * (0.85 + fine * 0.3), dirt_mask);
	ALBEDO = col;
	ROUGHNESS = 0.95;
	SPECULAR = 0.1;
}
"""
	var noise := FastNoiseLite.new()
	noise.seed = _rng.randi()
	noise.frequency = 0.02
	noise.fractal_octaves = 4
	var tex := NoiseTexture2D.new()
	tex.noise = noise
	tex.seamless = true
	tex.width = 256
	tex.height = 256
	tex.generate_mipmaps = true
	var mat := ShaderMaterial.new()
	mat.shader = shader
	mat.set_shader_parameter("noise_tex", tex)

	var plane := PlaneMesh.new()
	plane.size = Vector2(240, 240)
	plane.subdivide_width = 24
	plane.subdivide_depth = 24
	plane.material = mat
	var ground_mesh := MeshInstance3D.new()
	ground_mesh.mesh = plane
	ground_mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(ground_mesh)

	var body := StaticBody3D.new()
	body.name = "Ground"
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(240, 1, 240)
	shape.shape = box
	shape.position.y = -0.5
	body.add_child(shape)
	nav_region.add_child(body)


func _build_boundary() -> void:
	# Invisible wall just inside the dense tree ring.
	var body := StaticBody3D.new()
	body.name = "ArenaWall"
	var segments := 48
	var r := ARENA_RADIUS + 2.0
	for i in segments:
		var a := TAU * i / segments
		var cs := CollisionShape3D.new()
		var box := BoxShape3D.new()
		box.size = Vector3(TAU * r / segments + 0.6, 6, 1.0)
		cs.shape = box
		cs.position = Vector3(cos(a) * r, 3, sin(a) * r)
		cs.rotation.y = -a + PI / 2
		body.add_child(cs)
	add_child(body)  # outside the nav region: the bake area stops at the wall anyway

	# Dense forest ring that hides the edge of the world.
	var placed := 0
	var tries := 0
	while placed < 320 and tries < 6000:
		tries += 1
		var a := _rng.randf() * TAU
		var d := _rng.randf_range(ARENA_RADIUS + 1.5, ARENA_RADIUS + 42.0)
		var pos := Vector3(cos(a) * d, 0, sin(a) * d)
		if not _is_free(pos, 2.6):
			continue
		var id: String = TREES[_rng.randi() % TREES.size()] if _rng.randf() < 0.88 \
			else BARE_TREES[_rng.randi() % BARE_TREES.size()]
		_place(id, pos, _rng.randf_range(1.3, 2.1))
		_occupied.append(Vector3(pos.x, 2.6, pos.z))
		placed += 1
	for i in 140:
		var a := _rng.randf() * TAU
		var d := _rng.randf_range(ARENA_RADIUS - 1.0, ARENA_RADIUS + 12.0)
		_place(BUSHES[_rng.randi() % BUSHES.size()], Vector3(cos(a) * d, 0, sin(a) * d),
			_rng.randf_range(1.0, 1.8))


# --- Arena contents ------------------------------------------------------------

func _scatter() -> void:
	# Keep the central clearing open for the fight.
	_occupied.append(Vector3(0, 9.0, 0))

	_spread(16, 10.0, ARENA_RADIUS - 3.0, 6.0, func(pos: Vector3):
		var id: String = TREES[_rng.randi() % TREES.size()]
		var s := _rng.randf_range(1.2, 1.7)
		_place(id, pos, s)
		_add_cylinder(pos, 0.45 * s, 6.0))

	_spread(6, 10.0, ARENA_RADIUS - 3.0, 5.0, func(pos: Vector3):
		var id: String = BARE_TREES[_rng.randi() % BARE_TREES.size()]
		var s := _rng.randf_range(1.2, 1.6)
		_place(id, pos, s)
		_add_cylinder(pos, 0.3 * s, 4.0))

	_spread(10, 9.0, ARENA_RADIUS - 2.0, 4.5, func(pos: Vector3):
		var id: String = BIG_ROCKS[_rng.randi() % BIG_ROCKS.size()]
		var s := _rng.randf_range(0.8, 1.2)
		var aabb := _mesh_aabb(id)
		_place(id, pos, s)
		var radius := maxf(aabb.size.x, aabb.size.z) * 0.4 * s
		_add_cylinder(pos, radius, aabb.size.y * s))

	for i in 70:
		var pos := _random_point(4.0, ARENA_RADIUS + 6.0)
		_place(SMALL_ROCKS[_rng.randi() % SMALL_ROCKS.size()], pos + Vector3(0, -0.15, 0),
			_rng.randf_range(0.5, 1.0))
	_spread(45, 9.0, ARENA_RADIUS - 1.0, 1.6, func(pos: Vector3):
		_place(BUSHES[_rng.randi() % BUSHES.size()], pos, _rng.randf_range(0.8, 1.4)))

	for id in GRASS:
		_no_shadow[id] = true
	for i in 3200:
		var pos := _random_point(0.0, ARENA_RADIUS + 20.0)
		if pos.length() < 7.0 and _rng.randf() < 0.9:
			continue  # sparse grass in the dirt clearing
		# Shorter grass inside the arena keeps the fighters readable.
		var s := _rng.randf_range(0.6, 1.0) if pos.length() < ARENA_RADIUS else _rng.randf_range(0.9, 1.5)
		_place(GRASS[_rng.randi() % GRASS.size()], pos, s)

	# Remains of an old battle around the clearing: broken arrows and lost shields.
	var debris := ["Skeleton_Arrow_Broken", "Skeleton_Arrow_Broken_Half", "Skeleton_Arrow_Half",
		"Skeleton_Shield_Small_A", "Skeleton_Shield_Large_B", "Skeleton_Axe", "Skeleton_Blade"]
	for i in 26:
		var item: Node3D = load(WEAPONS % debris[_rng.randi() % debris.size()]).instantiate()
		var pos := _random_point(2.5, 14.0)
		item.position = pos + Vector3(0, 0.05, 0)
		if item.name.begins_with("Skeleton_Arrow"):
			item.rotation = Vector3(_rng.randf_range(-0.5, 0.5), _rng.randf() * TAU, _rng.randf_range(-0.5, 0.5))
		else:
			item.rotation = Vector3(-PI / 2 + _rng.randf_range(-0.15, 0.15), _rng.randf() * TAU, 0)
			item.position.y = 0.12
		add_child(item)


func _spread(count: int, min_r: float, max_r: float, spacing: float, place: Callable) -> void:
	var placed := 0
	var tries := 0
	while placed < count and tries < count * 60:
		tries += 1
		var pos := _random_point(min_r, max_r)
		if not _is_free(pos, spacing):
			continue
		_occupied.append(Vector3(pos.x, spacing, pos.z))
		place.call(pos)
		placed += 1


func _random_point(min_r: float, max_r: float) -> Vector3:
	var a := _rng.randf() * TAU
	var d := sqrt(_rng.randf_range(min_r * min_r, max_r * max_r))
	return Vector3(cos(a) * d, 0, sin(a) * d)


func _is_free(pos: Vector3, radius: float) -> bool:
	for o in _occupied:
		if Vector2(o.x - pos.x, o.z - pos.z).length() < maxf(o.y, radius):
			return false
	return true


func _add_cylinder(pos: Vector3, radius: float, height: float) -> void:
	var body := StaticBody3D.new()
	body.position = pos
	var cs := CollisionShape3D.new()
	var cyl := CylinderShape3D.new()
	cyl.radius = radius
	cyl.height = height
	cs.shape = cyl
	cs.position.y = height * 0.5
	body.add_child(cs)
	nav_region.add_child(body)


# --- Batched mesh instancing --------------------------------------------------

func _mesh_info(id: String) -> Array:
	if not _mesh_cache.has(id):
		var scene: Node3D = load(FOREST % id).instantiate()
		var mi: MeshInstance3D = scene.find_children("*", "MeshInstance3D", true, false)[0]
		_mesh_cache[id] = [mi.mesh, mi.transform]
		scene.free()
	return _mesh_cache[id]


func _mesh_aabb(id: String) -> AABB:
	var info := _mesh_info(id)
	return info[1] * (info[0] as Mesh).get_aabb()


func _place(id: String, pos: Vector3, scale_factor: float) -> void:
	var basis := Basis(Vector3.UP, _rng.randf() * TAU).scaled(Vector3.ONE * scale_factor)
	if not _batches.has(id):
		_batches[id] = []
	_batches[id].append(Transform3D(basis, pos) * _mesh_info(id)[1])


func _flush_batches() -> void:
	for id in _batches:
		var transforms: Array = _batches[id]
		var mm := MultiMesh.new()
		mm.transform_format = MultiMesh.TRANSFORM_3D
		mm.mesh = _mesh_info(id)[0]
		mm.instance_count = transforms.size()
		for i in transforms.size():
			mm.set_instance_transform(i, transforms[i])
		var mmi := MultiMeshInstance3D.new()
		mmi.name = id
		mmi.multimesh = mm
		if _no_shadow.has(id):
			mmi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		add_child(mmi)
	_batches.clear()


# --- Atmosphere -------------------------------------------------------------

func _build_particles() -> void:
	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mat.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	mat.vertex_color_use_as_albedo = true
	mat.albedo_texture = FX.soft_circle()
	var quad := QuadMesh.new()
	quad.size = Vector2(0.2, 0.2)
	quad.material = mat

	var fireflies := CPUParticles3D.new()
	fireflies.name = "Fireflies"
	fireflies.mesh = quad
	fireflies.amount = 90
	fireflies.lifetime = 6.0
	fireflies.preprocess = 6.0
	fireflies.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
	fireflies.emission_box_extents = Vector3(ARENA_RADIUS, 1.5, ARENA_RADIUS)
	fireflies.position.y = 2.0
	fireflies.gravity = Vector3.ZERO
	fireflies.direction = Vector3.UP
	fireflies.spread = 180.0
	fireflies.initial_velocity_min = 0.1
	fireflies.initial_velocity_max = 0.4
	var ramp := Gradient.new()
	ramp.set_color(0, Color(0.8, 1.0, 0.4, 0.0))
	ramp.set_color(1, Color(0.8, 1.0, 0.4, 0.0))
	ramp.add_point(0.3, Color(0.85, 1.0, 0.45, 1.0))
	ramp.add_point(0.7, Color(0.85, 1.0, 0.45, 0.9))
	fireflies.color_ramp = ramp
	add_child(fireflies)

	var leaf_mat := StandardMaterial3D.new()
	leaf_mat.albedo_color = Color(0.85, 0.55, 0.2)
	leaf_mat.cull_mode = BaseMaterial3D.CULL_DISABLED
	var leaf := QuadMesh.new()
	leaf.size = Vector2(0.14, 0.09)
	leaf.material = leaf_mat
	var leaves := CPUParticles3D.new()
	leaves.name = "FallingLeaves"
	leaves.mesh = leaf
	leaves.amount = 60
	leaves.lifetime = 8.0
	leaves.preprocess = 8.0
	leaves.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
	leaves.emission_box_extents = Vector3(ARENA_RADIUS, 0.5, ARENA_RADIUS)
	leaves.position.y = 9.0
	leaves.gravity = Vector3(0.4, -1.0, 0.2)
	leaves.angular_velocity_min = -180
	leaves.angular_velocity_max = 180
	leaves.particle_flag_rotate_y = true
	add_child(leaves)
