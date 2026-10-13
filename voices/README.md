# Sprechbrücke Stimmen

Eigenständiger Android-TTS-Dienst mit Gosia und Darkman (Polnisch) und Thorsten High (Deutsch). Native Synthese mit unverändertem Sherpa-ONNX 1.13.8, Download und SHA-256-Prüfung der originalen Modellarchive. Alle Texte bleiben lokal. Die App fordert Internetzugriff nur für die vom Benutzer gestarteten Modelldownloads an. Abgebrochene Downloads werden beim nächsten Versuch neu geladen; es gibt keine Hintergrunddienst-/Neustartgarantie. Bereits installierte Stimmen bleiben erhalten.

Dieses Modul enthält kein Übersetzungs-SDK. Android-Anwendungen greifen ausschließlich über die standardisierte TextToSpeech-Schnittstelle zu. Der Dienst meldet nur installierte Stimmen, unterstützt beide Sprachrichtungen sowie Stop und streamt PCM-Daten an Android. Das Modell wird erst beim Sprechen geladen; maximal eines bleibt im Speicher.

## Bauen

Java 17, Gradle 8.11.1, Android SDK 35. Vom Repository-Hauptverzeichnis:

```sh
bash ci/fetch-sherpa.sh
gradle :voices:testDebugUnitTest :voices:lintDebug :voices:assembleDebug
```

`ci/fetch-sherpa.sh` lädt das veröffentlichte AAR und prüft dessen festgelegte SHA-256-Summe. Um das AAR selbst zu bauen, siehe [unveränderte Originalquellen und Buildskripte](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8/android/SherpaOnnxAar). Die verwendeten Drittanbieter-Versionen und Quellen stehen in [Sherpas cmake-Dateien](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8/cmake).

## Lizenz

Copyright (c) 2026 Sprechbrücke contributors. Dieses eigenständige Sprachmodul einschließlich seiner neuen Java-Quellen wird unter GNU GPL Version 3 veröffentlicht. Keine Gewährleistung. Vollständiger Text: [LICENSE](LICENSE). Unveränderte Komponenten behalten ihre jeweiligen Lizenzen; siehe [Hinweise](src/main/assets/VOICE_NOTICES.txt). Modellkarten sind unter den dort angegebenen ursprünglichen Modellquellen verfügbar. Die Modelle werden auf ausdrücklichen Benutzerwunsch vom Upstream heruntergeladen.

## Test

`ci/piper-offline-test.sh` installiert die Test-APKs, überträgt die drei unveränderten Modellarchive, deaktiviert WLAN/Mobilfunk im Emulator und lässt die Produktionsroutine die Pakete prüfen und installieren. Danach erzeugt es mit jedem Modell eine WAV-Datei, prüft Audiosignal und Native-Abbruch sowie den Erhalt installierter Dateien nach abgebrochener Neuinstallation. Hörproben liegen im Build-Artefakt Android-test-reports unter piper/.
