# IP 5.4.0-paper26

Paper 26.1.2 port by Creeper_可能c. Drop-in replacement for IP 5.3.x on a Paper 26.1.2
server running Java 25.

## Compatibility

- **Requires Paper 26.1.2** (MC 1.21.11). Older Paper/Spigot/Folia builds refuse to load.
- **Requires Java 25** (Paper 26's own minimum).

## What changed

- pom: paper-api bumped to `26.1.2.build.72-stable`; compiler release set to 25; shade
  plugin bumped to 3.6.2 (3.6.0 chokes on Java 25 class files); the unresolvable
  `com.sb:vilib:2.1.5` jitpack dep replaced with `dev.efnilite:vilib:2.1.0-paper26` from
  the local Maven install of the bundled `D:/MC/temp/vilib` source.
- plugin.yml: dropped the `VoidGen` softdepend — vilib ships its own ChunkGenerator and
  was never actually using the external plugin. Bumped `api-version` to `'1.21'`.
- Source:
  - `PaperLib.teleportAsync(...)` → native `player.teleportAsync(...)` everywhere.
  - `player.kickPlayer(String)` → `player.kick(Component.text(...))`.
  - `ItemMeta#getDisplayName()` (returns Component in 26) handled via the new
    `isSchematicWand` helper that uses `displayName()` + `PlainTextComponentSerializer`.
  - `Particle.SPELL_INSTANT` (removed in 1.21) → `INSTANT_EFFECT` with a fallback chain
    so the plugin still loads after future enum churn.
  - `TextComponent.fromLegacyText(...)` (Bungee Chat removed in 26) → `MiniMessage`.
  - `config.yml` default particle bumped to `INSTANT_EFFECT`.

## Build

```
cd D:/MC/temp/vilib    && .\mvnw.cmd -B clean install -DskipTests
cd D:/MC/temp/IP       && .\mvnw.cmd -B clean install -DskipTests
cd D:/MC/temp/ipp-main && .\mvnw.cmd -B clean package -DskipTests
```

Output: `target/IP-5.4.0-paper26.jar`.
