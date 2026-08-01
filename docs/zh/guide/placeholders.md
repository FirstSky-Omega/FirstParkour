# PlaceholderAPI

安装 PlaceholderAPI 并重启服务器。变量扩展由 Infinite Parkour Reborn 自行注册，无需从 eCloud 另行下载。

## 方块跑酷：`witp`

| 变量 | 内容 |
| --- | --- |
| `%witp_version%` | 插件版本 |
| `%witp_leader%` | 默认模式纪录保持者 |
| `%witp_leader_score%` | 默认模式纪录分数 |
| `%witp_rank%` | 玩家默认模式排名 |
| `%witp_highscore%` | 玩家默认模式最高分 |
| `%witp_high_score_time%` | 最高分对应时间 |
| `%witp_score%` | 当前分数 |
| `%witp_time%` | 当前格式化用时 |
| `%witp_blocklead%` | 当前方块预生成数量 |
| `%witp_style%` | 当前风格键 |
| `%witp_time_preference%` | 玩家选择的世界时间 |
| `%witp_scoreboard%` | 是否显示计分板 |
| `%witp_difficulty%` | 模板难度数值 |
| `%witp_difficulty_string%` | 模板难度文字 |
| `%witp_score_until_100%` | 距离下一个 100 倍数还差多少分 |

别名包括 `%witp_ver%`、`%witp_record_player%`、`%witp_record_score%`、`%witp_record%`、`%witp_high_score%`、`%witp_current_score%`、`%witp_current_time%`、`%witp_lead%` 和 `%witp_time_pref%`。

### 排名变量

将 `<排名>` 替换为正整数：

```text
%witp_player_rank_<排名>%
%witp_score_rank_<排名>%
%witp_time_rank_<排名>%
%witp_difficulty_rank_<排名>%
%witp_difficulty_string_rank_<排名>%
```

在排名前加入模式键可查询指定模式：

```text
%witp_player_rank_speed_1%
%witp_score_rank_team_survival_3%
```

## 鞘翅跑酷：`iep`

以下变量要求玩家正在进行鞘翅跑酷：

| 变量 | 内容 |
| --- | --- |
| `%iep_score%` | 当前距离分数 |
| `%iep_time%` | 当前用时 |
| `%iep_seed%` | 当前种子 |
| `%iep_speed%` | 当前速度 |

排行榜格式为 `%iep_<模式>_<类型>_<排名>%`，类型可为 `name`、`score`、`time` 或 `seed`。

```text
%iep_default_name_1%
%iep_default_score_1%
```

请使用准确的内部模式键。名称含空格的模式并不适用于所有变量显示插件，上线前应在目标位置实际测试。
