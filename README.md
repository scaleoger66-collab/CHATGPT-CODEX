# Sprechbrücke – Android 0.3

Deutsch ↔ Polnisch für Android 12 und neuer, mit Textübersetzung, Gesprächsmodus, Live-Sprachabschnitten und Offline-Sprachausgabe.

## Design und Bedienung wechseln

**Einstellungen (Zahnrad) → Design & Bedienung → Design auswählen → Übernehmen**.

| Design | Darstellung | Modus nach Auswahl | Sprachbedienung |
| --- | --- | --- | --- |
| 2 · Nacht & Fokus | Dunkel, blaue Akzente | Live | Eine Start-/Stopptaste, Sprachrichtung umschaltbar |
| 3 · Dialog & Nähe | Hell, Koralle und Teal | Gespräch | Deutsch sprechen / Mów po polsku wählen direkt die passende Richtung |

Die Tabs **Text**, **Gespräch** und **Live** stehen in beiden Designs bereit. Design und zuletzt gewählter Modus bleiben nach einem Neustart ausgewählt. Beim Designwechsel bleiben Eingabe, fertige Übersetzung, Sitzungsverlauf und Stimmwahl erhalten. Laufende Aufnahme, Übersetzung und Sprachausgabe werden beendet, damit alte Ergebnisse keinen neuen Vorgang überschreiben. Es startet keine Aufnahme automatisch.

Ab 720 dp Displaybreite stehen die Textkarten nebeneinander; auf schmalen Displays untereinander. Die Seite ist auch mit vergrößerter Schrift scrollbar. Die Pegelanzeige nutzt Androids gemeldete Mikrofonlautstärke.

## Offline einrichten

1. Unter **Einstellungen → Sprachmodelle laden / prüfen** einmalig mit Internet die Übersetzungsmodelle laden.
2. In Androids Spracheinstellungen Offline-Erkennung für Deutsch und Polnisch installieren, sofern das Gerät bzw. der Dienst beide Sprachen unterstützt.
3. Unter **Stimme & Hörprobe** Anbieter und Offline-Stimme für die aktuelle Zielsprache auswählen. Über **Sprachpakete installieren** fehlende Stimmen laden. Optional Google-Sprachausgabe über den angebotenen Play-Store-Link installieren und mit Samsung vergleichen.
4. Danach im Flugmodus Text, Mikrofon und Vorlesen in beiden Richtungen ausprobieren.

Anbieter, Stimme pro Sprache/Anbieter und Sprechtempo werden gespeichert. Automatische Stimmauswahl bevorzugt die vom Anbieter gemeldete Qualität; das garantiert keine natürlichere Aussprache. Netzwerkstimmen und als nicht installiert markierte Stimmen sind ausgeschlossen.

## Umfang und Grenzen

- ML Kit übersetzt lokal nach dem Modelldownload. Whisper, Qwen und Piper sind noch nicht integriert.
- Gespräch: eine Äußerung erkennen, übersetzen und auf Wunsch vorlesen. Die andere Sprechtaste wählt die Gegenrichtung.
- Live: abgeschlossene Sprachabschnitte übersetzen. Während Übersetzung und Neustart entstehen Aufnahmeunterbrechungen. Vorlesen beendet die Aufnahme.
- Mikrofon verwendet ausschließlich Androids `createOnDeviceSpeechRecognizer`, ohne Cloud-Fallback. Fehlende Sprachunterstützung wird angezeigt.
- Texte bis 5.000 Zeichen; Vorlesen unterliegt zusätzlich der Längenbegrenzung des Android-Sprachdienstes.
- Gesprächsverlauf bleibt in der Sitzung und wird bei Activity-Neuerstellung, z. B. beim Aufklappen, wiederhergestellt. Keine dauerhafte Gesprächsdatenbank.
- Mikrofon stoppt beim Verlassen der App. Übersetzungsqualität und Spracherkennung müssen auf dem echten Gerät geprüft werden.
- INTERNET wird für Modelldownloads benötigt. Die SDK-Bedingungen von ML Kit gelten, einschließlich möglicher Diagnose-/Nutzungsdaten.

## APK bauen und installieren

[GitHub Actions](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/workflows/android.yml) baut auf Push sowie manuell über **Run workflow**. Nach erfolgreichem Lauf das Artefakt **Sprechbruecke-v0.3-Android** herunterladen. Bei GitHub anmelden, ZIP in Samsungs **Eigene Dateien** entpacken, `Sprechbruecke-v0.3-debug.apk` öffnen. Falls erforderlich Installation für die Dateien-App erlauben. Artefakte bleiben 14 Tage verfügbar.

0.3 ist eine Debug-Testversion. Bei einem Signaturkonflikt mit älteren Versionen muss die alte App vor der Installation deinstalliert werden; dabei gehen heruntergeladene Modelle und Einstellungen verloren. Ab 0.3 wird der Testsignaturschlüssel im GitHub-Actions-Cache wiederverwendet, solange dieser Cache verfügbar bleibt. Für eine Produktionsveröffentlichung ist ein dauerhaft verwalteter Release-Schlüssel erforderlich.

## Entwicklung und Prüfungen

Java 17, Android SDK 35, Android Gradle Plugin 8.9.2 und Gradle 8.11.1. Projekt in Android Studio öffnen oder mit installiertem Gradle bauen; derzeit kein Gradle Wrapper im Projekt.

```sh
gradle testDebugUnitTest lintDebug assembleDebug
gradle connectedDebugAndroidTest
```

Der Workflow prüft zusätzlich auf einem Android-15-Emulator den Designwechsel über Einstellungen, passende Bedienelemente, Erhalt von Eingaben und Verlauf, Neustart sowie die zwei Sprachrichtungen. Screenshots und Testberichte werden als **Android-test-reports** abgelegt. Der APK-Download wird erst nach erfolgreicher Prüfung und Signaturverifikation erstellt.

Auf dem Fold 7 zusätzlich prüfen: beide Designs geschlossen und aufgeklappt, vergrößerte Schrift, Aufnahme abbrechen, Design während einer Aufnahme wechseln, beide Offline-Sprachen und Stimmen sowie längere Gespräche. Emulator-Tests belegen keine reale Mikrofon- oder Übersetzungsqualität.
