# Better_Admin_Commands — Feature Plan & Bug Log

> Living document. Every bug the user reports is appended to **§8 Known issues** so
> fixes are easy to follow. **§9 Progress** tracks what is actually done.

---

## 1. Where we are (baseline)

- Paper **1.21.x** plugin, Java **21**, Gradle + shadowJar, no new hard dependencies.
- Storage: **MySQL** with an always-on **local safe file** (`LocalStore`) mirror per
  table. The plugin keeps working from `data/*.yml` while MySQL is down and resyncs
  on reconnect.
- Commands are `TabExecutor`s registered in `Better_Admin_Commands#registerCommands`.
- **No GUI/menu framework exists yet.** Only `Disposal_Command` builds a raw
  inventory with an `InventoryHolder`.
- Economy is `EconomyService` (in-memory, mirror to DB) exposed through Vault.
- Nicknames are published to PlaceholderAPI so TAB can render them.

## 2. Ground rules (keep the codebase consistent)

1. Optional plugins stay optional: add to `softdepend`, guard the hook, never crash
   without them.
2. All inventories go through the single reusable `gui` package + one listener.
3. Strings use legacy `&` codes through `Msg`.
4. New data = a table in `Database#createTables` **and** a `LocalStore` mirror under
   `data/`.
5. Reads come from an in-memory cache where practical (like `EconomyService`), writes
   update memory + safe file synchronously and push to MySQL **async**.
6. New config goes in `config.yml`; bump `config-version` so `ConfigUpdater` migrates
   it (missing options are added, existing values are never overwritten).
7. New permissions go in `plugin.yml`, granted through the existing `player`/`mod`/
   `admin` groups where sensible.

## 3. Phase 1 — GUI framework  ✅ done

Package `io.sniperjohnny.github.better_admin_commands.gui`.

- `Menu` — an `InventoryHolder` chest menu: `button(slot, item, clickAction)`,
  `fillEmpty(item)`, `open(player)`, `render()`, `handleClick(event)`.
- `Items` — small `ItemStack` builder (`of(material, name, lore…)`, `filler()`).
- `Gui_Listener` — one listener: cancels clicks/drags inside a `Menu` and dispatches
  to the slot's handler. Registered in `registerListeners()`.

Paging is done by rebuilding the menu per page (cheap: chest inventories are small).

## 4. Phase 2 — Auction house `/ah`  ✅ done

One command, `/ah`, opens the whole auction house. No subcommands — everything is a
button in the GUI.

**Main menu**: Browse listings · Sell held item · My listings · Claims (only shown
when something is waiting).

**Flows**
- *Browse*: paged grid of active listings. Click a listing → confirm menu showing the
  item, price and time left → buy.
- *Sell*: reads the held item, asks for a price in chat (chat prompt service), checks
  the config limits, then takes the stack and creates the listing.
- *My listings*: paged; click a listing to cancel it (the item goes to Claims).
- *Claims*: items from cancelled/expired listings are handed back here.

**Data** — table `bac_ah_listings` mirror of `data/auction.yml`:

| column | type | notes |
| --- | --- | --- |
| `id` | CHAR(36) PK | plugin-generated UUID, doubles as the safe-file key |
| `seller_uuid` / `seller_name` | CHAR(36) / VARCHAR(16) | who listed it |
| `item` | MEDIUMTEXT | Base64 of `ItemStack#serializeAsBytes()` (amount included) |
| `price` | DOUBLE | total price the buyer pays |
| `created_at` / `expires_at` | BIGINT | epoch millis; `expires_at = 0` never expires |
| `state` | VARCHAR(12) | `ACTIVE`, `SOLD`, `EXPIRED`, `CANCELLED` |
| `claimed` | TINYINT | 1 once the seller took the item back |
| `buyer_name` | VARCHAR(16) NULL | filled in when it sells |

**Rules / config** (`auction` section): `enabled`, `gui-rows`, `listing-hours`
(0 = never), `max-listings-per-player`, `tax-percent`, `listing-fee-percent`,
`min-price`, `max-price`.

**Expiry**: a repeating task marks overdue listings `EXPIRED`; the seller claims the
item from the GUI.

**Permissions**: `betteradmincommands.auction` (open / browse / buy, default `true`),
`betteradmincommands.auction.sell` (list items, default `true`).

