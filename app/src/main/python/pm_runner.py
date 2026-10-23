"""Executes user code for the PyMaster terminal.

Output is streamed to a Java/Kotlin bridge object which exposes
``onOutput(text, isError)`` and ``readInput(prompt)``.
"""

import builtins
import linecache
import os
import sys
import time
import traceback

_stop_requested = False


class _Stream:
    """File-like object that batches writes before handing them to Kotlin."""

    def __init__(self, bridge, is_error):
        self._bridge = bridge
        self._is_error = is_error
        self._buffer = []
        self._size = 0
        self._last_flush = time.monotonic()

    def write(self, text):
        if not isinstance(text, str):
            text = str(text)
        if not text:
            return 0
        self._buffer.append(text)
        self._size += len(text)
        now = time.monotonic()
        if self._size >= 2048 or now - self._last_flush >= 0.05:
            self.flush()
        return len(text)

    def flush(self):
        if self._buffer:
            data = "".join(self._buffer)
            self._buffer = []
            self._size = 0
            self._bridge.onOutput(data, self._is_error)
        self._last_flush = time.monotonic()

    def isatty(self):
        return False

    def writable(self):
        return True

    @property
    def encoding(self):
        return "utf-8"


def request_stop():
    global _stop_requested
    _stop_requested = True


def _tracer(frame, event, arg):
    if _stop_requested:
        raise KeyboardInterrupt
    return _tracer


def run(code, bridge, workdir=None):
    """Run ``code`` as ``__main__``. Returns an exit code.

    ``workdir`` (if given) becomes the current directory so programs can
    create and read files safely inside the app's private storage.
    """
    global _stop_requested
    _stop_requested = False
    if workdir:
        os.makedirs(workdir, exist_ok=True)
        os.chdir(workdir)

    out = _Stream(bridge, False)
    err = _Stream(bridge, True)
    saved = (sys.stdout, sys.stderr, builtins.input)

    def _input(prompt=""):
        out.write(str(prompt))
        out.flush()
        err.flush()
        line = bridge.readInput(str(prompt))
        if line is None or _stop_requested:
            raise KeyboardInterrupt
        return str(line)

    sys.stdout, sys.stderr = out, err
    builtins.input = _input
    namespace = {"__name__": "__main__", "__builtins__": builtins}
    exit_code = 0
    try:
        lines = code.splitlines(True)
        linecache.cache["main.py"] = (len(code), None, lines, "main.py")
        compiled = compile(code, "main.py", "exec")
        sys.settrace(_tracer)
        try:
            exec(compiled, namespace)
        finally:
            sys.settrace(None)
    except KeyboardInterrupt:
        out.flush()
        err.write("\nKeyboardInterrupt: program stopped\n")
        exit_code = 130
    except SystemExit as e:
        if isinstance(e.code, int):
            exit_code = e.code
        elif e.code is not None:
            err.write(str(e.code) + "\n")
            exit_code = 1
    except BaseException:
        out.flush()
        etype, value, tb = sys.exc_info()
        # Hide this runner's own frame from the traceback.
        if tb is not None and tb.tb_next is not None:
            tb = tb.tb_next
        elif etype is not None and issubclass(etype, SyntaxError):
            tb = None
        err.write("".join(traceback.format_exception(etype, value, tb)))
        exit_code = 1
    finally:
        out.flush()
        err.flush()
        sys.stdout, sys.stderr, builtins.input = saved
    return exit_code


def version():
    return sys.version.split()[0]
