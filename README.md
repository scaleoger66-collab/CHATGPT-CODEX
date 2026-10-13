# Sprechbrücke – Android 0.4

Deutsch ↔ Polnisch für Android 12 und neuer, mit Textübersetzung, Gesprächsmodus, Live-Sprachabschnitten und Offline-Sprachausgabe.

[Version 0.4 herunterladen (ZIP mit APK, ca. 31 MB)](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38363426716/artifacts/11796108040)

Der [Build vom 13. Oktober 2026](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38363426716) war erfolgreich: Unit-Tests, Android Lint, Bedienungstests, die Display-/Schriftgrößenmatrix und APK-Signaturprüfung bestanden. Gebauter Commit: `5d09c7843931d6753dcfc9d6a59b0254e55ff93d`. Die Emulator-Screenshots wurden kontrolliert. Ein abschließender Test auf dem echten Fold 7 steht noch aus.

## Design und Bedienung wechseln

**Einstellungen (Zahnrad) → Design & Bedienung → Design auswählen → Übernehmen**.

| Design | Darstellung | Modus nach Auswahl | Sprachbedienung |
| --- | --- | --- | --- |
| 2 · Nacht & Fokus | Dunkel, blaue Akzente | Live | Eine Start-/Stopptaste, Sprachrichtung umschaltbar |
| 3 · Dialog & Nähe | Hell, Koralle und Teal | Gespräch | Deutsch sprechen / Mów po polsku wählen direkt die passende Richtung |

Die Tabs **Text**, **Gespräch** und **Live** stehen in beiden Designs bereit. Design und zuletzt gewählter Modus bleiben nach einem Neustart ausgewählt. Beim Designwechsel bleiben Eingabe, fertige Übersetzung, Sitzungsverlauf und Stimmwahl erhalten. Laufende Aufnahme, Übersetzung und Sprachausgabe werden beendet, damit alte Ergebnisse keinen neuen Vorgang überschreiben. Es startet keine Aufnahme automatisch.

Version 0.4 nähert die Oberfläche den Designvorlagen an: kompakte Kacheln, separate Sprachfelder, korallfarbener Gesprächsreiter, runde Sprechtasten und Vorlesen direkt in der Übersetzungskachel. Im hellen Gesprächsmodus kann auch der Ausgangstext in seiner eigenen Sprache vorgelesen werden. Der Status „Text offline“ bezieht sich auf die geladenen Übersetzungsmodelle, nicht auf die Verfügbarkeit aller Sprachdienste.

Ab 680 dp verfügbarer Breite stehen die Textkarten nebeneinander, sofern die Systemschrift ausreichend Platz lässt. Auf schmalen Displays stehen sie untereinander. Schriftgrößen verwenden `sp` und berücksichtigen Androids Systemschriftvergrößerung einschließlich der nichtlinearen Skalierung von Android 14+. Displaydichte und Bildschirmzoom gehen über die verfügbare dp-Breite ein. Die Systemschrift wird nicht begrenzt oder durch automatisches Verkleinern aufgehoben. Bei sehr großer Schrift werden die beiden Sprechtasten untereinander angeordnet; bei Bedarf wachsen Kreise zu höheren Kapseln. Alle Inhalte bleiben scrollbar. Aufklappen und Konfigurationswechsel erzeugen die Ansicht passend neu, mit Erhalt von Eingabe, Übersetzung und Sitzungsverlauf. Die Pegelanzeige nutzt Androids gemeldete Mikrofonlautstärke.

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

[GitHub Actions](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/workflows/android.yml) baut auf Push sowie manuell über **Run workflow**. Nach erfolgreichem Lauf das Artefakt **Sprechbruecke-v0.4-Android** herunterladen. Bei GitHub anmelden, ZIP in Samsungs **Eigene Dateien** entpacken, `Sprechbruecke-v0.4-debug.apk` öffnen. Falls erforderlich Installation für die Dateien-App erlauben. Artefakte bleiben 14 Tage verfügbar.

0.4 ist eine Debug-Testversion. Bei einem Signaturkonflikt mit älteren Versionen muss die alte App vor der Installation deinstalliert werden; dabei gehen heruntergeladene Modelle und Einstellungen verloren. Der für 0.3 vorgesehene Schlüssel-Cache enthielt laut Buildprotokoll keinen Schlüssel (falscher Pfad). Ab 0.4 verwenden Build und Cache denselben ausdrücklich angegebenen Schlüsselpfad. Deshalb kann beim Wechsel von 0.3 auf 0.4 erneut eine Neuinstallation erforderlich sein. Der Testschlüssel wird für weitere Builds wiederverwendet, solange der Cache verfügbar bleibt. Für eine Produktionsveröffentlichung ist ein dauerhaft verwalteter Release-Schlüssel erforderlich.

## Entwicklung und Prüfungen

Java 17, Android SDK 35, Android Gradle Plugin 8.9.2 und Gradle 8.11.1. Projekt in Android Studio öffnen oder mit installiertem Gradle bauen; derzeit kein Gradle Wrapper im Projekt.

```sh
gradle testDebugUnitTest lintDebug assembleDebug
gradle connectedDebugAndroidTest
```

Der Workflow prüft zusätzlich auf einem Android-15-Emulator den Designwechsel über Einstellungen, passende Bedienelemente, Erhalt von Eingaben und Verlauf, Neustart sowie die zwei Sprachrichtungen. Die Displaymatrix prüft beide Designs bei 1080 × 2520 Pixel / 480 dpi (360 dp Breite) und 1968 × 2184 Pixel / 420 dpi (750 dp Breite), jeweils mit Systemschriftfaktor 1,0 und 2,0. Geprüft werden unter anderem sp-Skalierung, Text ohne vertikales Abschneiden, vollständige Reiterbeschriftung, Mindestgröße der Bedienelemente und sichtbare Mikrofontasten bei normaler Schrift. Screenshots enthalten ausdrücklich gesetzte Beispielsätze; dies ist kein Übersetzungsqualitätstest. Screenshots und Testberichte werden als **Android-test-reports** abgelegt. Der APK-Download wird erst nach erfolgreicher Prüfung und Signaturverifikation erstellt.

Auf dem Fold 7 zusätzlich prüfen: beide Designs geschlossen und aufgeklappt, vergrößerte Schrift, Aufnahme abbrechen, Design während einer Aufnahme wechseln, beide Offline-Sprachen und Stimmen sowie längere Gespräche. Emulator-Tests belegen keine reale Mikrofon- oder Übersetzungsqualität.