## 5. Phase 3 — Shop system + EconomyShopGUI takeover  ✅ done

Goal: replace **EconomyShopGUI** with our own DB-backed shop built from its files.

**How the takeover works (`EconomyShopGuiImporter`).** The two editions have different
layouts (`pages:`/`items:` vs `sections:`), so instead of matching one of them the
importer walks **every** `.yml` in `plugins/EconomyShopGUI*/` and treats anything that
*names a material* as an entry. That is layout-proof: free, premium and hand-written
shop files all import.

- Recognised entry keys: `material`/`item`/`type`; buy via
  `buy`/`buy-price`/`price`/`cost`/`money` (or a nested `price:` section); sell via
  `sell`/`sell-price`/`sell-amount`; plus `amount`, `display-name`/`name`, `lore`,
  `enchantments`, `custom-model-data`, `skull-owner`, `slot`, `page`.
- Entries may also be a plain list of material names or a list of maps.
- **One file = one shop.** The name comes from `shop-name`/`title`/`display-name`,
  else from the file name; `icon:`/`display-item:` is the list button and
  `rows:`/`size:` the window height.
- Slots/pages are used where they fit; a colliding or out-of-range slot is moved to
  the next free slot (spilling onto the next page), so nothing is ever dropped.
- Reading happens **async**; the swap into memory + the plugin disable happen back on
  the server thread. `shop.import.disable-plugin` (default on) then switches
  EconomyShopGUI off, and `rebindCommands()` re-binds our executors so `/shop` is ours.
- Config: `shop.enabled`, `gui-rows`, `selling-enabled`, `search-enabled`,
  `import.{enabled,force,disable-plugin,max-items,folders}`. `/shop import`
  (`betteradmincommands.shop.admin`, default op) re-reads the files on demand.

**Data** — `bac_shops` (id, display, icon, rows) + `bac_shop_items` (shop, item_key,
material, search, item, buy_price, sell_price, slot, page), mirrored into
`data/shops.yml` and `data/shop_items.yml`. `item` is again Base64 of
`ItemStack#serializeAsBytes()`, so names, lore and enchants survive.

**`/shop` GUI**: shop list (skipped when there is only one shop) → the shop's own
slots, pages → item view with Buy 1/8/16/32/64, a chat-typed custom amount and, where a
sell price exists, Sell 1 / Sell 8 / Sell everything → a search that filters every shop
at once and pages the results.

**In-game editor** (`/shop edit`, `shop/Shop_Editor`, `betteradmincommands.shop.admin`): shop
list (item counts, current access, count of unpriced entries) → shop view → entry editor (set buy
price, set sell price, `off` to remove, set amount, take off sale, delete with confirmation) and a
shop settings page that writes `shop.access.permissions` into `config.yml`. `ShopService` gained
`updateItem`/`deleteItem` (upsert/delete one row in memory + safe file + MySQL) so a single edit
never rewrites the whole shop, plus `setShopPermission`. A re-import still rebuilds the shop and so
discards edits; the import button warns about that.

**Per-shop access** (`shop.access`): a shop can be locked behind its own permission, decided
in this order — `access.permissions.<shop-id>` (explicit, wins), `access.free` (a shop id or
`*`), else `access.permission-prefix` + the shop id with anything outside `[a-z0-9_.-]` turned
into a `.` so `blocks/mob_drops` becomes `blocks.mob_drops`. The shipped prefix is **empty**,
so nothing changes until it is set. `access.hide-locked` hides locked shops instead of
greying them out. Enforced in three places, so the menu cannot be worked around: the shop list
(greyed, with the node it needs), `openShop`/`openItem` (a direct `/shop <id>` is refused),
and `ShopService.buy`/`sell`/`sellEverything` re-check server side (`NO_ACCESS`). Search
results are filtered by access, so a locked shop cannot be peeked at or bought from through
the search. `betteradmincommands.shop.admin` bypasses every check.

**Known limitation:** EconomyShopGUI's own commands (`/sellall`, its reload, …) and its
permission nodes do not exist afterwards; `/sell`, `/worth`, `/balance` cover the common cases,
and `shop.access` replaces its per-shop nodes (`economyshopgui.shop.<name>`). Player heads are imported without a skin when the owner is not already cached,
because a name lookup at start-up would block.

