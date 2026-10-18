import 'dart:math' as math;
import 'dart:typed_data';

import 'dsp.dart';

typedef Keys = List<List<double>>;

/// One formant (mouth resonance) whose frequency glides over the call.
class FormantSpec {
  const FormantSpec(this.freq, this.bandwidth, this.gain);

  final Keys freq;
  final double bandwidth;
  final double gain;
}

/// Everything needed to synthesize one animal "word" (a bark, a meow...).
class CallSpec {
  CallSpec({
    required this.duration,
    required this.f0,
    required this.formants,
    this.noise = 0.05,
    this.roughness = 0,
    this.drive = 1,
    this.attack = 0.01,
    this.release = 0.04,
    this.decay = 0,
    this.vibratoRate = 0,
    this.vibratoDepth = 0,
    this.jitter = 0.01,
    this.pure = false,
    this.tilt = 3000,
    this.lowpass,
    this.trillRate = 0,
    this.trillUntil = 0,
    this.ampKeys,
    this.gain = 1,
  });

  final double duration;
  final Keys f0;
  final List<FormantSpec> formants;
  final double noise;
  final double roughness;
  final double drive;
  final double attack, release, decay;
  final double vibratoRate, vibratoDepth;
  final double jitter;

  /// Pure tone (sine with a few harmonics) instead of a buzzy saw – for howls.
  final bool pure;
  final double tilt;
  final Keys? lowpass;
  final double trillRate, trillUntil;
  final Keys? ampKeys;
  final double gain;

  CallSpec withGain(double k) => CallSpec(
    duration: duration,
    f0: f0,
    formants: formants,
    noise: noise,
    roughness: roughness,
    drive: drive,
    attack: attack,
    release: release,
    decay: decay,
    vibratoRate: vibratoRate,
    vibratoDepth: vibratoDepth,
    jitter: jitter,
    pure: pure,
    tilt: tilt,
    lowpass: lowpass,
    trillRate: trillRate,
    trillUntil: trillUntil,
    ampKeys: ampKeys,
    gain: gain * k,
  );
}

/// Renders a [CallSpec] into a buffer, mixing at [offset] samples.
void renderCall(CallSpec s, Float64List out, int offset, math.Random rng) {
  final n = (s.duration * kSampleRate).round();
  final saw = SawOsc(rng.nextDouble());
  final sine = SineOsc(rng.nextDouble() * 6.28);
  final sub = SineOsc();
  final h2 = SineOsc(), h3 = SineOsc(), h4 = SineOsc();
  final vib = SineOsc(rng.nextDouble() * 6.28);
  final trill = SineOsc();
  final jit = SmoothNoise(rng, 35);
  final tiltLp = OnePole();
  final lp1 = OnePole(), lp2 = OnePole();
  final dc = DcBlock();
  final filters = [for (final _ in s.formants) Formant()];

  for (var i = 0; i < n; i++) {
    final idx = offset + i;
    if (idx >= out.length) break;
    final t = i / kSampleRate;
    final p = i / n;

    var f0 = curve(s.f0, p);
    f0 *= 1 + jit.next() * s.jitter;
    if (s.vibratoRate > 0) f0 *= 1 + vib.next(s.vibratoRate) * s.vibratoDepth;

    double src;
    if (s.pure) {
      src =
          sine.next(f0) +
          0.35 * h2.next(f0 * 2) +
          0.12 * h3.next(f0 * 3) +
          0.05 * h4.next(f0 * 4);
    } else {
      src = tiltLp.process(saw.next(f0), s.tilt) * 2.2;
    }
    if (s.roughness > 0) {
      src *= 1 + s.roughness * sub.next(f0 * 0.5);
    }
    final noise = (rng.nextDouble() * 2 - 1) * s.noise;
    final excite = src * (1 - s.noise * 0.5) + noise * 3;

    double y;
    if (s.formants.isEmpty) {
      y = excite;
    } else {
      y = 0;
      for (var k = 0; k < s.formants.length; k++) {
        final fs = s.formants[k];
        y +=
            filters[k].process(excite, curve(fs.freq, p), fs.bandwidth) *
            fs.gain;
      }
    }

    if (s.lowpass != null) {
      final c = curve(s.lowpass!, p);
      y = lp2.process(lp1.process(y, c), c);
    }

    var amp = envelope(
      t,
      s.duration,
      attack: s.attack,
      release: s.release,
      decay: s.decay,
    );
    if (s.ampKeys != null) amp *= curve(s.ampKeys!, p);
    if (s.trillRate > 0 && p < s.trillUntil) {
      final depth = 1 - p / s.trillUntil;
      amp *= 1 - depth * 0.6 * (0.5 + 0.5 * trill.next(s.trillRate));
    }

    y = dc.process(y);
    if (s.drive > 1) y = softClip(y * s.drive) / softClip(s.drive);
    out[idx] += y * amp * s.gain;
  }
}

