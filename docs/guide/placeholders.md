# PlaceholderAPI

Install PlaceholderAPI and restart the server. The expansions are registered by Infinite Parkour Reborn; no separate eCloud download is required.

## Block parkour: `witp`

### General and player values

| Placeholder | Value |
| --- | --- |
| `%witp_version%` | Plugin version |
| `%witp_leader%` | Default-mode record holder |
| `%witp_leader_score%` | Default-mode record score |
| `%witp_rank%` | Player's default-mode rank |
| `%witp_highscore%` | Player's default-mode high score |
| `%witp_high_score_time%` | Time attached to that high score |
| `%witp_score%` | Current score |
| `%witp_time%` | Current formatted run time |
| `%witp_blocklead%` | Current block lead |
| `%witp_style%` | Current style key |
| `%witp_time_preference%` | Selected world time |
| `%witp_scoreboard%` | Whether the scoreboard is enabled |
| `%witp_difficulty%` | Numeric schematic difficulty |
| `%witp_difficulty_string%` | Easy, medium, hard, or very hard |
| `%witp_score_until_100%` | Points remaining until the next multiple of 100 |

Aliases include `%witp_ver%`, `%witp_record_player%`, `%witp_record_score%`, `%witp_record%`, `%witp_high_score%`, `%witp_current_score%`, `%witp_current_time%`, `%witp_lead%`, and `%witp_time_pref%`.

### Ranked values

Replace `<rank>` with a positive position:

```text
%witp_player_rank_<rank>%
%witp_score_rank_<rank>%
%witp_time_rank_<rank>%
%witp_difficulty_rank_<rank>%
%witp_difficulty_string_rank_<rank>%
```

Insert a mode key before the rank for a mode-specific leaderboard. For example:

```text
%witp_player_rank_speed_1%
%witp_score_rank_team_survival_3%
```

## Elytra parkour: `iep`

The player must be in an elytra run for these values:

| Placeholder | Value |
| --- | --- |
| `%iep_score%` | Current distance score |
| `%iep_time%` | Current elapsed time |
| `%iep_seed%` | Current seed |
| `%iep_speed%` | Current speed |

Leaderboard placeholders use `%iep_<mode>_<type>_<rank>%`, where type is `name`, `score`, `time`, or `seed`.

```text
%iep_default_name_1%
%iep_default_score_1%
```

Use the exact internal mode key. Modes containing spaces are not suitable for every placeholder-consuming plugin, so test them in the target display before deployment.
