#!/usr/bin/env bash
set -euo pipefail
report_dir="tarhplus-android/app/build/reports/smoke"
mkdir -p "$report_dir"
adb wait-for-device
adb install -r dist/Tarhplus-Abadeh-1.0.apk
adb shell am start -W -n ir.tarhplus.watch/.MainActivity
adb shell uiautomator dump /sdcard/tarhplus-screen.xml
adb pull /sdcard/tarhplus-screen.xml "$report_dir/screen.xml"
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
p = Path('tarhplus-android/app/build/reports/smoke/screen.xml')
root = ET.parse(p).getroot()
texts = [node.get('text', '') for node in root.iter('node')]
assert any('پایش طرح' in text for text in texts), 'App home screen did not appear'
assert any('هنوز بررسی نشده' in text for text in texts), 'Initial state was not rendered'
assert any('اتاق عمل' in text and 'آباده' in text for text in texts), 'Wrong monitoring target'
print('Android 16 launch smoke test passed')
PY
adb shell screencap -p /sdcard/tarhplus-home.png
adb pull /sdcard/tarhplus-home.png "$report_dir/home.png"
adb logcat -d -b crash > "$report_dir/crash.log"
python3 - "$report_dir/crash.log" <<'PY'
from pathlib import Path
import sys
log = Path(sys.argv[1]).read_text()
if 'Process: ir.tarhplus.watch' in log:
    raise SystemExit('Application crash was recorded during the launch test')
print('Application crash log check passed')
PY