double _r(math.Random rng, double a, double b) =>
    a + rng.nextDouble() * (b - a);

List<FormantSpec> _scale(List<FormantSpec> f, double k) => [
  for (final x in f)
    FormantSpec(
      [
        for (final kf in x.freq) [kf[0], kf[1] * k],
      ],
      x.bandwidth * k,
      x.gain,
    ),
];

// ---------------------------------------------------------------------------
// Voices. Each returns one or more calls (a "word") with random variation so
// the translation never sounds exactly the same twice.
// ---------------------------------------------------------------------------

class PlacedCall {
  PlacedCall(this.spec, this.delay);

  final CallSpec spec;

  /// Seconds after the start of the word.
  final double delay;
}

typedef VoiceBuilder = List<PlacedCall> Function(
  math.Random rng,
  double syllableLen,
  double intensity,
);

CallSpec _bark(
  math.Random rng, {
  required double baseF0,
  required double formantScale,
  required double dur,
  double noise = 0.28,
  double roughness = 0.45,
}) {
  final f = baseF0 * _r(rng, 0.9, 1.12);
  final peakAt = _r(rng, 0.12, 0.22);
  final fs = _scale([
    const FormantSpec(
      [
        [0, 520],
        [0.2, 780],
        [1, 460],
      ],
      160,
      1.0,
    ),
    const FormantSpec(
      [
        [0, 1150],
        [0.2, 1450],
        [1, 950],
      ],
      200,
      0.7,
    ),
    const FormantSpec(
      [
        [0, 2600],
        [1, 2400],
      ],
      320,
      0.35,
    ),
    const FormantSpec(
      [
        [0, 3600],
        [1, 3400],
      ],
      450,
      0.15,
    ),
  ], formantScale * _r(rng, 0.94, 1.06));
  return CallSpec(
    duration: dur,
    f0: [
      [0, f * 0.82],
      [peakAt, f * 1.12],
      [0.55, f * 0.85],
      [1, f * 0.62],
    ],
    formants: fs,
    noise: noise,
    roughness: roughness,
    drive: 2.2,
    attack: 0.006,
    release: dur * 0.35,
    decay: 2.5 / dur,
    jitter: 0.03,
    tilt: 2600 * formantScale,
  );
}

List<PlacedCall> dogVoice(math.Random rng, double len, double intensity) {
  final choice = rng.nextDouble();
  CallSpec b([double k = 1, double d = 0.22]) => _bark(
    rng,
    baseF0: 420 * k,
    formantScale: 1.0,
    dur: d * _r(rng, 0.9, 1.15),
    roughness: _r(rng, 0.3, 0.6),
  );
  if (len > 0.42 || choice < 0.25) {
    // "woof-woof"
    return [PlacedCall(b(), 0), PlacedCall(b(0.93), _r(rng, 0.26, 0.34))];
  } else if (choice < 0.5) {
    // short, higher "arf"
    return [PlacedCall(b(1.18, 0.15), 0)];
  } else if (choice < 0.65) {
    // deeper "ruff"
    return [
      PlacedCall(
        _bark(
          rng,
          baseF0: 300,
          formantScale: 0.86,
          dur: _r(rng, 0.24, 0.3),
          noise: 0.36,
          roughness: 0.7,
        ),
        0,
      ),
    ];
  }
  return [PlacedCall(b(), 0)];
}

