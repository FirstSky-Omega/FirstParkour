# Developer API

The Java API exposes player/session lookup, registries, and Bukkit events. Add Infinite Parkour Reborn as a compile-only/provided dependency and declare it as a dependency or soft dependency in your plugin metadata.

## JitPack dependency

Replace `VERSION` with a Git tag or commit available from the repository.

::: code-group

```xml [Maven]
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.LostUmbrella58</groupId>
  <artifactId>IP-Reborn</artifactId>
  <version>VERSION</version>
  <scope>provided</scope>
</dependency>
```

```kotlin [Gradle Kotlin DSL]
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.LostUmbrella58:IP-Reborn:VERSION")
}
```

:::

## Player lookup

`ParkourAPI.getUser(player)` returns any active parkour participant, including supported user types. `ParkourAPI.getPlayer(player)` returns a block-parkour player or `null` when the player is not in that state.

```java
ParkourUser user = ParkourAPI.getUser(player);
if (user != null) {
    // The player is in an active parkour session.
}
```

Always handle a `null` result; players can leave between ticks.

## Bukkit events

Available events in `dev.efnilite.ip.api.event`:

- `ParkourJoinEvent`
- `ParkourLeaveEvent`
- `ParkourFallEvent`
- `ParkourScoreEvent`
- `ParkourBlockGenerateEvent`
- `ParkourSchematicGenerateEvent`
- `ParkourSpectateEvent`

```java
@EventHandler
public void onScore(ParkourScoreEvent event) {
    String playerName = event.player.getName();
    getLogger().info(playerName + " scored a parkour point");
}
```

These events are informational. Do not perform blocking database or network work on the server thread.

## Registry

`Registry.getModes()` and `Registry.getStyles()` return the registered entries. `Registry.getMode(name)` and `Registry.getStyle(name)` perform exact-name lookups and may return `null`.

The API may evolve between releases. Compile and test against the same release deployed on the server.
