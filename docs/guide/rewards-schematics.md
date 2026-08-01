# Rewards and schematics

## Block-parkour rewards

Enable rewards in `plugins/IP/rewards-v2.yml`:

```yaml
enabled: true

score-rewards:
  100:
    - "send:<green>You reached 100 points!"
    - "give %player% diamond 1"

interval-rewards:
  25:
    - "vault:10"

one-time-rewards:
  1:
    - "send:You made your first jump!"
```

Each line is one reward. Available forms are:

| Form | Effect |
| --- | --- |
| `command %player%` | Run a console command without a leading slash |
| `leave:command` | Run the command when the player leaves |
| `send:<message>` | Send a MiniMessage-formatted message |
| `vault:<amount>` | Deposit or withdraw money through Vault |
| `<mode>:<reward>` | Restrict the reward to a mode such as `speed` or `team_survival` |

Scores of zero are ignored. One-time rewards are recorded by score and are not issued again merely because their commands change.

## Elytra rewards

Enable `plugins/IP/elytra/rewards.yml`. Every entry uses four `||`-separated fields:

```text
<time>||<mode>||<command>||<value>
```

- Time: `now` or `leave`.
- Mode: `all`, `default`, `close`, `min speed`, `obstacle`, `speed demon`, or `time trial`.
- Command: `vault`, `console command`, `player command`, or `send`.
- Value: the amount, command, or message; `%player%` inserts the player name.

Example:

```yaml
enabled: true
interval:
  100:
    - "now||all||vault||10"
    - "leave||time trial||send||<green>Good job!"
```

## Block-parkour schematics

1. Use `/ip schematic wand`, or set both corners with `/ip schematic pos1` and `/ip schematic pos2`.
2. Run `/ip schematic save`.
3. Note the generated code shown in chat.
4. Add that code and a difficulty to `plugins/IP/schematics/schematics.yml`.
5. Reload or restart, then test the schematic.

```yaml
difficulty:
  a1b2c3d4: 0.5
```

Difficulty ranges are:

| Value | Label |
| --- | --- |
| 0–0.25 | Easy |
| above 0.25–0.5 | Medium |
| above 0.5–0.75 | Hard |
| above 0.75–1.0 | Very hard |

Use `/ip schematic paste <file>` to preview a loaded schematic at your position.

## Elytra schematics

Operators can save a cuboid with `/iep schematic <x,y,z> <x,y,z>`. The generated UUID is used as the filename under `plugins/IP/elytra/schematics/`.
