# 权限

在 `plugins/IP/config.yml` 中设置 `permissions.enabled: true` 后，方块跑酷和多人模式才会检查普通权限。鞘翅权限由 `plugins/IP/elytra/config.yml` 的 `permissions: true` 单独控制。方块跑酷管理命令始终需要 `ip.admin`；鞘翅重置和模板命令需要 OP。

## 方块跑酷与菜单

| 权限 | 功能 |
| --- | --- |
| `ip.main` | 主菜单 |
| `ip.play` | 游玩菜单 |
| `ip.play.single` | 单人模式选择 |
| `ip.play.spectator` | 旁观模式选择 |
| `ip.community` | 社区菜单 |
| `ip.community.leaderboards` | 排行榜 |
| `ip.settings` | 设置菜单 |
| `ip.settings.parkour_settings` | 跑酷设置子菜单 |
| `ip.settings.styles` | 风格设置 |
| `ip.settings.leads` | 方块预生成数量 |
| `ip.settings.time` | 时间设置 |
| `ip.settings.schematics` | 模板难度 |
| `ip.settings.show_scoreboard` | 计分板开关 |
| `ip.settings.fall_message` | 掉落消息开关 |
| `ip.settings.particles` | 粒子开关 |
| `ip.settings.sound` | 音效开关 |
| `ip.settings.special_blocks` | 特殊方块开关 |
| `ip.settings.lang` | 语言设置 |
| `ip.settings.chat` | 聊天设置 |
| `ip.lobby` | 大厅菜单 |
| `ip.lobby.visibility` | 玩家可见性 |
| `ip.lobby.player_management` | 玩家管理 |
| `ip.join` | 加入命令 |
| `ip.quit` | 退出按钮与离开命令 |
| `ip.admin` | 重载、重置、强制操作、背包恢复和模板工具 |

## 多人模式

| 权限 | 功能 |
| --- | --- |
| `ip.multiplayer` | 创建多人会话 |
| `ip.active` | 查看活跃房间 |
| `ip.settings.practice_settings` | 练习模式设置 |
| `ip.invite` | 邀请菜单 |

启用权限检查后，每个模式还会使用 `ip.gamemode.<模式>`，例如 `ip.gamemode.speed` 和 `ip.gamemode.team_survival`。

启用 `permissions.per-style` 后，风格使用 `ip.settings.styles.<风格>`。风格名中的空格会变成点，例如 `light blue` 对应 `ip.settings.styles.light.blue`。

## 鞘翅跑酷

| 权限 | 功能 |
| --- | --- |
| `iep.play` | 游玩菜单 |
| `iep.leaderboard` | 排行榜菜单 |
| `iep.setting` | 设置菜单 |
| `iep.leave` | 离开命令 |
| `iep.setting.seed` | 种子命令 |
| `iep.setting.style.<风格>` | 指定风格 |
| `iep.leaderboard.<模式>` | 指定模式排行榜 |
| `iep.setting.<设置>` | 指定设置项 |

模式和设置名使用插件内的键，例如 `min speed`、`time trial`、`radius` 或 `metric`。
