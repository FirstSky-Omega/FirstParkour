# 插件联动

所有联动均为可选。正常安装对应插件并重启服务器后，Infinite Parkour Reborn 会在启动时检测它们。

| 插件 | 功能 |
| --- | --- |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | `%witp_*%` 和 `%iep_*%` 变量 |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | 通过已安装的经济插件发放奖励 |
| Multiverse-Core | 多世界管理兼容 |
| HolographicDisplays | 全息文字支持 |
| floodgate | 配合兼容的 Geyser 环境处理基岩版玩家 |
| Chunky | 可用时启用区块管理联动 |

## MySQL

MySQL 为内置功能，不需要额外联动插件。方块跑酷使用 `plugins/IP/config.yml` 的 `sql`，鞘翅跑酷使用 `plugins/IP/elytra/config.yml` 的 `mysql`。

启用前请创建数据库和有权访问它的账号，填写正确地址、凭据及可选表前缀。首次迁移时先关闭所有写入同一数据库的服务器，只启动一台并检查控制台，再启动其他服务器。

## 代理模式

核心配置中的 `bungeecord` 同时适用于 BungeeCord 风格网络和 Velocity。鞘翅部分另有 `proxy` 设置。请确认返回服务器确实存在，并且代理允许插件消息传递。
