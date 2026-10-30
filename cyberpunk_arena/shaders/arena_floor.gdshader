shader_type spatial;
// Clean white arena floor: anti-aliased tile grid that fades with distance,
// plus glowing concentric rings in the centre. Fully procedural (no textures)
// and cheap enough for mobile GPUs.

uniform vec3 base_color : source_color = vec3(0.94, 0.95, 0.965);
uniform vec3 line_color : source_color = vec3(0.80, 0.83, 0.87);
uniform vec3 accent_color : source_color = vec3(0.0, 0.82, 1.0);
uniform float tile_size = 2.0;
uniform float line_width = 0.025;
uniform float major_every = 5.0;
uniform float grid_fade_start = 18.0;
uniform float grid_fade_end = 48.0;
uniform float ring_radius_a = 3.0;
uniform float ring_radius_b = 7.5;
uniform float ring_width = 0.05;
uniform float ring_glow = 1.6;
uniform float roughness_value : hint_range(0.0, 1.0) = 0.42;

varying vec3 world_pos;

float grid_line(vec2 p, float cell, float width) {
	vec2 g = abs(fract(p / cell - 0.5) - 0.5) * cell;
	float d = min(g.x, g.y);
	float aa = max(fwidth(p.x), fwidth(p.y)) * 1.2;
	return 1.0 - smoothstep(width - aa, width + aa, d);
}

float ring(float r, float radius, float width) {
	float aa = fwidth(r) * 1.2;
	return 1.0 - smoothstep(width - aa, width + aa, abs(r - radius));
}

void vertex() {
	world_pos = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
	vec2 p = world_pos.xz;
	float dist = length(world_pos - CAMERA_POSITION_WORLD);
	float fade = 1.0 - smoothstep(grid_fade_start, grid_fade_end, dist);

	float minor = grid_line(p, tile_size, line_width) * 0.55;
	float major = grid_line(p, tile_size * major_every, line_width * 1.6);
	float lines = max(minor, major) * fade;

	float r = length(p);
	float rings = max(ring(r, ring_radius_a, ring_width), ring(r, ring_radius_b, ring_width * 1.4));

	// Very soft darkening toward the arena edge adds depth to the white space.
	float vignette = smoothstep(10.0, 40.0, r) * 0.05;

	ALBEDO = mix(base_color, line_color, lines) - vignette;
	ALBEDO = mix(ALBEDO, accent_color, rings);
	EMISSION = accent_color * rings * ring_glow;
	ROUGHNESS = roughness_value;
	SPECULAR = 0.35;
}
