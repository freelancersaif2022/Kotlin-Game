#version 460 core
// Photographic sky model. Colour keys (zenith / horizon toward the sun /
// horizon away from the sun / sun glow) come from real sky photography at each
// sun altitude and are interpolated on the CPU. This shader adds the
// physically shaped parts: Mie-style forward glow, horizon haze, the Earth's
// shadow and the pink Belt of Venus at twilight, and a limb-darkened sun.
#include <flutter/runtime_effect.glsl>
precision highp float;

uniform vec2 uSize;
uniform float uViewAz;   // compass bearing at screen centre (deg)
uniform float uHFov;     // horizontal field of view (deg)
uniform float uVFov;     // vertical field of view (deg)
uniform float uHorizon;  // horizon position, 0 = top, 1 = bottom
uniform float uSunAz;    // sun bearing (deg)
uniform float uSunAlt;   // sun altitude (deg)
uniform vec3 uZenith;
uniform vec3 uHorSun;
uniform vec3 uHorAway;
uniform vec3 uGlow;

out vec4 fragColor;

vec3 dirFrom(float azDeg, float altDeg) {
  float az = radians(azDeg);
  float alt = radians(altDeg);
  return vec3(cos(alt) * sin(az), sin(alt), cos(alt) * cos(az));
}

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
  vec2 frag = FlutterFragCoord().xy;
  vec2 uv = frag / uSize;

  float az = uViewAz + (uv.x - 0.5) * uHFov;
  float elev = (uHorizon - uv.y) * uVFov;
  float e = max(elev, 0.0);

  vec3 rd = dirFrom(az, e);
  vec3 sd = dirFrom(uSunAz, uSunAlt);
  float ang = acos(clamp(dot(rd, sd), -1.0, 1.0));

  // 1 when looking toward the sun's azimuth, 0 when looking away.
  float side = pow(0.5 + 0.5 * cos(radians(az - uSunAz)), 1.7);
  vec3 horizon = mix(uHorAway, uHorSun, side);

  // Base gradient: compressed towards the horizon like a real sky.
  float t = pow(clamp(e / 90.0, 0.0, 1.0), 0.42);
  vec3 col = mix(horizon, uZenith, smoothstep(0.0, 1.0, t));

  // Bright haze layer hugging the horizon.
  col += horizon * 0.16 * exp(-e / 2.5);

  // Twilight: Earth's shadow rising on the anti-solar side, topped by the
  // pinkish Belt of Venus.
  float tw = smoothstep(2.0, -0.5, uSunAlt) * smoothstep(-9.0, -3.0, uSunAlt);
  float anti = 1.0 - side;
  float beltH = clamp(-uSunAlt * 1.1 + 1.5, 1.0, 9.0);
  float shadow = (1.0 - smoothstep(beltH - 1.5, beltH + 1.0, e)) * anti * tw;
  col = mix(col, col * vec3(0.55, 0.62, 0.88), shadow * 0.65);
  float belt = exp(-pow((e - beltH - 3.0) / 3.2, 2.0)) * anti * tw;
  col += vec3(0.30, 0.14, 0.20) * belt * 0.55;

  // Sun glow: strong forward scattering, widest when the sun is low.
  float lowSun = smoothstep(30.0, 1.0, uSunAlt);
  float glowAmt = mix(0.35, 1.0, lowSun) * smoothstep(-11.0, -1.5, uSunAlt);
  float horizonBoost = mix(1.0, 1.8, exp(-e / 6.0) * lowSun);
  vec3 glow = uGlow * (exp(-ang * 10.0) * 0.95 + exp(-ang * 2.4) * 0.38) * glowAmt * horizonBoost;
  col += glow * (1.0 - clamp(col, 0.0, 1.0) * 0.55);

  // Sun disc with limb darkening, reddened near the horizon.
  if (uSunAlt > -1.0 && elev > -0.3) {
    float sunR = radians(0.62);
    float r = ang / sunR;
    float disc = 1.0 - smoothstep(0.88, 1.06, r);
    float limb = 0.72 + 0.28 * sqrt(max(1.0 - r * r, 0.0));
    vec3 discCol = mix(uGlow * 1.25 + vec3(0.18, 0.05, 0.0), vec3(1.0, 0.99, 0.95), smoothstep(1.0, 14.0, uSunAlt));
    col = mix(col, min(discCol * limb * 1.15, vec3(1.0)), disc);
    col += uGlow * exp(-r * 0.9) * 0.35 * smoothstep(-1.0, 0.0, uSunAlt);
  }

  // Ground below the horizon: dark, tinted by the horizon light.
  if (elev < 0.0) {
    float k = smoothstep(0.0, 10.0, -elev);
    col = mix(horizon * 0.45, horizon * 0.06 + vec3(0.012, 0.012, 0.018), k);
  }

  col = clamp(col, 0.0, 1.0);
  col += (hash(frag) - 0.5) / 255.0; // dither: no banding
  fragColor = vec4(col, 1.0);
}
