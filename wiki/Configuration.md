# Configuration

BadApple stores its runtime data in YAML to make rank and permission management easy for server owners.

## Default file locations

After the first server startup, the plugin creates the following structure:

```text
plugins/
└── BadApple/
    ├── config.yml
    ├── groups/
    │   ├── Copper.yml
    │   ├── Iron.yml
    │   ├── Gold.yml
    │   ├── Diamond.yml
    │   └── Dev.yml
    ├── tracks/
    │   ├── staff.yml
    │   └── users.yml
    ├── players/
    ├── config/
    └── ...
```

## Groups

Each file in `plugins/BadApple/groups/` represents one rank. Group definitions typically include:

- name
- prefix
- weight
- inheritance
- permissions
- cosmetic unlock data

Example structure:

```yaml
name: Dev
prefix: "&b[DEV]"
weight: 10
inherit: [Gold, Diamond]
permissions:
  - badapple.group
  - badapple.tp
  - badapple.vanish
```

## Tracks

Track files live in `plugins/BadApple/tracks/` and define progression logic. These can be used to represent rank ladders or staff paths.

## Player data

Each player gets a YAML file under `plugins/BadApple/players/` used for:

- current group
- suffix data
- custom permissions
- cosmetic selection
- nickname data

## Modifying defaults

The default group and track YAML files are generated automatically, so admins can edit them directly and reload as needed. The plugin is designed to prefer config/editable files over hardcoded group definitions.

## Recommended workflow

1. Review the default group file content.
2. Change prefixes, weights, and inheritance.
3. Add or remove permission nodes as needed.
4. Update player assignments through `/groupmenu` or `/group`.
5. Adjust any cosmetic access or staff rank permissions in the YAML definitions.