**Not carried over by design:** the two plugins must not both own `/shop`, which is why
the disable is the default.

## 6. Phase 4 — Report / ticket system  ✅ done

- Tables: `bac_reports` (id, reporter, target, category, status, created/updated),
  `bac_report_messages` (the thread) and `bac_report_participants` (everyone involved
  plus a `last_read` marker). Report ids are plugin-generated UUIDs.
- Presets are **config driven** (`report.presets`) so the menu scales automatically:
  shipped list is `hacking`, `rulebreaking`, `harassment`, `scamming`, `bugreport`,
  plus whatever the server adds. `target-required: false` skips the player picker.
- `/report` GUI: Create a report → preset → target (if the preset needs one) →
  describe in chat. Also **My tickets**. Staff additionally see **All open** and
  **All closed** lists; `/reports` jumps straight to the open list.
- Ticket view: category/target/reporter/status plus the message thread (paged,
  newest first), a **Reply** button (chat prompt), and **Close/Reopen**.
- **Any number of staff** can work a ticket: opening or replying adds you as a
  participant. On a new message every other online participant is notified in chat
  with a clickable `[open]`; players with unread messages are reminded on join.
- Multi-staff read state is per participant via `last_read`, so unread counts are
  personal (only for people who actually took part).
- **Difference to the rest of the plugin:** reports need MySQL - there is no local
  safe file for them, so the feature reports itself unavailable while the DB is down.

## 6b. Not built yet

- Phase 5 is partly open: `/kit`, `/homes` and `/warps` have menus, the rest of the list
  in §7 is still text only.

## 7. Phase 5 — GUIs for existing features  ⏳ partly done

Convert the most-used public commands to menus once the framework is proven:
`/kit`, `/homes`, `/warps`, `/balance`+`/baltop`, `/mail`, `/ignorelist`,
`/jail`/`/jails`, `/ptime`, `/pweather`, `/unlimited`, `/realname`. Text commands stay
as fallbacks.

**Done so far** — each keeps its text form:

| Command | Menu | Text form | Notes |
| --- | --- | --- | --- |
| `/warps` | `warp/Warp_Gui` | `/warps list` (console always) | locked warps greyed out with the reason |
| `/homes` | `home/Home_Gui` | `/homes list` | left-click teleports, right-click confirms a delete |
| `/kit` | `kit/Kit_Gui` | `/kit <name>` | contents, cooldown and permission state per kit |
| `/balance` | `economy/Balance_Gui` | `/balance <player>` | amount still printed in chat; send-money flow |
| `/baltop` | `economy/Baltop_Gui` | `/baltop [page\|list]` | own rank button, top three marked |
| `/mail` | `mail/Mail_Gui` | `/mail send\|read\|clear` | inbox, read/reply/delete, compose, clear |
| `/ignorelist` | `player/IgnoreList_Gui` | `/ignorelist list` | click a name to stop ignoring it |
| `/jails` | `jail/Jails_Gui` | `/jails list` (console always) | click a cell to teleport; delete stays `/deljail` |
| `/ptime` | `player/Ptime_Gui` | `/ptime <value>` | header shows the current offset; custom ticks via chat |
| `/pweather` | `player/Pweather_Gui` | `/pweather <value>` | header shows the active setting |
| `/unlimited` | `player/Unlimited_Gui` | `/unlimited <toggle\|on\|off\|list\|clear>` | switch + `list` permission sees who has it on |
| `/realname` | `player/Realname_Gui` | `/realname <nickname>` | nickname overview, left-click confirms, right-click balance |

Three pieces of logic were pulled out of commands so a menu and its command cannot drift
apart: `KitManager#claim` (kit claiming), `EconomyService#transfer` (player-to-player
payments, shared by `/pay` and the balance menu) and `util/PersonalDisplay` (the `/ptime`
presets and the `/pweather` values, shared by both commands and both menus).
`Items#head(owner, name, lore)` was added to the GUI helper for the menus that show
players.

**Phase 5 is complete.** `/eco` and the moderation commands were deliberately left as
commands - they are one-shot actions rather than things to browse.

## 8. Known issues / bug log

_Add a line here every time the user reports something broken._