List<PlacedCall> puppyVoice(math.Random rng, double len, double intensity) {
  CallSpec yip([double k = 1]) {
    final f = 880 * k * _r(rng, 0.9, 1.12);
    final d = _r(rng, 0.11, 0.17);
    return CallSpec(
      duration: d,
      f0: [
        [0, f * 0.8],
        [0.3, f * 1.25],
        [1, f * 0.85],
      ],
      formants: _scale(const [
        FormantSpec(
          [
            [0, 700],
            [0.3, 1000],
            [1, 650],
          ],
          180,
          1,
        ),
        FormantSpec(
          [
            [0, 1600],
            [1, 1500],
          ],
          260,
          0.7,
        ),
        FormantSpec(
          [
            [0, 3300],
            [1, 3100],
          ],
          400,
          0.3,
        ),
      ], _r(rng, 1.15, 1.3)),
      noise: 0.15,
      roughness: 0.15,
      drive: 1.8,
      attack: 0.005,
      release: d * 0.4,
      decay: 2 / d,
      jitter: 0.02,
      tilt: 4000,
    );
  }

  final c = rng.nextDouble();
  if (len > 0.38 || c < 0.3) {
    final count = 2 + rng.nextInt(2);
    var t = 0.0;
    return [
      for (var i = 0; i < count; i++)
        PlacedCall(yip(1 + i * 0.04), t += (i == 0 ? 0 : _r(rng, 0.15, 0.2))),
    ];
  }
  return [PlacedCall(yip(), 0)];
}

List<PlacedCall> catVoice(math.Random rng, double len, double intensity) {
  final c = rng.nextDouble();
  if (c < 0.22) {
    // tiny "mew"
    final f = _r(rng, 850, 1050);
    final d = _r(rng, 0.22, 0.32);
    return [
      PlacedCall(
        CallSpec(
          duration: d,
          f0: [
            [0, f * 0.9],
            [0.4, f * 1.08],
            [1, f * 0.85],
          ],
          formants: const [
            FormantSpec(
              [
                [0, 450],
                [0.4, 800],
                [1, 500],
              ],
              140,
              1,
            ),
            FormantSpec(
              [
                [0, 2300],
                [0.5, 1900],
                [1, 1500],
              ],
              220,
              0.6,
            ),
            FormantSpec(
              [
                [0, 3600],
                [1, 3400],
              ],
              350,
              0.25,
            ),
          ],
          noise: 0.03,
          attack: 0.03,
          release: 0.08,
          vibratoRate: 6,
          vibratoDepth: 0.012,
          tilt: 3500,
        ),
        0,
      ),
    ];
  }
  final trill = c < 0.4;
  final d =
      (trill ? _r(rng, 0.4, 0.55) : _r(rng, 0.55, 0.9)) *
      (len > 0.4 ? 1.15 : 1);
  final f = _r(rng, 430, 560);
  final peak = _r(rng, 0.28, 0.42);
  return [
    PlacedCall(
      CallSpec(
        duration: d,
        f0: [
          [0, f * 0.85],
          [peak, f * _r(rng, 1.4, 1.6)],
          [0.75, f * 1.05],
          [1, f * 0.8],
        ],
        formants: [
          FormantSpec(
            [
              [0, 350],
              [peak, 980],
              [0.7, 700],
              [1, 420],
            ],
            150,
            1,
          ),
          FormantSpec(
            [
              [0, 2100],
              [peak, 1650],
              [0.75, 1150],
              [1, 900],
            ],
            220,
            0.65,
          ),
          const FormantSpec(
            [
              [0, 3200],
              [1, 2800],
            ],
            320,
            0.28,
          ),
          const FormantSpec(
            [
              [0, 4400],
              [1, 4200],
            ],
            500,
            0.1,
          ),
        ],
        noise: 0.035,
        roughness: trill ? 0 : 0.05,
        attack: 0.045,
        release: d * 0.3,
        vibratoRate: _r(rng, 5, 6.5),
        vibratoDepth: 0.014,
        jitter: 0.012,
        tilt: 3200,
        trillRate: trill ? 26 : 0,
        trillUntil: 0.45,
        ampKeys: const [
          [0, 0.55],
          [0.3, 1],
          [0.8, 0.85],
          [1, 0.6],
        ],
      ),
      0,
    ),
  ];
}

List<PlacedCall> wolfVoice(math.Random rng, double len, double intensity) {
  final d = _r(rng, 1.1, 1.9) * (len > 0.45 ? 1.2 : 1);
  final f = _r(rng, 300, 380);
  final top = f * _r(rng, 1.7, 2.0);
  final twoTone = rng.nextDouble() < 0.3;
  return [
    PlacedCall(
      CallSpec(
        duration: d,
        pure: true,
        f0: twoTone
            ? [
                [0, f],
                [0.2, top],
                [0.45, top * 1.03],
                [0.55, top * 0.82],
                [0.8, top * 0.85],
                [1, f * 1.05],
              ]
            : [
                [0, f],
                [0.25, top],
                [0.7, top * 1.06],
                [1, f * 1.15],
              ],
        formants: const [],
        noise: 0.02,
        attack: 0.18,
        release: d * 0.3,
        vibratoRate: _r(rng, 4, 5.5),
        vibratoDepth: 0.011,
        jitter: 0.006,
        lowpass: const [
          [0, 900],
          [0.25, 2600],
          [1, 1400],
        ],
        gain: 0.8,
      ),
      0,
    ),
  ];
}

