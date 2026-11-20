# BadApple

BadApple is a configurable Spigot/Paper plugin for Minecraft server staff management, permissions, rank groups, cosmetics, and utility tools. It is designed around YAML-driven configuration so admins can change behavior without recompiling the plugin.

## Features

- Config-driven rank groups and permission nodes
- Group inheritance with recursive permission resolution
- Weight-based tab sorting and staff hierarchy support
- Track system for rank progression
- Per-player group assignment and custom suffixes
- Modern in-game GUI management for groups and players
- Cosmetics system with trails, particles, and wings
- Vanish support with spectator mode visibility handling
- Staff-only teleport system and player teleport requests
- Authenticated API for external server management
- Chat, nickname, fly, and gamemode utilities
- Chunk pre-generation GUI with persistent progress, pause/resume/cancel controls, radius settings, and memory-retention options
- Geyser/Floodgate Bedrock client detection and compatibility
- Velocity server-network integration with an in-game server selector

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| /group | badapple.group | Create, edit, and delete groups |
| /groupmenu | badapple.group | Open the group management menu |
| /api | badapple.api | Generate, rotate, revoke, or inspect a website API key |
| /punishmenu | badapple.punish | Open punishment actions |
| /vanish | badapple.vanish | Toggle staff vanish mode |
| /broadcast | badapple.broadcast | Send a server-wide announcement |
| /fly | badapple.fly | Toggle flight |
| /gamemode | badapple.gamemode | Change a player gamemode |
| /nickname | badapple.nickname | Set or clear a nickname |
| /cosmetics | badapple.cosmetics | Open the cosmetics menu |
| /tp | badapple.tp | Open the staff teleport menu |
| /tpa | none | Request a teleport to staff |
| /tpaccept | badapple.tp | Accept a teleport request |
| /tpdeny | badapple.tp | Deny a teleport request |
| /compass | badapple.tp | Open the Velocity server selector in the lobby |
| /chunkmenu | badapple.chunkpregeneration | Open the chunk pre-generation GUI |
| /chunkpregeneration | badapple.chunkpregeneration | Show or control chunk pre-generation status |
| /badapplehelp | none | Show command help and docs link |

## Default folder layout

After the plugin starts, BadApple creates these folders under the server plugins directory:

- `plugins/BadApple/`
  - `config.yml`
  - `groups/`
  - `tracks/`
  - `players/`
  - `config/`

## Config and defaults

BadApple keeps rank data user-editable in YAML. The default groups and tracks are created automatically on startup so server owners can modify them directly without editing Java code.

Example config concepts:

- `/plugins/BadApple/groups/` holds each rank as a YAML file
- `/plugins/BadApple/tracks/` holds progression track definitions
- `/plugins/BadApple/config/` stores plugin config overrides and server defaults
- `/plugins/BadApple/players/` stores per-player metadata for groups, suffixes, and cosmetics

## Geyser and Bedrock support

BadApple supports Bedrock-compatible servers that use Geyser or Floodgate. It detects `Geyser-Spigot`, `Geyser`, or `Floodgate` on startup and allows their Bedrock login handling to proceed normally rather than rejecting the connection.

Recommended setup:

- Install Geyser-Floodgate or Geyser-Spigot on the backend Paper server.
- Keep Bedrock and Java Edition clients on compatible Minecraft protocol versions.
- Configure Geyser/Floodgate according to their official documentation.
- Restart the server after installing or changing the Bedrock bridge.

BadApple does not implement the Bedrock protocol itself. A Geyser/Floodgate plugin or another Bedrock gateway must be installed for Bedrock clients to connect.

## Velocity proxy integration

Velocity support enables the lobby server selector and `/compass` command. Configure the proxy type and optional backend name in `plugins/BadApple/config.yml`:

```yaml
proxy:
  type: "velocity"

server:
  name: "lobby"
```

Set `server.name` to `smp` on the Survival backend to prevent the server selector compass from being given there. On lobby servers, players receive the compass in the hotbar and can use it to open the server-selection GUI.

Integration behavior:

- `/compass` is registered only when Velocity integration is enabled.
- The plugin uses the `BungeeCord` plugin-message channel to send server-switch requests.
- Configure server names in your Velocity configuration and make sure they match the names used by the server selector.
- Install BadApple on each backend that uses BadApple features; the proxy and backend configurations do not share plugin files.

## Chunk pre-generation

BadApple includes a lightweight chunk pre-generation task manager for preparing world storage and warming chunk caches.

