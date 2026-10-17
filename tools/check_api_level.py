#!/usr/bin/env python3
"""Reports platform methods/fields the app calls that do not exist in an older android.jar.
Calls listed here crash with NoSuchMethodError/NoClassDefFoundError on that Android version unless
the caller guards them with Build.VERSION.SDK_INT (review each listed caller).
Usage: python3 -I tools/check_api_level.py <classes-dir> <android-min.jar> <android-target.jar>"""
import os, struct, sys, zipfile

def parse(data):
    if data[:4] != b"\xca\xfe\xba\xbe":
        return None
    count = struct.unpack(">H", data[8:10])[0]
    cp = [None] * count
    i, idx = 10, 1
    while idx < count:
        tag = data[i]
        if tag == 1:
            ln = struct.unpack(">H", data[i + 1:i + 3])[0]
            cp[idx] = ("utf8", data[i + 3:i + 3 + ln].decode("utf-8", "replace")); i += 3 + ln
        elif tag == 7: cp[idx] = ("class", struct.unpack(">H", data[i + 1:i + 3])[0]); i += 3
        elif tag in (9, 10, 11):
            cp[idx] = ({9: "field", 10: "method", 11: "imethod"}[tag], struct.unpack(">H", data[i + 1:i + 3])[0], struct.unpack(">H", data[i + 3:i + 5])[0]); i += 5
        elif tag == 12: cp[idx] = ("nat", struct.unpack(">H", data[i + 1:i + 3])[0], struct.unpack(">H", data[i + 3:i + 5])[0]); i += 5
        elif tag in (8, 16, 19, 20): i += 3
        elif tag == 15: i += 4
        elif tag in (3, 4, 17, 18): i += 5
        elif tag in (5, 6): i += 9; idx += 1
        else: raise ValueError(tag)
        idx += 1
    u = lambda k: cp[k][1]
    cname = lambda k: u(cp[k][1])
    access, this_c, super_c = struct.unpack(">HHH", data[i:i + 6]); i += 6
    nint = struct.unpack(">H", data[i:i + 2])[0]; i += 2
    ifaces = [cname(struct.unpack(">H", data[i + 2 * k:i + 2 * k + 2])[0]) for k in range(nint)]; i += 2 * nint
    members = set()
    for kind in ("f", "m"):
        n = struct.unpack(">H", data[i:i + 2])[0]; i += 2
        for _ in range(n):
            _, ni, di, ac = struct.unpack(">HHHH", data[i:i + 8]); i += 8
            members.add((kind, u(ni), u(di)))
            for _ in range(ac):
                ln = struct.unpack(">I", data[i + 2:i + 6])[0]; i += 6 + ln
    refs = []
    for e in cp:
        if e and e[0] in ("field", "method", "imethod"):
            owner = cname(e[1])
            nat = cp[e[2]]
            refs.append(("f" if e[0] == "field" else "m", owner, u(nat[1]), u(nat[2])))
    return {"name": cname(this_c), "super": cname(super_c) if super_c else None, "ifaces": ifaces, "members": members, "refs": refs}

def load_jar(path):
    out = {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if n.endswith(".class"):
                c = parse(z.read(n))
                if c: out[c["name"]] = c
    return out

def has(platform, owner, kind, name, desc, seen=None):
    c = platform.get(owner)
    if c is None: return False
    if (kind, name, desc) in c["members"]: return True
    if kind == "f" and any(m[0] == "f" and m[1] == name for m in c["members"]): return True
    seen = seen or set()
    for p in [c["super"]] + c["ifaces"]:
        if p and p not in seen:
            seen.add(p)
            if has(platform, p, kind, name, desc, seen): return True
    return False

def main():
    cdir, minjar, tgtjar = sys.argv[1:4]
    old, new = load_jar(minjar), load_jar(tgtjar)
    problems = {}
    for root, _, files in os.walk(cdir):
        for f in files:
            if not f.endswith(".class"): continue
            c = parse(open(os.path.join(root, f), "rb").read())
            for kind, owner, name, desc in c["refs"]:
                if owner.startswith("["): continue
                if owner not in new: continue          # app class, not platform
                if has(old, owner, kind, name, desc): continue
                if not has(new, owner, kind, name, desc):
                    continue                            # call through an app subclass etc.
                problems.setdefault(f"{owner}.{name}{desc if kind == 'm' else ''}", set()).add(c["name"])
    for k in sorted(problems):
        users = sorted(problems[k])
        print(f"{k}\n    <- {', '.join(users[:4])}{' ...' if len(users) > 4 else ''}")
    print(f"members newer than {os.path.basename(minjar)}: {len(problems)}")

main()
