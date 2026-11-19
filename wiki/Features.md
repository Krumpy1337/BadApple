# Features

This page summarizes the major features added to BadApple.

## Permissions system

BadApple stores groups in YAML and supports:

- permission creation and deletion
- group prefix configuration
- weight sorting
- inheritance chains
- player assignment and removal
- runtime permission application

## Group management

Admins can manage groups through:

- raw chat commands via `/group`
- menu-based management via `/groupmenu`
- YAML-based default files in `plugins/BadApple/groups/`

## Rank tracks

The plugin includes track logic for rank moves and progression structures. Default track files are generated automatically so staff can tailor them without modifying the Java source.

## Tab list and chat

BadApple includes staff-aware rank styling for:

- tab list ordering
- prefix and suffix display
- chat formatting
- rank metadata and hover information

## Cosmetics

Players can unlock and toggle:

- particle trails
- cosmetic particles
- wings
- rank-based staff visual unlocks

## Teleport and vanish

Built-in support includes:

- staff-only teleport menu
- player teleport request flow
- /tpa, /tpaccept, /tpdeny flow
- vanish behavior that enters spectator mode and hides the user from other players

## Website API

The authenticated plugin API exposes server status, online players, groups, and tracks, with scoped group and player-rank management actions. Generate or revoke its bearer key in game with `/api`. The key hash is stored by the plugin; the raw key is shown once.

The API binds to localhost by default. Remote website access should go through a TLS reverse proxy, and the website backend should keep the key encrypted server-side. The API does not expose arbitrary console command execution.

## Player utilities

BadApple includes a full set of player/squad utility tools such as:

- nicknames
- fly toggles
- gamemode changes
- broadcast announcements
- punishment actions

## Why this approach

The project prefers user-editable YAML and runtime group application over hardcoded rules. This allows a server to adjust rank behavior without recompiling or changing Java logic for each change.
