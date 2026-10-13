#!/usr/bin/env bash
set -uo pipefail
gradle --no-daemon connectedDebugAndroidTest
test_result=$?
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
