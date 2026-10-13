#!/usr/bin/env bash
set -euo pipefail
mkdir -p .voice-models app/build/reports/piper
python3 - <<'PY'
from pathlib import Path
import re, hashlib, subprocess
source=Path('voices/src/main/java/de/sprechbruecke/voices/PiperVoice.java').read_text()
for model,size,digest in re.findall(r'"((?:pl_PL|de_DE)-[^"]+)", (\d+)L,\s*"([a-f0-9]{64})"',source):
    filename='vits-piper-'+model+'.tar.bz2'
    path=Path('.voice-models')/filename
    def valid():
        return path.exists() and path.stat().st_size==int(size) and hashlib.file_digest(path.open('rb'),'sha256').hexdigest()==digest
    if not valid():
        subprocess.run(['curl','--fail','--location','--retry','3','--output',str(path),'https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/'+filename],check=True)
    assert valid(),filename+' checksum mismatch'
    print('Verified model:',filename)
PY
gradle --no-daemon :voices:installDebug :voices:installDebugAndroidTest
adb shell run-as de.sprechbruecke.voices mkdir -p cache
for archive in .voice-models/*.tar.bz2; do
  name=$(basename "$archive")
  adb shell "run-as de.sprechbruecke.voices sh -c 'cat > cache/$name'" < "$archive"
done
trap 'adb logcat -d > app/build/reports/piper/logcat.txt; tail -n 250 app/build/reports/piper/logcat.txt; adb shell svc wifi enable; adb shell svc data enable' EXIT
adb logcat -c
adb shell svc wifi disable
adb shell svc data disable
adb shell am instrument -w -r -e requirePiperModels true -e class de.sprechbruecke.voices.PiperIntegrationTest de.sprechbruecke.voices.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/reports/piper/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
text=Path('app/build/reports/piper/instrumentation.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES' not in text and 'INSTRUMENTATION_FAILED' not in text, text
PY
for voice in gosia darkman thorsten; do
  adb exec-out run-as de.sprechbruecke.voices cat "files/$voice.wav" > "app/build/reports/piper/$voice.wav"
  adb exec-out run-as de.sprechbruecke.voices cat "files/$voice-android.wav" > "app/build/reports/piper/$voice-android.wav"
done
