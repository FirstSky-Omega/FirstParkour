# Integrations

All integrations are optional. Install their plugin jars normally and restart the server; Infinite Parkour Reborn detects them at startup.

| Plugin | Integration |
| --- | --- |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | `%witp_*%` and `%iep_*%` placeholders |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy rewards through an installed Vault economy provider |
| Multiverse-Core | Compatibility with managed worlds |
| HolographicDisplays | Hologram support |
| floodgate | Bedrock-player handling alongside a compatible Geyser setup |
| Chunky | Chunk-management integration when available |

## MySQL

MySQL is built in and does not require an integration plugin. Block parkour uses the `sql` section in `plugins/IP/config.yml`; elytra parkour uses the `mysql` section in `plugins/IP/elytra/config.yml`.

Before enabling either connection:

1. Create the database and a user with access to it.
2. Enter the correct address, credentials, and optional table prefix.
3. Stop all servers that write to the same data while performing the first migration.
4. Start one server, check the console, then start the remaining servers.

## Proxy mode

The `bungeecord` section in the core config applies to BungeeCord-style and Velocity networks. Elytra parkour has a separate `proxy` section. Make sure the configured return server exists and that plugin messaging is available through the proxy.
