# 游戏模式

## 方块跑酷

默认玩法会持续生成普通方块、冰块、台阶、栅栏、玻璃板和已配置的建筑模板。玩家可按服务器开放的项目调整方块预生成数量、风格、时间、模板难度、计分板、粒子、音效和特殊方块。

主菜单还可以旁观正在进行的会话。

## 多人与扩展模式

执行 `/ipp create` 选择已启用的模式。模式开关位于 `plugins/IP/plus/config.yml`。

| 配置键 | 模式 |
| --- | --- |
| `hourglass` | 沙漏 |
| `lobby` | 大厅 |
| `practice` | 练习 |
| `speed` | 竞速 |
| `super_jump` | 超级跳跃 |
| `time_trial` | 计时试炼 |
| `wave_trial` | 波次试炼 |
| `duels` | 决斗 |
| `team_survival` | 团队生存 |

计时试炼、波次试炼、决斗和团队生存还提供目标分数或人数上限设置。

## 鞘翅跑酷

执行 `/iep play` 可选择 Default、Close、Min Speed、Obstacle、Speed Demon 和 Time Trial。除默认模式外，其余模式可在 `plugins/IP/elytra/config.yml` 的 `mode-settings` 中启用或关闭。

玩家可调整风格、管道半径、世界时间、种子、语言、坠落行为、信息显示和公制单位。
