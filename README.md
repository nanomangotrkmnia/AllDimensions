# AllDimensions

A small Paper plugin that makes commands affect **every dimension** instead of only the
sender's dimension. Console commands run in the overworld by default, so commands like
`/time`, `/weather` or `/gamerule` normally miss the nether and the end. This plugin
intercepts a command and re-runs it once per configured dimension, and it can be toggled
on and off at runtime.

## Requirements

- Paper (or a Paper fork) for Minecraft 1.21.x — built against `paper-api:1.21.11-R0.1-SNAPSHOT`
- Java 21

## Installation

1. Grab `AllDimensions-<version>.jar` from `build/libs/` (or build it yourself, see below).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.
4. Enable it with `/alldim on`.

## Commands

| Command | Description |
| --- | --- |
| `/alldim on` | Enable all-dimension command mode. |
| `/alldim off` | Disable all-dimension command mode. |
| `/alldim status` | Show whether the mode is enabled. |
| `/alldim reload` | Reload `config.yml` from disk. |

The state is persisted to `config.yml`, so it survives restarts.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `alldim.manage` | `op` | Allows `/alldim on\|off\|status\|reload`. |
| `alldim.use` | `op` | Allows a player's own commands to be replicated across dimensions. Non-op players are left untouched. |

## Configuration

`plugins/AllDimensions/config.yml`:

```yaml
enabled: false
dimensions:
  - minecraft:overworld
  - minecraft:the_nether
  - minecraft:the_end
excluded-commands:
  - alldim
  - execute
  - stop
  - restart
  - reload
  - spark
```

- **`enabled`** — the on/off switch (also managed by `/alldim on|off`).
- **`dimensions`** — the dimension keys the command is run in. Add custom worlds here
  using their dimension key.
- **`excluded-commands`** — commands that are never replicated. Matching is done on the
  command name (namespace stripped, case-insensitive).

## How it works

1. Listens to `ServerCommandEvent` (console) and `PlayerCommandPreprocessEvent` (players).
2. If enabled and the command is not excluded, the original event is cancelled.
3. The command is dispatched once per dimension as `execute in <dimension> run <command>`.

A re-entrancy guard prevents the plugin's own dispatches from being intercepted again.

## Notes and limitations

- Console commands are always affected while enabled. Player commands are only affected
  for players holding `alldim.use` (default `op`).
- Wrapping a player command in `execute` requires the `minecraft.command.execute`
  permission, which ops have by default.
- Commands that are not dimension-scoped (for example `/say` or `/msg`) will run once per
  dimension. Add them to `excluded-commands` if that is not desired.
- The whole command is wrapped, so dimension-specific side effects (such as teleports)
  apply once per dimension.

## Building

The project ships with a Gradle wrapper. With a JDK 21 installed:

```
gradlew.bat build      # Windows
./gradlew build        # Linux / macOS
```

The plugin jar is written to `build/libs/`. Copy it to your server's `plugins/` folder.
