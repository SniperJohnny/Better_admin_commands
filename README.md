# Better_Admin_Commands

One administration plugin for a **Paper / Spigot 1.21+** server: moderation, teleporting and homes,
warps, jail, an auction house, a shop, trading, reports, an economy with Vault support, social
commands and a pile of item, world and admin tools. Everything player-related is stored in
**MySQL/MariaDB** and mirrored into local safe files, so the server keeps running even while the
database is down.

Every part of it is a **module** that can be switched off in `config.yml`, and every dialog a player
has to answer (a price, a permission, a search term, a report description) is a real window rather
than a line typed into chat.

The first half of this file is the user manual for **server owners** — what it does, how to install
it, how to configure it and how to switch parts off. The second half is for **developers**:
how the project is laid out and how to add to it.

---

## Table of contents

**For server owners**

- [What this plugin is for](#what-this-plugin-is-for)
- [What you get](#what-you-get)
- [Requirements](#requirements)
- [Installation](#installation)
- [Connecting a database](#connecting-a-database)
- [Turning features on and off (modules)](#turning-features-on-and-off-modules)
- [Menus and dialogs](#menus-and-dialogs)
- [Commands](#commands)
- [Shop: taking over EconomyShopGUI](#shop-taking-over-economyshopgui)
- [Economy and Vault](#economy-and-vault)
- [Trading, reports and notifications](#trading-reports-and-notifications)
- [Nicknames, ranks and skins](#nicknames-ranks-and-skins)
- [Permissions](#permissions)
- [Configuration reference](#configuration-reference)
- [How storage works](#how-storage-works)
- [Backups and data files](#backups-and-data-files)
- [Troubleshooting](#troubleshooting)

**For developers**

- [Building from source](#building-from-source)
- [Project layout](#project-layout)
- [How it fits together](#how-it-fits-together)
- [The module system](#the-module-system)
- [Adding a command](#adding-a-command)
- [Adding a config option](#adding-a-config-option)
- [Adding stored data](#adding-stored-data)
- [Dependencies](#dependencies)
- [House rules](#house-rules)

---

# For server owners

## What this plugin is for

Running a server usually means stacking up a dozen small plugins: one for moderation, one for
homes and warps, one for the shop, one for the auction house, one for the economy, one for
nicknames, one for the jail. They each have their own config, their own database story and their
own idea of what a menu looks like.

This plugin is the alternative: **one jar** that covers the day-to-day administration of a survival
or community server, stores all of it in one database, and looks and behaves the same everywhere.
It is built for two kinds of server in particular:

- **small and medium servers** that would rather run one plugin than fifteen, and that want a
  working setup with sensible defaults and no permission plugin required,
- **servers that already have a permission plugin** (LuckPerms) and a database (or a hosting panel
  that hands out a connection string), and want everything driven from `config.yml`.

What it is *not*: it is not a lightweight, do-one-thing plugin, and it is not a region-claim or
anti-grief system. It is the "staff toolbox + economy + shop" layer of a server.

Two deliberate design choices are worth knowing before you install it:

- **Every feature can be switched off.** If you already have a shop you are happy with, turn the
  shop module off and use the rest. See
  [Turning features on and off](#turning-features-on-and-off-modules).
- **The database is optional at runtime.** If MySQL cannot be reached, the plugin still starts,
  keeps working from the local safe files in `data/`, and pushes everything into the database once
  the connection comes back. See [How storage works](#how-storage-works).

## What you get

| Area | What it covers |
| --- | --- |
| **Admin** | gamemode, fly, god, vanish, heal, feed, speed, repair, give, clear, enchant, exp, broadcast, hat, craft, enderchest, invsee, sudo, near, time/weather (personal too), world tools, spawner/mob tools |
| **Moderation** | kick, kickall, ban, tempban, IP ban, unban, unbanip, banlist, mute/unmute with timers, vanish with a tab-list cue for staff |
| **Teleporting** | spawn, warps, homes, `/tp`, `/tphere`, `/tpall`, `/tppos`, `/tpa` and `/tpahere` with clickable accept/deny, `/tptoggle`, `/tpaall`, `/back`, `/rtp`, `/top`, `/bottom`, `/descend`, `/jump` |
| **Economy** | balances, `/pay`, `/baltop`, `/eco`, `/worth`, `/sell`, exposed to other plugins through **Vault** |
| **Trading** | `/trade` with a two-sided window, items and money, dual confirmation, full rollback, and a searchable history |
| **Auction house** | `/ah` as a GUI: browse, sort, search, sell, my listings, claims, taxes and fees |
| **Shop** | `/shop`, imported from EconomyShopGUI, with per-shop permissions and an in-game editor |
| **Reports** | `/report` tickets between players and staff, with categories, threads, participants and unread markers |
| **Social** | `/msg`, `/reply`, `/socialspy`, `/ignore`, `/ignorelist`, `/me`, `/mail` (offline messages) |
| **Info** | `/whois`, `/seen`, `/list`, `/ping`, `/gc`, `/playtime`, `/realname`, `/reveal`, `/afk`, `/depth`, `/getpos`, `/motd`, `/rules` |
| **Items** | `/more`, `/rename`, `/lore`, `/skull`, `/book`, `/sort`, `/stack`, `/condense`, `/powertool`, `/unlimited`, `/disposal` |
| **Jail** | named cells, `/jail`, `/unjail`, `/togglejail`, automatic release when the timer runs out |
| **Nick & skin** | `/nick` (tab list, chat, rank prefix, borrowed ranks), `/skinchange` (Mojang skin lookup) |
| **Menus** | `/warps`, `/homes`, `/kit`, `/balance`, `/baltop`, `/mail`, `/ignorelist`, `/jails`, `/ptime`, `/pweather`, `/unlimited`, `/realname`, `/ah`, `/shop`, `/report` |

Everything player-related — balances, homes, mutes, settings, mail, listings, shops, trades — lives
in MySQL, with an always-updated local YAML copy in `plugins/Better_Admin_Commands/data/`.

## Requirements

- **Paper (or Spigot) 1.21+** — `plugin.yml` declares `api-version: '1.21'`.
  The dialog windows need **1.21.6+** clients; see [Menus and dialogs](#menus-and-dialogs).
- **Java 21** (the project targets Java 21).
- **MySQL 5.7+ / MariaDB 10.x+** — optional. Without it the plugin runs from the local safe files.
  PostgreSQL and SQLite are *detected* but not usable; the plugin says so plainly instead of failing
  with a driver error.
- **Vault** — optional, only so other plugins can use this plugin's economy.
- **LuckPerms** — optional, for rank prefixes in the tab list and for borrowed ranks in `/nick`.
- **PlaceholderAPI** — optional, publishes the nickname so TAB (or another tab-list plugin) can
  render it.
- **TAB** — optional, used through its API for tab-list names, rank prefixes, sorting and hiding
  the name tags above players' heads.

The MySQL/MariaDB JDBC driver is bundled inside the plugin jar and relocated, so nothing extra has
to be installed on the server.

## Installation

1. Drop `Better_Admin_Commands.jar` into your server's `plugins/` folder.
2. Start the server once. A default `plugins/Better_Admin_Commands/config.yml` is generated.
3. Turn the server off, [configure the database](#connecting-a-database), and start it again.
4. Optional: install **Vault** if other plugins should use this plugin's economy, **LuckPerms** for
   ranks, **TAB** for the tab list and name tags.

> `config.yml` is re-read on a restart, and `/betteradmincommands reload` re-reads the config,
> `spawn.yml`, `warps.yml` and `jails.yml` *and* re-applies the modules (listeners, commands and
> tasks). **Database and economy settings need a full restart.**

## Connecting a database

### The easy way: paste the connection string

Nearly every hosting panel hands out a ready-made connection string. Paste it into
`database.connection-string` and the plugin does the rest — it works out the database type, the
address, the database name and the login, and it decodes escapes such as `%2B` (a plus) and `%40`
(an at sign) that panels put in passwords:

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
| `postgresql://user:pass@host:5432/db` | PostgreSQL — **detected but not usable** |
| `sqlite:/path/to/file.db` | SQLite — **detected but not usable** |
| `127.0.0.1:3306/db` | MySQL, no credentials in the string |

Anything the string leaves out is filled in from the `host`/`port`/`name`/`user`/`password` fields
below it, so a string with only an address is fine. Query parameters in the string
(`?useSSL=false&…`) are passed on to the driver.

### Or create the database yourself

The plugin creates its own **tables**, and it also creates the **database** when it is missing and
`database.create-if-missing` is on (the default). If your account may not run `CREATE DATABASE`:

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
- Use `'bac_user'@'localhost'` when the database runs on the same machine and you never connect
  from elsewhere.
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
| `<prefix>ah_listings` | auction listings (item, price, seller, state, expiry) |
| `<prefix>shops`, `<prefix>shop_items` | the imported shop |
| `<prefix>trades` | finished trades |
| `<prefix>reports`, `<prefix>report_messages`, `<prefix>report_participants` | report tickets |

Check the live state in-game at any time with:

```
/betteradmincommands info
```

It reports whether the database is `connected` or `unavailable (using local safe files)`, which
table prefix is in use and how many modules are switched off. For a step-by-step connection check:

```
/betteradmincommands database
```

The plugin does **not** refuse to start when the database is unreachable. It logs a warning, keeps
running from the local safe files, and retries every `reconnect-interval-minutes`. You do not have
to restart once the database is back — force an attempt immediately with
`/betteradmincommands reconnect`.

| Symptom | Likely cause |
| --- | --- |
| `Access denied for user ...` | Wrong `user`/`password`, or the user is not allowed from this host (`'user'@'%'` vs `'user'@'localhost'`) |
| `Unknown database 'better_admin_commands'` | The database does not exist and `create-if-missing` is off, or your account may not run `CREATE DATABASE` — create it yourself |
| "... points at PostgreSQL/SQLite, which this plugin cannot talk to" | The string names an engine the plugin does not support; use MySQL/MariaDB |
| `Communications link failure` / timeout | Wrong `host`/`port`, firewall, or MySQL not listening on an external interface |
| `Public Key Retrieval is not allowed` | Leave `allowPublicKeyRetrieval` in place (the plugin adds it) or enable `use-ssl` |
| Tables not created | The user lacks `CREATE` permission on the database |

## Turning features on and off (modules)

Every command of the plugin belongs to exactly **one module**, and each module can be switched off
in `config.yml`:

```yaml
modules:
  shop:
    enabled: false
  trade:
    enabled: false
  # a single command, on top of its module
  disabled-commands:
    - "sell"
```

What switching a module off does:

- its **commands stay registered** but answer with a short note naming the option that switched the
  feature off. A command that silently vanishes confuses players more than one that explains itself;
- its **listeners are not registered** and its **repeating tasks do not run**, so a switched-off
  feature costs nothing while the server runs;
- the **management command always works**, whatever else is off.

Changes are applied by `/betteradmincommands reload` or a restart. `/betteradmincommands modules`
prints the state of every module:

```
[BetterAdmin] Feature modules (config.yml » modules)
 &aON  &7admin &8(Administration, 36 command(s))
 &cOFF &7shop &8(Shop, 1 command(s))
 ...
```

The modules are:

| Module | Commands |
| --- | --- |
| `admin` | enchant, gm, fly, smite, heal, feed, god, speed, repair, vanish, near, give, clear, broadcast, hat, craft, enderchest, invsee, sudo, exp, time, weather, ptime, pweather, world, break, tree, bigtree, spawner, spawnmob, nuke, fireball, potion, burn, ext, recipe, killall, butcher, remove |
| `moderation` | kick, ban, tempban, ipban, unban, unbanip, kickall, banlist, mute, unmute, kill, suicide |
| `economy` | balance, baltop, pay, eco, worth, sell (+ the Vault economy and the autosave) |
| `trade` | trade |
| `auction` | ah |
| `shop` | shop |
| `report` | report, reports |
| `teleport` | back, tp, tppos, tphere, tpall, tpa, tpahere, tpaaccept, tpadeny, tptoggle, tpaall, rtp, jump, top, bottom, descend |
| `spawn` | spawn, setspawn (+ the join rules and respawning at spawn) |
| `homes` | home, sethome, delhome, homes |
| `warps` | warp, setwarp, delwarp, warps |
| `social` | msg, reply, socialspy, ignore, ignorelist, me, mail |
| `info` | whois, seen, list, ping, gc, depth, getpos, playtime, realname, reveal, motd, rules, notify |
| `items` | more, rename, lore, skull, book, condense, stack, sort, unlimited, disposal, powertool |
| `jail` | jail, setjail, deljail, jails, unjail, togglejail (+ the jail rules and the automatic release) |
| `kits` | kit |
| `nick` | nick (+ rank prefixes, hidden name tags, the PlaceholderAPI expansion) |
| `skin` | skinchange |
| `afk` | afk (+ activity tracking and auto-away) |

`disabled-commands` takes a command name or one of its aliases, so `sell`, `money` or `ah` all
work. Everything else in those modules keeps working.

> **Four modules have an older switch of their own.** `economy.enabled`, `trade.enabled`,
> `auction.enabled` and `shop.enabled` already existed before modules did, and they still count:
> such a module runs only while **both** switches are on. A server that turned its shop off long
> ago therefore stays off after updating.

There is also a plugin-wide switch that does not touch `config.yml`:
`/betteradmincommands disable` unregisters every listener, stops every task and makes every command
answer with a notice — useful for a maintenance window. `/betteradmincommands enable` brings it all
back. Because the plugin stays loaded, replacing the jar still needs a restart.

## Menus and dialogs

Almost every browsable command has a menu: `/warps`, `/homes`, `/kit`, `/balance`, `/baltop`,
`/mail`, `/ignorelist`, `/jails`, `/ptime`, `/pweather`, `/unlimited`, `/realname`, `/ah`, `/shop`
and `/report`. Every window shares one theme — a dark border, a lighter background, consistent
headings and a navigation row — so they feel like one piece instead of a pile of screens. Text
forms stay available where they existed (`/warps list`, `/homes list`, `/baltop list`, …), which is
also what the console gets.

**Whenever a menu needs a value from you, it opens a real window** (Paper's dialog API) instead of
asking you to type into chat: a price in the auction house, a custom amount in the shop, a search
term, a shop permission, the money in a trade, a report description, your personal time. Pressing
**Cancel**, or closing the window with escape, answers nothing: the value stays as it was, and you are
put back on the screen you came from (or the command simply stops, when there was no screen).

The same question is also printed in chat, because a client that cannot show a dialog — anything
older than 1.21.6 — never opens one. The first answer wins, and a question that nobody answers is
forgotten after two minutes, so a window you close can never swallow a later chat message.

## Commands

`/betteradmincommands` (aliases `/bac`, `/betteradmin`, both changeable) manages the plugin:

| Subcommand | Description |
| --- | --- |
| `reload` | Re-read `config.yml` (adding new options), `spawn.yml`, `warps.yml`, `jails.yml` and re-apply the modules |
| `modules` | List every feature module with its state |
| `backup` | Dump every table to `backups/<timestamp>/` |
| `reconnect` | Retry MySQL now and sync local data back up |
| `database` | Walk through the connection step by step and report what fails |
| `info` | Show state, economy, Vault, database, warps, your nickname, module count and any commands another plugin took over |
| `disable` / `enable` | Switch the plugin's features off and on again without unloading it |

The name and aliases of the management command are configurable (`commands.root-name`,
`commands.root-aliases`); set `root-name: staff` and `/staff reload` takes over. Names may only
contain `a-z`, `0-9`, `_` and `-`; invalid or taken names fall back to `/betteradmincommands` with a
console warning. The permission stays `betteradmincommands.command`.

### Admin — module `admin`

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
| `/broadcast` | `/broadcast <message>` | `betteradmincommands.broadcast` |
| `/hat` | `/hat` | `betteradmincommands.hat` |
| `/craft` | `/craft` | `betteradmincommands.craft` |
| `/enderchest` | `/enderchest [player]` | `betteradmincommands.enderchest` |
| `/invsee` | `/invsee <player>` | `betteradmincommands.invsee` |
| `/sudo` | `/sudo <player> <command>` | `betteradmincommands.sudo` |
| `/exp` | `/exp <show\|give\|set> [player] [amount]` | `betteradmincommands.exp` |
| `/time` | `/time <set\|add> <value> [world]` | `betteradmincommands.time` |
| `/weather` | `/weather <sun\|rain\|thunder> [world]` | `betteradmincommands.weather` |
| `/ptime` | `/ptime [value]` | `betteradmincommands.ptime` |
| `/pweather` | `/pweather [value]` | `betteradmincommands.pweather` |
| `/world` | `/world [name]` | `betteradmincommands.world` (`.world.manage` to create/remove) |

#### World and mob tools — module `admin`

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

### Moderation — module `moderation`

| Command | Usage | Permission |
| --- | --- | --- |
| `/kick` | `/kick <player> [reason]` | `betteradmincommands.kick` |
| `/kickall` | `/kickall [reason]` | `betteradmincommands.kickall` |
| `/ban` | `/ban <player> [reason]` | `betteradmincommands.ban` |
| `/tempban` | `/tempban <player> <duration> [reason]` | `betteradmincommands.tempban` |
| `/ipban` | `/ipban <player> [reason]` | `betteradmincommands.ipban` |
| `/unban` | `/unban <player>` | `betteradmincommands.unban` |
| `/unbanip` | `/unbanip <address\|player>` | `betteradmincommands.unbanip` |
| `/banlist` | `/banlist` | `betteradmincommands.banlist` |
| `/mute` | `/mute <player> [duration] [reason]` | `betteradmincommands.mute` |
| `/unmute` | `/unmute <player>` | `betteradmincommands.unmute` |
| `/kill` | `/kill [player]` | `betteradmincommands.kill` |
| `/suicide` | `/suicide` | `betteradmincommands.suicide` |

**Vanishing.** `/vanish` hides a player from everyone who is not allowed to see them. With
`betteradmincommands.vanish.see` the player stays visible in the world *and* in the tab list, where
they carry the cue from `moderation.vanish-tab-cue` (default `[V]`) in front of their rank prefix —
with TAB installed the cue is drawn through TAB's API. Vanish state is in memory only: a restart
brings everyone back visible.

### Economy — module `economy`

| Command | Usage | Permission |
| --- | --- | --- |
| `/balance` | `/balance [player]` | `betteradmincommands.balance` |
| `/baltop` | `/baltop [page\|list]` | `betteradmincommands.baltop` |
| `/pay` | `/pay <player> <amount>` | `betteradmincommands.pay` |
| `/eco` | `/eco <give\|take\|set\|reset\|resetall\|balance\|top> <player> [amount]` | `betteradmincommands.eco` (or `.eco.<action>`) |
| `/worth` | `/worth [item]` | `betteradmincommands.worth` |
| `/sell` | `/sell <hand\|all\|amount>` | `betteradmincommands.sell` |

`/balance` prints the amount **and** opens the balance menu, where **Send money** picks the receiver
from the online players (no name to type) and then asks for the amount in a window. `/baltop` is a
paged grid of heads with the viewer's own rank highlighted. Every transfer through the menu goes
through the same code as `/pay`, so the rules cannot differ between the two.

### Trading — module `trade`

`/trade <player>` asks the other side first (they get a clickable accept/deny message, unless
`trade.require-accept: false`) and then opens a two-sided window. Each player puts items into their
own half and can offer money with the gold button; when **both** sides confirm, the offers are
swapped. Closing the window, disconnecting or a balance that dropped in the meantime cancels the
trade and hands everything back, so an item can never be lost.

```
/trade <player>           ask for a trade
/trade accept [player]    accept a request
/trade deny [player]      decline a request
/trade log [player]       the last trades (your own, or everyone's for staff)
```

Every finished trade is written to the `trades` table **and** mirrored into `data/trades.yml`.
Records older than `trade.log.retention-hours` (48 by default) are deleted from both, and only the
newest `trade.log.max-cached` records are held in memory.

### Auction house — module `auction`

`/ah` opens the whole auction house as a menu:

- **Browse** — a paged grid of every active listing. Click one to see the item, the price and how
  long it still runs, then confirm the purchase. A **sort** button cycles through newest, oldest,
  cheapest, most expensive and item name A-Z, and a **search** button filters by item name and
  seller; both stick until cleared.
- **Sell held item** — hold an item, press the button and pick the price with +/- buttons
  (1 / 10 / 100 / 1000 / 100000) or an exact price in a dialog window. The stack is taken out of
  your hand once the listing exists.
- **My listings** — everything you listed; click a listing to cancel it.
- **Claims** — items from cancelled or expired listings wait here until collected.

The buyer pays the listed price; the seller receives it minus `auction.tax-percent`. An optional
up-front `auction.listing-fee-percent` and the price window (`auction.min-price` /
`auction.max-price`) are enforced. Listings live in MySQL (`<prefix>ah_listings`) and are mirrored
into `data/auction.yml`.

### Reports — module `report`

`/report` is the whole report system as a menu:

- **Create a report** — pick a category, then the player it is about (categories that need no player
  skip that step), then describe it in a window.
- **My tickets** — every ticket you opened, open and closed, with an unread marker.
- Staff with `betteradmincommands.report.staff` also get **All open** and **All closed**, and
  `/reports` jumps straight to the open ones.

A ticket is a two-way thread: the player and **any number of staff** reply into it, everybody who
took part is a participant, and when a message arrives the other online participants are told in
chat with a clickable button — and again when they join if they still have unread messages.
Categories live in `report.presets`; the menu pages itself automatically.

Reports are stored in MySQL (`<prefix>reports`, `report_messages`, `report_participants`). Unlike the
economy, homes and mail they have **no local safe file**, so reports need the database to be
reachable.

### Teleporting — modules `teleport` and `spawn`

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
| `/tpaaccept` | `/tpaaccept [player]` (aliases `/tpaccept`, `/tpyes`) | `betteradmincommands.tpaccept` |
| `/tpadeny` | `/tpadeny [player]` (aliases `/tpdeny`, `/tpno`) | `betteradmincommands.tpdeny` |
| `/tptoggle` | `/tptoggle` | `betteradmincommands.tptoggle` |
| `/back` | `/back` | `betteradmincommands.back` |
| `/rtp` | `/rtp` | `betteradmincommands.rtp` |
| `/jump` | `/jump` | `betteradmincommands.jump` |
| `/top` | `/top` | `betteradmincommands.top` |
| `/bottom` | `/bottom` | `betteradmincommands.bottom` |
| `/descend` | `/descend` | `betteradmincommands.descend` |

A `/tpa` request shows the player who has to answer it a notice with two clickable buttons:

```
[BetterAdmin] Steve wants to teleport to you. [Accept] [Deny]
```

**Accept** runs `/tpaaccept Steve` and **Deny** runs `/tpadeny Steve` for whoever clicked. Teleports
are instant for staff holding `betteradmincommands.teleport.bypass` — no warm-up countdown and no
`/back` cooldown.

### Homes and warps — modules `homes` and `warps`

| Command | Usage | Permission |
| --- | --- | --- |
| `/home` | `/home [name]` | `betteradmincommands.home` |
| `/sethome` | `/sethome [name]` | `betteradmincommands.sethome` |
| `/delhome` | `/delhome <name>` | `betteradmincommands.delhome` |
| `/homes` | `/homes` | `betteradmincommands.homes` |
| `/warp` | `/warp <name>` | `betteradmincommands.warp` (and `.warp.<name>`) |
| `/warps` | `/warps` | `betteradmincommands.warps` |
| `/setwarp` | `/setwarp <name>` | `betteradmincommands.setwarp` |
| `/delwarp` | `/delwarp <name>` | `betteradmincommands.delwarp` |

`/homes` shows your homes — **left-click teleports**, **right-click opens a confirmation** before a
home is deleted. `/warps` shows every warp as a button; warps a player may not use are greyed out
with the reason, so they can see a warp exists without being able to jump there. Both keep a text
form (`/homes list`, `/warps list`).

### Items — module `items`

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

### Kits — module `kits`

`/kit [name]` gives a configured kit; without an argument it opens the kit menu, where every kit
shows its contents, its cooldown and whether it is for you. Cooldown and permission states are greyed
out with the reason, so "not yet" and "not for me" look different. Kit entries are re-read on every
use, so editing them does not need a restart. Each kit can have its own `permission:` and `icon:`.

### Jail — module `jail`

| Command | Usage | Permission |
| --- | --- | --- |
| `/jail` | `/jail <player> [cell] [duration]` | `betteradmincommands.jail` |
| `/setjail` | `/setjail <name>` | `betteradmincommands.setjail` |
| `/deljail` | `/deljail <name>` | `betteradmincommands.deljail` |
| `/jails` | `/jails [list]` | `betteradmincommands.jails` |
| `/unjail` | `/unjail <player>` | `betteradmincommands.unjail` |
| `/togglejail` | `/togglejail <player> [cell]` | `betteradmincommands.togglejail` |

`/jails` lists every cell with its world and coordinates; **clicking one teleports you to it**, which
is what the list is for. Deleting a cell is deliberately *not* in the menu — `/deljail` stays the
only way, so a misclick cannot empty a cell someone is sitting in.

### Social, info and the rest

| Command | Usage | Permission |
| --- | --- | --- |
| `/msg` | `/msg <player> <message>` | `betteradmincommands.msg` |
| `/reply` | `/reply <message>` | `betteradmincommands.reply` |
| `/socialspy` | `/socialspy` | `betteradmincommands.socialspy` |
| `/ignore` | `/ignore <player>` | `betteradmincommands.ignore` |
| `/ignorelist` | `/ignorelist [list]` | `betteradmincommands.ignorelist` |
| `/me` | `/me <action>` | `betteradmincommands.me` |
| `/mail` | `/mail [send\|read\|clear] [player] [message]` | `betteradmincommands.mail` |
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
| `/notify` | `/notify [<name> [on\|off]\|all on\|off\|reset]` | `betteradmincommands.notify` |
| `/nick` | `/nick <nickname\|off> [luckperms-group]` | `betteradmincommands.nick` |
| `/skinchange` | `/skinchange <username\|uuid\|value <texture> <signature>\|off>` | `betteradmincommands.skinchange` |

`/mail` is the mailbox: the inbox newest first, one head per sender, with the date, a preview and a
**New message** marker. Opening a message marks it read and offers **Reply** and **Delete**, and the
menu can also **Write a message**, **Mark all as read** and **Clear mailbox** behind a
confirmation. `/ignorelist` shows every ignored name as a head; **clicking one stops ignoring that
player**.

## Shop: taking over EconomyShopGUI

`/shop` opens the server shop, which is **imported from EconomyShopGUI** rather than written by
hand. On the first start the plugin reads every YAML file it finds in `plugins/EconomyShopGUI*/`,
turns the entries into its own shops, and then **switches EconomyShopGUI off**
(`shop.import.disable-plugin`, on by default) so the two cannot both answer to `/shop`.

The importer is deliberately version-proof: instead of relying on one edition's layout it walks the
whole file and treats *anything that names a material* as a shop entry, which covers the free and the
premium edition, `pages:` and `sections:` layouts, and shop files written by hand.

**One file becomes one shop.** The shop is named after the file, unless the file names itself with
`shop-name` / `title` / `display-name`. An `icon:` (or `display-item:`) sets the button in the shop
list, and `rows:` / `size:` sets the window height.

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

1. Make sure EconomyShopGUI is installed and working as usual.
2. Update this plugin and restart once. Watch the console for
   `Imported <n> shop(s) with <m> item(s) from EconomyShopGUI`.
3. Run `/shop`. Shops, pages, prices and displays should match what you had.
4. Keep the `plugins/EconomyShopGUI*/` folder for a while as a backup.

### Editing the shop in game

`/shop edit` (or the **Edit shop** button, both for `betteradmincommands.shop.admin`) changes the
shop without touching a file: shop list → shop view → entry editor (set buy price, set sell price,
`off` removes it, set the amount one purchase hands out, take it off sale, delete behind a
confirmation) plus a shop settings page that writes `shop.access.permissions` into `config.yml` for
you. Everything is entered in a window.

Edits go into memory, the local safe file **and** MySQL, so they survive a restart. A re-import with
`/shop import` (or `shop.import.force`) rebuilds the shop from the EconomyShopGUI files and therefore
discards in-game edits — the import button says so.

### Shop access

Every shop can be locked behind its own permission. Three ways, checked in this order:

1. `shop.access.permissions.<shop-id>` — an explicit permission for one shop.
2. `shop.access.free` — a list of shop ids that stay open whatever the prefix says (`*` = all).
3. `shop.access.permission-prefix` — the permission every other shop needs, with the shop id
   appended. The id is the file path without the extension, with anything outside `a-z`, `0-9`, `_`,
   `-` and `.` turned into a `.`, so `blocks/mob_drops.yml` becomes `blocks.mob_drops`.

**The default prefix is empty, which leaves every shop open.** A locked shop is handled in three
places so it cannot be worked around: it shows greyed out in the list (or hidden, with
`shop.access.hide-locked: true`), direct openings — including from search results — are refused, and
buying/selling re-checks server side. `betteradmincommands.shop.admin` bypasses every check.

### What is not carried over

- **EconomyShopGUI's own commands** (`/sellall`, its reload, …) go away with the plugin. `/sell`,
  `/worth` and `/balance` cover the common cases.
- **Its permission nodes** (`economyshopgui.shop.<name>`, …) are replaced by `shop.access`.
- **Player heads by name** are imported without a skin when the owning account is not already
  cached, because looking a name up over the network during the import would freeze the start-up.

## Economy and Vault

The built-in economy is stored in the `players` table and offered to **Vault**, so any Vault-aware
plugin can read and modify balances.

- If Vault is missing, the plugin logs a warning and the economy still works in-game — it just is not
  shared with other plugins.
- If another economy plugin is registered with Vault, this plugin registers itself with the *highest*
  priority and logs a warning. Run one economy at a time.
- Balances are cached in memory and written back to MySQL asynchronously every
  `economy.save-interval-seconds` (and on shutdown), so the main thread never waits on the database.
- Switching the `economy` module off (or `economy.enabled: false`) disables the economy, its commands
  and its Vault registration together.

## Trading, reports and notifications

The plugin tells staff about things that happen (a trade, a new report, a kick, a ban, an unban) and
reminds players about unread mail. Each of those is a **category** with its own permission, listed
under `notifications.categories`. A player receives a notification only when they hold its
permission *and* have not switched it off:

```
/notify                     list every notification with its state
/notify <name>              flip one on or off
/notify <name> on|off       set one outright
/notify all on|off          set every one at once
/notify reset               back to the server defaults
```

Add or remove categories in `config.yml` and the command picks them up automatically.

## Nicknames, ranks and skins

### `/nick`

`/nick <nickname>` changes **your own** name in the **tab list** and in **chat**. The nickname is
stored in the database, so it survives restarts, and `/realname <nick>` finds the player behind it.
Only your own name can be changed.

```
/nick <nickname>          your nickname
/nick <nickname> <group>  borrow the prefix of a LuckPerms group
/nick <nickname> off      drop the group prefix, keep the nickname
/nick off                 remove the nickname entirely
```

The group argument does **not** change permissions or the real LuckPerms group — it only takes that
group's **prefix** and shows it in front of the nickname. TAB completion for the group comes from
LuckPerms, and with a borrowed group TAB is told to treat the player as if they really held that rank
(`setTemporaryGroup`), so an owner who nicks as `player` is sorted as a player.

**Ranks.** With LuckPerms installed, `/nick <nickname>` shows **your own rank prefix** in front of
the nickname. A rank prefix is shown in front of **every** player's name — nick or not — in the tab
list, in chat and in the commands that name players (`/msg`, `/reply`, `/me`, `/list`, the social
spy line). `nick.show-rank-prefix: false` turns the automatic prefix off.

**Name tags.** There is no name tag above a player's head at all (`nick.hide-nametag`, on by
default), so the tab list is the only place a name shows up. With TAB installed the plugin drives it
through **TAB's API** (tab list name, rank prefix, borrowed rank, hidden name tag); without TAB the
same is done through Bukkit and one scoreboard team per player. The teams are removed again when the
plugin is disabled, and `hide-nametag: false` turns the scoreboard part off for servers where
another plugin relies on the teams.

**Colour in chat.** Players holding `betteradmincommands.chat.color` can use `&` colour codes in
their chat messages; `betteradmincommands.nick.color` does the same for a nickname.

**Showing the nickname in TAB** (or another tab-list plugin) works through PlaceholderAPI instead of
fighting over the same packets:

| Placeholder | Value |
| --- | --- |
| `%betteradmincommands_nickname%` | group prefix + nickname, or the real name when none is set |
| `%betteradmincommands_nickname_raw%` | only the nickname, or the real name when none is set |
| `%betteradmincommands_nick_prefix%` | the borrowed LuckPerms group prefix, or empty |

For TAB, set `customtabname` in `groups.yml`/`users.yml` to `%betteradmincommands_nickname%`, and
replace `%essentials_nickname%` with the same placeholder in the `nick` condition in `config.yml`.

### `/skinchange`

`/skinchange <username|uuid>` puts the skin of a premium account on **you**; only your own skin can
be changed.

```
/skinchange <username>         use the skin of that premium account
/skinchange <uuid>             the same, naming the account by its UUID
/skinchange value <texture> <signature>   use a pasted texture
/skinchange off                back to your own skin
```

There is no NameMC API — NameMC is a viewer for Mojang's own data — so the plugin asks Mojang
directly: `api.minecraftservices.com` resolves the name to a UUID (`api.mojang.com` as a fallback),
then `sessionserver.mojang.com` returns the skin as a texture **with its Mojang signature** (clients
refuse to render unsigned textures, which is why this works on an offline-mode server too).

- The lookup runs asynchronously, never on the server thread.
- Results are cached for `skin.cache-minutes` (default 60) and identical requests at the same time
  only send one request, so the server is not rate limited.
- The borrowed skin is stored and applied again on every join; `/skinchange off` drops it and asks
  Mojang for the player's own skin.

The server needs outbound HTTPS access to `api.mojang.com` and `sessionserver.mojang.com`; no API key
is required.

## Permissions

The permission tree is defined in `plugin.yml`:

| Permission | Default | Grants |
| --- | --- | --- |
| `betteradmincommands.*` | op | Everything below |
| `betteradmincommands.player` | true | Every command a normal player may use |
| `betteradmincommands.mod` | op | Moderation, jail, vanish, invsee, sudo |
| `betteradmincommands.admin` | op | Full administrative access |

Extra permissions used for finer control:

- `betteradmincommands.vanish.see` — see vanished players in the world and in the tab list
- `betteradmincommands.getpos.others`, `.playtime.others`, `.balance.others` — target other players
- `betteradmincommands.nick.color`, `.chat.color` — use colour codes in a nickname / in chat
- `betteradmincommands.realname`, `.reveal` — look up the real name and rank behind a nickname
- `betteradmincommands.auction` / `.auction.sell` — open the auction house / list items (default `true`)
- `betteradmincommands.shop` / `.shop.sell` (default `true`), `.shop.admin` (default `op`) — open the
  shop, sell to it, and import/bypass `shop.access`
- `betteradmincommands.shop.<shop-id>` — access to a single shop when `shop.access.permission-prefix`
  is set
- `betteradmincommands.report` (default `true`), `.report.staff` (default `op`) — open tickets / see
  and answer every ticket
- `betteradmincommands.trade` (default `true`), `.trade.log.others`, `.trade.notify`
- `betteradmincommands.kick.notify`, `.ban.notify`, `.unban.notify` — staff notifications (each can be
  switched off per player with `/notify`)
- `betteradmincommands.enderchest.others`, `.broadcast.receive`, `.unlimited.list`
- `betteradmincommands.world.manage` — create and remove worlds
- `betteradmincommands.teleport.bypass` — skips the teleport warm-up and the `/back` cooldown
- `betteradmincommands.homes.limit.<amount>` — per-player home limit override
- `betteradmincommands.kit.<name>` — access to one kit
- `betteradmincommands.warp.public` (default `true`), `.warp.<name>` — warp access
- `better_admin_commands.permissionall` — one node that unlocks everything (left of the classic
  `betteradmincommands.*` wildcard, both are accepted)

Handing out a hundred nodes is tedious, so that single node unlocks the whole plugin wherever a
specific node is checked. On a server **without** a permission plugin, the `permissions` section of
`config.yml` decides what everyone gets from the start:

```yaml
permissions:
  default-access: default   # default | all | none
  groups:
    player: true            # /home, /pay, /ah, /shop, /trade, ...
    mod: false              # /ban, /kick, /mute, ...
    admin: false            # /gm, /give, /eco, ...
```

`default-access: all` gives every command to everyone, `none` gives nothing. The group switches apply
in `default` mode. They are applied when a player joins and after `/betteradmincommands reload`.

## Configuration reference

All settings live in `config.yml` (generated on first start).

### Updating config.yml safely

The file starts with a version marker:

```yaml
config-version: 14
```

On every start (and on `/betteradmincommands reload`) the plugin compares your file with the template
shipped inside the jar and **adds options that are missing**. Options that already exist are never
touched, so your values survive plugin updates. When the version marker changes, a copy of the
previous file is written next to it as `config-backup-v<old>-<timestamp>.yml` first. The file is only
rewritten when something actually changed, and comments for newly added options are copied over.
Do not edit `config-version` by hand — that is what tells the plugin which migration is outstanding.

### `modules`

See [Turning features on and off](#turning-features-on-and-off-modules). One `enabled: true/false`
per module, plus `disabled-commands` for single commands.

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

### `permissions`

| Key | Default | Description |
| --- | --- | --- |
| `default-access` | `default` | `default`, `all` or `none` — what everyone may use out of the box |
| `groups.player` / `.mod` / `.admin` | `true` / `false` / `false` | Which bundles every player gets in `default` mode |
| `wildcards` | `better_admin_commands.permissionall`, `betteradmincommands.*` | Nodes that unlock everything |

### `auction`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable the auction house and its expiry task (the module must be on too) |
| `gui-rows` | `6` | Chest rows per window (3-6); the bottom row is navigation |
| `listing-hours` | `48` | How long a listing stays up, `0` never expires |
| `max-listings-per-player` | `10` | Active listings a player may have at once |
| `tax-percent` | `5.0` | Share of the sale price the server keeps |
| `listing-fee-percent` | `0.0` | Up-front fee when listing, as a % of the price |
| `min-price` / `max-price` | `1.0` / `1000000000.0` | Allowed price window |

### `shop`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable `/shop` (the module must be on too) |
| `gui-rows` | `6` | Chest rows per window (3-6); the bottom row is navigation |
| `selling-enabled` | `true` | Let players sell items back where the import has a sell price |
| `search-enabled` | `true` | Show the search button that filters every shop |
| `access.permission-prefix` | `""` (open) | Permission every shop needs, with the shop id appended |
| `access.free` | `[]` | Shop ids that stay open, or `*` for all of them |
| `access.permissions.<shop-id>` | — | Per-shop permission; wins over the prefix |
| `access.hide-locked` | `false` | Hide shops a player cannot use instead of greying them out |
| `import.enabled` | `true` | Import the EconomyShopGUI files on start-up when no shop is stored yet |
| `import.force` | `false` | Re-import and overwrite on every start-up |
| `import.disable-plugin` | `true` | Switch EconomyShopGUI off once its shops are imported |
| `import.max-items` | `5000` | Stop after this many imported items |
| `import.folders` | `EconomyShopGUI`, `EconomyShopGUI-Premium` | Folders inside `plugins/` to read |

Any folder in `plugins/` whose name starts with `EconomyShopGUI` is read as well, so the free and the
premium edition are both found without editing the list.

### `trade`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable `/trade` (the module must be on too) |
| `require-accept` | `true` | Ask the other player first (they get a clickable accept/deny) |
| `request-expire-seconds` | `60` | Lifetime of a trade request |
| `log.enabled` | `true` | Keep a history of finished trades |
| `log.retention-hours` | `48` | How long a record is kept before it is deleted |
| `log.max-cached` | `2000` | Records held in memory for `/trade log` |
| `log.notify-permission` | `betteradmincommands.trade.notify` | Who is told about a finished trade |

### `economy`

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enable the built-in economy and Vault registration (the module must be on too) |
| `currency-name-singular` / `-plural` | `Dollar` / `Dollars` | Currency names shown by Vault |
| `currency-symbol` | `$` | Symbol used when formatting amounts |
| `starting-balance` | `100.0` | Balance a player gets the first time they are seen |
| `max-balance` | `1000000000.0` | Hard ceiling for balances |
| `save-interval-seconds` | `300` | How often dirty balances are written to MySQL |
| `allow-payments` | `true` | Allow `/pay` between players |
| `minimum-payment` | `0.01` | Smallest accepted payment |

### `report`

| Key | Default | Description |
| --- | --- | --- |
| `report.presets.<id>.display` | — | Name shown on the category button |
| `report.presets.<id>.icon` | — | Material used as the button |
| `report.presets.<id>.description` | — | Short line under the name |
| `report.presets.<id>.target-required` | `true` | Whether a player is picked before the description |

### `nick`, `skin`

| Key | Default | Description |
| --- | --- | --- |
| `nick.restricted` | `dev`, `owner` | Nicks and LuckPerms groups only server operators may use |
| `nick.show-rank-prefix` | `true` | Show the player's own LuckPerms rank prefix with `/nick <nick>` |
| `nick.hide-nametag` | `true` | Hide the name tag above **every** player's head |
| `nick.chat-format` | `&f<%nickname%>&r %message%` | The chat line, used while no other plugin formats chat |
| `skin.cache-minutes` | `60` | How long a skin fetched from Mojang is cached |
| `skin.timeout-seconds` | `10` | How long to wait for an answer from Mojang |

### `spawn`, `homes`, `warps`, `teleport`

| Key | Default | Description |
| --- | --- | --- |
| `spawn.teleport-on-join` | `false` | Teleport every player to spawn on join |
| `spawn.teleport-on-first-join` | `false` | Teleport only on a player's first join |
| `spawn.respawn-at-spawn` | `false` | Respawn players at spawn |
| `homes.max` | `3` | Default homes per player (override with `betteradmincommands.homes.limit.<amount>`) |
| `homes.gui-rows` / `homes.gui-icon` | `6` / `RED_BED` | Layout of the `/homes` menu |
| `warps.gui-rows` / `warps.gui-icon` | `6` / `COMPASS` | Layout of the `/warps` menu |
| `warps.gui-icons.<name>` | — | Optional icon for one warp, e.g. `spawn: NETHER_STAR` |
| `teleport.warmup-seconds` | `3` | Time a player must stand still before a delayed teleport |
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

### Menu layouts

| Key | Default | Description |
| --- | --- | --- |
| `baltop.gui-rows` | `6` | Rows of the leaderboard menu (3-6) |
| `mail.gui-rows` | `6` | Rows of the mailbox menu (3-6) |
| `ignorelist.gui-rows` | `6` | Rows of the ignore menu (3-6) |
| `jails.gui-rows` / `jails.gui-icon` | `6` / `IRON_BARS` | Layout of the jail list |
| `ptime.gui-rows` / `pweather.gui-rows` | `3` | Rows of the personal time and weather menus |
| `unlimited.gui-rows` | `4` | Rows of the unlimited-items menu |
| `realname.gui-rows` | `6` | Rows of the nickname overview |
| `kit-gui.rows` / `kit-gui.icon` | `3` / `CHEST` | Layout of the `/kit` menu; a single kit can set its own `icon:` |

### `notifications`, `worth`, `kits`, `messages`

- **`notifications.categories.<id>`** — `display`, `permission` and `default` for each notification a
  player can toggle with `/notify`. The shipped ids are `trade`, `report`, `kick`, `ban`, `unban` and
  `mail`; add your own and they show up in the command automatically.
- **`worth`** — map of `MATERIAL: price` used by `/worth` and `/sell`. Only listed items can be sold.
- **`kits`** — kit definitions. Item format: `"MATERIAL[:AMOUNT]"` or
  `"MATERIAL:AMOUNT:ENCHANTMENT:LEVEL"`. A cooldown of `-1` disables the cooldown. Kit entries are
  re-read on every use, so editing them does not need a restart.
- **`messages`** — `prefix`, `motd` and `rules` lists. Colour codes use `&` (e.g. `&6`).

## How storage works

The plugin runs in one of two modes, switched automatically:

- **Database available** — MySQL is the source of truth. Reads come from the database; writes are
  applied in memory immediately and flushed asynchronously (balances every
  `economy.save-interval-seconds`, other services per operation). Every write is *also* mirrored into
  the local safe files, so they stay current.
- **Database unavailable** — the plugin starts anyway and falls back to the local safe files in
  `plugins/Better_Admin_Commands/data/`. Players can keep playing, nothing errors out, and every write
  is stored locally.

When the connection comes back (periodic retry or `/betteradmincommands reconnect`), the local safe
files are synced into MySQL so no data is lost. This makes the database optional: you can run the
plugin entirely from the local files and add MySQL later without losing what players already did.

Reports are the one exception — they have no local safe file and need the database.

## Backups and data files

`/betteradmincommands backup` dumps every table into

```
plugins/Better_Admin_Commands/backups/<yyyy-MM-dd_HH-mm-ss>/
├── players.yml
├── homes.yml
├── player_settings.yml
├── mail.yml
└── meta.yml
```

The dump reads its columns from the result set, so it keeps working if a table gains a column, and it
falls back to the local safe files when the database is unavailable. It runs asynchronously, but on a
large server it is still worth running during low traffic.

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
down and are pushed back into the database once the connection returns — so do not delete them while
the database is unreachable, and keep them if you migrate servers.

## Troubleshooting

| Symptom | What to do |
| --- | --- |
| A command answers "this feature is switched off" | Check `modules.<id>.enabled` (and for economy, trade, auction and shop also their own `enabled` key) — `/betteradmincommands modules` shows the state |
| Nothing works at all, every command answers with a notice | The plugin was switched off with `/betteradmincommands disable`; use `/betteradmincommands enable` |
| A command does nothing at all | Another plugin declares the same name — `/betteradmincommands info` lists the commands this plugin could not bind |
| No dialogues open | The client is older than 1.21.6; the same question is printed in chat and the answer is read from there |
| Shop is empty after installing EconomyShopGUI | `/shop import`, or check `shop.import.folders` and the console line naming the files that were read |
| Shop prices look wrong | Entries the import could not read get `buy_price = -1`; the editor marks them, `/shop edit` fixes them |
| Report system "unavailable" | Reports need MySQL; there is no local safe file for tickets |
| Name tags are back | `nick.hide-nametag` is off, or another plugin (scoreboard/TAB) took the teams over |
| Rank prefix missing in chat | Another chat plugin formats chat; make it use the display name or the `%betteradmincommands_nickname%` placeholder |
| Database wrote nothing | `/betteradmincommands database` walks the connection and names the failing step |

---

# For developers

## Building from source

The project uses the Gradle wrapper and the Shadow plugin (the MySQL driver is shaded in and
relocated).

```bash
./gradlew build          # compiles and produces the shaded plugin jar
./gradlew runServer      # starts a Paper 1.21 test server with the plugin installed
```

Requirements: **JDK 21**.

> `build.gradle` contains a hardcoded output path in `tasks.shadowJar`:
>
> ```groovy
> def serverdir = "D:\\devlopment\\Minecraft\\Plugins\\TestServer\\1.21.11\\plugins"
> ```
>
> Change that line to your own `plugins/` folder before building, otherwise the jar is written to a
> path that probably does not exist on your machine. Alternatively grab the jar from `build/libs/`.

## Project layout

```
src/main/java/io/sniperjohnny/github/better_admin_commands/
├── Better_Admin_Commands.java   plugin entry point: builds every service, wires it together
├── feature/                     FeatureService: the config.yml "modules" switchboard
├── config/                      ConfigUpdater: adds missing options to config.yml
├── commands/                    one class per command, grouped by area
│   └── admin/ economy/ home/ info/ items/ jail/ moderation/ social/ teleport/
├── listeners/                   join, quit, chat, activity, respawn, trade, jail, powertool, unlimited
├── gui/                         Menu, Items, Theme, Gui_Listener, ChatPromptService, DialogPromptService
├── storage/                     Database (JDBC pool) and LocalStore (the data/*.yml mirror)
├── economy/ auction/ shop/ trade/ report/ mail/ jail/ kit/ home/ warp/ spawn/
├── moderation/ notify/ permission/ player/ placeholder/ powertool/ skin/ teleport/
└── util/                        Msg, Targets, PersonalDisplay, ...

src/main/resources/
├── plugin.yml                   every command, its usage and its permission
└── config.yml                   the template shipped inside the jar (see ConfigUpdater)
```

## How it fits together

`Better_Admin_Commands#onEnable` is the single place where the plugin is assembled, in this order:

1. **config** — `ConfigUpdater#update()` brings `config.yml` up to date, then `Msg.setPrefix`.
2. **permissions** — `PermissionService` decides what a player may use when no permission plugin
   hands out the nodes.
3. **features** — `FeatureService` reads the `modules` section. Everything registered later is gated
   by it.
4. **storage** — the `LocalStore` mirrors are loaded, then `Database#connect()` is attempted. A
   failure here is *not* fatal: the plugin logs and keeps going from the local files.
5. **services** — economy, homes, mutes, mail, auction, shop, trades, reports, jail, and the runtime
   services (teleport, tpa, vanish, nick, tab, skin, afk, …).
6. **wiring** — `registerCommands()`, `applyRootCommand()`, `registerListeners()`, `startTasks()`,
   `registerPlaceholders()`.

The getters at the bottom of the main class (`plugin.economy()`, `plugin.shops()`, `plugin.dialogs()`,
…) are how the rest of the code reaches a service — nothing keeps its own static instance except
`get_Instance()` for the rare listener that has no reference.

### Storage model

- **`Database`** owns a small JDBC pool and creates every table in `createTables`. Nothing else
  opens a connection.
- **`LocalStore`** is one YAML file per table under `data/`. Every service writes memory + safe file
  synchronously and pushes to MySQL asynchronously, and `resyncToDatabase()` on each service is what
  pushes the local files back after a reconnect. Adding a new service means adding it to
  `resyncLocalToDatabase()`.
- Reads come from an in-memory cache where practical (`EconomyService` is the model for this).

### Menus

All chest windows go through the `gui` package:

- `Menu` is an `InventoryHolder` chest with `button(slot, item, handler)`, `frame()`, `open(player)`
  and `handleClick(event)`. Paging rebuilds the menu per page — chest inventories are small and this
  keeps the code simple.
- `Items` builds display items (and player heads), `Theme` holds the shared border/filler/title look.
- `Gui_Listener` is the *only* inventory listener: it finds the `Menu` behind an inventory, cancels
  the click and dispatches it to the slot's handler.

### Dialogs

`DialogPromptService` is the one place that asks a player for a value. It uses Paper's dialog API
(`Dialog`, `DialogType`, `DialogInput`) so the player types into a field in a window:

```java
plugin.dialogs().text(player, title, question, label, initial, maxLength, answer -> { … });
plugin.dialogs().number(player, title, question, label, initial, maxLength, answer -> { … });
plugin.dialogs().message(player, title, question, label, initial, maxLength, lines, answer -> { … });
plugin.dialogs().slider(player, title, question, label, min, max, step, initial, answer -> { … });
plugin.dialogs().confirm(player, title, question, yes, no, answer -> { … });
```

Three rules the service guarantees, so callers do not have to think about them:

1. the callback runs **on the server thread**, exactly once (an `AtomicBoolean` guards it), with
   `CANCEL` for the cancel button — check it with `DialogPromptService.isCancel(answer)` rather than
   comparing the string yourself, so every caller agrees on what "backed out" means;
2. a client that cannot show a dialog answers the same question **in chat** instead —
   `ChatPromptService` is armed first and both paths share the once-only callback;
3. an escape or a click outside answers nothing at all.

The callback usually reopens the menu it came from, because opening a dialog closes the inventory.
When a flow must keep its window open while the question is asked (the trade money button), the
session is marked with `setAwaitingInput(true)` so the close handler does not cancel it.

**Use a dialog, not a chat prompt, for anything a menu button asks.** `ChatPromptService` exists only
as the fallback described above.

### Dialog conventions

- **Titles** follow `<Screen> » <Field>`, e.g. `Shop » Search`, `Shop editor » Buy price`,
  `Reports » Reply #a1b2`. The screen half matches the command the player came from.
- **Cancel always goes back.** Every cancel path prints one short line and reopens the menu the
  dialog was opened from (or, for a command, simply stops), so a player can never end up staring at
  an empty screen.
- **Invalid input is explained and the menu is reopened** — a dialog cannot stay open after a click,
  so `That is not a valid price.` plus the originating menu is the flow, rather than silently doing
  nothing.
- **Chat wording follows the client.** `askInChat(...)` sends the question plus a one-line reminder
  for a client that can show the dialog (`getProtocolVersion() >= 771`, i.e. 1.21.6), and the full
  question plus the verbose hint for an older one. An unknown client always gets the fallback.
- **A pending chat answer expires** after `ChatPromptService.LIFETIME_MILLIS` (two minutes). Without
  that, dismissing a dialog with escape would leave the armed prompt in place and the player's next
  chat message would be consumed instead of being sent.
- **`confirm(...)` has a chat fallback too**: a client that cannot show the dialog types
  `yes`/`confirm` or `no`/`deny` (and `cancel` answers nothing).

## The module system

`FeatureService` (package `feature`) is the switchboard. It holds the module table:

```java
new Module("shop", "Shop", List.of("shop"))
```

and derives two lookups from it at construction time — module id → module, and **command or
alias → module**, read from `plugin.yml` through `getCommand(name).getAliases()`, so `/money`
follows `/balance` without extra bookkeeping.

Public API:

| Method | Purpose |
| --- | --- |
| `modules()` / `moduleIds()` / `module(id)` | the module table, for `/betteradmincommands modules` and tab completion |
| `moduleOf(command)` / `display(id)` | which module a command belongs to, and its display name |
| `enabled(id)` | module on? Honours the module key **and** the legacy `economy.enabled` / `trade.enabled` / `auction.enabled` / `shop.enabled` key (both must be on) |
| `commandEnabled(command)` | module on **and** not in `modules.disabled-commands` |
| `blockReason(command)` | the message shown by a switched-off command, or `null` |
| `describeAll()` | pre-formatted lines for the command output |

Where it is enforced:

- **commands** — `Better_Admin_Commands#bind(name)` is the single decision point. It points a
  `PluginCommand` at its real executor, at `FeatureDisabled_Command` (which reads the module back
  out of the `Command` it is handed and prints `blockReason`), or at `Disabled_Command` when the
  whole plugin is off. `register(...)`, `rebindCommands()`, `setPluginDisabled(...)` and
  `applyFeatures()` all funnel through it, so a binding can never drift away from `config.yml`.
  The management command is deliberately left alone.
- **listeners** — `registerListeners()` consults `features.enabled(...)` per module and always
  starts with `unregisterListeners()` so re-registering cannot double-handle events.
- **tasks** — `startTasks()` / `start*Task()` check the module before scheduling.
- **behaviour that is not a command** — for example `Join_Listener` checks `features.enabled("spawn")`
  before moving a player to spawn.

### Adding a module

1. Add a `new Module("yourid", "Your name", List.of("command1", "command2"))` entry to
   `FeatureService.DEFINITIONS`.
2. If it owns listeners or tasks, gate them in `registerListeners()` / `startTasks()`.
3. Add the module to the `modules:` section of `config.yml` (with `enabled: true`) and bump
   `config-version` so existing installs pick the new section up.
4. Document it in the README table.

Modules only control what is *registered*. Gating a command that is already bound is enough to make
it answer with the "switched off" note, which is why nothing has to exist twice.

## Adding a command

1. Create a `TabExecutor` in the right `commands/` sub-package.
2. Add its name, usage, description and permission to `plugin.yml` — a command that is not declared
   there cannot be bound.
3. Register it in `registerCommands()` with `register("name", new Your_Command(this))`. `register`
   stores the executor and calls `bind`, which applies the module state for you.
4. If it is not part of an existing module, add it to the module's `commands()` list in
   `FeatureService` and mention the permission in the README.

Prefer pulling logic into a service (as `KitManager#claim`, `EconomyService#transfer` and
`util/PersonalDisplay` do) when both a command and a menu need it, so the two cannot drift apart.

## Adding a config option

1. Add the key with a comment to `src/main/resources/config.yml`.
2. **Bump `config-version`** at the top of that file. `ConfigUpdater` copies missing options —
   including their comments — into an installed `config.yml` and never overwrites a value that is
   already there. It is the version marker that triggers the migration and the backup.
3. Read it with `getConfig().getBoolean/…("section.key", default)` — and think about whether the
   default belongs in a module check, so a server that never touches the file behaves as before.

## Adding stored data

1. Add the table to `Database#createTables`.
2. Add a `LocalStore` for it in `onEnable` (mirror under `data/`) and load it.
3. Write through the same pattern as `EconomyService`: memory + safe file synchronously, MySQL
   asynchronously, and a `resyncToDatabase()` that the main class calls after a reconnect.
4. Add the table to `BackupService` so `/betteradmincommands backup` picks it up.

## Dependencies

| Dependency | Scope | Why |
| --- | --- | --- |
| `io.papermc.paper:paper-api` | `compileOnly` | the server API, including the dialog API |
| `com.github.MilkBowl:VaultAPI` | `compileOnly` | register the economy with Vault (guarded: only touched when Vault is installed) |
| `net.luckperms:api` | `compileOnly` | read group prefixes for `/nick` — never writes |
| `me.clip:placeholderapi` | `compileOnly` | publish the nickname for tab-list plugins |
| `com.github.NEZNAMY:TAB-API` | `compileOnly` | tab list names, rank prefixes, borrowed ranks, name tags |
| `com.mysql:mysql-connector-j` | `implementation` | shaded in and relocated to `…libs.mysql` by shadowJar |

All optional plugins are declared in `softdepend` and are only touched behind a
`getPluginManager().getPlugin("…") == null` guard, so the plugin runs without them.

## House rules

1. **Optional plugins stay optional.** Add to `softdepend`, guard the hook, never crash without it.
2. **All inventories go through the `gui` package** and the single `Gui_Listener`.
3. **Ask for input with `plugin.dialogs()`**, not with a chat prompt, and test a cancel answer with
   `DialogPromptService.isCancel(...)`.
4. **Strings use legacy `&` codes through `Msg`.**
5. **New data** = a table in `Database#createTables` **and** a `LocalStore` mirror under `data/`.
6. **Reads come from memory** where practical; writes update memory + safe file synchronously and
   push to MySQL asynchronously.
7. **New config** goes in `config.yml` and bumps `config-version`.
8. **New permissions** go in `plugin.yml` and are granted through the `player`/`mod`/`admin` bundles
   where sensible.
9. **New features are modules**: register them through `FeatureService` so they can be switched off.

---

## License

MIT — see [LICENSE](LICENSE).

```
MIT License

Copyright (c) 2026 AffenixStudios
```

You may use, modify and redistribute this plugin, including commercially, as long as the copyright
notice and the license text are kept with it.
