# Wolf-Crashes / Creach Report Fix

Ein clientseitiger Fix gegen die Crashes im Minecraft-Spielmodus **"Wolf"** (Minecraft Forge 1.19.2).

Der Mod fängt die typischen Abstürze beim Ressourcen-Reload, beim Model-Baking und beim
Item-Registry-Zugriff ab, statt sie bis zum Crash durchzureichen. Zusätzlich zeigt er nach
einem Crash beim nächsten Start ein Fenster im Stil von *Crash Assistant* mit einer
verständlichen Diagnose.

## Was der Mod macht

| Bereich | Maßnahme |
| --- | --- |
| Nebenläufigkeit | Begrenzt die Hintergrund-Threads (`max.bg.threads`, ForkJoinPool) auf ¼ der CPU-Kerne, maximal 3 |
| ModernFix | Deaktiviert die zwei Mixins, die Reload-Crashes verursachen (`dedicated_reload_executor`, `thread_priorities`) – nur wenn ModernFix installiert ist |
| FramedBlocks | Fängt einen noch nicht initialisierten `baseModel` ab, statt eine `NullPointerException` zu werfen |
| MrCrayfish's Gun Mod | Erstellt eine Kopie der Item-Registry, damit kein `ConcurrentModificationException` beim Meta-Laden entsteht |
| ModelBakery | Iteriert eine stabile Kopie des Item-Registry-KeySets statt der Live-Sicht – behebt den häufigsten Crash (`ConcurrentModificationException` in `ModelBakery.<init>`) |
| Vanilla | Blockiert F3+T / F3+S (manueller Ressourcen-Reload) und zeigt einen Hinweis im Chat |
| Loading-Overlay | Lässt das Overlay bei einem Server-Resource-Pack-Reload durchsichtig werden und pausiert das Spiel nicht mehr |
| Logging | Ergänzt bei unbehandelten Exceptions einen lesbaren Hinweis im Log |
| Crash Assistant | Analysiert nach einem Crash den neuesten Report und zeigt beim nächsten Start Ursache, Tipps und verdächtige Mods |

## Crash Assistant

Beim Start sucht der Mod im Ordner `crash-reports/` den neuesten `crash-*.txt`. Ist er neuer
als die zuletzt angezeigte Datei (und nicht älter als das konfigurierte Limit), erscheint
statt des Titelbildschirms ein Diagnose-Fenster:

* **Beschreibung** und **Fehlerzeile** aus dem Report
* **Erkannte Probleme** mit konkreten Lösungshinweisen (Registry-/Reload-Konflikte,
  Litematica/WorldSchematic, Embeddium-Flüssigkeiten, Block-Update-Konflikte, RAM,
  fehlende Abhängigkeiten, Mixin-Konflikte, Grafiktreiber, doppelte Mods …)
* **Verdächtige Mods**, aufgelöst über die Mod-Liste
* Buttons: **Report-Ordner** öffnen, **Diagnose kopieren**, **Schliessen**

Jeder Report wird nur einmal angezeigt. Es läuft dabei nichts während des Crashes selbst –
die Auswertung passiert erst beim nächsten Start.

### Konfiguration

Beim ersten Start wird `config/creachreportfix-client.properties` angelegt:

```properties
# Creach Report Fix - Crash Assistant
crash-screen-enabled=true
crash-report-max-age-days=30
```

| Schlüssel | Bedeutung |
| --- | --- |
| `crash-screen-enabled` | Fenster nach einem Crash anzeigen (`true`/`false`) |
| `crash-report-max-age-days` | Reports, die älter sind, werden ignoriert |

## Was wurde aus den Crash-Reports abgeleitet

Grundlage sind 58 eingesandte Client-Crash-Reports aus der Wolf-Instanz (Forge 1.19.2, 43.3.0):

| Anzahl | Ursache | Status |
| --- | --- | --- |
| 45 | `ConcurrentModificationException` in `ModelBakery.<init>` beim Item-Registry-KeySet | **behoben** (ModelBakery-Guard) |
| 6 | `ConcurrentModificationException` in MrCrayfish's Gun Mod (`MetaLoader`) | **behoben** (Registry-Snapshot) |
| 4 | `ClassCastException` WorldSchematic → ClientLevel (Litematica/Forgematica) | erkannt, Hinweis im Crash-Fenster |
| 2 | `NoSuchElementException` im Block-Update (`CollectingNeighborUpdater`) | erkannt, Hinweis im Crash-Fenster |
| 1 | `NullPointerException` im Chunk-Mesh (Embeddium-Fluid-Cache) | erkannt, Hinweis im Crash-Fenster |

## Installation

1. Minecraft **Forge 1.19.2** (Version 43.x) installieren.
2. Die JAR aus [`releases/`](releases) in den Ordner `.minecraft/mods` legen.
3. Fertig – der Mod muss nur auf dem Client installiert sein.

Beim Aktualisieren die alte JAR löschen, damit nur eine Version im `mods`-Ordner liegt.

## Selbst bauen

Voraussetzungen: **JDK 17** und Internetzugang für den ersten Build.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Das fertige Mod-JAR liegt danach unter `build/libs/creach-report-fix-1.2.0.jar`.

Zum Testen in einer Entwicklungsumgebung:

```bash
gradlew.bat runClient
```

## Projektstruktur

```
.
├── build.gradle                  Gradle-/Forge-Konfiguration
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/               Gradle Wrapper (Gradle 7.6, von ForgeGradle 5 benötigt)
├── releases/                     Fertige, spielbare Mod-JARs
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

## Hinweise

* Alle Mixins sind auf `require = 0` gesetzt: Fehlt eine Zielklasse (z. B. weil FramedBlocks
  oder die Gun Mod nicht installiert ist), wird der jeweilige Guard einfach übersprungen –
  der Mod crasht nicht.
* Der Mod ist **clientseitig**; auf einem Server ist er weder nötig noch wirksam.
* Bereits gesetzte JVM-Properties und ModernFix-Einstellungen werden nie überschrieben.
* `ClientPacketListenerMixin.java` ist vorhanden, aber **nicht** in `creachreportfix.mixins.json`
  registriert: Es lehnt Server-Resource-Packs ab und könnte auf Servern zum Kick führen.
  Wer es will, trägt es dort in der `client`-Liste ein.

## Lizenz

[MIT](LICENSE)
