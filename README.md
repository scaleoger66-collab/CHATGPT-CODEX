# Sprechbrücke – Android 0.2

Native Android-App (Java), Android 12 oder neuer, Deutsch ↔ Polnisch.

## Neu in 0.2

Unter **Stimme & Hörprobe** lassen sich installierte Anbieter (z. B. Samsung und Google), Offline-Stimmen für die aktuelle Zielsprache und das Sprechtempo auswählen. Die Auswahl wird pro Anbieter und Sprache gespeichert. Eine Hörprobe verwendet einen festen deutschen bzw. polnischen Text. Der automatische Modus priorisiert die vom Anbieter gemeldete Qualität; das ist keine Garantie für natürlichere Aussprache. Netzwerkstimmen und als nicht installiert gemeldete Stimmen werden ausgeschlossen.

Zum Vergleichen Google-Sprachausgabe über den Play Store installieren, als Anbieter wählen und unter **Sprachpakete installieren** Polnisch herunterladen. Danach mit der Hörprobe vergleichen. Ohne passende Stimme wird ein Hinweis angezeigt.

Der [Build von 0.2](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38343653438) inklusive Unit-Tests, Android Lint und APK-Signaturprüfung war erfolgreich. [Version 0.2 herunterladen](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38343653438/artifacts/11787603916). Der Gerätetest der Stimmwahl und ein Hörvergleich stehen noch aus. Bei einem Signaturkonflikt mit 0.1 ist vor der Installation eine Deinstallation nötig; dabei gehen die heruntergeladenen Übersetzungsmodelle verloren.

## Erste Testversion installieren

[Sprechbrücke 0.1 herunterladen (GitHub-Artefakt, ca. 31 MB)](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38342481631/artifacts/11787333143). Für den Download bei GitHub anmelden. Dieser Build ist bis zum 27. Oktober 2026 verfügbar; danach den Workflow erneut starten.

ZIP in Samsungs „Eigene Dateien“ entpacken und `Sprechbruecke-v0.1-debug.apk` öffnen. Falls Android nachfragt, die Installation für die verwendete Dateien-App erlauben. Danach Sprechbrücke öffnen und einmalig mit Internet die Sprachmodelle laden.

## Umfang

- Texteingabe und lokale Übersetzung mit Google ML Kit.
- Gesprächsmodus: eine Äußerung aufnehmen, übersetzen, optional vorlesen; Richtung für die andere Person umschalten.
- Live-Modus: erkannte Sprachabschnitte übersetzen und Erkennung wieder starten. Keine simultane Wort-für-Wort-Übersetzung. Während der Übersetzung entstehen Aufnahmeunterbrechungen; in diesen Pausen Gesprochenes wird nicht erfasst.
- Vorlesen über installierte Android-Offline-Stimmen. Im Live-Modus erfolgt keine automatische Sprachausgabe; manuelles Vorlesen beendet die Aufnahme.
- Verlauf nur im Arbeitsspeicher; Mikrofon wird beim Verlassen der App gestoppt.

## Offline-Einrichtung

1. App mit Internet starten und „Sprachmodelle laden / prüfen“ antippen. Bei Richtungswechsel ebenfalls prüfen. Bereits installierte Modelle benötigen keinen erneuten Download.
2. In Androids Spracheinstellungen Offline-Spracherkennung für Deutsch und Polnisch sowie Offline-Stimmen installieren, sofern das Gerät dies unterstützt. Installierte Übersetzungsmodelle werden beim App-Start automatisch erkannt.
3. Mikrofonberechtigung erteilen und im Flugmodus beide Richtungen, Text, Gespräch und Live prüfen.

Das Mikrofon verwendet ausschließlich `createOnDeviceSpeechRecognizer`, ohne Cloud-Fallback. Nicht jedes Android-Gerät unterstützt beide Sprachen offline. Bei fehlender Unterstützung bleibt die Textübersetzung verfügbar. Die INTERNET-Berechtigung wird für Modelldownloads benötigt. ML Kit kann gemäß den SDK-Bedingungen Diagnose-/Nutzungsdaten verarbeiten; eine vollständige Aussage zur Netzwerkkommunikation erfordert weitere Prüfung.

ML Kit übersetzt Texte, liefert aber keine separate Grammatikprüfung und erzeugt keine inhaltlichen Chat-Antworten. Übersetzungsqualität und Kontextverständnis sind begrenzt; für diese Funktionen wäre ein zusätzliches lokales Sprachmodell mit eigener Laufzeit und passender Hardware erforderlich.

## APK mit GitHub Actions bauen

Das Projekt enthält `.github/workflows/android.yml`. Nach dem Hochladen der Projektdateien in ein freigegebenes GitHub-Repository baut jeder Push auf `main`, `master` oder `codex/**` die App. Der Workflow lässt sich außerdem über **Actions → Android APK → Run workflow** starten.

Der Build installiert Java 17, Android SDK 35 und Gradle 8.11.1, führt Unit-Tests und Android Lint aus und prüft die APK-Signatur. Bei Erfolg erscheint im Workflow-Lauf unter **Artifacts** der Download **Sprechbruecke-v0.1-Android**. Darin liegen die signierte Test-APK und ihre SHA-256-Prüfsumme. Der Download ist 14 Tage verfügbar.

Das Repository benötigt aktivierte GitHub Actions; die Verbindung zum Hochladen muss auch Workflow-Dateien schreiben dürfen. Es sind keine API-Schlüssel nötig. Die APK ist eine Debug-Version für erste Gerätetests. Verschiedene Builds können unterschiedliche Debug-Signaturen haben; dann muss die vorherige App vor der Installation deinstalliert werden, wobei heruntergeladene Modelle verloren gehen.

## Lokal bauen

Ordner in Android Studio öffnen, JDK 17 oder neuer und Android SDK 35 installieren, Gradle 8.11.1 auswählen und das Projekt synchronisieren. Android Gradle Plugin: 8.9.2. Es liegt noch kein Gradle Wrapper bei.

Mit lokal installiertem Gradle:

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

APK danach: `app/build/outputs/apk/debug/app-debug.apk`.

## Verifikation

Der [GitHub-Build vom 13. Oktober 2026](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38342481631) war erfolgreich: `testDebugUnitTest`, `lintDebug`, `assembleDebug` und die APK-Signaturprüfung sind durchgelaufen. Gebauter Commit: `37468417845314f14eee985cf7da13c9c6dcb5e1`. Ein Test auf einem echten Android-Gerät steht noch aus; Build-Erfolg bestätigt nicht die Erkennungs- oder Übersetzungsqualität.

Für den Fold-7-Test: beide Übersetzungsrichtungen im Flugmodus, Mikrofonberechtigung ablehnen/erlauben, Aufnahme stoppen und erneut starten, Sprachrichtung wechseln, App in den Hintergrund schicken sowie Auf-/Zuklappen während Texteingabe und Aufnahme prüfen. Aufnahme wird bei Unterbrechungen gestoppt; Text und Verlauf werden bei einer Activity-Neuerstellung wiederhergestellt. Verzögerte Ergebnisse abgebrochener Vorgänge dürfen keine neueren Ergebnisse überschreiben.

Whisper, Qwen und Piper sind in 0.1 noch nicht integriert. Die erste Version nutzt ML Kit und Androids lokale Sprachdienste. Sie ist eine Grundlage für Gerätetests, keine Zusicherung fehlerfreier oder durchgehend simultaner Übersetzung.
