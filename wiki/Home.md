# BadApple Wiki

Welcome to the BadApple documentation. This wiki covers the plugin setup, configuration, permissions, command reference, and the staff and player tools that ship with the project.

- Repository: [github.com/Krumpy1337/BadApple](https://github.com/Krumpy1337/BadApple)
- Maintainer: [Krumpy1337](https://github.com/Krumpy1337)

## Overview

BadApple is a configurable Spigot/Paper plugin built for:

- YAML-backed group permissions
- Rank and track management
- Staff tools and moderation helpers
- Cosmetic unlocks and visual effects
- Player teleport, vanish, and utility commands
- Menu-based administration for faster day-to-day control

## Getting started

1. Put the plugin jar in your server's `plugins/` folder.
2. Start the server once so the plugin creates its default configuration files.
3. Edit the generated YAML files in `plugins/BadApple/`.
4. Use `/groupmenu` or `/group` to manage ranks and players.
5. Review the command reference and feature pages below.

## Wiki pages

- [Configuration](Configuration.md)
- [Command Reference](Command-Reference.md)
- [Features](Features.md)

## Main features

### Permissions and groups

- Groups are stored in YAML files under `plugins/BadApple/groups/`
- Group prefixes can be assigned through config or GUI actions
- Permissions are inherited through the configured group hierarchy
- Group weights determine rank ordering and tab sorting

### Tracks and progression

- Tracks are stored under `plugins/BadApple/tracks/`
- Default staff and user tracks are generated automatically
- Groups can be assigned to routes and promoted/demoted through track logic

### Player data

- Per-player metadata is stored in `plugins/BadApple/players/`
- Group assignment, suffixes, and cosmetic state are tracked per user
- Player-specific UI data can be managed through the admin menus

### Cosmetics

- The plugin supports particles, trails, and wings
- Only one active cosmetic is usually enabled at a time
- Staff ranks can inherit higher-tier cosmetic access

### Utility commands

- `/vanish` toggles spectator-mode vanish
- `/tp` opens a teleport menu for staff
- `/tpa` sends a teleport request to a staff member
- `/tpaccept` and `/tpdeny` manage request flow
- `/nickname`, `/fly`, `/gamemode`, and `/broadcast` round out the staff toolset

### Management tools

- `/groupmenu` provides a GUI for rank management
- `/api` creates or revokes the secure website API key
- `/badapplehelp` shows the built-in help menu and links to the wiki
