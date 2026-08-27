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
- Developer dashboard and management tooling
- Chat, nickname, fly, and gamemode utilities

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| /group | badapple.group | Create, edit, and delete groups |
| /groupmenu | badapple.group | Open the group management menu |
| /manage | badapple.manage | Open the developer dashboard |
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

## Quick start

1. Place the plugin jar into your Spigot/Paper `plugins/` folder.
2. Start the server once so BadApple generates its default folders and config files.
3. Edit your YAML groups/tracks under `plugins/BadApple/groups` and `plugins/BadApple/tracks`.
4. Use `/groupmenu` or `/group` to manage permissions and assignments.
5. Use `/cosmetics`, `/vanish`, and `/tp` for staff and player utilities.

## Documentation

Read the built-in repo wiki at:

- `wiki/Home.md`
- `wiki/Configuration.md`
- `wiki/Command-Reference.md`
- `wiki/Features.md`

Online GitHub wiki (when published):

- https://github.com/CommanderCortex/BadApple/wiki

## Notes

This project is designed for servers that want a data-driven permissions model, simple staff tooling, and a menu-first management approach. The focus is on maintainability and easy user-editable YAML configuration rather than hardcoded values.
