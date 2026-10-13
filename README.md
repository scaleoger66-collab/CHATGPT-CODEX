# Sprechbrücke 0.7 · feste App-Schriftgrößen

Größere Schrift und Symbole, unabhängig von der Android-Systemschrift (80 %, 100 %, 200 %). Die verfügbare Fensterauflösung wird ausgelesen und über die Displaydichte in logische Größen umgerechnet. Dadurch bleiben Beschriftungen auf unterschiedlich dichten Smartphones vergleichbar groß. Das aufgeklappte Display ändert das Layout, nicht die Schriftgröße. Einstellungen und App-Dialoge verwenden ebenfalls Schriftfaktor 1.

Neue Größen: Beschriftungen 16 dp, Hinweise 14 dp, Übersetzung 24 dp; Mikrofon-Symbol 40 dp, Reiter und Zeilensymbole 24 dp. Schmale Fenster bleiben scrollbar. Android-eigene Berechtigungsdialoge und die eigenständige Stimmen-App werden vom jeweiligen System bzw. ihrer eigenen App dargestellt.

Die bisherige Anpassung an die Systemschrift wird auf ausdrücklichen Nutzerwunsch ersetzt. CI prüft beide Designs bei Systemfaktor 0,8, 1,0 und 2,0 auf schmalem und breitem Display.

## Vorherige Version

# Sprechbrücke 0.6 · Deutsch ↔ Polski

Android-App für Offline-Textübersetzung, Gespräch und Live-Übersetzung in Sprachabschnitten. Für Galaxy Z Fold 7 entwickelt; Mindestversion Android 12.

## Update 0.6: polnisches Mikrofon

Die polnische Aufnahme verwendet jetzt **Vosk direkt in der App**, unabhängig vom Android-Spracherkennungsdienst. Deutsch verwendet weiterhin Androids On-Device-Erkennung ohne Cloud-Fallback.

[Version 0.6 herunterladen (ZIP, ca. 53 MB)](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38418487752/artifacts/11825572527)

[Erfolgreicher Android-15-Testlauf](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38418487752), geprüft am 13.10.2026. Erkennung kurzer Wörter kann ungenau sein; beispielsweise wurde die Begrüßung „cześć“ in den synthetischen Tests falsch erkannt, während „jak się masz, miło cię poznać“ erkannt wurde.

Bei installierter Version 0.5 nur **Sprechbruecke-v0.6-debug.apk** darüber installieren. Nicht deinstallieren. **Sprechbrücke Stimmen 0.2** und bereits heruntergeladene Stimmen bleiben erhalten.

Danach **Einstellungen → Mikrofon: Polnisch offline einrichten → Herunterladen**. Alternativ „Mów po polsku“ antippen. Für den einmaligen Download Internet einschalten und die App geöffnet lassen; Verlassen oder Konfigurationswechsel bricht den Download ab. Nach erfolgreichem Download erneut die Sprechtaste antippen.

## Drei getrennte Aufgaben

| Aufgabe | Technik | Einrichtung |
| --- | --- | --- |
| Text übersetzen | Google ML Kit Translate 17.0.3 | Einstellungen → Textübersetzung: Modelle laden / prüfen |
| Polnisches Mikrofon verstehen | Vosk Android 0.3.75, JNA 5.18.1, small Polish 0.22 | Einstellungen → Mikrofon: Polnisch offline einrichten |
| Deutsche Aufnahme | Android-On-Device-Spracherkennung | Deutsches Offline-Erkennungspaket des Geräts erforderlich |
| Text vorlesen | Eigenständige App Sprechbrücke Stimmen 0.2, Piper/Sherpa-ONNX | Stimme & Hörprobe → Stimmen herunterladen / verwalten |

**Gosia medium**, **Darkman medium** und **Thorsten high** sind Vorlesestimmen. Sie ersetzen kein Mikrofon-Erkennungsmodell. Polnische Stimme unter „Stimme & Hörprobe“ wählen; Deutsch verwendet Thorsten High.

Das polnische Vosk-Modell umfasst 52.979.372 Bytes Download und etwa 96 MB entpackte Daten. Etwa 250 MB freien Speicher für die Installation bereithalten. Quelle: https://alphacephei.com/vosk/models/vosk-model-small-pl-0.22.zip

SHA-256: `c4cd16498ea544f446f9e9a55cbd602b71cfe5a2b6f2b0834d81e1b6fce15f0d`.

