#!/usr/bin/env bash
set -uo pipefail
bash ci/piper-offline-test.sh
piper_result=$?
if [ "$piper_result" -ne 0 ]; then exit "$piper_result"; fi
bash ci/polish-offline-test.sh || exit $?
# Heavy native model tests can leave the emulator launcher with an ANR dialog.
# Stop only the unrelated launcher; never dismiss errors from either tested app.
adb shell am force-stop com.google.android.apps.nexuslauncher
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
gradle --no-daemon :app:connectedDebugAndroidTest
test_result=$?
if [ "$piper_result" -ne 0 ]; then test_result=$piper_result; fi
mkdir -p app/build/reports/display-matrix/baseline
cp -R app/build/reports/androidTests app/build/reports/display-matrix/baseline/ 2>/dev/null || true
# Device-sized windows, including Android accessibility font scaling. Always restore settings.
trap 'adb shell wm size reset; adb shell wm density reset; adb shell settings put system font_scale 1.0' EXIT
for configuration in '1080x2520 480 0.8' '1080x2520 480 1.0' '1080x2520 480 2.0' '1968x2184 420 0.8' '1968x2184 420 1.0' '1968x2184 420 2.0'; do
  read -r display_size display_density font_scale <<< "$configuration"
  adb shell wm size "$display_size"
  adb shell wm density "$display_density"
  adb shell settings put system font_scale "$font_scale"
  sleep 2
  gradle --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.sprechbruecke.ResponsiveLayoutTest --rerun-tasks
  result=$?
  report_dir="app/build/reports/display-matrix/${display_size}-${display_density}-font-${font_scale}"
  mkdir -p "$report_dir"
  cp -R app/build/reports/androidTests "$report_dir/" 2>/dev/null || true
  if [ "$result" -ne 0 ]; then test_result=$result; fi
done
mkdir -p app/build/reports/screenshots
adb pull /data/local/tmp/sprechbruecke-screenshots/. app/build/reports/screenshots || true
python3 - <<'PY'
from pathlib import Path
import base64
import xml.etree.ElementTree as ET
for p in Path('app/build/outputs/androidTest-results').rglob('*.xml'):
    try:
        root = ET.parse(p).getroot()
        for failure in root.iter('failure'):
            print('UI_TEST_FAILURE', failure.attrib, failure.text)
    except ET.ParseError:
        pass
for p in sorted(Path('app/build/reports/screenshots').glob('*.png')):
    if not p.is_file():
        continue
    data = base64.b64encode(p.read_bytes()).decode('ascii')
    for start in range(0, len(data), 4000):
        print('UI_SCREENSHOT:' + p.stem + ':' + data[start:start+4000])
PY
exit "$test_result"
