"""Tiny adb + uiautomator driver for the CI smoke test."""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = sys.argv[1] if len(sys.argv) > 1 else "shots"
step = [0]


def adb(*args, check=False):
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check).stdout


def dump():
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("shell", "cat", "/sdcard/ui.xml")
        if "<hierarchy" in xml:
            return ET.fromstring(xml[xml.index("<hierarchy"):])
        time.sleep(1)
    return None


def find(text, exact=True):
    root = dump()
    if root is None:
        return None
    for n in root.iter("node"):
        for attr in ("text", "content-desc"):
            v = n.get(attr, "")
            if (v == text) if exact else (text.lower() in v.lower()):
                b = re.findall(r"\d+", n.get("bounds", ""))
                if len(b) == 4:
                    x1, y1, x2, y2 = map(int, b)
                    if x2 > x1 and y2 > y1:
                        return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def wait(text, timeout=15, exact=True):
    end = time.time() + timeout
    while time.time() < end:
        p = find(text, exact)
        if p:
            return p
        time.sleep(1)
    print(f"!! not found: {text}")
    return None


def tap(text, timeout=15, exact=True, settle=1.2):
    p = wait(text, timeout, exact)
    if p:
        adb("shell", "input", "tap", str(p[0]), str(p[1]))
        print(f"tap {text} @ {p}")
        time.sleep(settle)
        return True
    return False


def tap_xy(x, y, settle=1.0):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(settle)


def type_text(s):
    adb("shell", "input", "text", s.replace(" ", "%s").replace("@", "\\@"))
    time.sleep(0.6)


def back(settle=1.2):
    adb("shell", "input", "keyevent", "4")
    time.sleep(settle)


def swipe_up():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "300")
    time.sleep(1)


def shot(name):
    step[0] += 1
    fn = f"{OUT}/{step[0]:02d}_{name}.png"
    with open(fn, "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print("shot", fn)


def digits(code):
    for d in code:
        tap(d, timeout=5, settle=0.35)


def main():
    import os
    os.makedirs(OUT, exist_ok=True)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", "com.zyropay.wallet/com.zyropay.app.MainActivity")
    time.sleep(1.2)
    shot("splash")
    wait("Skip", 20)
    shot("onboarding_1")
    tap("Next")
    shot("onboarding_2")
    tap("Skip")
    shot("welcome")
    tap("Create account")
    shot("signup")
    back()
    tap("I already have an account")
    tap("name@example.com", exact=False)
    type_text("alex.morgan@gmail.com")
    tap("Enter password")
    type_text("Demo@1234")
    shot("login_filled")
    adb("shell", "input", "keyevent", "111")
    time.sleep(0.8)
    tap("Log in")
    wait("Check your email", 15)
    shot("otp")
    tap("Autofill", 12, settle=3)
    wait("Create passcode", 15)
    shot("passcode")
    digits("135792")
    time.sleep(1)
    digits("135792")
    time.sleep(2.5)
    if find("Not now") is None and find("Maybe later"):
        shot("biometric")
        tap("Maybe later")
    shot("notification_permission")
    tap("Not now", 10, settle=3)
    shot("home")
    swipe_up()
    shot("home_scrolled")
    swipe_up()
    shot("home_bottom")
    tap("Card", settle=2)
    shot("card_tab")
    tap("Details", settle=2)
    digits("135792")
    time.sleep(2)
    shot("card_details")
    back()
    tap("Wallet", settle=2)
    shot("wallet_tab")
    tap("BTC", settle=2)
    shot("coin_detail")
    back()
    tap("Me", settle=2)
    shot("me_tab")
    tap("Dark mode", settle=2)
    shot("me_dark")
    tap("Home", settle=2)
    shot("home_dark")
    tap("Deposit", settle=2)
    shot("select_coin_dark")
    tap("USDT", settle=2)
    shot("deposit_networks")
    tap("Tron (TRC20)", settle=2)
    shot("deposit_address")
    back(); back()
    tap("Swap", settle=2)
    shot("swap")
    back()
    p = wait("Pay", 8)
    if p:
        tap_xy(p[0], p[1] - 70, settle=4)
    shot("scanner")
    time.sleep(3)
    shot("pay_merchant")
    for d in "25":
        tap(d, timeout=4, settle=0.3)
    tap("Pay $25.00", 6, settle=1)
    digits("135792")
    time.sleep(4)
    shot("payment_result")
    tap("Done", 6, settle=2)
    tap("Me", settle=2)
    tap("Dark mode", settle=2)
    tap("Security center", settle=2)
    shot("security")
    back()
    swipe_up()
    tap("Help center", settle=2)
    shot("help_center")
    back()
    tap("Live chat", settle=2)
    tap("Type a message…", timeout=6)
    type_text("My card is lost")
    adb("shell", "input", "keyevent", "66")
    time.sleep(5)
    shot("support_chat")
    back()
    tap("Home", settle=2)
    tap("View all", settle=2)
    shot("transactions")
    back()
    adb("shell", "input", "keyevent", "3")
    time.sleep(1)
    crash = adb("logcat", "-d", "-s", "AndroidRuntime:E")
    open(f"{OUT}/logcat_crash.txt", "w").write(crash)
    if "FATAL EXCEPTION" in crash:
        print(crash)
        sys.exit(1)
    print("SMOKE TEST PASSED")


main()