Download und ZIP-Entpacken sind begrenzt, gegen Pfadmanipulation geschützt und werden erst nach erfolgreicher Prüfung atomar freigeschaltet. Abgebrochene Downloads gelten nicht als installiert.

## Bedienung und Darstellung

- Text-, Gesprächs- und Live-Modus; Text bis 5.000 Zeichen, Sitzungsverlauf ohne dauerhafte Speicherung.
- Live verarbeitet Sprachabschnitte bis 30 Sekunden bei polnischer Aufnahme und pausiert während der Übersetzung. Aufnahme stoppt beim Verlassen der App und vor dem Vorlesen.
- Design 2 „Nacht & Fokus“ und Design 3 „Dialog & Nähe“ unter Einstellungen umschaltbar, mit passenden Bedienelementen und gespeichertem Design.
- Feste dichteunabhängige App-Schriftgrößen ohne System-Schriftvergrößerung. Displaydichte und Fensterbreite bestimmen Layout und Abstände; ab 680 dp zwei Textkarten nebeneinander, auf schmalen Fenstern Scrollbarkeit.
- Modellinitialisierung und polnische Audioverarbeitung laufen auf einem Hintergrund-Thread. Abgebrochene Sitzungen können keine veralteten Ergebnisse in die Oberfläche schreiben.

Nach der Ersteinrichtung arbeiten Textübersetzung, polnische Erkennung und installierte Piper-Stimmen offline. Die deutsche Erkennung hängt vom verfügbaren Android-On-Device-Paket ab. Übersetzungs- und Erkennungsfehler sind möglich.

## Build und Prüfung

[GitHub Actions](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/workflows/android.yml) baut mit Java 17, Gradle 8.11.1, Android SDK 35 und AGP 8.9.2. Vor dem lokalen Build `bash ci/fetch-sherpa.sh` ausführen, dann `gradle testDebugUnitTest lintDebug assembleDebug`.

Das Download-Artefakt **Sprechbruecke-v0.6-Android** enthält den Übersetzer 0.6, Stimmen-Modul 0.2, Installations- und Lizenzhinweise sowie APK-Prüfsummen. Zum Herunterladen bei GitHub anmelden und das ZIP in Samsungs „Eigene Dateien“ entpacken. Artefakte bleiben 14 Tage verfügbar.

CI prüft:

- Unit-Tests und Android-Lint; ZIP-Pfadschutz, Abbruch und falsche Prüfsummen.
- 16-KB-ELF-Ausrichtung aller nativen Bibliotheken im Übersetzer.
- Original-Piper-Modelle aller drei Stimmen, native Audioerzeugung, Abbruch und Android-TTS-Anbindung bei ausgeschaltetem Emulator-Netz.
- Original-Vosk-Modell mit polnischen Audios von Gosia und Darkman bei ausgeschaltetem Emulator-Netz; Satzbestandteile müssen erkannt werden. Zusätzlich startet der echte AudioRecord-Adapter mit Mikrofonberechtigung und wird abgebrochen.
- Bedienoberfläche und beide Designs auf Android 15, 360 dp und 750 dp Fensterbreite sowie Schriftfaktoren 1,0 und 2,0.

Die Audiofixtures sind synthetisch. Diese Tests belegen weder reale Mikrofonqualität noch menschliche Spracherkennung auf dem Fold 7. Auf dem Gerät insbesondere Flugmodus, Sprecherwechsel, Aufnahmeabbruch und aufgeklapptes Display ausprobieren.

Beide APKs verwenden den bisherigen Testschlüssel. Zertifikat SHA-256: `968c1fb70fe098bae2ff8a956ed06df4f4f16bfb2911e44d48e9f95e727aba17`. Ein passendes Update bewahrt Modelle und Einstellungen. Es handelt sich um Debug-Testversionen; der Schlüssel wird über den bestehenden CI-Cache wiederverwendet.

## Lizenzen

Vosk und das polnische Modell stehen unter Apache 2.0; JNA wird unter der angebotenen Apache-2.0-Lizenz verwendet. Hinweise und Lizenztext befinden sich in `app/src/main/assets`.

Das separate Stimmen-Modul verwendet Sherpa-ONNX 1.13.8 mit eSpeak-NG und steht unter GPL 3.0. Es kommuniziert ausschließlich über Android-TTS mit dem Übersetzer; kein nativer GPL-Code wird in den ML-Kit-Übersetzer eingebunden. Details, Modelllizenzen und Quellverweise stehen in `voices/README.md` und `voices/src/main/assets`.