List<PlacedCall> duckVoice(math.Random rng, double len, double intensity) {
  CallSpec quack([double k = 1, double g = 1]) {
    final f = _r(rng, 240, 300) * k;
    final d = _r(rng, 0.17, 0.25);
    return CallSpec(
      duration: d,
      f0: [
        [0, f * 1.1],
        [0.2, f * 1.15],
        [1, f * 0.85],
      ],
      formants: [
        FormantSpec(
          [
            [0, 700],
            [0.25, 1150],
            [1, 800],
          ],
          110,
          1,
        ),
        const FormantSpec(
          [
            [0, 1750],
            [1, 1650],
          ],
          140,
          0.9,
        ),
        const FormantSpec(
          [
            [0, 2750],
            [1, 2600],
          ],
          200,
          0.55,
        ),
        const FormantSpec(
          [
            [0, 3900],
            [1, 3700],
          ],
          300,
          0.25,
        ),
      ],
      noise: 0.12,
      roughness: 0.8,
      drive: 3.2,
      attack: 0.008,
      release: d * 0.3,
      decay: 1.6 / d,
      jitter: 0.04,
      tilt: 6000,
      gain: g,
    );
  }

  final c = rng.nextDouble();
  if (len > 0.4 || c < 0.4) {
    final count = len > 0.55 ? 3 : 2;
    return [
      for (var i = 0; i < count; i++)
        PlacedCall(quack(1 - i * 0.05, 1 - i * 0.15), i * _r(rng, 0.2, 0.26)),
    ];
  }
  return [PlacedCall(quack(), 0)];
}

List<PlacedCall> cowVoice(math.Random rng, double len, double intensity) {
  final d = _r(rng, 0.9, 1.5) * (len > 0.45 ? 1.15 : 1);
  final f = _r(rng, 110, 145);
  final open = _r(rng, 0.25, 0.4);
  return [
    PlacedCall(
      CallSpec(
        duration: d,
        f0: [
          [0, f * 0.9],
          [open, f * 1.22],
          [0.8, f * 1.0],
          [1, f * 0.8],
        ],
        formants: [
          FormantSpec(
            [
              [0, 280],
              [open, 560],
              [0.8, 480],
              [1, 320],
            ],
            110,
            1,
          ),
          FormantSpec(
            [
              [0, 700],
              [open, 1000],
              [1, 760],
            ],
            150,
            0.6,
          ),
          const FormantSpec(
            [
              [0, 2300],
              [1, 2200],
            ],
            260,
            0.15,
          ),
        ],
        noise: 0.05,
        roughness: 0.12,
        drive: 1.6,
        attack: 0.12,
        release: d * 0.28,
        vibratoRate: _r(rng, 3.5, 5),
        vibratoDepth: 0.012,
        jitter: 0.015,
        tilt: 900,
        lowpass: [
          [0, 500],
          [open, 2600],
          [1, 1000],
        ],
        ampKeys: const [
          [0, 0.5],
          [0.3, 1],
          [1, 0.75],
        ],
      ),
      0,
    ),
  ];
}

/// Per-voice timing & mixing info.
class VoiceProfile {
  const VoiceProfile(this.builder, this.reverb, this.minGap, this.maxWords);

  final VoiceBuilder builder;
  final double reverb;
  final double minGap;
  final int maxWords;
}

const Map<String, VoiceProfile> voiceProfiles = {
  'dog': VoiceProfile(dogVoice, 0.16, 0.12, 14),
  'puppy': VoiceProfile(puppyVoice, 0.14, 0.1, 16),
  'cat': VoiceProfile(catVoice, 0.14, 0.15, 7),
  'wolf': VoiceProfile(wolfVoice, 0.38, 0.25, 3),
  'duck': VoiceProfile(duckVoice, 0.12, 0.12, 12),
  'cow': VoiceProfile(cowVoice, 0.2, 0.25, 4),
};
