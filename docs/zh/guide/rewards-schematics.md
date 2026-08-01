# 奖励与建筑模板

## 方块跑酷奖励

在 `plugins/IP/rewards-v2.yml` 中启用奖励：

```yaml
enabled: true

score-rewards:
  100:
    - "send:<green>你达到了 100 分！"
    - "give %player% diamond 1"

interval-rewards:
  25:
    - "vault:10"

one-time-rewards:
  1:
    - "send:第一次跳跃完成！"
```

每行代表一条奖励：

| 格式 | 效果 |
| --- | --- |
| `命令 %player%` | 以控制台执行命令，不加 `/` |
| `leave:命令` | 玩家离开时执行命令 |
| `send:<消息>` | 发送 MiniMessage 消息 |
| `vault:<金额>` | 通过 Vault 存入或扣除金额 |
| `<模式>:<奖励>` | 只在指定模式发放，例如 `speed` |

分数 0 不生效。一次性奖励按分数记录，仅修改奖励命令不会让已领取玩家再次获得。

## 鞘翅奖励

在 `plugins/IP/elytra/rewards.yml` 中启用奖励。每项由四个 `||` 分隔字段组成：

```text
<执行时间>||<模式>||<命令类型>||<值>
```

- 执行时间：`now` 或 `leave`。
- 模式：`all`、`default`、`close`、`min speed`、`obstacle`、`speed demon` 或 `time trial`。
- 命令类型：`vault`、`console command`、`player command` 或 `send`。
- 值：金额、命令或消息；`%player%` 会替换为玩家名。

```yaml
enabled: true
interval:
  100:
    - "now||all||vault||10"
    - "leave||time trial||send||<green>做得好！"
```

## 方块跑酷模板

1. 执行 `/ip schematic wand`，或用 `/ip schematic pos1` 与 `/ip schematic pos2` 设置两个角。
2. 执行 `/ip schematic save`。
3. 记录聊天中显示的随机代码。
4. 将代码和难度写入 `plugins/IP/schematics/schematics.yml`。
5. 重载或重启后测试模板。

```yaml
difficulty:
  a1b2c3d4: 0.5
```

| 数值 | 难度 |
| --- | --- |
| 0–0.25 | 简单 |
| 大于 0.25–0.5 | 中等 |
| 大于 0.5–0.75 | 困难 |
| 大于 0.75–1.0 | 非常困难 |

可用 `/ip schematic paste <文件>` 在当前位置预览已加载的模板。

## 鞘翅模板

OP 可执行 `/iep schematic <x,y,z> <x,y,z>` 保存长方体区域。生成的 UUID 即 `plugins/IP/elytra/schematics/` 下的文件名。
