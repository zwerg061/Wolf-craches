# Creach Report Fix

[![Minecraft](https://img.shields.io/badge/Minecraft-1.19.2-62b47a?style=flat-square)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-43.x-f1a13b?style=flat-square)](https://files.minecraftforge.net/)
[![Seite](https://img.shields.io/badge/Seite-Client-4a90d9?style=flat-square)](#wichtig-clientseitig)
[![Version](https://img.shields.io/badge/Version-1.2.0-blue?style=flat-square)](CHANGELOG.md)
[![Lizenz](https://img.shields.io/badge/Lizenz-MIT-green?style=flat-square)](LICENSE)

Clientseitiger Stabilitäts-Fix für Minecraft **Forge 1.19.2** gegen die Abstürze im
Spielmodus **„Wolf"** – plus ein Diagnose-Fenster im Stil von *Crash Assistant*, das nach
einem Crash beim nächsten Start die Ursache erklärt.

## Download

| Datei | Größe | SHA-256 |
| --- | --- | --- |
| **[creach-report-fix-1.2.0-client.jar](releases/creach-report-fix-1.2.0-client.jar)** | 41 731 B | `214873BB…C5670F` |

Die vollständige Prüfsumme liegt daneben in
[`releases/creach-report-fix-1.2.0-client.jar.sha256`](releases/creach-report-fix-1.2.0-client.jar.sha256).

Installation in drei Schritten:

1. **Minecraft Forge 1.19.2** (Version 43.x) installieren.
2. Alte Versionen aus `.minecraft/mods` löschen (z. B. `creach-report-fix-1.1.x`).
3. Die JAR nach `.minecraft/mods` kopieren – fertig.

> Beim Aktualisieren immer die alte JAR entfernen, damit nur **eine** Version im `mods`-Ordner liegt.

## Was der Mod macht

| Bereich | Maßnahme |
| --- | --- |
| **ModelBakery** | Iteriert eine stabile Kopie des Item-Registry-KeySets statt der Live-Sicht – behebt den häufigsten Crash (`ConcurrentModificationException` in `ModelBakery.<init>`) |
| **MrCrayfish's Gun Mod** | Erstellt eine Kopie der Item-Registry, damit kein `ConcurrentModificationException` beim Meta-Laden entsteht |
| **FramedBlocks** | Fängt einen noch nicht initialisierten `baseModel` ab, statt eine `NullPointerException` zu werfen |
| **Nebenläufigkeit** | Begrenzt die Hintergrund-Threads (`max.bg.threads`, ForkJoinPool) auf ¼ der CPU-Kerne, maximal 3 |
| **ModernFix** | Deaktiviert die zwei Mixins, die Reload-Crashes verursachen (`dedicated_reload_executor`, `thread_priorities`) – nur wenn ModernFix installiert ist |
| **Loading-Overlay** | Lässt das Overlay bei einem Server-Resource-Pack-Reload durchsichtig werden und pausiert das Spiel nicht mehr |
| **Vanilla** | Blockiert F3+T / F3+S (manueller Ressourcen-Reload) und zeigt einen Hinweis im Chat |
| **Logging** | Ergänzt bei unbehandelten Exceptions einen lesbaren Hinweis im Log |
| **Crash Assistant** | Analysiert nach einem Crash den neuesten Report und zeigt beim nächsten Start Ursache, Tipps und verdächtige Mods |

Alle Eingriffe sind **abgesichert**: Jeder Mixin steht auf `require = 0`. Fehlt eine Zielklasse
(z. B. weil FramedBlocks oder die Gun Mod gar nicht installiert ist), wird der Guard einfach
übersprungen – der Mod selbst crasht dadurch nicht.

## Crash Assistant

Beim Start sucht der Mod in `crash-reports/` den neuesten `crash-*.txt`. Ist er neuer als der
zuletzt angezeigte Report (und nicht älter als das konfigurierte Limit), erscheint statt des
Titelbildschirms ein Diagnose-Fenster:

* **Beschreibung** und **Fehlerzeile** aus dem Report
* **Erkannte Probleme** mit konkreten Lösungshinweisen
* **Verdächtige Mods**, aufgelöst über die geladene Mod-Liste
* Buttons: **Report-Ordner öffnen**, **Diagnose kopieren**, **Schließen**

Der Mod läuft dabei **nicht** während des Crashes mit – die Auswertung passiert erst beim
nächsten Start. Jeder Report wird nur einmal angezeigt.

### Konfiguration

Beim ersten Start wird `config/creachreportfix-client.properties` angelegt:

```properties
# Creach Report Fix - Crash Assistant
crash-screen-enabled=true
crash-report-max-age-days=30
```

| Schlüssel | Standard | Bedeutung |
| --- | --- | --- |
| `crash-screen-enabled` | `true` | Fenster nach einem Crash anzeigen |
| `crash-report-max-age-days` | `30` | Reports, die älter sind, werden ignoriert |

## Ausgewertete Crash-Reports

Grundlage sind **58 eingesandte Client-Crash-Reports** aus der Wolf-Instanz
(Forge 1.19.2, 43.3.0):

| Anzahl | Ursache | Status |
| ---: | --- | --- |
| 45 | `ConcurrentModificationException` in `ModelBakery.<init>` beim Item-Registry-KeySet | **behoben** (ModelBakery-Guard) |
| 6 | `ConcurrentModificationException` in MrCrayfish's Gun Mod (`MetaLoader`) | **behoben** (Registry-Snapshot) |
| 4 | `ClassCastException` WorldSchematic → ClientLevel (Litematica/Forgematica) | erkannt, Hinweis im Crash-Fenster |
| 2 | `NoSuchElementException` im Block-Update (`CollectingNeighborUpdater`) | erkannt, Hinweis im Crash-Fenster |
| 1 | `NullPointerException` im Chunk-Mesh (Embeddium-Fluid-Cache) | erkannt, Hinweis im Crash-Fenster |

Alle 58 Reports laufen durch den Analyzer (58/58 ohne Fehlschlag).

## Selbst bauen

Voraussetzungen: **JDK 17** und Internetzugang für den ersten Build.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Das fertige Mod-JAR liegt danach unter `build/libs/creach-report-fix-<version>.jar`.
Zum Testen in einer Entwicklungsumgebung:

```bash
gradlew.bat runClient
```

> Beim Bau werden drei `@Shadow`-Warnungen für `LoadingOverlay` gemeldet. Das ist normal:
> Die Felder sind absichtlich mit SRG-Namen deklariert, damit sie auch im laufenden Spiel passen.

## Projektstruktur

```
.
├── build.gradle                      Gradle-/Forge-Konfiguration
├── settings.gradle
├── gradle.properties                 Mod-Version und Maven-Koordinaten
├── gradle/wrapper/                   Gradle Wrapper (Gradle 7.6, von ForgeGradle 5 benötigt)
├── CHANGELOG.md                      Versionshistorie
├── releases/                         Fertige, spielbare Mod-JARs + Prüfsumme
│   └── legacy/                       Alte Versionen (nur als Referenz)
└── src/main/
    ├── java/de/wolfmod/creachreportfix/
    │   ├── CreachReportFixMod.java   Einstiegspunkt
    │   ├── crash/                    Crash-Report-Analyse und Diagnose-Fenster
    │   ├── support/                  Hilfsklassen (Threads, ModernFix, Registry-Snapshots …)
    │   └── mixin/                    Mixins gegen Vanilla- und Mod-Klassen
    └── resources/
        ├── META-INF/mods.toml        Mod-Metadaten
        ├── creachreportfix.mixins.json
        └── pack.mcmeta
```

## Wichtig: clientseitig

Der Mod ist **rein clientseitig**. Auf einem Server ist er weder nötig noch wirksam – er
verändert keine Spielmechanik und sendet nichts an den Server.

Deshalb ist `ClientPacketListenerMixin.java` zwar im Quelltext vorhanden, aber **nicht** in
`creachreportfix.mixins.json` registriert: Es lehnt Server-Resource-Packs ab, was auf Servern
zu einem Kick führen kann. Wer es trotzdem testen will, trägt es dort in der `client`-Liste ein.

## Rechtliches

* Lizenz: [MIT](LICENSE)
* Kein offizielles Minecraft-, Mojang- oder Forge-Projekt.
* „Minecraft" ist ein Markenzeichen von Mojang Studios.
