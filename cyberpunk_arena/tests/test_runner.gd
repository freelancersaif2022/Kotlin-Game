extends SceneTree
## Entry point for the automated gameplay tests.
## The test node is loaded at runtime (after autoloads such as GameInput are
## registered) so scripts that use the autoload compile correctly.
##
## Usage (rendering needs a display, e.g. Xvfb):
##   xvfb-run -s "-screen 0 1600x720x24" godot --path . --resolution 1600x720 \
##       --fixed-fps 60 -s res://tests/test_runner.gd -- [screenshot_dir]
## Exit code 0 = all checks passed.

func _initialize() -> void:
	root.add_child.call_deferred(load("res://tests/gameplay_tests.gd").new())
