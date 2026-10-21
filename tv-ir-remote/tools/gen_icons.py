#!/usr/bin/env python3
"""Generates res/drawable/ic_*.xml vector icons from Material Design Icons (@mdi/js, Apache 2.0).

    npm pack @mdi/js@7.4.47 && tar xzf mdi-js-7.4.47.tgz
    python3 tools/gen_icons.py package/mdi.js app/src/main/res/drawable
"""
import os
import re
import sys

ICONS = """Power VolumePlus VolumeMinus VolumeOff ChevronUp ChevronDown ChevronLeft ChevronRight Home
ArrowLeft Menu InformationOutline VideoInputHdmi Cog DotsHorizontal TelevisionGuide Play Pause PlayPause
Stop Rewind FastForward SkipPrevious SkipNext Record Magnify Plus Check Close Television RemoteTv
PowerSleep ClosedCaption AspectRatio CardTextOutline FormatListBulleted Star Translate ImageOutline
Speaker Apps ExitToApp DeleteOutline PencilOutline History Netflix Youtube CheckCircle AlertCircleOutline
Vibrate DotsVertical MagnifyScan AccessPoint LightbulbOutline Minus Refresh ArrowRight""".split()

src, out = sys.argv[1], sys.argv[2]
js = open(src).read()
os.makedirs(out, exist_ok=True)
for name in ICONS:
    path = re.search(r'export var mdi%s = "([^"]+)"' % name, js).group(1)
    snake = re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()
    with open(os.path.join(out, "ic_%s.xml" % snake), "w") as f:
        f.write('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                '    android:width="24dp" android:height="24dp"\n'
                '    android:viewportWidth="24" android:viewportHeight="24">\n'
                '    <path android:fillColor="#FFFFFFFF" android:pathData="%s"/>\n'
                '</vector>\n' % path)
print("icons:", len(ICONS))
