# Sprechbrücke – Android 0.5

Deutsch ↔ Polnisch für Android 12 und neuer, mit Textübersetzung, Gesprächsmodus, Live-Sprachabschnitten und Offline-Sprachausgabe.

Version 0.5 ergänzt **Gosia medium**, **Darkman medium** und **Thorsten high** als herunterladbare Offline-Stimmen. Die Sprachausgabe läuft in der separaten Android-App **Sprechbrücke Stimmen**; der Übersetzer spricht sie über Androids Standard-TTS-Schnittstelle an.

[Version 0.5 herunterladen (ZIP mit beiden APKs, ca. 46 MB)](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38395780400/artifacts/11812712321)

Der [Build vom 13. Oktober 2026](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38395780400) hat Unit-Tests, Android Lint, echte Offline-Synthese mit allen drei Stimmen, Android-TTS-Stimmenwechsel, Bedienungstests, Display-/Schriftgrößenmatrix und beide APK-Signaturprüfungen bestanden. Gebauter Commit: `72283f14d7450d5d4861c632c4fa4d91464df921`. Hörproben und Prüfprotokolle: [Testartefakt](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/runs/38395780400/artifacts/11812627248). Ein abschließender Test auf dem echten Fold 7 steht noch aus.

Beide APKs verwenden denselben Testschlüssel wie der geprüfte 0.4-Build. Zertifikat SHA-256: `968c1fb70fe098bae2ff8a956ed06df4f4f16bfb2911e44d48e9f95e727aba17`.

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

1. Beide APKs aus dem ZIP installieren: **Sprechbruecke-v0.5-debug.apk** und **Sprechbruecke-Stimmen-v0.1.apk**.
2. Im Übersetzer **Stimme & Hörprobe → Stimmen herunterladen / verwalten → Alle drei Stimmen herunterladen** wählen. Die Stimmen-App währenddessen geöffnet lassen. Download insgesamt ca. 250 MB; zusätzlich wird Speicher zum Entpacken benötigt.
3. Zurück im Übersetzer unter **Stimme & Hörprobe → Polnische Stimme** zwischen **Gosia** und **Darkman** wählen. Für Deutsch nutzt Piper automatisch **Thorsten High**, auch beim Vorlesen des deutschen Ausgangstexts. Hörproben sind in beiden Apps verfügbar.
4. Unter **Einstellungen → Sprachmodelle laden / prüfen** die separaten Übersetzungsmodelle laden.
5. In Androids Spracheinstellungen Offline-Erkennung für Deutsch und Polnisch installieren, sofern der Gerätedienst beide unterstützt.
6. Danach im Flugmodus Text, Mikrofon und Vorlesen in beiden Richtungen ausprobieren.

| Stimme | Sprache | Paketdownload |
| --- | --- | --- |
| Gosia medium | Polnisch | ca. 67 MB |
| Darkman medium | Polnisch | ca. 67 MB |
| Thorsten high | Deutsch | ca. 116 MB |

Pakete werden über HTTPS von den Sherpa-ONNX GitHub-Releases geladen und vor der Installation gegen eine fest hinterlegte SHA-256-Prüfsumme geprüft. Abgebrochene Downloads gelten nicht als installiert. Die Spracherzeugung selbst läuft lokal. Heruntergeladene Stimmen bleiben erhalten, wenn nur der Übersetzer deinstalliert wird; beim Entfernen der Stimmen-App werden sie gelöscht.

Piper ist voreingestellt. Fehlt die gewählte Stimme, erscheint ein Hinweis; es wird keine andere Stimme automatisch gewählt. Unter **Sprachausgabe → Android** lassen sich weiterhin andere installierte Anbieter nutzen. Polnische Stimmwahl und Sprechtempo werden gespeichert. Der Klang und die Geschwindigkeit müssen auf dem echten Gerät beurteilt werden.

## Umfang und Grenzen

- ML Kit übersetzt lokal nach dem Modelldownload. Piper übernimmt die Sprachausgabe; Whisper und Qwen sind nicht integriert.
- Gespräch: eine Äußerung erkennen, übersetzen und auf Wunsch vorlesen. Die andere Sprechtaste wählt die Gegenrichtung.
- Live: abgeschlossene Sprachabschnitte übersetzen. Während Übersetzung und Neustart entstehen Aufnahmeunterbrechungen. Vorlesen beendet die Aufnahme.
- Mikrofon verwendet ausschließlich Androids `createOnDeviceSpeechRecognizer`, ohne Cloud-Fallback. Fehlende Sprachunterstützung wird angezeigt.
- Texte bis 5.000 Zeichen; Vorlesen unterliegt zusätzlich der Längenbegrenzung des Android-Sprachdienstes.
- Gesprächsverlauf bleibt in der Sitzung und wird bei Activity-Neuerstellung, z. B. beim Aufklappen, wiederhergestellt. Keine dauerhafte Gesprächsdatenbank.
- Mikrofon stoppt beim Verlassen der App. Übersetzungsqualität und Spracherkennung müssen auf dem echten Gerät geprüft werden.
- INTERNET wird für Modelldownloads benötigt. Die SDK-Bedingungen von ML Kit gelten, einschließlich möglicher Diagnose-/Nutzungsdaten.

