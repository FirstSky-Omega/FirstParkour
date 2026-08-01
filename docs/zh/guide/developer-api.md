# 开发者 API

Java API 提供玩家/会话查询、注册表和 Bukkit 事件。将 Infinite Parkour Reborn 作为 compile-only/provided 依赖，并在你的插件元数据中声明依赖或软依赖。

## JitPack 依赖

将 `VERSION` 替换为仓库中存在的 Git 标签或提交。

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

## 玩家查询

`ParkourAPI.getUser(player)` 返回正在参与跑酷的用户；`ParkourAPI.getPlayer(player)` 返回方块跑酷玩家，不符合状态时返回 `null`。

```java
ParkourUser user = ParkourAPI.getUser(player);
if (user != null) {
    // 玩家当前处于跑酷会话中。
}
```

始终处理 `null`，因为玩家可能在两个 tick 之间离开。

## Bukkit 事件

`dev.efnilite.ip.api.event` 中提供：

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

这些事件用于通知状态。不要在服务器线程中执行阻塞的数据库或网络操作。

## 注册表

`Registry.getModes()` 与 `Registry.getStyles()` 返回已注册内容；`Registry.getMode(name)` 与 `Registry.getStyle(name)` 按准确名称查询，并可能返回 `null`。

API 可能随版本变化。请使用与服务器部署版本相同的构件编译并测试。
