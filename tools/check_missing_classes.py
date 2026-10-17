#!/usr/bin/env python3
"""Lists classes referenced by the APK's code that exist neither in the APK nor in android.jar.
Such references crash with NoClassDefFoundError when the code runs.
Usage: python3 -I tools/check_missing_classes.py <classes-dir> <android.jar> [prefix-to-report...]"""
import os, struct, sys, zipfile

def class_refs(data):
    if data[:4] != b"\xca\xfe\xba\xbe":
        return set(), None
    count = struct.unpack(">H", data[8:10])[0]
    i, idx = 10, 1
    utf8, classes = {}, []
    while idx < count:
        tag = data[i]
        if tag == 1:
            ln = struct.unpack(">H", data[i + 1:i + 3])[0]
            utf8[idx] = data[i + 3:i + 3 + ln].decode("utf-8", "replace"); i += 3 + ln
        elif tag == 7:
            classes.append(struct.unpack(">H", data[i + 1:i + 3])[0]); i += 3
        elif tag in (8, 16, 19, 20): i += 3
        elif tag == 15: i += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): i += 5
        elif tag in (5, 6): i += 9; idx += 1
        else: raise ValueError("bad constant pool tag %d" % tag)
        idx += 1
    this_idx = struct.unpack(">H", data[i + 2:i + 4])[0]
    names = set()
    for c in classes:
        n = utf8.get(c, "")
        while n.startswith("["): n = n[1:]
        if n.startswith("L") and n.endswith(";"): n = n[1:-1]
        if n and len(n) > 1: names.add(n)
    return names, utf8.get([c for c in classes][0]) if False else None

def main():
    cdir, ajar = sys.argv[1], sys.argv[2]
    prefixes = sys.argv[3:]
    defined, refs = set(), {}
    for root, _, files in os.walk(cdir):
        for f in files:
            if f.endswith(".class"):
                p = os.path.join(root, f)
                name = os.path.relpath(p, cdir)[:-6]
                defined.add(name)
                r, _ = class_refs(open(p, "rb").read())
                for x in r: refs.setdefault(x, set()).add(name)
    with zipfile.ZipFile(ajar) as z:
        platform = {n[:-6] for n in z.namelist() if n.endswith(".class")}
    missing = {k: v for k, v in refs.items() if k not in defined and k not in platform}
    shown = 0
    for k in sorted(missing):
        users = sorted(missing[k])
        if prefixes and not any(u.startswith(tuple(prefixes)) for u in users):
            continue
        shown += 1
        print(f"{k}  <- {', '.join(users[:3])}{' ...' if len(users) > 3 else ''}")
    print(f"missing classes referenced: {len(missing)} total, {shown} shown")

main()
