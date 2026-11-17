# Changelog

Alle wichtigen Änderungen an diesem Projekt. Format orientiert sich an
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[Semantic Versioning](https://semver.org/lang/de/).

## [1.2.0] – 2026-11-17

Diese Version behebt die beiden häufigsten Crash-Ursachen aus den eingesandten Reports
endgültig. Der entscheidende Fehler der Vorgänger war, dass nur *ein* Mixin registriert war
und die ModelBakery-/Gun-Mod-Guards dadurch nie aktiv wurden.

### Behoben

* `ConcurrentModificationException` in `ModelBakery.<init>` beim Iterieren des
  Item-Registry-KeySets (45 von 58 Reports). `ModelBakeryMixin` iteriert jetzt eine stabile
  Kopie; `RegistrySnapshots` erstellt sie mit bis zu 40 Versuchen und Backoff
  (`LockSupport.parkNanos`), falls die Registry gerade umgebaut wird.
* `ConcurrentModificationException` in MrCrayfish's Gun Mod (`MetaLoader.getResourceSuppliers`,
  6 Reports). `CgmMetaLoaderMixin` leitet `IForgeRegistry.getValues()` auf einen Snapshot um.
* `NullPointerException` in FramedBlocks, wenn `baseModel` noch nicht gesetzt ist. Die Mixins
  greifen jetzt über `support/FieldLookup` (Reflection) zu, statt Felder per `@Shadow` zu
  deklarieren – dadurch startet der Mod auch ohne FramedBlocks.
* Hänger/Standbild durch das Lade-Overlay bei Server-Resource-Packs
  (`LoadingOverlayMixin`, SRG-Felder `f_96163_`–`f_96165_`).
* Debug-Tasten-Abfang (`KeyboardHandlerMixin`) korrigiert auf `m_90932_`.

### Hinzugefügt

* **Crash Assistant**: Fenster nach einem Crash mit Beschreibung, Fehlerzeile, erkannten
  Problemen, Lösungshinweisen und verdächtigen Mods. Erscheint beim nächsten Start.
* `CrashAnalyzer` mit Regeln für Registry-/Reload-Konflikte, Litematica/WorldSchematic,
  Embeddium-Flüssigkeiten, Block-Update-Konflikte, RAM, fehlende Abhängigkeiten,
  Mixin-Konflikte, Grafiktreiber und doppelte Mods.
* Konfiguration `config/creachreportfix-client.properties`
  (`crash-screen-enabled`, `crash-report-max-age-days`).
* Deutsche Doku, `CHANGELOG.md`, Prüfsumme zur JAR.

### Geändert

* `creachreportfix.mixins.json` registriert jetzt **alle** sieben Client-Mixins.
* Projektstruktur aufgeräumt: Quelltext unter `src/main/...`, fertige JARs unter `releases/`.
* `ClientPacketListenerMixin` bleibt bewusst unregistriert (Kick-Gefahr auf Servern).

### Entfernt

* Alte Sammelordner-Struktur (`cooldown deteckt/` inklusive Build-Artefakten).
* Veraltete `creach-report-fix-1.1.1-client.jar` aus `releases/`.

## [1.1.3] und älter

Nicht mehr dokumentiert. Die JARs liegen unter `releases/legacy/` und sind **nicht** zu
verwenden – dort fehlen die meisten Fixes, weil die Mixins nicht registriert waren.

[1.2.0]: https://github.com/zwerg061/Wolf-craches/compare/c68ed3d...48cfcdb
