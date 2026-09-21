# Better_Admin_Commands

An all-in-one administration plugin for **Paper / Spigot 1.21+** servers: admin tools, moderation,
teleporting, homes, warps, jail, a full economy with Vault support, and social commands — all backed
by **MySQL/MariaDB** with local safe-file fallback.

---

## Table of contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Connecting a database](#connecting-a-database)
  - [1. Create the database and user](#1-create-the-database-and-user)
  - [2. Configure config.yml](#2-configure-configyml)
  - [3. Restart and verify](#3-restart-and-verify)
  - [4. Troubleshooting the connection](#4-troubleshooting-the-connection)
- [How storage works](#how-storage-works)
- [Configuration reference](#configuration-reference)
- [Commands](#commands)
- [Permissions](#permissions)
- [Economy and Vault](#economy-and-vault)
- [Backups](#backups)
- [Building from source](#building-from-source)
- [Data files](#data-files)

---

## Features

| Area | What you get |
| --- | --- |
| **Admin** | gamemode, fly, god, vanish, heal, feed, speed, repair, give, clear, kits, broadcast, nick (tab list, name tag and chat, optionally with a LuckPerms group prefix), skin change, hat, craft, enderchest, invsee, sudo, exp, time/weather (personal too), world tools |
| **Moderation** | kick, kickall, ban, tempban, IP ban, unban, unbanip, banlist, mute/unmute with timers |
| **Economy** | balances, `/pay`, `/baltop`, `/eco`, `/worth`, `/sell`, exposed to other plugins through **Vault** |
| **Teleporting** | spawn, warps, homes, tpa/tpahere with clickable accept/deny buttons, `/back`, `/tp`, `/tphere`, `/tpall`, `/tppos`, `/rtp` |
| **Jail** | named cells, `/jail`, `/unjail`, `/togglejail`, automatic release when the timer expires |
| **Social** | `/msg`, `/reply`, `/socialspy`, `/ignore`, `/me`, `/mail` (offline messages) |
| **Info** | `/whois`, `/seen`, `/list`, `/ping`, `/gc`, `/playtime`, `/realname`, `/motd`, `/rules` |
| **Items & world** | `/more`, `/rename`, `/lore`, `/skull`, `/book`, `/sort`, `/stack`, `/condense`, `/powertool`, `/unlimited`, `/tree`, `/spawnmob`, `/butcher`, and more |

Everything player-related (balances, homes, mutes, settings, mail) is stored in MySQL, with an
always-updated local YAML copy in `plugins/Better_Admin_Commands/data/`.

---

## Requirements

- **Paper (or Spigot) 1.21+** — `plugin.yml` declares `api-version: '1.21'`
- **Java 21** (the project targets Java 21)
- **MySQL 5.7+ / MariaDB 10.x+** (optional, see below — the plugin also runs without it)
- **Vault** (optional, only needed so other plugins can use this plugin's economy)
- **LuckPerms** (optional, only needed so `/nick` can show a group prefix)

The MySQL/MariaDB JDBC driver is bundled inside the plugin jar and relocated, so nothing extra has
to be installed on the server.

---

## Installation

1. Drop `Better_Admin_Commands.jar` into your server's `plugins/` folder.
2. Start the server once. A default `plugins/Better_Admin_Commands/config.yml` is generated.
3. Turn the server off, [configure the database](#connecting-a-database), and start it again.
4. Optional: install **Vault** if other plugins should use this plugin's economy.

> `config.yml` is only fully re-read on a restart. `/betteradmincommands reload` reloads the config,
> `spawn.yml` and `warps.yml`, but **database and economy settings require a restart**.

---

## Connecting a database

### The easy way: paste the connection string

Nearly every hosting panel hands out a ready-made connection string. Paste it into
`database.connection-string` and the plugin does the rest - it works out the database type, the
address, the database name and the login, and it decodes escapes such as `%2B` (a plus) and `%40` (an
at sign) that panels put in passwords:

```yaml
database:
  connection-string: "jdbc:mysql://u5_6lYOrDlWi2:S5fyBH5u8%2BzaX3X%40AUdZZw@172.18.0.1:3306/s5_MysqlPlugin"
```

That single line carries the user `u5_6lYOrDlWi2`, the password `S5fyBH5u8+zaX3X@AUdZZw`, the host
`172.18.0.1`, port `3306` and the database `s5_MysqlPlugin`. These forms all work:

| You paste | The plugin reads |
| --- | --- |
| `jdbc:mysql://user:pass@host:3306/db` | MySQL |
| `mysql://user:pass@host/db` | MySQL, default port |
| `mariadb://user:pass@host/db` | MariaDB |
| `postgresql://user:pass@host:5432/db` | PostgreSQL - **detected but not usable**, see below |
| `sqlite:/path/to/file.db` | SQLite - **detected but not usable**, see below |
| `127.0.0.1:3306/db` | MySQL, no credentials in the string |

Anything the string leaves out is filled in from the `host`/`port`/`name`/`user`/`password` fields
below it, so a string with only an address is fine. Query parameters in the string
(`?useSSL=false&...)` are passed on to the driver.

> **Only MySQL and MariaDB are actually supported.** When the string points at PostgreSQL, SQLite or
> anything else, the plugin says so plainly and keeps running from the local safe files instead of
> failing with a driver error. Use a MySQL/MariaDB database, or clear `connection-string` and fill in
> the fields.

### Or create the database yourself

The plugin creates its own **tables**, and it also creates the **database** when it is missing and
`database.create-if-missing` is on (the default). If your account may not run `CREATE DATABASE`,
making the database by hand just needs:

```sql
CREATE DATABASE better_admin_commands
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

CREATE USER 'bac_user'@'%' IDENTIFIED BY 'a_strong_password';

GRANT ALL PRIVILEGES ON better_admin_commands.* TO 'bac_user'@'%';

FLUSH PRIVILEGES;
```

Notes:

- `utf8mb4` matters — nicknames, chat and mail can contain emoji.
- Use `'bac_user'@'localhost'` instead of `'%'` when the database runs on the same machine as the
  server and you never connect from elsewhere.
- Prefer a dedicated user limited to this one database over `root`.

### Configure config.yml

Either paste a connection string (above), or fill in the fields:

```yaml
database:
  connection-string: ""      # paste a full connection string here, or leave empty
  create-if-missing: true    # create the database when it does not exist yet
  host: "127.0.0.1"          # database host or IP
  port: 3306                 # 3306 is the MySQL/MariaDB default
  name: "better_admin_commands"
  user: "bac_user"
  password: "a_strong_password"
  table-prefix: "bac_"       # every table the plugin creates starts with this
  pool-size: 4               # pooled connections, 2-8 is fine for one server
  use-ssl: false             # enable for remote/cloud databases
  reconnect-interval-minutes: 30
  connection-parameters: "useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
```

### Restart and verify

Restart the server and check the console. On a successful connection you should see:

```
[Better_Admin_Commands] Connected to the MySQL database.
[Better_Admin_Commands] Registered the MySQL economy with Vault.
```

The plugin then creates the tables (with your prefix) automatically:

| Table | Contents |
| --- | --- |
| `<prefix>players` | UUID, name, balance, last seen, mute state |
| `<prefix>homes` | one row per home (`uuid` + `home` primary key) |
| `<prefix>player_settings` | per-player key/value settings (nickname, social spy, ignore list, tptoggle, afk) |
| `<prefix>mail` | offline messages, with read flag |

You can confirm the live state in-game at any time with:

```
/betteradmincommands info
```

It reports whether the database is `connected` or `unavailable (using local safe files)`, and which
table prefix is in use.

### Troubleshooting the connection

Start with the built-in check, which walks the connection in order and names the step that fails:

```
/betteradmincommands database
```

```
Database check
 OK   Engine: MySQL
 OK   Settings: MySQL bac_user@172.18.0.1:3306/better_admin_commands (from connection-string)
 OK   Address: 172.18.0.1 resolves to 172.18.0.1
 FAIL Port: cannot reach port 3306 - check database.port, the firewall and whether the database allows remote connections
 FAIL Login: Access denied for user 'bac_user'@'172.18.0.1' - the user name or password is wrong, or the user is not allowed to connect from this server
```

The same hints are added to the console warning at start-up, and `/betteradmincommands info` shows
what the plugin parsed out of your settings. The classic causes:

The plugin does **not** refuse to start when the database is unreachable. It logs a warning, keeps
running from the local safe files, and retries every `reconnect-interval-minutes`. You do not have to
restart once the database is back — force an attempt immediately with:

```
/betteradmincommands reconnect
```

When the connection succeeds, everything written locally in the meantime is pushed into MySQL
automatically.

| Symptom | Likely cause |
| --- | --- |
| `Access denied for user ...` | Wrong `user`/`password`, or the user is not allowed from this host (`'user'@'%'` vs `'user'@'localhost'`) |
| `Unknown database 'better_admin_commands'` | The database does not exist and `create-if-missing` is off, or your account may not run `CREATE DATABASE` - create it yourself |
| "... points at PostgreSQL/SQLite, which this plugin cannot talk to" | The connection string names an engine the plugin does not support; use MySQL/MariaDB |
| `Communications link failure` / timeout | Wrong `host`/`port`, firewall, or MySQL not listening on an external interface |
| `Public Key Retrieval is not allowed` | Leave `allowPublicKeyRetrieval` in place (the plugin adds it) or enable `use-ssl` |
| Tables not created | The user lacks `CREATE` permission on the database |

Connection changes are read at start-up, so apply `config.yml` edits with a full restart.

---

## How storage works

The plugin runs in one of two modes, switched automatically:

- **Database available** — MySQL is the source of truth. Reads come from the database; writes are
  applied in memory immediately and flushed asynchronously (balances every
  `economy.save-interval-seconds`, other services per operation). Every write is *also* mirrored into
  the local safe files, so they stay current.
- **Database unavailable** — the plugin starts anyway and falls back to the local safe files in
  `plugins/Better_Admin_Commands/data/`. Players can keep playing, nothing errors out, and every
  write is stored locally.

When the connection comes back (periodic retry or `/betteradmincommands reconnect`), the local safe
files are synced into MySQL so no data is lost.

This makes the database optional: you can run the plugin entirely from the local files, and add
MySQL later without losing what players already did.

---

## Configuration reference

All settings live in `config.yml` (generated on first start).

### Updating config.yml safely

The file starts with a version marker:

```yaml
config-version: 1
```

On every start (and on `/betteradmincommands reload`) the plugin compares your file with the template
shipped inside the jar and **adds options that are missing**. Options that already exist in your file
are never touched, so your values survive plugin updates. When the version marker changes, a copy of
the previous file is written next to it as `config-backup-v<old>-<timestamp>.yml` first.

The file is only rewritten when something actually changed, and comments for newly added options are
copied over. Keys you added yourself are left alone as well. Do not edit `config-version` by hand —
that is what tells the plugin which migration is still outstanding.

### `database`

See [Connecting a database](#connecting-a-database). Additional keys:

| Key | Default | Description |
| --- | --- | --- |
| `connection-string` | empty | A full connection string; when set it wins over the fields below |
| `create-if-missing` | `true` | Create the database itself when it does not exist yet |
| `table-prefix` | `bac_` | Prefix for every table, useful when sharing a database |
| `pool-size` | `4` | Number of pooled JDBC connections |
| `use-ssl` | `false` | Enable SSL for the connection |
| `reconnect-interval-minutes` | `30` | How often a down database is retried |
| `connection-parameters` | — | Extra JDBC parameters appended to the URL |

### `commands`

| Key | Default | Description |
| --- | --- | --- |
| `root-name` | `betteradmincommands` | Name of the plugin management command |
| `root-aliases` | `bac`, `betteradmin` | Aliases for the management command |

See [Renaming the management command](#renaming-the-management-command).

### `skin`

| Key | Default | Description |
| --- | --- | --- |
| `skin.cache-minutes` | `60` | How long a skin fetched from Mojang is cached |
| `skin.timeout-seconds` | `10` | How long to wait for an answer from Mojang |

### `nick`

| Key | Default | Description |
| --- | --- | --- |
| `nick.restricted` | `dev`, `owner` | Nicks and LuckPerms groups that only server operators may use (see [Nicknames](#nicknames)) |

### `economy`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable the built-in economy and Vault registration |
| `currency-name-singular` / `-plural` | `Dollar` / `Dollars` | Currency names shown by Vault |
| `currency-symbol` | `$` | Symbol used when formatting amounts |
| `starting-balance` | `100.0` | Balance a player gets the first time they are seen |
| `max-balance` | `1000000000.0` | Hard ceiling for balances |
| `save-interval-seconds` | `300` | How often dirty balances are written to MySQL |
| `allow-payments` | `true` | Allow `/pay` between players |
| `minimum-payment` | `0.01` | Smallest accepted payment |

### `spawn`, `homes`, `teleport`

| Key | Default | Description |
| --- | --- | --- |
| `spawn.teleport-on-join` | `false` | Teleport every player to spawn on join |
| `spawn.teleport-on-first-join` | `false` | Teleport only on a player's first join |
| `spawn.respawn-at-spawn` | `false` | Respawn players at spawn |
| `homes.max` | `3` | Default homes per player (override per player with `betteradmincommands.homes.limit.<amount>`) |
| `teleport.warmup-seconds` | `3` | Time a player must stand still before a delayed teleport (skipped by `betteradmincommands.teleport.bypass`) |
| `teleport.request-expire-seconds` | `60` | Lifetime of a `/tpa` request |
| `teleport.back-cooldown-seconds` | `0` | Cooldown before `/back` can be used again |
| `teleport.back-enabled` | `true` | Remember previous teleport locations for `/back` |

### `moderation`, `afk`, `jail`, `rtp`

| Key | Default | Description |
| --- | --- | --- |
| `moderation.default-mute-seconds` | `-1` | Default mute length; `-1` means permanent |
| `moderation.mute-message` | see file | Message muted players see when they chat (`%reason%`) |
| `moderation.vanish-tab-cue` | `&7[&8V&7] &r` | Marker shown in front of a vanished player in the tab list |
| `afk.auto-afk-enabled` | `true` | Mark players as away automatically |
| `afk.auto-afk-minutes` | `10` | Idle minutes before auto-afk kicks in |
| `jail.radius` | `8.0` | How far a jailed player may move before being pulled back |
| `rtp.max-radius` / `min-radius` | `2000` / `200` | Ring used by `/rtp` |
| `rtp.max-attempts` | `30` | Attempts to find a safe spot |
| `rtp.disabled-worlds` | nether/end | Worlds where `/rtp` is not allowed |

### `worth`, `kits`, `messages`

- **`worth`** — map of `MATERIAL: price` used by `/worth` and `/sell`. Only listed items can be sold.
- **`kits`** — kit definitions. Item format: `"MATERIAL[:AMOUNT]"` or
  `"MATERIAL:AMOUNT:ENCHANTMENT:LEVEL"`. A cooldown of `-1` disables the cooldown. Kit entries are
  re-read on every use, so editing them does not need a restart.
- **`messages`** — `prefix`, `motd` and `rules` lists. Colour codes use `&` (e.g. `&6`).

---

## Commands

`/betteradmincommands` (aliases `/bac`, `/betteradmin`, both changeable — see
[Renaming the management command](#renaming-the-management-command)) manages the plugin:

| Subcommand | Description |
| --- | --- |
| `reload` | Reload `config.yml` (adding new options), `spawn.yml`, `warps.yml` and `jails.yml` |
| `backup` | Dump every table to `backups/<timestamp>/` |
| `reconnect` | Retry MySQL now and sync local data back up |
| `database` | Walk through the connection step by step and report what fails |
| `disable` | Switch the plugin's features off without unloading it |
| `enable` | Switch the features back on |
| `info` | Show state, economy, Vault, database, warp status, your nickname and any commands another plugin took over |

### Renaming the management command

The name and aliases of the management command are configurable:

```yaml
commands:
  root-name: "betteradmincommands"
  root-aliases:
    - "bac"
    - "betteradmin"
```

Set `root-name` to whatever fits your server (for example `staff`) and `/staff reload` takes over.
Names may only contain `a-z`, `0-9`, `_` and `-`. Invalid or already taken names fall back to
`/betteradmincommands` with a warning in the console. The permission stays
`betteradmincommands.command`. A restart or a reload of the plugin applies the change — the old name
stops working, so make sure you can still reach the command.

### Disabling and enabling the plugin

`/betteradmincommands disable` switches the plugin off without unloading it:

- every listener is unregistered and all repeating tasks (autosave, auto-afk, jail checks,
database reconnect) are cancelled,
- every command except the management command answers with a notice instead of running,
- balances are written back before the features stop, so nothing is lost.

`/betteradmincommands enable` brings everything back, and `info` always shows the current state.
The state is kept in memory only: a restart starts the plugin enabled again. Because the plugin is
still loaded, replacing the jar file itself still needs a server restart — use disable for planned
maintenance windows, for example while another plugin is updated or a database migration runs.

### Admin

| Command | Usage | Permission |
| --- | --- | --- |
| `/enchant` | `/enchant <enchantment> [level]` | `betteradmincommands.enchant` |
| `/gm` | `/gm <mode> [player]` | `betteradmincommands.gamemode` |
| `/fly` | `/fly [player]` | `betteradmincommands.fly` |
| `/smite` | `/smite [player]` | `betteradmincommands.smite` |
| `/heal` | `/heal [player]` | `betteradmincommands.heal` |
| `/feed` | `/feed [player]` | `betteradmincommands.feed` |
| `/god` | `/god [player]` | `betteradmincommands.god` |
| `/speed` | `/speed <walk\|fly> <0-10>` | `betteradmincommands.speed` |
| `/repair` | `/repair [hand\|all]` | `betteradmincommands.repair` |
| `/vanish` | `/vanish [player]` | `betteradmincommands.vanish` |
| `/near` | `/near [radius]` | `betteradmincommands.near` |
| `/give` | `/give <player> <item> [amount]` | `betteradmincommands.give` |
| `/clear` | `/clear [player]` | `betteradmincommands.clear` |
| `/kit` | `/kit <name>` | `betteradmincommands.kit` (or `…kit.<name>`) |
| `/broadcast` | `/broadcast <message>` | `betteradmincommands.broadcast` |
| `/nick` | `/nick [player] <nickname\|off> [luckperms-group]` | `betteradmincommands.nick` |
| `/hat` | `/hat` | `betteradmincommands.hat` |
| `/skinchange` | `/skinchange [player] <username\|off>` | `betteradmincommands.skinchange` |
| `/craft` | `/craft` | `betteradmincommands.craft` |
| `/enderchest` | `/enderchest [player]` | `betteradmincommands.enderchest` |
| `/invsee` | `/invsee <player>` | `betteradmincommands.invsee` |
| `/sudo` | `/sudo <player> <command>` | `betteradmincommands.sudo` |
| `/exp` | `/exp <show\|give\|set> [player] [amount]` | `betteradmincommands.exp` |
| `/time` | `/time <set\|add> <value> [world]` | `betteradmincommands.time` |
| `/weather` | `/weather <sun\|rain\|thunder> [world]` | `betteradmincommands.weather` |
| `/ptime` | `/ptime <reset\|day\|noon\|night\|midnight\|ticks>` | `betteradmincommands.ptime` |
| `/pweather` | `/pweather <reset\|sun\|rain>` | `betteradmincommands.pweather` |
| `/world` | `/world [name]` | `betteradmincommands.world` |

#### Nicknames

`/nick <nickname>` replaces the player's name in the **tab list**, in the **name tag** above their
head and in **chat**. The nickname is stored in the database, so it survives restarts, and
`/realname <nick>` finds the player behind it again.

```
/nick <nickname>                   your own nickname
/nick <nickname> <group>           borrow the prefix of a LuckPerms group
/nick <player> <nickname> [group]  someone else, needs betteradmincommands.nick.others
/nick <nickname> off               drop the group prefix, keep the nickname
/nick off                          remove the nickname entirely
```

The group argument does **not** change the player's permissions or their real LuckPerms group - it
only takes that group's **prefix** and shows it in front of the nickname. Tab completion for the
group comes straight from LuckPerms, and the prefix is looked up again whenever the player joins, so
a prefix you change in LuckPerms applies after their next login.

Using the nicks or groups listed under `nick.restricted` (by default `dev` and `owner`) requires
being a server operator.

Chat works out of the box because Paper's default chat renderer uses the display name. If another
plugin formats chat, make it use the display name (`%player_displayname%` with PlaceholderAPI)
rather than the real name.

#### Skins

`/skinchange <username>` puts the skin of a premium account on a player.

```
/skinchange <username>            your own skin
/skinchange <player> <username>   someone else, needs betteradmincommands.skinchange.others
/skinchange off                   back to your own skin
```

**Where the skin comes from.** There is no NameMC API - NameMC is a viewer for Mojang's own player
data. The plugin therefore asks Mojang directly:

1. `api.minecraftservices.com` resolves the name to a UUID (`api.mojang.com` is tried as a fallback,
   because that host has had outages and blocks),
2. `sessionserver.mojang.com` returns the skin as a texture **with its Mojang signature**.

The signature matters: clients refuse to render unsigned textures. That is also why this works on an
offline-mode server, while only the command's user - not the target account - has to exist there.

**Behaviour**

- The lookup runs asynchronously, never on the server thread, and answers with one of: skin applied,
  "no premium account with that name", or "Mojang could not be reached".
- Results are cached for `skin.cache-minutes` (default 60), and asking for the same name twice at the
  same time only sends one request, so the server does not get rate limited.
- The borrowed skin is stored in the database and applied again on every join, so it survives a
  restart. `/skinchange off` drops it and asks Mojang for the player's own skin; if there is none
  (an offline-mode account), the borrowed texture is simply removed.
- Changing the skin of a source account in Minecraft does **not** update players who borrowed it -
  re-run the command to refresh it.

The server needs outbound HTTPS access to `api.mojang.com` and `sessionserver.mojang.com`; no API key
is required.

### Moderation

| Command | Usage | Permission |
| --- | --- | --- |
| `/kick` | `/kick <player> [reason]` | `betteradmincommands.kick` |
| `/kickall` | `/kickall [reason]` | `betteradmincommands.kickall` |
| `/ban` | `/ban <player> [reason]` | `betteradmincommands.ban` |
| `/tempban` | `/tempban <player> <duration> [reason]` | `betteradmincommands.tempban` |
| `/ipban` | `/ipban <player> [reason]` | `betteradmincommands.ipban` |
| `/unban` | `/unban <player>` | `betteradmincommands.unban` |
| `/banlist` | `/banlist` | `betteradmincommands.banlist` |
| `/mute` | `/mute <player> [duration] [reason]` | `betteradmincommands.mute` |
| `/unmute` | `/unmute <player>` | `betteradmincommands.unmute` |
| `/unbanip` | `/unbanip <address\|player>` | `betteradmincommands.unbanip` |

#### Vanishing

`/vanish` hides a player from everyone who is not allowed to see them.

- **Without `betteradmincommands.vanish.see`:** the vanished player is gone - not in the world, not in
  the tab list.
- **With `betteradmincommands.vanish.see`:** the player stays visible in the world *and* in the tab
  list, where they carry the cue from `moderation.vanish-tab-cue` (default `[V]`), so staff always know
  who is vanished:

```
[V] Steve
```

Those two are handled separately on purpose: in current Paper `hidePlayer` only hides the entity, it
does not remove the player from the tab list, so the plugin explicitly unlists them for players who
may not see them, and lists them again for players who may. The cue sits on the player's tab list name,
so a nickname set with [`/nick`](#nicknames) and the cue appear together.

Vanish state is kept in memory: a restart brings everyone back visible.

### Economy

| Command | Usage | Permission |
| --- | --- | --- |
| `/balance` | `/balance [player]` | `betteradmincommands.balance` |
| `/baltop` | `/baltop [page]` | `betteradmincommands.baltop` |
| `/pay` | `/pay <player> <amount>` | `betteradmincommands.pay` |
| `/eco` | `/eco <give\|take\|set\|reset\|balance> <player> [amount]` | `betteradmincommands.eco` |
| `/worth` | `/worth [item]` | `betteradmincommands.worth` |
| `/sell` | `/sell <hand\|all\|amount>` | `betteradmincommands.sell` |

### Teleporting

| Command | Usage | Permission |
| --- | --- | --- |
| `/spawn` | `/spawn` | `betteradmincommands.spawn` |
| `/setspawn` | `/setspawn` | `betteradmincommands.setspawn` |
| `/tp` | `/tp <player>` \| `/tp <x> <y> <z>` \| `/tp <player> <target>` | `betteradmincommands.tp` |
| `/tphere` | `/tphere <player>` | `betteradmincommands.tphere` |
| `/tpall` | `/tpall` | `betteradmincommands.tpall` |
| `/tppos` | `/tppos <x> <y> <z> [world]` | `betteradmincommands.tppos` |
| `/tpa` | `/tpa <player>` | `betteradmincommands.tpa` |
| `/tpahere` | `/tpahere <player>` | `betteradmincommands.tpahere` |
| `/tpaall` | `/tpaall` | `betteradmincommands.tpaall` |
| `/vanish` | `/vanish [player]` | `betteradmincommands.vanish` |
| `/tpaaccept` | `/tpaaccept [player]` (aliases `/tpaccept`, `/tpyes`) | `betteradmincommands.tpaccept` |
| `/tpadeny` | `/tpadeny [player]` (aliases `/tpdeny`, `/tpno`) | `betteradmincommands.tpdeny` |
| `/tptoggle` | `/tptoggle` | `betteradmincommands.tptoggle` |
| `/back` | `/back` | `betteradmincommands.back` |
| `/warp` | `/warp <name>` | `betteradmincommands.warp` |
| `/warps` | `/warps` | `betteradmincommands.warps` |
| `/setwarp` | `/setwarp <name>` | `betteradmincommands.setwarp` |
| `/delwarp` | `/delwarp <name>` | `betteradmincommands.delwarp` |
| `/home` | `/home [name]` | `betteradmincommands.home` |
| `/sethome` | `/sethome [name]` | `betteradmincommands.sethome` |
| `/delhome` | `/delhome <name>` | `betteradmincommands.delhome` |
| `/homes` | `/homes` | `betteradmincommands.homes` |

#### Teleport requests

When someone sends a `/tpa` or `/tpahere` request, the player who has to answer it gets a notice with
two clickable buttons:

```
[BetterAdmin] Steve wants to teleport to you. [Accept] [Deny]
```

**Accept** runs `/tpaaccept Steve` and **Deny** runs `/tpadeny Steve` for whoever clicked, so a request
can be answered without typing anything. Both commands still work under their old names `/tpaccept`
and `/tpdeny`; without an argument they answer the most recent request.

Teleports are instant for staff holding `betteradmincommands.teleport.bypass`: no warm-up countdown
and no `/back` cooldown. That permission is part of `betteradmincommands.admin`, and it applies to
every teleport, including `/spawn`, `/home`, `/warp` and `/back`.

### Social

| Command | Usage | Permission |
| --- | --- | --- |
| `/msg` | `/msg <player> <message>` | `betteradmincommands.msg` |
| `/reply` | `/reply <message>` | `betteradmincommands.reply` |
| `/socialspy` | `/socialspy` | `betteradmincommands.socialspy` |
| `/ignore` | `/ignore <player>` | `betteradmincommands.ignore` |
| `/ignorelist` | `/ignorelist` | `betteradmincommands.ignorelist` |
| `/me` | `/me <action>` | `betteradmincommands.me` |
| `/mail` | `/mail <send\|read\|clear> [player] [message]` | `betteradmincommands.mail` |

### Info

| Command | Usage | Permission |
| --- | --- | --- |
| `/whois` | `/whois [player]` | `betteradmincommands.whois` |
| `/seen` | `/seen <player>` | `betteradmincommands.seen` |
| `/list` | `/list` | `betteradmincommands.list` |
| `/ping` | `/ping [player]` | `betteradmincommands.ping` |
| `/gc` | `/gc` | `betteradmincommands.gc` |
| `/afk` | `/afk [reason]` | `betteradmincommands.afk` |
| `/playtime` | `/playtime [player]` | `betteradmincommands.playtime` |
| `/realname` | `/realname <nickname>` | `betteradmincommands.realname` |
| `/depth` | `/depth` | `betteradmincommands.depth` |
| `/getpos` | `/getpos [player]` | `betteradmincommands.getpos` |
| `/motd` | `/motd` | `betteradmincommands.motd` |
| `/rules` | `/rules` | `betteradmincommands.rules` |

### Items and movement

| Command | Usage | Permission |
| --- | --- | --- |
| `/more` | `/more` | `betteradmincommands.more` |
| `/rename` | `/rename <name\|off>` | `betteradmincommands.rename` |
| `/lore` | `/lore <add\|set\|clear> [line] [text]` | `betteradmincommands.lore` |
| `/skull` | `/skull [player]` | `betteradmincommands.skull` |
| `/book` | `/book <give\|author\|title\|unlock> [args]` | `betteradmincommands.book` |
| `/condense` | `/condense` | `betteradmincommands.condense` |
| `/stack` | `/stack` | `betteradmincommands.stack` |
| `/sort` | `/sort` | `betteradmincommands.sort` |
| `/unlimited` | `/unlimited [toggle\|list\|clear]` | `betteradmincommands.unlimited` |
| `/disposal` | `/disposal` | `betteradmincommands.disposal` |
| `/powertool` | `/powertool <command\|clear\|list>` | `betteradmincommands.powertool` |
| `/top` | `/top` | `betteradmincommands.top` |
| `/bottom` | `/bottom` | `betteradmincommands.bottom` |
| `/descend` | `/descend` | `betteradmincommands.descend` |
| `/jump` | `/jump` | `betteradmincommands.jump` |
| `/rtp` | `/rtp` | `betteradmincommands.rtp` |

### World, mobs and jail

| Command | Usage | Permission |
| --- | --- | --- |
| `/break` | `/break` | `betteradmincommands.break` |
| `/tree` / `/bigtree` | `/tree [type]` | `betteradmincommands.tree` |
| `/spawner` | `/spawner <type>` | `betteradmincommands.spawner` |
| `/spawnmob` | `/spawnmob <type> [amount] [player]` | `betteradmincommands.spawnmob` |
| `/nuke` | `/nuke [player]` | `betteradmincommands.nuke` |
| `/fireball` | `/fireball [small] [power]` | `betteradmincommands.fireball` |
| `/potion` | `/potion <type> [player] [duration] [amplifier]` | `betteradmincommands.potion` |
| `/burn` | `/burn <player> [seconds]` | `betteradmincommands.burn` |
| `/ext` | `/ext [player]` | `betteradmincommands.ext` |
| `/recipe` | `/recipe [item]` | `betteradmincommands.recipe` |
| `/killall` | `/killall [type]` | `betteradmincommands.killall` |
| `/butcher` | `/butcher [radius]` | `betteradmincommands.butcher` |
| `/remove` | `/remove <type> [radius]` | `betteradmincommands.remove` |
| `/kill` | `/kill [player]` | `betteradmincommands.kill` |
| `/suicide` | `/suicide` | `betteradmincommands.suicide` |
| `/jail` | `/jail <player> [cell] [duration]` | `betteradmincommands.jail` |
| `/setjail` | `/setjail <name>` | `betteradmincommands.setjail` |
| `/deljail` | `/deljail <name>` | `betteradmincommands.deljail` |
| `/jails` | `/jails` | `betteradmincommands.jails` |
| `/unjail` | `/unjail <player>` | `betteradmincommands.unjail` |
| `/togglejail` | `/togglejail <player> [cell]` | `betteradmincommands.togglejail` |

---

## Permissions

The permission tree is defined in `plugin.yml`:

| Permission | Default | Grants |
| --- | --- | --- |
| `betteradmincommands.*` | op | Everything below |
| `betteradmincommands.player` | true | Every command a normal player may use |
| `betteradmincommands.mod` | op | Moderation, jail, vanish, invsee, sudo |
| `betteradmincommands.admin` | op | Full administrative access |

Extra permissions used for finer control:

- `betteradmincommands.vanish.see` — see vanished players in the world and in the tab list, where
  they are marked with `moderation.vanish-tab-cue` (already part of `betteradmincommands.mod`)
- `betteradmincommands.getpos.others`, `.playtime.others`, `.balance.others` — target other players
- `betteradmincommands.nick.others`, `.nick.color` — nickname others / use colour codes
- `betteradmincommands.enderchest.others`, `.broadcast.receive`, `.unlimited.list`
- `betteradmincommands.kick.notify`, `.ban.notify`, `.unban.notify` — receive staff notifications
- `betteradmincommands.world.manage` — create/remove worlds
- `betteradmincommands.teleport.bypass` — skips the teleport warm-up and the `/back` cooldown (granted to admins)
- `betteradmincommands.skinchange.others` — change another player's skin (the self-only
  `betteradmincommands.skinchange` is part of `betteradmincommands.player`)
- `betteradmincommands.homes.limit.<amount>` — per-player home limit override
- `betteradmincommands.kit.<name>` — access to a specific kit
- `betteradmincommands.warp.public` (default `true`) and `betteradmincommands.warp.<name>` — warp access

---

## Economy and Vault

The built-in economy is stored in the `players` table and is offered to **Vault**, so any Vault-aware
plugin can read and modify balances (shops, signs, chest shops, …).

- If Vault is missing, the plugin logs a warning and the economy still works in-game — it just is not
  shared with other plugins.
- If another economy plugin is registered with Vault, this plugin registers itself with the
  *highest* priority and logs a warning. Run one economy at a time to avoid confusion.
- Set `economy.enabled: false` to disable the economy entirely.
- Balances are cached in memory and written back to MySQL asynchronously every
  `economy.save-interval-seconds` (and on shutdown), so the main thread never waits on the database.

---

## Backups

`/betteradmincommands backup` dumps every table into

```
plugins/Better_Admin_Commands/backups/<yyyy-MM-dd_HH-mm-ss>/
├── players.yml
├── homes.yml
├── player_settings.yml
├── mail.yml
└── meta.yml
```

The dump reads its columns from the result set, so it keeps working if a table gains a column.
It falls back to the local safe files when the database is unavailable. The command runs
asynchronously, but on a large server it is still worth running during low traffic.

---

## Building from source

The project uses the Gradle wrapper and the Shadow plugin (the MySQL driver is shaded in and
relocated).

```bash
./gradlew build          # compiles and produces the shaded plugin jar
./gradlew runServer      # starts a Paper 1.21 test server with the plugin installed
```

> `build.gradle` contains a hardcoded output path in `tasks.shadowJar`:
>
> ```groovy
> def serverdir = "D:\\devlopment\\Minecraft\\Plugins\\TestServer\\1.21.11\\plugins"
> ```
>
> Change that line to your own `plugins/` folder before building, otherwise the jar is written to a
> path that probably does not exist on your machine. Alternatively grab the jar from `build/libs/`.

Requirements for building: **JDK 21**.

---

## Data files

Everything the plugin generates lives in `plugins/Better_Admin_Commands/`:

```
config.yml                      main configuration
config-backup-v<n>-<stamp>.yml  copy of config.yml taken before a version migration
spawn.yml                       spawn location (written by /setspawn)
warps.yml                       warp list (written by /setwarp)
jails.yml                       jail cells (written by /setjail)
data/players.yml                local mirror of the players table
data/homes.yml                  local mirror of the homes table
data/player_settings.yml        local mirror of the player_settings table
data/mail.yml                   local mirror of the mail table
backups/<timestamp>/            table dumps created by /betteradmincommands backup
```

The `data/` files are written on every change. They are the fallback source of truth while MySQL is
down, and are pushed back into the database once the connection returns — so do not delete them while
the database is unreachable, and keep them if you migrate servers.

---

## License

No license file is included in this repository. Contact the author before redistributing.
