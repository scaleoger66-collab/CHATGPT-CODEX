from pathlib import Path
import zipfile, tempfile, subprocess, re
with zipfile.ZipFile('app/build/outputs/apk/debug/app-debug.apk') as apk, tempfile.TemporaryDirectory() as tmp:
    for name in apk.namelist():
        if name.endswith('.so') and name.startswith('lib/'):
            file = Path(tmp) / Path(name).name
            file.write_bytes(apk.read(name))
            report = subprocess.check_output(['readelf', '-lW', str(file)], text=True)
            aligns = [int(line.split()[-1], 16) for line in report.splitlines() if line.strip().startswith('LOAD ')]
            assert aligns and min(aligns) >= 16384, (name, aligns)
            print('16 KB ELF alignment:', name)
