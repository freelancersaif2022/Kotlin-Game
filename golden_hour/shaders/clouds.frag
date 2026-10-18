#version 460 core
// Drifting volumetric-looking clouds on a perspective plane, lit by the
// current sun colour with forward-scattering "silver lining" near the sun.
#include <flutter/runtime_effect.glsl>
precision highp float;

uniform vec2 uSize;
uniform float uTime;
uniform float uHorizon;   // 0..1 from top
uniform vec2 uSunUv;      // sun position on screen (0..1), may be off-screen
uniform vec3 uSunColor;   // linear-ish colour of direct sunlight
uniform vec3 uSkyColor;   // ambient sky colour
uniform float uCover;     // 0 clear .. 1 overcast
uniform float uNight;     // 0 day .. 1 night

out vec4 fragColor;

float hash(vec2 p) {
  p = fract(p * vec2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
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
  mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
  for (int i = 0; i < 6; i++) {
    v += a * noise(p);
    p = rot * p * 2.02 + 3.7;
    a *= 0.5;
  }
  return v;
}

void main() {
  vec2 frag = FlutterFragCoord().xy;
  vec2 uv = frag / uSize;
  float above = uHorizon - uv.y;
  if (above <= 0.004) {
    fragColor = vec4(0.0);
    return;
  }
  float aspect = uSize.x / uSize.y;

  // Project onto a cloud plane: far away near the horizon, close overhead.
  float depth = 0.35 / max(above, 0.035);
  vec2 p = vec2((uv.x - 0.5) * aspect * depth, depth);
  vec2 wind = vec2(uTime * 0.012, uTime * 0.004);
  vec2 q = p * vec2(1.1, 1.8) + wind;

  float n = fbm(q);
  float detail = fbm(q * 3.1 - wind * 1.7);
  float d = n * 0.78 + detail * 0.22;

  float threshold = mix(0.66, 0.36, uCover);
  float dens = smoothstep(threshold, threshold + 0.22, d);

  // Self-shadow: sample towards the sun.
  vec2 toSun = normalize(uSunUv - uv + 1e-4);
  float dShadow = fbm(q + toSun * 0.08) * 0.78 + detail * 0.22;
  float shade = clamp(1.0 - (dShadow - d) * 3.5, 0.35, 1.2);

  float sunDist = distance(vec2(uv.x * aspect, uv.y), vec2(uSunUv.x * aspect, uSunUv.y));
  float forward = exp(-sunDist * 3.2);
  float lining = pow(1.0 - dens, 2.0) * forward * 2.4;

  vec3 lit = uSunColor * (0.55 * shade + lining) + uSkyColor * 0.55;
  vec3 dark = uSkyColor * 0.45 + uSunColor * 0.08;
  vec3 col = mix(lit, dark, smoothstep(0.4, 1.0, dens) * 0.55);
  col = mix(col, vec3(0.05, 0.06, 0.09), uNight * 0.85);

  // Fade into haze at the horizon.
  float haze = smoothstep(0.01, 0.17, above);
  float alpha = dens * haze * mix(0.95, 0.55, uNight);
  fragColor = vec4(col * alpha, alpha);
}