- Open `/chunkmenu` for radius, start, pause, resume, cancel, and memory-retention controls.
- Start generation from the player's current chunk location.
- Use `/chunkpregeneration status|pause|resume|cancel` for direct command control.
- Tasks persist in `plugins/BadApple/pregeneration.yml` and resume when enabled after restart.
- Choose whether generated chunks remain loaded in server memory or are unloaded so normal Paper storage caching applies.

Configure limits and throughput in `plugins/BadApple/config.yml`:

```yaml
pregeneration:
  default-radius: 8
  max-radius: 2048
  chunks-per-tick: 2
```

Keep throughput modest on production servers. Larger radius values can take substantial time and disk space, especially before the world has existing data.

## Website management API

The old embedded developer dashboard has been removed. BadApple now exposes an authenticated, plugin-scoped API for the Discord-authenticated website backend.

1. Keep the listener bound to `127.0.0.1` and put it behind an HTTPS reverse proxy. Do not expose the plugin's plain HTTP listener directly to the internet. The default port is `8787`; if another process already uses it, set `api.port` in `plugins/BadApple/config.yml` to an available port. BadApple will continue loading if the listener cannot bind, but the API will be unavailable until the conflict is resolved.
2. Set `api.public-url` in `plugins/BadApple/config.yml` to the HTTPS base URL configured in your reverse proxy (for example, `https://mc-api.example.com`, without a trailing slash).
3. In game, run `/api` (or `/api generate`) as an operator. It displays copyable API key and URL fields; the key is shown once. `/api rotate`, `/api revoke`, and `/api status` manage the key and show the URL.
4. In your monitoring website, provide the HTTPS API URL and key. For a server health check, the endpoint is `<api.public-url>/v1/server`. The website backend encrypts credentials at rest; the key is never saved in browser storage.

The website backend requires `SERVER_CREDENTIALS_KEY`, a unique 32-byte key encoded as 64 hex characters. Generate one with `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"` and keep it private in the backend environment.

Available plugin API operations include server status, online players, groups, tracks, group creation/deletion/prefix changes, player rank assignment, and track promotion/demotion. Arbitrary console commands are not exposed. API calls require a valid bearer key and are logged by the plugin.

For automated provisioning, a hosting provisioner can generate a key before the Minecraft server starts, place it in `api.provisioning-key` in the plugin config, and save that same key to the website backend over HTTPS. On first startup BadApple stores only its hash and clears the config value. The current website server code supports secure linking, but automatic injection must be wired into the actual server-provisioning process.

The website's authenticated backend exposes `GET/POST /api/minecraft/servers`, `DELETE /api/minecraft/servers/:id`, `GET /api/minecraft/servers/:id/{server|players|groups|tracks}`, and `POST /api/minecraft/servers/:id/actions`. Action names are allowlisted (`create-group`, `delete-group`, `set-prefix`, `set-player-group`, `promote`, `demote`). These routes are restricted to Discord IDs listed in `DISCORD_ADMIN_USER_IDS`; each user's links are isolated to their Discord account.

## Compatibility and support

- Primary platform: Paper 26.3
- Java: Java 25
- Java Edition: Spigot/Paper-compatible server APIs
- Proxy: Velocity server selection and server-switch plugin messaging
- Bedrock: Geyser and Floodgate detection, with an external Bedrock gateway required
- Storage: YAML files under `plugins/BadApple/`

If a feature does not appear, verify that the required plugin or proxy is running and review the server console for BadApple startup messages.

## Quick start

1. Place the plugin jar into your Spigot/Paper `plugins/` folder.
2. Start the server once so BadApple generates its default folders and config files.
3. Edit your YAML groups/tracks under `plugins/BadApple/groups` and `plugins/BadApple/tracks`.
4. Use `/groupmenu` or `/group` to manage permissions and assignments.
5. Use `/cosmetics`, `/vanish`, and `/tp` for staff and player utilities.
6. Use `/chunkmenu` to pre-load a world around your current chunk.
7. For Bedrock support, install Geyser/Floodgate and restart the server.
8. For a Velocity network, set `proxy.type: "velocity"` and configure matching server names.

## Documentation

Read the built-in repo wiki at:

- `wiki/Home.md`
- `wiki/Configuration.md`
- `wiki/Command-Reference.md`
- `wiki/Features.md`

GitHub project:

- Repository: https://github.com/Krumpy1337/BadApple
- Wiki: https://github.com/Krumpy1337/BadApple/wiki

Maintainer: [Krumpy1337](https://github.com/Krumpy1337)

## Notes

This project is designed for servers that want a data-driven permissions model, simple staff tooling, and a menu-first management approach. The focus is on maintainability and easy user-editable YAML configuration rather than hardcoded values.
