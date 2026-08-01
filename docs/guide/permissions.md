# Permissions

Block-parkour and multiplayer permission checks are enabled with `permissions.enabled: true` in `plugins/IP/config.yml`. Elytra permissions are controlled separately by `permissions: true` in `plugins/IP/elytra/config.yml`. Administrative block-parkour commands always require `ip.admin`; elytra reset and schematic commands require operator status.

## Block parkour and menus

| Permission | Access |
| --- | --- |
| `ip.main` | Main menu |
| `ip.play` | Play menu |
| `ip.play.single` | Single-player selection |
| `ip.play.spectator` | Spectator selection |
| `ip.community` | Community menu |
| `ip.community.leaderboards` | Leaderboards |
| `ip.settings` | Settings menu |
| `ip.settings.parkour_settings` | Parkour settings submenu |
| `ip.settings.styles` | Style setting |
| `ip.settings.leads` | Block-lead setting |
| `ip.settings.time` | Time setting |
| `ip.settings.schematics` | Schematic difficulty setting |
| `ip.settings.show_scoreboard` | Scoreboard toggle |
| `ip.settings.fall_message` | Fall-message toggle |
| `ip.settings.particles` | Particle toggle |
| `ip.settings.sound` | Sound toggle |
| `ip.settings.special_blocks` | Special-block toggle |
| `ip.settings.lang` | Language setting |
| `ip.settings.chat` | Chat setting |
| `ip.lobby` | Lobby menu |
| `ip.lobby.visibility` | Player visibility |
| `ip.lobby.player_management` | Player management |
| `ip.join` | Join command |
| `ip.quit` | Quit option and leave command |
| `ip.admin` | Reload, reset, force, recovery, and schematic tools |

## Multiplayer

| Permission | Access |
| --- | --- |
| `ip.multiplayer` | Create multiplayer sessions |
| `ip.active` | View active lobbies |
| `ip.settings.practice_settings` | Practice settings |
| `ip.invite` | Invitation menu |

When `permissions.enabled` is true, each mode also uses `ip.gamemode.<mode>`, for example `ip.gamemode.speed` or `ip.gamemode.team_survival`.

When `permissions.per-style` is true, styles use `ip.settings.styles.<style>`. Spaces in a style name become dots; for example, `light blue` becomes `ip.settings.styles.light.blue`.

## Elytra

| Permission | Access |
| --- | --- |
| `iep.play` | Play menu |
| `iep.leaderboard` | Leaderboard menu |
| `iep.setting` | Settings menu |
| `iep.leave` | Leave command |
| `iep.setting.seed` | Seed command |
| `iep.setting.style.<style>` | A specific elytra style |
| `iep.leaderboard.<mode>` | A specific mode's leaderboard |
| `iep.setting.<setting>` | A specific setting entry |

Mode and setting names match the keys used by the plugin, such as `min speed`, `time trial`, `radius`, or `metric`.
