#version 460 core
// Procedural moon: sphere with maria + craters, lit with Lommel-Seeliger
// shading from the real phase angle, plus faint earthshine on the dark side.
#include <flutter/runtime_effect.glsl>
precision highp float;

uniform vec2 uSize;
uniform float uPhase;     // 0 new, 0.25 first quarter, 0.5 full, 0.75 last
uniform float uTilt;      // rotation of the bright limb (radians)
uniform float uEarthshine;

out vec4 fragColor;

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
             mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
  float v = 0.0;
  float a = 0.5;
  for (int i = 0; i < 5; i++) {
    v += a * noise(p);
    p *= 2.07;
    a *= 0.5;
  }
  return v;
}

float craters(vec2 p) {
  float c = 0.0;
  for (int i = 0; i < 3; i++) {
    vec2 g = p * (3.0 + float(i) * 3.5);
    vec2 id = floor(g);
    vec2 f = fract(g) - 0.5;
    vec2 o = vec2(hash(id), hash(id + 7.3)) - 0.5;
    float r = length(f - o * 0.5);
    float size = 0.12 + 0.18 * hash(id + 2.1);
    float rim = smoothstep(size, size * 0.75, r) - smoothstep(size * 0.75, size * 0.3, r) * 0.6;
    c += rim * (0.5 / (1.0 + float(i)));
  }
  return c;
}

void main() {
  vec2 frag = FlutterFragCoord().xy;
  // Canvas is 1.35x the disc so the soft glow fits around it.
  vec2 uv = ((frag / uSize) * 2.0 - 1.0) * 1.35;
  float r = length(uv);
  if (r > 1.0) {
    // soft atmospheric glow
    float glow = exp(-(r - 1.0) * 6.0) * 0.25 * smoothstep(0.0, 0.5, sin(uPhase * 3.14159));
    fragColor = vec4(vec3(0.85, 0.88, 0.95) * glow, glow);
    return;
  }

  vec3 n = vec3(uv.x, -uv.y, sqrt(1.0 - r * r));

  // Albedo: dark maria + bright highlands + craters.
  vec2 tex = n.xy * 1.6 + 4.0;
  float maria = smoothstep(0.40, 0.66, fbm(tex * 0.9 + 1.3));
  float albedo = mix(0.80, 0.50, maria) + (fbm(tex * 7.0) - 0.5) * 0.14;
  // Craters only on the bright highlands, and subtle.
  albedo += craters(tex * 0.8) * 0.06 * (1.0 - maria);

  // Light direction from the phase angle.
  float theta = uPhase * 6.2831853;
  float ct = cos(uTilt);
  float st = sin(uTilt);
  vec3 l = normalize(vec3(sin(theta), 0.0, -cos(theta)));
  l = vec3(l.x * ct - l.y * st, l.x * st + l.y * ct, l.z);

  float mu0 = max(dot(n, l), 0.0);
  float mu = n.z;
  float ls = mu0 / (mu0 + mu + 1e-4) * 2.0; // Lommel-Seeliger
  float terminator = smoothstep(-0.02, 0.06, dot(n, l));
  vec3 sunlit = vec3(1.0, 0.97, 0.92) * albedo * ls * terminator;
  vec3 earthshine = vec3(0.30, 0.36, 0.48) * albedo * uEarthshine * (1.0 - terminator);

  vec3 col = sunlit + earthshine;
  col = 1.0 - exp(-col * 1.3);
  float edge = smoothstep(1.0, 0.985, r);
  fragColor = vec4(col * edge, edge);
}
