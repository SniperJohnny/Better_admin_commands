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
- [Taking over EconomyShopGUI](#taking-over-economyshopgui)
  - [Shop access](#shop-access)
- [Permissions](#permissions)
- [Economy and Vault](#economy-and-vault)
- [Backups](#backups)
- [Building from source](#building-from-source)
- [Data files](#data-files)

---

## Features

| Area | What you get |
| --- | --- |
| **Admin** | gamemode, fly, god, vanish, heal, feed, speed, repair, give, clear, kits, broadcast, nick (tab list + chat with the rank prefix, optionally borrowing a LuckPerms group; no name tags above heads), skin change, hat, craft, enderchest, invsee, sudo, exp, time/weather (personal too), world tools |
| **Auction & shop** | a full `/ah` auction house as a GUI with sorting and search, and a `/shop` that takes over EconomyShopGUI's shops |
| **Menus** | `/warps`, `/homes`, `/kit`, `/balance`, `/baltop`, `/mail`, `/ignorelist`, `/jails`, `/ptime`, `/pweather`, `/unlimited` and `/realname` open clickable menus instead of text lists (each keeps a text form) |
| **Moderation** | kick, kickall, ban, tempban, IP ban, unban, unbanip, banlist, mute/unmute with timers |
| **Economy** | balances, `/pay`, `/baltop`, `/eco`, `/worth`, `/sell`, exposed to other plugins through **Vault** |
| **Teleporting** | spawn, warps, homes, tpa/tpahere with clickable accept/deny buttons, `/back`, `/tp`, `/tphere`, `/tpall`, `/tppos`, `/rtp` |
| **Jail** | named cells, `/jail`, `/unjail`, `/togglejail`, automatic release when the timer expires |
| **Social** | `/msg`, `/reply`, `/socialspy`, `/ignore`, `/me`, `/mail` (offline messages) |
| **Info** | `/whois`, `/seen`, `/list`, `/ping`, `/gc`, `/playtime`, `/realname`, `/reveal`, `/motd`, `/rules` |
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
  connection-string: "jdbc:mysql://bac_user:Str0ng%2BPass%40word@db.example.com:3306/better_admin_commands"
```

That single line carries the user `bac_user`, the password `Str0ng+Pass@word`, the host
`db.example.com`, port `3306` and the database `better_admin_commands`. These forms all work:

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
 OK   Settings: MySQL bac_user@db.example.com:3306/better_admin_commands (from connection-string)
 OK   Address: db.example.com resolves to 203.0.113.10
 FAIL Port: cannot reach port 3306 - check database.port, the firewall and whether the database allows remote connections
 FAIL Login: Access denied for user 'bac_user'@'203.0.113.10' - the user name or password is wrong, or the user is not allowed to connect from this server
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
config-version: 11
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

### `auction`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable the auction house and its expiry task |
| `gui-rows` | `6` | Chest rows per window (3-6); the bottom row is navigation |
| `listing-hours` | `48` | How long a listing stays up, `0` never expires |
| `max-listings-per-player` | `10` | Active listings a player may have at once |
| `tax-percent` | `5.0` | Share of the sale price the server keeps |
| `listing-fee-percent` | `0.0` | Up-front fee when listing, as a % of the price |
| `min-price` / `max-price` | `1.0` / `1000000000.0` | Allowed price window |

Browsing has a **sort** button that cycles through *newest*, *oldest*, *cheapest*, *most expensive*
and *item name A-Z*, and a **search** button that filters the listings by item name (custom name or
material) and by seller name. Both are remembered per player while they browse.

### `shop`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable `/shop` |
| `gui-rows` | `6` | Chest rows per window (3-6); the bottom row is navigation |
| `selling-enabled` | `true` | Let players sell items back where the import has a sell price |
| `search-enabled` | `true` | Show the search button that filters every shop |
| `access.permission-prefix` | `""` (open) | Permission every shop needs, with the shop id appended |
| `access.free` | `[]` | Shop ids that stay open, or `*` for all of them |
| `access.permissions.<shop-id>` | - | Per-shop permission; wins over the prefix |
| `access.hide-locked` | `false` | Hide shops a player cannot use instead of greying them out |
| `import.enabled` | `true` | Import the EconomyShopGUI files on start-up when no shop is stored yet |
| `import.force` | `false` | Re-import and overwrite on every start-up |
| `import.disable-plugin` | `true` | Switch EconomyShopGUI off once its shops are imported |
| `import.max-items` | `5000` | Stop after this many imported items |
| `import.folders` | `EconomyShopGUI`, `EconomyShopGUI-Premium` | Folders inside `plugins/` to read |

Any folder in `plugins/` whose name starts with `EconomyShopGUI` is read as well, so the free and the
premium edition are both found without editing the list. See
[Taking over EconomyShopGUI](#taking-over-economyshopgui).

### `trade`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable `/trade` |
| `require-accept` | `true` | Ask the other player first (they get a clickable accept/deny) |
| `request-expire-seconds` | `60` | Lifetime of a trade request |
| `log.enabled` | `true` | Keep a history of finished trades |
| `log.retention-hours` | `48` | How long a record is kept before it is deleted |
| `log.max-cached` | `2000` | Records held in memory for `/trade log` |
| `log.notify-permission` | `betteradmincommands.trade.notify` | Who is told about a finished trade |

### `permissions`

| Key | Default | Description |
| --- | --- | --- |
| `default-access` | `default` | `default`, `all` or `none` - what everyone may use out of the box |
| `groups.player` / `.mod` / `.admin` | `true` / `false` / `false` | Which bundles every player gets in `default` mode |
| `wildcards` | `better_admin_commands.permissionall`, `betteradmincommands.*` | Nodes that unlock everything |

### `notifications`

`notifications.categories.<id>` holds `display`, `permission` and `default` for each notification a
player can toggle with `/notify`. The shipped ids are `trade`, `report`, `kick`, `ban`, `unban` and
`mail`; add your own and they show up in the command automatically.

### `nick` additions

| Key | Default | Description |
| --- | --- | --- |
| `show-rank-prefix` | `true` | Show the player's own LuckPerms rank prefix with `/nick <nick>` |
| `hide-nametag` | `true` | Hide the name tag above **every** player's head, so the tab list is the only place a name shows |
| `chat-format` | `&f<%nickname%>&r %message%` | The chat line, used while no other plugin formats chat |

### `report`

| Key | Default | Description |
| --- | --- | --- |
| `report.presets.<id>.display` | - | Name shown on the category button |
| `report.presets.<id>.icon` | - | Material used as the button |
| `report.presets.<id>.description` | - | Short line under the name |
| `report.presets.<id>.target-required` | `true` | Whether a player is picked before the description |

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
| `homes.gui-rows` / `homes.gui-icon` | `6` / `RED_BED` | Layout of the `/homes` menu (left-click teleports, right-click deletes) |
| `warps.gui-rows` / `warps.gui-icon` | `6` / `COMPASS` | Layout of the `/warps` menu |
| `warps.gui-icons.<name>` | - | Optional icon for one warp, e.g. `spawn: NETHER_STAR` |
| `kit-gui.rows` / `kit-gui.icon` | `3` / `CHEST` | Layout of the `/kit` menu; a single kit can set its own `icon:` |
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

### `baltop`, `mail`, `ignorelist`

| Key | Default | Description |
| --- | --- | --- |
| `baltop.gui-rows` | `6` | Rows of the leaderboard menu (3-6); the bottom row is navigation |
| `mail.gui-rows` | `6` | Rows of the mailbox menu (3-6) |
| `ignorelist.gui-rows` | `6` | Rows of the ignore menu (3-6) |
| `jails.gui-rows` / `jails.gui-icon` | `6` / `IRON_BARS` | Layout of the jail list (clicking a cell teleports to it) |
| `ptime.gui-rows` | `3` | Rows of the personal time menu |
| `pweather.gui-rows` | `3` | Rows of the personal weather menu |
| `unlimited.gui-rows` | `4` | Rows of the unlimited-items menu |
| `realname.gui-rows` | `6` | Rows of the nickname overview |

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
| `/kit` | `/kit [name]` | `betteradmincommands.kit` (or `…kit.<name>`) |
| `/broadcast` | `/broadcast <message>` | `betteradmincommands.broadcast` |
| `/nick` | `/nick <nickname\|off> [luckperms-group]` | `betteradmincommands.nick` |
| `/hat` | `/hat` | `betteradmincommands.hat` |
| `/skinchange` | `/skinchange <username\|uuid\|value <texture> <signature>\|off>` | `betteradmincommands.skinchange` |
| `/ah` | `/ah [browse\|search <text>\|sort <order>\|sell [price]\|mine\|claims\|help]` | `betteradmincommands.auction` |
| `/report` | `/report` | `betteradmincommands.report` |
| `/reports` | `/reports` | `betteradmincommands.report.staff` |
| `/craft` | `/craft` | `betteradmincommands.craft` |
| `/enderchest` | `/enderchest [player]` | `betteradmincommands.enderchest` |
| `/invsee` | `/invsee <player>` | `betteradmincommands.invsee` |
| `/sudo` | `/sudo <player> <command>` | `betteradmincommands.sudo` |
| `/exp` | `/exp <show\|give\|set> [player] [amount]` | `betteradmincommands.exp` |
| `/time` | `/time <set\|add> <value> [world]` | `betteradmincommands.time` |
| `/weather` | `/weather <sun\|rain\|thunder> [world]` | `betteradmincommands.weather` |
| `/ptime` | `/ptime [value]` | `betteradmincommands.ptime` |
| `/pweather` | `/pweather [value]` | `betteradmincommands.pweather` |
| `/world` | `/world [name]` | `betteradmincommands.world` |

#### Nicknames

`/nick <nickname>` changes **your own** name in the **tab list** and in **chat**.
The nickname is stored in the database, so it survives restarts, and `/realname <nick>` finds the
player behind it again. Only your own name can be changed - there is no way to rename someone else.

```
/nick <nickname>          your nickname
/nick <nickname> <group>  borrow the prefix of a LuckPerms group
/nick <nickname> off      drop the group prefix, keep the nickname
/nick off                 remove the nickname entirely
```

The group argument does **not** change the player's permissions or their real LuckPerms group - it
only takes that group's **prefix** and shows it in front of the nickname. Tab completion for the
group comes straight from LuckPerms, and the prefix is looked up again whenever the player joins, so
a prefix you change in LuckPerms applies after their next login.

Using the nicks or groups listed under `nick.restricted` (by default `dev` and `owner`) requires
being a server operator.

**Ranks.** With LuckPerms installed, `/nick <nickname>` shows **your own rank prefix** in the tab
list in front of the nickname, so the rank you already wear appears with the nick - no group has to
be named. `/nick <nickname> off` drops the prefix and keeps the nickname, `/nick <nickname> <group>`
borrows a different group's prefix, and `nick.show-rank-prefix: false` turns the automatic prefix
off. The tab list entry is written again on every `/nick`, so the rank in the tab bar always matches
the nickname - the own rank, or the group the nickname was set with.

A rank prefix is shown in front of **every** player's name - nick or not - in the tab list, in chat
and in the commands that name players (`/msg`, `/reply`, `/me`, `/list`, the social spy line, ...).

When a group is named (`/nick <nick> <group>`), TAB is told to treat the player as if they really
held that rank, so an owner who nicks as `player` is **sorted as a player** and does not stay at the
top of the tab list.

**Seeing the real name.** The nickname is all anyone ever sees; there is no reveal. There is no name
tag above a player's head at all (`nick.hide-nametag`, on by default), so the tab list is the only
place a name shows up. `/realname <nick>` is the deliberate, staff-only lookup and needs
`betteradmincommands.realname` (`op` by default).

Chat works out of the box because Paper's default chat renderer uses the display name. If another
plugin formats chat, make it use the display name (`%player_displayname%` with PlaceholderAPI)
rather than the real name.

**Colour in chat.** Players holding `betteradmincommands.chat.color` can use `&` colour codes in
their chat messages; for everyone else the codes stay visible as plain text.

**Name tags and TAB.** Name tags above the players' heads are hidden for **everyone** - nick or not -
so a real name cannot be read off a player standing in front of you; the tab list is the only place a
name shows up. TAB owns the tab list and the name tags on most servers, so instead of fighting it
over the same packets the plugin drives it through **TAB's API**: the tab list name and the rank
prefix next to it are written by the plugin, a borrowed rank is applied through `setTemporaryGroup`,
and the name tag is hidden with TAB's name tag manager. Without TAB the same is done through Bukkit,
and the tag is hidden with a scoreboard team (`nick.hide-nametag: false` turns that off for servers
where another plugin relies on the scoreboard teams). The teams are created on the main scoreboard
and on every player's own scoreboard, so they also work with a scoreboard plugin, and they are removed
again when the plugin is disabled.

**Showing the nickname in TAB (or another tab list plugin).** A plugin such as TAB renders the tab
list and the name tags from its own packets, so it overwrites whatever the server sets. Instead of
fighting over them, this plugin publishes the nickname to PlaceholderAPI and lets that plugin do the
drawing:

| Placeholder | Value |
| --- | --- |
| `%betteradmincommands_nickname%` | group prefix + nickname, or the real name when none is set |
| `%betteradmincommands_nickname_raw%` | only the nickname, or the real name when none is set |
| `%betteradmincommands_nick_prefix%` | the borrowed LuckPerms group prefix, or empty |

For TAB, set `customtabname` in `groups.yml`/`users.yml` to `%betteradmincommands_nickname%`, and
replace `%essentials_nickname%` with the same placeholder in the `nick` condition in `config.yml`.
Note that a name tag plugin can only put text *around* a player's real name, so above the head the
nickname appears together with the real name unless you let the plugin use packets for it.

#### Skins

`/skinchange <username|uuid>` puts the skin of a premium account on **you**. Only your own skin can
be changed - there is no way to change someone else's skin.

```
/skinchange <username>   use the skin of that premium account
/skinchange <uuid>       the same, naming the account by its UUID
/skinchange value <texture-value> <signature>   use a pasted texture
/skinchange off          back to your own skin
```

**Where the skin comes from.** There is no NameMC API - NameMC is a viewer for Mojang's own player
data. The plugin therefore asks Mojang directly:

1. `api.minecraftservices.com` resolves the name to a UUID (`api.mojang.com` is tried as a fallback,
   because that host has had outages and blocks) - skipped when you give the UUID straight away,
2. `sessionserver.mojang.com` returns the skin as a texture **with its Mojang signature**.

The signature matters: clients refuse to render unsigned textures. That is also why this works on an
offline-mode server, while only the command's user - not the target account - has to exist there.

**Behaviour**

- The lookup runs asynchronously, never on the server thread, and answers with one of: skin applied,
  "no premium account with that name or UUID", or "Mojang could not be reached".
- Results are cached for `skin.cache-minutes` (default 60), and asking for the same name twice at the
  same time only sends one request, so the server does not get rate limited.
- The borrowed skin is stored in the database and applied again on every join, so it survives a
  restart. `/skinchange off` drops it and asks Mojang for the player's own skin; if there is none
  (an offline-mode account), the borrowed texture is simply removed.
- Changing the skin of a source account in Minecraft does **not** update players who borrowed it -
  re-run the command to refresh it.

The server needs outbound HTTPS access to `api.mojang.com` and `sessionserver.mojang.com`; no API key
is required.

#### Auction House

`/ah` opens the whole auction house as a GUI - there is no second command for it:

- **Browse** - a paged grid of every active listing. Click one to see the item, the
  price and how long it still runs, then confirm the purchase. A **sort** button cycles
  through newest, oldest, cheapest, most expensive and item name A-Z, and a **search**
  button filters by item name (custom name or material) and by seller name; both stick
  until you clear them.
- **Sell held item** - hold an item, press the button and set the price in the window that
  opens: buttons raise or lower it by 1 / 10 / 100 / 1000 / 100000, **Type an exact price**
  falls back to typing it in chat. The stack is taken out of your hand once the listing exists.
- **My listings** - everything you listed; click a listing to cancel it.
- **Claims** - items from cancelled or expired listings wait here until collected.

The buyer pays the listed price and the seller receives it minus `auction.tax-percent`.
An optional up-front `auction.listing-fee-percent` and the price window
(`auction.min-price` / `auction.max-price`) are enforced. `auction.listing-hours` sets
how long a listing stays up (`0` = never) and `auction.max-listings-per-player` caps
how many a player may have at once.

Listings live in MySQL (`<prefix>ah_listings`) and are mirrored into
`data/auction.yml`, so they survive a restart and keep working while MySQL is down.

#### Shop

`/shop` opens the server shop, which is filled by importing EconomyShopGUI (see
[Taking over EconomyShopGUI](#taking-over-economyshopgui)):

- **Shop list** - one button per imported shop. When there is only one shop it opens
  straight away.
- **Shop page** - the items of the shop in the slots they were imported into, with the
  imported display names, lore and enchantments kept. Extra items spill onto further
  pages instead of overwriting each other.
- **Item view** - the item plus one-click **Buy 1/8/16/32/64**, a **custom amount** you
  type in chat, and - where the import has a sell price - **Sell 1**, **Sell 8** and
  **Sell everything** straight out of your inventory.
- **Search** - `/shop search <text>`, or the search button, filters every shop at once by
  item name and pages through the results.

Prices are per unit, where a unit is the amount stored on the entry, so an `amount: 16`
entry sells bundles of sixteen and only complete bundles are bought back. The shop needs
MySQL (there is a local mirror, but the import itself is a database write).

#### Editing the shop in game

`/shop edit` (or the **Edit shop** button, both for `betteradmincommands.shop.admin`) changes
the shop without touching a single file:

- **Shop editor** - every shop with its item count and its current access, and a count of entries
  that came out of the import *without a price*.
- **Shop view** - the entries in their slots; click one to edit it.
- **Entry** - set the buy price, the sell price (`off` removes it), the amount one purchase hands
  out, or **take it off sale**; or **delete** the entry behind a confirmation.
- **Shop settings** - set the permission that unlocks the shop, or clear it. Written to
  `shop.access.permissions` in `config.yml` for you.

Edits go into memory, the local safe file **and** MySQL, so they survive a restart. One thing to
know: a re-import with `/shop import` (or `shop.import.force`) rebuilds the shop from the
EconomyShopGUI files and therefore discards in-game edits - the import button says so.

#### Reports

`/report` is the whole report system as a GUI:

- **Create a report** - pick a category, then the player it is about (categories that
  need no player skip that step), then describe it in chat.
- **My tickets** - every ticket you opened, open and closed, with an unread marker.
- **Staff** with `betteradmincommands.report.staff` also get **All open** and **All
  closed** lists, and `/reports` jumps straight to the open ones.

A ticket is a two-way thread: the player and **any number of staff** reply into it.
Everybody who took part is a participant - simply opening a ticket adds you to it -
and when a message arrives the other participants who are online are told in chat
with a clickable button, and again when they join the server if they still have
unread messages. A ticket can be closed and reopened; the ticket menu shows who is
currently on it.

Categories live in `report.presets`; add or remove entries and the menu pages itself
automatically. The shipped list is `hacking`, `rulebreaking`, `harassment`,
`scamming` and `bugreport`.

Reports are stored in MySQL (`<prefix>reports`, `<prefix>report_messages`,
`<prefix>report_participants`). Unlike the economy, homes and mail they have **no
local safe file**, so the report system needs the database to be reachable.

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
  who is vanished. With TAB installed the cue is put in front of the rank prefix through TAB's API:

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
| `/baltop` | `/baltop [page\|list]` | `betteradmincommands.baltop` |
| `/pay` | `/pay <player> <amount>` | `betteradmincommands.pay` |
| `/eco` | `/eco <give\|take\|set\|reset\|resetall\|balance\|top> <player> [amount]` | `betteradmincommands.eco` (or `betteradmincommands.eco.<action>`) |
| `/trade` | `/trade <player\|accept\|deny\|log\|help>` | `betteradmincommands.trade` |
| `/notify` | `/notify [<name> [on\|off]\|all on\|off\|reset]` | `betteradmincommands.notify` |
| `/worth` | `/worth [item]` | `betteradmincommands.worth` |
| `/sell` | `/sell <hand\|all\|amount>` | `betteradmincommands.sell` |
| `/ah` | `/ah` | `betteradmincommands.auction` |
| `/shop` | `/shop [search <text>\|edit\|import]` (alias `/shops`) | `betteradmincommands.shop` |

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

#### Economy and social menus

- **`/balance`** - prints the amount in chat as always **and** opens the balance menu: your balance, a
  **Send money** flow that picks the receiver from the online players (so no name to type) and then
  asks for the amount in chat, and a shortcut to the leaderboard. The chat line is kept because a
  balance is a single number people want to read without opening a window.
- **`/baltop`** - the leaderboard as a paged grid of heads, one per player, with the rank in front of
  the name and the top three marked. A **You are #n** button shows where the viewer stands without
  paging through, and highlights their own entry. `/baltop <page>` and `/baltop list` still print the
  text version.
- **`/mail`** - the mailbox: the inbox newest first, one head per sender, with the date, a preview and a
  **New message** marker. Opening a message marks it read and offers **Reply** (to the sender, online or
  not) and **Delete**. The menu can also **Write a message** - pick an online player, or type a name for
  someone offline - **Mark all as read** and **Clear mailbox** behind a confirmation. The `/mail send`,
  `/mail read` and `/mail clear` subcommands are unchanged.
- **`/ignorelist`** - every ignored name as a head; **clicking one stops ignoring that player** (safe,
  since ignoring them again is one click). **Ignore a player** picks someone online or lets you type a
  name, for players who are offline. `/ignorelist list` prints the text version.

#### Staff and personal-setting menus

- **`/jails`** - every cell with its world and coordinates; **clicking one teleports you to it**, which
  is what the list is for (inspecting a cell, or fetching whoever is in it). Deleting a cell is
  deliberately *not* in the menu: `/deljail` stays the only way, so a misclick cannot empty a cell that
  someone is sitting in. `/jails list` prints the text version.
- **`/ptime`** - one button per preset (day, noon, sunset, night, midnight, sunrise), a **reset**, a
  custom tick count typed in chat, and a header showing your current offset. `/ptime <value>` still
  works, and both forms accept exactly the same values.
- **`/pweather`** - **Sun**, **Rain** and **Reset**, with a header showing which of them is active
  instead of making you remember.
- **`/unlimited`** - a switch that shows the current state, an explicit **Turn it off**, and - for
  `betteradmincommands.unlimited.list` - a list of everyone who has the mode on. `/unlimited` with an
  argument behaves as before (`toggle`, `on`, `off`, `list`, `clear`).
- **`/realname`** - every online player using a nickname, sorted by nickname, with the real account name
  underneath. Left-click confirms who they are, right-click shows their balance if you may see it, and
  **Look up a nickname** asks about one directly. `/realname <nickname>` gives the same answer as before.
- **`/reveal <name|nickname>`** - the deliberate staff lookup: answers with the online player's real
  account name, their LuckPerms rank and their nickname. Unlike the old automatic reveal it only
  runs when a staff member asks for it.

Every transfer made through the balance menu goes through the same economy code as `/pay`, so the
payment rules cannot differ between the two.

#### Warp, home and kit menus

`/warps`, `/homes` and `/kit` each open a menu instead of a text list:

- **`/warps`** - every warp as a button that teleports on click. Warps a player may not use are shown
  greyed out, so they can see that a warp exists without being able to jump there. `/warps list`
  still prints the plain list, which is also what the console gets.
- **`/homes`** - your homes; **left-click teleports**, **right-click opens a confirmation** before the
  home is deleted. `/homes list` prints the text version.
- **`/kit`** - every kit with its contents, cooldown and permission state. Kits that are on cooldown
  or not for you are greyed out with the reason, so "not yet" and "not for me" look different.
  `/kit <name>` still gives a kit directly.

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
| `/ignorelist` | `/ignorelist [list]` | `betteradmincommands.ignorelist` |
| `/me` | `/me <action>` | `betteradmincommands.me` |
| `/mail` | `/mail [send\|read\|clear] [player] [message]` | `betteradmincommands.mail` |

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
| `/realname` | `/realname [nickname]` | `betteradmincommands.realname` |
| `/reveal` | `/reveal <name\|nickname>` | `betteradmincommands.reveal` |
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
| `/unlimited` | `/unlimited [toggle\|on\|off\|list\|clear]` | `betteradmincommands.unlimited` |
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
| `/jails` | `/jails [list]` | `betteradmincommands.jails` |
| `/unjail` | `/unjail <player>` | `betteradmincommands.unjail` |
| `/togglejail` | `/togglejail <player> [cell]` | `betteradmincommands.togglejail` |

---

## Taking over EconomyShopGUI

The shop is not written by hand — it is imported from **EconomyShopGUI**. On the first start after
updating, the plugin reads every YAML file it finds in `plugins/EconomyShopGUI*/`, turns the entries
into its own shops, and then **switches EconomyShopGUI off** (`shop.import.disable-plugin`, on by
default) so the two cannot both answer to `/shop`.

The importer is deliberately version-proof: instead of relying on one edition's layout it walks the
whole file and treats *anything that names a material* as a shop entry. That covers the free and the
premium edition, `pages:` layouts, `sections:` layouts and shop files written by hand.

**One file becomes one shop.** The shop is named after the file, unless the file names itself with
`shop-name` / `title` / `display-name`. An `icon:` (or `display-item:`) sets the button in the shop
list, and `rows:` / `size:` sets the window height.

These spellings are understood on an entry:

| What | Accepted keys |
| --- | --- |
| Item | `material`, `item`, `type` |
| Buy price | `buy`, `buy-price`, `price`, `cost`, `money`, or a nested `price: { … }` section |
| Sell price | `sell`, `sell-price`, `sell-amount` |
| Amount | `amount`, `quantity`, `give-amount` |
| Looks | `display-name`/`name`, `lore`, `enchantments`, `custom-model-data`, `skull-owner` |
| Place | `slot`, `page` |

Slots and pages are used where they fit; an entry whose slot collides or falls outside the window is
moved to the next free slot instead of disappearing, so nothing is ever lost in the import. Entries
can also be written as a plain list of material names, or as a list of maps.

### Migrating

1. Make sure EconomyShopGUI is installed and working as usual. (Prices can be corrected in game
   afterwards with `/shop edit` - see above.)
2. Update this plugin and restart once. Watch the console for
   `Imported <n> shop(s) with <m> item(s) from EconomyShopGUI`.
3. Run `/shop`. The shops, pages, prices and displays should match what you had.
4. Keep the `plugins/EconomyShopGUI*/` folder for a while as a backup; it is no longer read from once
   the shop is stored in the database.

Editing prices afterwards is done in `/shop import`'s source (the EconomyShopGUI files) followed by
`/shop import`, or directly in the `shop_items` table. `/shop import` re-reads the files on demand and
is limited to `betteradmincommands.shop.admin`.

### Shop access

Every shop can be locked behind its own permission. There are three ways, checked in this order:

1. `shop.access.permissions.<shop-id>` — an explicit permission for one shop, e.g.
   ```yaml
   shop:
     access:
       permissions:
         vip: "betteradmincommands.shop.vip"
   ```
2. `shop.access.free` — a list of shop ids that stay open whatever the prefix says (`*` = all of them).
3. `shop.access.permission-prefix` — the permission every other shop needs, with the shop id appended.
   With the prefix `betteradmincommands.shop.` the shop `blocks` needs
   `betteradmincommands.shop.blocks`. The id is the file path inside the plugin folder without the
   extension, with anything that is not `a-z`, `0-9`, `_`, `-` or `.` turned into a `.` — so
   `blocks/mob_drops.yml` becomes `blocks.mob_drops`.

Shop ids are what `/shop` lists and what `shop_items.shop` holds, so they are easy to look up.

**The default prefix is empty, which leaves every shop open** — nothing changes until you set it.

A locked shop is handled in three places, so it cannot be worked around:

- it shows greyed out in the shop list with the permission it needs (`shop.access.hide-locked: true`
  removes it from the list instead);
- `/shop <id>` and any direct attempt to open it — including from the **search results**, which are
  filtered — is refused;
- buying, selling and selling everything re-check the permission server side, so a stale menu cannot
  be used after a permission is revoked.

Staff holding `betteradmincommands.shop.admin` may use every shop, which doubles as the way to
preview a shop before granting it out.

### What is not carried over

- **EconomyShopGUI's own commands** (`/sellall`, `/shop reload`, its `/balance` integration, …) go away
  with the plugin. `/sell`, `/worth` and `/balance` from this plugin cover the common cases.
- **EconomyShopGUI's permission nodes** (`economyshopgui.shop.<name>`, …) are not used — they are
  replaced by `shop.access` below, so a per-shop permission has to be written once.
- **Player heads by name** are imported without their skin when the owning account is not already
  cached on the server, because looking a name up over the network during the import would freeze the
  start-up. Set the head's owner in-game afterwards, or use a `custom-model-data` entry instead.

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
- `betteradmincommands.nick.color` — use colour codes in a nickname
- `betteradmincommands.chat.color` — use colour codes in chat
- `betteradmincommands.reveal` — report the real name and rank behind a nickname
- `betteradmincommands.auction` / `.auction.sell` — open the auction house / list items (both default `true`)
- `betteradmincommands.shop` / `.shop.sell` — open the shop / sell to it (both default `true`);
  `.shop.admin` (default `op`) — import the EconomyShopGUI files with `/shop import` and use every
  shop regardless of `shop.access`
- `betteradmincommands.shop.<shop-id>` — access to a single shop when `shop.access.permission-prefix`
  is set (see [Shop access](#shop-access)); the node is written once via `shop.access` instead
- `betteradmincommands.report` (default `true`) — open report tickets; `.report.staff`
  (default `op`) — see every ticket and answer them
- `betteradmincommands.enderchest.others`, `.broadcast.receive`, `.unlimited.list`
- `betteradmincommands.kick.notify`, `.ban.notify`, `.unban.notify` — receive staff notifications
  (any of these can be switched off per player with `/notify`)
- `betteradmincommands.trade` (default `true`) — use `/trade`; `.trade.log.others` — read every
  player's trade history; `.trade.notify` — be told what a trade exchanged
- `better_admin_commands.permissionall` — one node that unlocks everything (alias of `betteradmincommands.*`)
- `betteradmincommands.notify` (default `true`) — the `/notify` command itself
- `betteradmincommands.world.manage` — create/remove worlds
- `betteradmincommands.teleport.bypass` — skips the teleport warm-up and the `/back` cooldown (granted to admins)
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

## Player to player trading

`/trade <player>` asks the other side for a trade and opens a two-sided window once they accept
(the ask-ahead step can be turned off with `trade.require-accept: false`). Each player puts items
into their own half and can offer money with the gold button; when **both** sides confirm, the
offers are swapped. Closing the window, disconnecting or a balance that dropped in the meantime
cancels the trade and hands everything back, so an item can never be lost.

```
/trade <player>           ask for a trade
/trade accept [player]    accept a request
/trade deny [player]      decline a request
/trade log [player]       the last trades (your own, or everyone's for staff)
```

Every finished trade is written to the `trades` table **and** mirrored into `data/trades.yml`.
Records older than `trade.log.retention-hours` (48 by default) are deleted from both. Only the
newest `trade.log.max-cached` records are held in memory, so a busy market does not grow the heap.

Staff who hold `betteradmincommands.trade.notify` are told in chat what each side gave; the
notification can be switched off per player with `/notify`.

---

## Notifications

The plugin tells staff about things that happen (a trade, a new report, a kick, a ban, an unban)
and reminds players about unread mail. Each of those is a **category** with its own permission,
listed under `notifications.categories` in `config.yml`. A player receives a notification only when
they hold its permission *and* have not switched it off:

```
/notify                     list every notification with its state
/notify <name>              flip one on or off
/notify <name> on|off       set one outright
/notify all on|off          set every one at once
/notify reset               back to the server defaults
```

Add or remove categories in `config.yml` and the command picks them up automatically; a category
removed from the config falls back to its built-in default.

---

## One permission for everything

Handing out a hundred nodes is tedious, so there is a single node that unlocks the whole plugin:

```yaml
better_admin_commands.permissionall: true
```

It is accepted wherever a specific node is checked and also satisfies the command permissions,
exactly like `betteradmincommands.*`. On a server without a permission plugin, the `permissions`
section of `config.yml` decides what everyone gets from the start:

```yaml
permissions:
  default-access: default   # default | all | none
  groups:
    player: true            # /home, /pay, /ah, /shop, /trade, ...
    mod: false              # /ban, /kick, /mute, ...
    admin: false            # /gm, /give, /eco, ...
```

`default-access: all` gives every command to everyone, `none` gives nothing (access then comes only
from a permission plugin). The group switches apply in `default` mode and let a small server, for
example, hand everyone the moderation bundle without making them operators. The switches are applied
when a player joins and after `/betteradmincommands reload`.

---

## Menu look

Every window shares one theme now: a dark border, a lighter background and consistent headings, so
the auction house, the shop and the menus for homes, warps, kits, mail, reports and the rest feel
like one piece. `/ah` was rebuilt around it with buttons that explain themselves, item and balance
counts, the current price range and the running time of the whole listing, and every button still
has a text form (see the Auction House section).

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
data/auction.yml                local mirror of the ah_listings table
data/shops.yml                  local mirror of the shops table
data/shop_items.yml             local mirror of the shop_items table
data/trades.yml                 local mirror of the trades table
backups/<timestamp>/            table dumps created by /betteradmincommands backup
```

The `data/` files are written on every change. They are the fallback source of truth while MySQL is
down, and are pushed back into the database once the connection returns — so do not delete them while
the database is unreachable, and keep them if you migrate servers.

---

## License

No license file is included in this repository. Contact the author before redistributing.
