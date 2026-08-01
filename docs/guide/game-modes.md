# Game modes

## Block parkour

The default experience generates a continuing path of normal blocks, ice, slabs, fences, glass panes, and configured schematics. Players can change enabled settings such as block lead, style, time, schematic difficulty, scoreboard, particles, sound, and special blocks.

The main play menu also provides spectator access to active sessions.

## Multiplayer and alternative modes

Open `/ipp create` to choose an enabled mode. Modes are controlled in `plugins/IP/plus/config.yml`.

| Configuration key | Mode |
| --- | --- |
| `hourglass` | Hourglass |
| `lobby` | Lobby |
| `practice` | Practice |
| `speed` | Speed |
| `super_jump` | Super Jump |
| `time_trial` | Time Trial |
| `wave_trial` | Wave Trial |
| `duels` | Duels |
| `team_survival` | Team Survival |

Time Trial, Wave Trial, Duels, and Team Survival have goal or player-limit settings. Disabling a mode removes it from the available selection after the configuration is reloaded.

## Elytra parkour

Open `/iep play` to select an elytra mode:

- Default
- Close
- Min Speed
- Obstacle
- Speed Demon
- Time Trial

The optional modes can be enabled or disabled under `mode-settings` in `plugins/IP/elytra/config.yml`. Player settings include style, pipe radius, world time, seed, locale, fall behavior, information display, and metric units.
