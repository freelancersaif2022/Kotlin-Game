extends Node
## Autoload "Sfx": pooled one-shot sound effects plus looping music/ambience.

const SOUNDS := {
	"swing": "res://assets/audio/swing.wav",
	"hit": "res://assets/audio/hit.wav",
	"bone_break": "res://assets/audio/bone_break.wav",
	"hurt": "res://assets/audio/hurt.wav",
	"heal": "res://assets/audio/heal.wav",
	"magic": "res://assets/audio/magic.wav",
	"jump": "res://assets/audio/jump.wav",
	"wave_horn": "res://assets/audio/wave_horn.wav",
	"game_over": "res://assets/audio/game_over.wav",
	"victory": "res://assets/audio/victory.wav",
	"click": "res://assets/audio/click.wav",
}
const POOL_SIZE := 12

var _streams := {}
var _pool: Array[AudioStreamPlayer] = []
var _next := 0
var _music: AudioStreamPlayer
var _ambience: AudioStreamPlayer


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	for key in SOUNDS:
		_streams[key] = load(SOUNDS[key])
	for i in POOL_SIZE:
		var p := AudioStreamPlayer.new()
		add_child(p)
		_pool.append(p)
	_music = _make_loop("res://assets/audio/music.wav", -14.0)
	_ambience = _make_loop("res://assets/audio/ambience.wav", -8.0)


func _make_loop(path: String, volume_db: float) -> AudioStreamPlayer:
	var stream: AudioStreamWAV = load(path).duplicate()
	stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
	stream.loop_begin = 0
	stream.loop_end = int(stream.get_length() * stream.mix_rate)
	var p := AudioStreamPlayer.new()
	p.stream = stream
	p.volume_db = volume_db
	add_child(p)
	return p


func play(key: String, volume_db := 0.0, pitch_variance := 0.1) -> void:
	var p := _pool[_next]
	_next = (_next + 1) % POOL_SIZE
	p.stream = _streams[key]
	p.volume_db = volume_db
	p.pitch_scale = 1.0 + randf_range(-pitch_variance, pitch_variance)
	p.play()


func start_loops() -> void:
	if not _music.playing:
		_music.play()
	if not _ambience.playing:
		_ambience.play()
