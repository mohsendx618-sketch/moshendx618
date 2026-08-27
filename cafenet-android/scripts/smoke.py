#!/usr/bin/env python3
"""Functional UI smoke test on a disposable Android emulator, with network off."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

REPORT = Path("cafenet-android/app/build/reports/smoke")
REPORT.mkdir(parents=True, exist_ok=True)
PACKAGE = "ir.cafenet.hamyar"
TITLE = "دریافت گواهی کد پستی"
checks = []

def adb(*args, binary=False):
    r = subprocess.run(["adb", *args], check=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, timeout=45)
    return r.stdout if binary else r.stdout.decode("utf-8", errors="replace")

def dump(name="latest"):
    for attempt in range(4):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/cafenet-ui.xml")
            raw = adb("exec-out", "cat", "/sdcard/cafenet-ui.xml")
            tree = ET.fromstring(raw[raw.index("<?xml"):])
            (REPORT / f"{name}.xml").write_text(raw, encoding="utf-8")
            return tree
        except (ValueError, ET.ParseError, subprocess.CalledProcessError):
            if attempt == 3:
                raise
            time.sleep(0.5)

def nodes():
    return list(dump().iter("node"))

def locate(text=None, resource=None, description=None, contains=None, class_name=None):
    for node in nodes():
        if text is not None and node.get("text") != text:
            continue
        if resource is not None and node.get("resource-id") != f"{PACKAGE}:id/{resource}":
            continue
        if description is not None and node.get("content-desc") != description:
            continue
        if contains is not None and contains not in node.get("text", ""):
            continue
        if class_name is not None and node.get("class") != class_name:
            continue
        bounds = list(map(int, re.findall(r"-?\d+", node.get("bounds", ""))))
        if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
            return node
    return None

def tap(scroll=False, **query):
    node = locate(**query)
    for _ in range(5 if scroll else 0):
        if node is not None:
            break
        size = adb("shell", "wm", "size")
        w, h = map(int, re.findall(r"(\d+)x(\d+)", size)[-1])
        adb("shell", "input", "swipe", str(w // 2), str(int(h * .84)),
            str(w // 2), str(int(h * .48)), "350")
        node = locate(**query)
    assert node is not None, f"Could not locate {query}"
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.25)
    return node

def expect(text):
    assert any(text in n.get("text", "") or text in n.get("content-desc", "")
               for n in nodes()), f"Expected text not visible: {text}"

def shot(name):
    (REPORT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    dump(name)

def launch():
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")

def home():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    assert locate(resource="search_input") is not None, "Back did not return to home"

try:
    adb("wait-for-device")
    adb("install", "-r", "dist/Hamyar-CafeNet-1.0.apk")
    adb("logcat", "-c")
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    assert adb("shell", "settings", "get", "global", "airplane_mode_on").strip() == "1"
    (REPORT / "network-state.txt").write_text(
        adb("shell", "cmd", "connectivity", "airplane-mode") + "\n" +
        adb("shell", "dumpsys", "connectivity"), encoding="utf-8")
    checks.append("Airplane mode, Wi-Fi and mobile data off before first launch")
    launch()
    expect("همیار کافی‌نت")
    expect("۲۴ راهنما")
    expect(TITLE)
    shot("01-home-offline")
    checks.append("24 packaged guides loaded offline")

    tap(resource="search_input")
    adb("shell", "input", "text", "postal")
    adb("shell", "input", "keyevent", "KEYCODE_ENTER")
    expect("۱ راهنما")
    expect(TITLE)
    shot("02-search-offline")
    checks.append("Offline title/alias search returned the postal certificate")
    tap(description="باز کردن «" + TITLE + "»")
    assert locate(resource="guide_title") is not None
    expect("مراحل")
    shot("03-guide-offline")

    tap(resource="favorite_button")
    expect("★ نشان‌شده")
    tap(text="گیر کردم")
    expect("کدپستی را ندارم")
    shot("04-troubleshooting-offline")
    checks.append("Offline troubleshooting rendered")
    tap(text="منابع")
    expect("حدود اعتبار این آموزش")
    expect("۵ شهریور ۱۴۰۵")
    checks.append("Source review date and caveats visible")
    tap(text="قبل شروع")
    expect("خروجی این کار")
    tap(text="باز کردن سایت مربوط", scroll=True)
    expect("باز شدن مرورگر؛ نیازمند اینترنت")
    expect("https://gnaf.post.ir")
    tap(text="برگشت")
    checks.append("External link requires explicit confirmation")
    tap(text="مراحل")
    tap(text="ادامه از تیک‌نخورده", scroll=True)
    tap(class_name="android.widget.CheckBox", scroll=True)
    assert any(n.get("class") == "android.widget.CheckBox" and n.get("checked") == "true"
               for n in nodes()), "Checklist tick not reflected in UI"
    shot("05-checklist-offline")
    checks.append("Checklist tick stored")

    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap(text="نشان‌شده")
    expect(TITLE)
    expect("۱ راهنما")
    tap(description="باز کردن «" + TITLE + "»")
    expect("۱ از ۱۴ مرحله")
    checks.append("Favorites and checklist survived a process restart")
    tap(text="مشتری جدید", scroll=True)
    expect("شروع دوباره این راهنما؟")
    tap(text="پاک کردن تیک‌ها")
    expect("۰ از ۱۴ مرحله")
    checks.append("New-customer action reset only this guide's ticks")
    tap(description="تغییر اندازه نوشته")
    expect(TITLE)
    shot("06-large-text")
    home()
    tap(text="همه")
    tap(resource="search_input")
    adb("shell", "input", "text", "zzznomatch123")
    adb("shell", "input", "keyevent", "KEYCODE_ENTER")
    expect("۰ راهنما")
    expect("اینجا چیزی پیدا نشد")
    tap(text="نمایش همه راهنماها")
    expect("۲۴ راهنما")
    checks.append("No-results state and reset-to-all work")
    tap(text="پست و نشانی")
    expect("۴ راهنما")
    checks.append("Category filter returned four postal guides")
    shot("07-category-offline")
    log = adb("logcat", "-d", "-b", "crash")
    (REPORT / "crash.log").write_text(log, encoding="utf-8")
    assert "Process: " + PACKAGE not in log, "Application crash recorded"
    checks.append("No application crash recorded")
    (REPORT / "result.json").write_text(json.dumps({
        "result":"passed", "device":"Android 16 / API 36", "checks":checks,
        "live_portal_transactions":False
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"result":"passed", "checks":checks}, ensure_ascii=False, indent=2))
except Exception as error:
    try:
        shot("failure")
        (REPORT / "crash.log").write_text(adb("logcat", "-d", "-b", "crash"), encoding="utf-8")
    except Exception:
        pass
    (REPORT / "result.json").write_text(json.dumps({
        "result":"failed", "completed_checks":checks, "error":str(error)
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    raise