| # | Date | Area | Symptom | Cause | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | 2026-09-22 | Nick / Skin | `/nick` did nothing in tab/nametag/chat; `/skinchange` rejected UUIDs; other-player targeting not wanted | TAB owns the tab list + nametags; skin only accepted names | Fixed for nick via PlaceholderAPI hook; skin now accepts UUID; `.others` removed |
| 2 | 2026-09-22 | Shop (found while writing) | Selling could pay for items the player did not have: with an `amount: 16` entry and 1 item in hand, `units` was forced to at least 1 and the payout covered a whole bundle | `Math.max(1, have / unit)` in `sell`/`sellEverything` | Fixed: `sellableUnits()` only counts complete bundles, capped by the requested amount |
| 3 | 2026-09-22 | Shop (found while writing) | EconomyShopGUI was never actually switched off, and `/shop` could stay bound to its (disabled) command | The import ran during our own `onEnable`, before other plugins were enabled, and nothing re-bound our executors afterwards | Fixed: the import is deferred by one tick, and `rebindCommands()` re-binds every command after a plugin is disabled |
| 4 | 2026-09-22 | Shop (found while writing) | Disabling another plugin from the async import thread | `disablePluginIfPresent()` was called off the server thread | Fixed: only the file reading is async; the disable and the swap now run on the server thread |
| 5 | 2026-09-22 | Shop | Every shop was open to everyone, so a rank/VIP shop could not be limited to the people meant to see it | No access concept existed | Fixed: `shop.access` (`permissions.<id>` → `free` → `permission-prefix`), enforced in the shop list, in `openShop`/`openItem`, in the search results and again in `buy`/`sell`/`sellEverything` |
| 6 | 2026-09-22 | Shop import (found in review) | Entries from shop files that gave no `slot` landed on the wrong position: the first two of every page were swapped and each further page drifted, so imported shops looked shuffled | `slot < 0 ? page : slot` used the *page number* as the slot instead of leaving it unset | Fixed: the slot stays `-1` when the file does not provide one, so the layout drops the entry into the next free slot in file order |
| 7 | 2026-09-22 | Mail menu | Opening the mailbox read it on the server thread, so a slow database froze the whole server for the duration of the click | `Mail_Gui` called `MailService#inbox` (blocking JDBC) from the click handler | Fixed: the read runs async and the window is drawn afterwards, skipped if the player left. `MailService` writes now return a `CompletableFuture` so a redraw after a send/delete waits for the write instead of racing it |
| 8 | 2026-09-22 | Shop access (found in review) | A shop id containing a dot (a file like `mob.drops.yml`) could not be locked: `permissionFor` compared raw top-level keys, so a key YAML had parsed as nested never matched | Dotted keys are nested by `ConfigurationSection`, and ids with `/` vs keys with `.` were compared literally | Fixed: `accessKey()` normalises `/`, `\` and `.` to `_` on both sides and `getValues(true)` sees nested keys; the editor writes the normalised key so it can never be read back as a section |
| 9 | 2026-09-23 | Nick / rank | `/nick` did not show on everyone's tab/nametag/chat, staff could not tell who a nicked player really was, and the rank prefix never appeared unless a LuckPerms group was named | Only `displayName`/`playerListName` were set; no chat renderer of our own; a group was required for any prefix | Fixed: the nickname is also rendered by our own chat renderer (lowest priority, so another chat plugin can still take over), `nicknamePrefix` falls back to the player's own rank, `/realname` and the reveal node moved to staff, and `betteradmincommands.nick.see` adds the real name in brackets for staff only |
| 10 | 2026-09-23 | Skin | `/skinchange` reported "that account exists but has no skin" for accounts that do have one | The profile request did not force the signed texture, and a missing/short texture was reported as a missing skin | Fixed: the request asks for the signed texture explicitly, the error names the real reason, and `/skinchange value <texture> <signature>` accepts a pasted texture |
| 11 | 2026-09-23 | Trade (found while writing) | Clicking the money button in the trade window cancelled the whole trade | The chat prompt service closed the open window, and closing a trade window cancels the trade | Fixed: the prompt can now keep the window open, and cancelling the amount only drops the money entry |
| 12 | 2026-09-23 | Trade history (RAM) | Keeping every trade meant holding every traded `ItemStack` in memory for 48 hours | Records stored the item arrays directly | Fixed: records hold the encoded form plus a ready-made summary, and only the newest `trade.log.max-cached` are kept in memory |
| 13 | 2026-09-23 | Notifications | Staff notifications (trade, report, kick, ban, unban, mail) could not be switched off without taking the permission away | Notifications were sent directly with `hasPermission`/`Bukkit.broadcast` | Fixed: a `NotificationService` with per-category permissions and per-player toggles, managed with `/notify`; every sender now goes through it |
| 14 | 2026-09-23 | Auction UI | `/ah` had no consistent look and no way to reach its actions without clicking | One flat menu, no subcommands | Fixed: a shared `Theme` (frame/border/heading) for every menu, a rebuilt `/ah` with counts, balance and price range, and `/ah browse|search|sort|sell|mine|claims|help` |

## 9. Progress

- [x] Phase 1 — GUI framework
- [x] Phase 2 — Auction house `/ah` (active listings, sell, my listings, claims)
- [x] Phase 2b — Auction sorting (newest/oldest/cheapest/most expensive/name) + search by item and seller
- [x] Phase 3 — Shop system + EconomyShopGUI takeover
- [x] Phase 3b — Per-shop access control (`shop.access`, enforced in the menu and in the trade paths)
- [x] Phase 3c — In-game shop editor (`/shop edit`) for prices, amounts and shop access
- [x] Phase 5a — Menus for `/warps`, `/homes`, `/kit`, `/balance`, `/baltop`, `/mail`, `/ignorelist`
- [x] Phase 5b — Menus for `/jails`, `/ptime`, `/pweather`, `/unlimited`, `/realname`
- [x] Phase 4 — Report / ticket system
- [ ] Phase 5 — GUIs for existing features (`/warps`, `/homes`, `/kit` done)
- [x] Nick/skin fixes from §8 entry #1
- [x] Bug log entries #2–#14 fixed
- [x] Permissions: `better_admin_commands.permissionall`, config-driven default access per group, cached wildcard checks
- [x] Nick: chat rendering, permission-gated real-name reveal, own-rank prefix by default
- [x] Skin: signed texture request, pasted-texture input, clearer errors
- [x] Economy: XConomy-style `/money set|give|take|reset|resetall|balance|top` behind `/eco`/`/balance`/`/money`, granular `betteradmincommands.eco.<action>` nodes
- [x] Trade: two-sided GUI with items and money, dual confirm, request flow, full rollback on cancel/quit
- [x] Trade logging: `trades` table + `data/trades.yml`, 48h retention, bounded in-memory cache, staff notification
- [x] Notifications: `/notify` toggles per category, wired into trade/report/kick/ban/unban/mail
- [x] UI: shared `Theme` frame applied across every menu
- [ ] Phase 5 — remaining text/GUI gaps (see §10 #6)

## 10. Open questions

1. **Auction sorting/search** — ✅ answered: sorting (newest/oldest/cheapest/most
expensive/name) and a search over item name and seller are both built.
2. **Shop takeover** — ✅ answered: the importer is layout-proof, so the installed
   edition no longer matters. Worth re-checking on the live server: how many items were
   imported, and whether the prices came through as expected (`/shop import` prints the
   counts). If a shop file uses an unusual price spelling, its entries arrive with
   `buy_price = -1` (not for sale) and can be spotted in the `shop_items` table.
3. **Report add-ons** — should reports also be reachable with `/report create <player>`
   for the chat-only users, or is the GUI the only entry point?
4. **Phase 5 order** — ✅ answered: every command on the list now has a menu.
   `/eco` and the moderation commands were deliberately left as commands.
5. **`/balance` shape** — it now prints the amount **and** opens the menu. If the menu on
   every lookup turns out to be noise, moving it behind `/balance menu` is a one-line change.
6. **Tab/nametag reveal** — the nickname is shown to everyone through the tab list entry (which
   is also what 1.21 draws above the head). A *per-viewer* nametag, where only staff see the
   real name above the head and not just in chat, needs per-player packets and therefore a
   packet library; the chat reveal and `/realname` cover the common cases without one.
7. **Trade escrow** — money is checked at the moment both sides confirm rather than being
   moved aside when offered. That keeps the code simple and cannot lose money; if a balance
   drops between offering and confirming, the trade is cancelled with a message.