## APK bauen und installieren

[GitHub Actions](https://github.com/scaleoger66-collab/CHATGPT-CODEX/actions/workflows/android.yml) baut auf Push sowie manuell über **Run workflow**. Nach erfolgreichem Lauf das Artefakt **Sprechbruecke-v0.5-Android** herunterladen. Bei GitHub anmelden, ZIP in Samsungs **Eigene Dateien** entpacken, beide APKs öffnen und installieren. Falls erforderlich Installation für die Dateien-App erlauben. Artefakte bleiben 14 Tage verfügbar.

0.5 ist eine Debug-Testversion. Bei einem Signaturkonflikt mit älteren Versionen muss die alte App vor der Installation deinstalliert werden; dabei gehen heruntergeladene Modelle und Einstellungen verloren. Der für 0.3 vorgesehene Schlüssel-Cache enthielt laut Buildprotokoll keinen Schlüssel (falscher Pfad). Ab 0.4 verwenden Build und Cache denselben ausdrücklich angegebenen Schlüsselpfad. Deshalb kann beim Wechsel von 0.3 auf 0.4 erneut eine Neuinstallation erforderlich sein. Der Testschlüssel wird für weitere Builds wiederverwendet, solange der Cache verfügbar bleibt. Für eine Produktionsveröffentlichung ist ein dauerhaft verwalteter Release-Schlüssel erforderlich.

## Entwicklung und Prüfungen

Java 17, Android SDK 35, Android Gradle Plugin 8.9.2 und Gradle 8.11.1. Projekt in Android Studio öffnen oder mit installiertem Gradle bauen; derzeit kein Gradle Wrapper im Projekt.

```sh
bash ci/fetch-sherpa.sh
gradle testDebugUnitTest lintDebug assembleDebug
gradle :app:connectedDebugAndroidTest
```

Der Workflow prüft zusätzlich auf einem Android-15-Emulator den Designwechsel über Einstellungen, passende Bedienelemente, Erhalt von Eingaben und Verlauf, Neustart sowie die zwei Sprachrichtungen. Die Displaymatrix prüft beide Designs bei 1080 × 2520 Pixel / 480 dpi (360 dp Breite) und 1968 × 2184 Pixel / 420 dpi (750 dp Breite), jeweils mit Systemschriftfaktor 1,0 und 2,0. Geprüft werden unter anderem sp-Skalierung, Text ohne vertikales Abschneiden, vollständige Reiterbeschriftung, Mindestgröße der Bedienelemente und sichtbare Mikrofontasten bei normaler Schrift. Screenshots enthalten ausdrücklich gesetzte Beispielsätze; dies ist kein Übersetzungsqualitätstest. Screenshots und Testberichte werden als **Android-test-reports** abgelegt. Der APK-Download wird erst nach erfolgreicher Prüfung und Signaturverifikation erstellt.

Auf dem Fold 7 zusätzlich prüfen: beide Designs geschlossen und aufgeklappt, vergrößerte Schrift, Aufnahme abbrechen, Design während einer Aufnahme wechseln, beide Offline-Sprachen und Stimmen sowie längere Gespräche. Emulator-Tests belegen keine reale Mikrofon- oder Übersetzungsqualität.

## Stimmen-Modul und Lizenzen

Das unabhängige Modul `voices` verwendet Sherpa-ONNX 1.13.8 und eSpeak NG zur lokalen Piper-Synthese. Es wird unter GPL-3.0 bereitgestellt und enthält keine ML-Kit-Abhängigkeit. Der Übersetzer bindet diese Bibliotheken nicht ein; beide Apps kommunizieren über Androids TTS-Dienst. Quellcode, Lizenztexte und Modellquellen stehen unter [voices](voices/README.md).

`ci/piper-offline-test.sh` installiert alle drei originalen Pakete im Emulator und schaltet WLAN und mobile Daten vor dem Test ab. Es prüft echte PCM-Ausgabe, Abbruch der nativen Synthese, Erhalt einer installierten Stimme bei abgebrochener Neuinstallation sowie Android-TTS-Dienstauswahl und Dateisynthese für alle drei Stimmen. Hörproben und Protokolle werden unter `app/build/reports/piper` im Testartefakt abgelegt.
