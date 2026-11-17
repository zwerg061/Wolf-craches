# Releases

| Datei | Beschreibung |
| --- | --- |
| `creach-report-fix-1.2.0-client.jar` | **Diese Datei installieren.** Enthält alle aktiven Fixes und den Crash Assistant. |
| `wolf-crash-fix-legacy.jar` | Sehr alte Version (nur als Referenz). **Nicht verwenden** – dort sind die meisten Fixes nicht registriert. |

## Installation

1. Minecraft Forge **1.19.2** (43.x) installieren.
2. Alte Versionen aus `.minecraft/mods` löschen (z. B. `creach-report-fix-1.1.x`).
3. `creach-report-fix-1.2.0-client.jar` nach `.minecraft/mods` kopieren.
4. Fertig – der Mod ist rein clientseitig.

## Was 1.2.0 behebt

* `ConcurrentModificationException` in `ModelBakery.<init>` (der häufigste Crash in den
  eingesandten Reports) – die Item-Registry wird jetzt als stabile Kopie iteriert.
* `ConcurrentModificationException` in MrCrayfish's Gun Mod beim Meta-Laden.
* `NullPointerException` in FramedBlocks, wenn `baseModel` noch nicht gesetzt ist.
* Hänger/Standbild durch das Lade-Overlay bei Server-Resource-Packs.
* Absturz beim manuellen Ressourcen-Reload (F3+T / F3+S).

## Neu in 1.2.0

* **Crash Assistant:** Nach einem Crash erscheint beim nächsten Start ein Fenster mit
  Beschreibung, erkannten Problemen, Lösungshinweisen und verdächtigen Mods.
* Optionen in `config/creachreportfix-client.properties`.

## Hinweis

Die JAR wird mit

```
gradlew.bat build
```

erzeugt. Das Ergebnis liegt danach unter `../build/libs/`.
