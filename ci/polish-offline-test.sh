#!/usr/bin/env bash
set -euo pipefail
mkdir -p .voice-models app/build/reports/polish
curl --fail --location --retry 3 https://alphacephei.com/vosk/models/vosk-model-small-pl-0.22.zip -o .voice-models/polish.zip
echo 'c4cd16498ea544f446f9e9a55cbd602b71cfe5a2b6f2b0834d81e1b6fce15f0d  .voice-models/polish.zip' | sha256sum -c -
python3 - <<'PY'
from pathlib import Path
import wave, struct, math
for name in ['gosia','darkman']:
    with wave.open('app/build/reports/piper/'+name+'.wav','rb') as wav:
        assert wav.getnchannels()==1 and wav.getsampwidth()==2
        rate=wav.getframerate()
        data=wav.readframes(wav.getnframes())
        samples=struct.unpack('<'+'h'*(len(data)//2),data)
    output=[]
    for i in range(int(len(samples)*16000/rate)):
        pos=i*rate/16000
        left=int(pos); fraction=pos-left
        output.append(round(samples[left]*(1-fraction)+samples[min(left+1,len(samples)-1)]*fraction))
    output += [0]*32000
    Path('.voice-models/'+name+'.pcm').write_bytes(struct.pack('<'+'h'*len(output),*output))
PY
gradle --no-daemon :app:installDebug :app:installDebugAndroidTest
adb shell run-as de.sprechbruecke mkdir -p cache
for name in polish.zip gosia.pcm darkman.pcm; do
  adb shell "run-as de.sprechbruecke sh -c 'cat > cache/$name'" < ".voice-models/$name"
done
trap 'adb logcat -d > app/build/reports/polish/logcat.txt; grep PolishTest app/build/reports/polish/logcat.txt || true; adb shell svc wifi enable; adb shell svc data enable' EXIT
adb shell svc wifi disable
adb shell svc data disable
adb shell am instrument -w -r -e requirePolishModel true -e class de.sprechbruecke.PolishRecognitionTest de.sprechbruecke.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/reports/polish/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
text=Path('app/build/reports/polish/instrumentation.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES' not in text and 'INSTRUMENTATION_FAILED' not in text, text
PY
