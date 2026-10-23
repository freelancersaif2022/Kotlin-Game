"""Builds course.json, projects.json and quiz_bank.json for the app.

Usage:  python3.11 content-src/build.py
"""

import json
import os
import shutil
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "app", "src", "main", "python"))

import pm_runner  # noqa: E402  (the same runner the app uses)
from course_a import MODULES_A  # noqa: E402
from course_b import MODULES_B  # noqa: E402
from course_c import MODULES_C  # noqa: E402
from projects import PROJECTS  # noqa: E402
from quiz_bank import QUESTIONS  # noqa: E402


class Bridge:
    def __init__(self, stdin):
        self.stdin = list(stdin)
        self.out = []

    def onOutput(self, text, is_error):
        self.out.append(text)

    def readInput(self, prompt):
        if not self.stdin:
            raise RuntimeError("example needs more stdin values")
        value = self.stdin.pop(0)
        self.out.append(value + "\n")
        return value


def run(code, stdin, where):
    workdir = tempfile.mkdtemp(prefix="pym_")
    cwd = os.getcwd()
    os.chdir(workdir)
    try:
        bridge = Bridge(stdin)
        exit_code = pm_runner.run(code, bridge)
    finally:
        os.chdir(cwd)
        shutil.rmtree(workdir, ignore_errors=True)
    out = "".join(bridge.out)
    if exit_code not in (0, 1):
        raise SystemExit(f"{where}: exit code {exit_code}\n{out}")
    if bridge.stdin:
        raise SystemExit(f"{where}: unused stdin {bridge.stdin}")
    if len(out) > 4000:
        out = out[:4000] + "\n...\n"
    return out, exit_code


def main():
    modules = MODULES_A + MODULES_B + MODULES_C
    seen = set()
    errors_expected = 0
    for m in modules:
        assert len(m["quiz"]) >= 5, m["id"]
        for q in m["quiz"]:
            assert 0 <= q["answer"] < len(q["options"]), q
        for lesson in m["lessons"]:
            assert lesson["id"] not in seen, lesson["id"]
            seen.add(lesson["id"])
            for i, b in enumerate(lesson["blocks"]):
                stdin = b.pop("_stdin", None)
                if b["type"] != "code" or "output" in b:
                    continue
                out, code = run(b["code"], stdin or [], f"{lesson['id']} block {i}")
                if code == 1:
                    errors_expected += 1
                b["output"] = out
    for p in PROJECTS:
        stdin = p.pop("_stdin")
        out, code = run(p["code"], stdin, p["id"])
        if code != 0:
            raise SystemExit(f"project {p['id']} failed:\n{out}")
        p["sampleOutput"] = out
        for s in p["steps"]:
            if s["code"]:
                compile(s["code"], p["id"], "exec")
    for q in QUESTIONS:
        assert 0 <= q["answer"] < len(q["options"]), q

    os.makedirs(ASSETS, exist_ok=True)

    def dump(name, data):
        with open(os.path.join(ASSETS, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    dump("course.json", {"modules": modules})
    dump("projects.json", {"projects": PROJECTS})
    dump("quiz_bank.json", {"questions": QUESTIONS})
    lessons = sum(len(m["lessons"]) for m in modules)
    print(f"modules={len(modules)} lessons={lessons} module_quiz={sum(len(m['quiz']) for m in modules)} "
          f"projects={len(PROJECTS)} bank={len(QUESTIONS)} error_examples={errors_expected}")


if __name__ == "__main__":
    main()
