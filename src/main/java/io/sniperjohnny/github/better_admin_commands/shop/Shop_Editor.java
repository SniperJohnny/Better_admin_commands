package io.sniperjohnny.github.better_admin_commands.shop;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The in-game shop editor behind {@code /shop edit}.
 *
 * <p>Everything the shop shows can be changed here without touching YAML: a
 * shop's access permission, and per entry its buy price, its sell price, the
 * amount one purchase hands out, and whether the entry exists at all. Edits go
 * straight into memory, the local safe file and MySQL, so they survive a
 * restart; a re-import from EconomyShopGUI would overwrite them, which is called
 * out in the menu.</p>
 *
 * <p>Entries that arrived without a price are marked, because a shop file with an
 * unusual price spelling is the usual reason an import looks half-broken - and
 * this is where it gets fixed.</p>
 */
public class Shop_Editor {

    private final Better_Admin_Commands plugin;

    public Shop_Editor(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /* --------------------------------------------------------- shop list -- */

    public void openShopList(Player player, int page) {
        List<ShopService.Shop> shops = plugin.shops().shops();
        if (shops.isEmpty()) {
            Msg.error(player, "No shop has been imported yet, so there is nothing to edit.");
            return;
        }
        int rows = Math.max(3, Math.min(6, plugin.shops().guiRows()));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(shops.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Shop editor", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < shops.size(); index++) {
            ShopService.Shop shop = shops.get(start + index);
            int unpriced = 0;
            for (ShopService.ShopItem item : plugin.shops().itemsOf(shop.id())) {
                if (!item.buyable() && !item.sellable()) {
                    unpriced++;
                }
            }
            String permission = plugin.shops().permissionFor(shop.id());
            List<String> lore = new ArrayList<>();
            lore.add("&7Items: &f" + plugin.shops().itemsOf(shop.id()).size());
            lore.add("&7Access: " + (permission == null ? "&aopen to everyone" : "&c" + permission));
            if (unpriced > 0) {
                lore.add("&c" + unpriced + " entry/entries without a price");
            }
            lore.add("");
            lore.add("&eClick to edit");
            menu.button(index, Items.of(shop.icon().getType(), "&6" + shop.display(), lore),
                    event -> openShop(player, shop.id(), 0));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openShopList(player, current - 1));
        }
        menu.button(nav + 3, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.button(nav + 5, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 7, Items.of(Material.WRITABLE_BOOK, "&7Back to the shop"),
                event -> plugin.shopCommand().openMain(player));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openShopList(player, current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------------- items -- */

    private void openShop(Player player, String shopId, int page) {
        ShopService.Shop shop = plugin.shops().shop(shopId);
        if (shop == null) {
            openShopList(player, 0);
            return;
        }
        int rows = shop.rows() > 0 ? Math.max(3, Math.min(6, shop.rows())) : plugin.shops().guiRows();
        int pages = plugin.shops().pages(shopId);
        int current = Math.max(1, Math.min(page, pages));

        Menu menu = new Menu("&8Editing &f" + shop.display(), rows);
        menu.frame();

        Map<Integer, ShopService.ShopItem> items = plugin.shops().page(shopId, current);
        for (Map.Entry<Integer, ShopService.ShopItem> entry : items.entrySet()) {
            ShopService.ShopItem item = entry.getValue();
            menu.button(entry.getKey(), describe(item), event -> openItem(player, item.key(), shopId, current));
        }

        int nav = (rows - 1) * 9;
        if (current > 1) {
            menu.button(nav, Items.arrow(true, true), event -> openShop(player, shopId, current - 1));
        }
        menu.button(nav + 2, Items.of(Material.NAME_TAG, "&bShop settings",
                        "&7Who may use this shop.",
                        "",
                        "&eClick to edit"),
                event -> openSettings(player, shopId, current));
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + current + "&7/&f" + pages,
                "&7Items: &f" + plugin.shops().itemsOf(shopId).size()));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cBack"), event -> openShopList(player, 0));
        if (current < pages) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openShop(player, shopId, current + 1));
        }
        menu.open(player);
    }

    private void openItem(Player player, String itemKey, String shopId, int shopPage) {
        ShopService.ShopItem item = plugin.shops().findItem(shopId, itemKey);
        if (item == null) {
            Msg.error(player, "That entry no longer exists.");
            openShop(player, shopId, shopPage);
            return;
        }

        Menu menu = new Menu("&8Editing entry", 4);
        menu.frame();
        menu.button(13, describe(item));

        menu.button(9, Items.of(Material.EMERALD, "&aSet buy price",
                        "&7Currently: " + (item.buyable()
                                ? "&6" + plugin.economy().format(item.buyPrice())
                                : "&cnot for sale"),
                        "",
                        "&eClick and type the price"),
                event -> promptPrice(player, item, true, shopId, shopPage));

        menu.button(10, Items.of(Material.GOLD_INGOT, "&6Set sell price",
                        "&7Currently: " + (item.sellable()
                                ? "&6" + plugin.economy().format(item.sellPrice())
                                : "&cnot bought back"),
                        "",
                        "&eClick and type the price"),
                event -> promptPrice(player, item, false, shopId, shopPage));

        menu.button(11, Items.of(Material.NAME_TAG, "&bSet amount",
                        "&7One purchase hands out &f" + item.unit() + "&7.",
                        "",
                        "&eClick and type the amount"),
                event -> plugin.chatPrompts().request(player,
                        "&7How many items should one purchase give?", answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                openItem(player, itemKey, shopId, shopPage);
                                return;
                            }
                            Integer amount = Targets.parseInt(answer);
                            if (amount == null || amount <= 0 || amount > 64) {
                                Msg.error(player, "The amount has to be between 1 and 64.");
                                openItem(player, itemKey, shopId, shopPage);
                                return;
                            }
                            plugin.shops().updateItem(ShopService.withAmount(item, amount));
                            Msg.success(player, "One purchase now hands out "
                                    + plugin.shops().findItem(shopId, itemKey).unit() + " item(s).");
                            openItem(player, itemKey, shopId, shopPage);
                        }));

        menu.button(12, item.buyable()
                        ? Items.of(Material.RED_DYE, "&cTake it off sale",
                                "&7The entry stays, but cannot be bought.",
                                "",
                                "&eClick to disable")
                        : Items.of(Material.GRAY_DYE, "&7Not for sale",
                                "&7Set a buy price above to offer it."),
                event -> {
                    if (!item.buyable()) {
                        Msg.error(player, "That entry already has no buy price.");
                        return;
                    }
                    plugin.shops().updateItem(ShopService.withBuyPrice(item, -1.0));
                    Msg.success(player, "The entry can no longer be bought.");
                    openItem(player, itemKey, shopId, shopPage);
                });

        menu.button(14, Items.of(Material.LAVA_BUCKET, "&cDelete entry",
                        "&7Removes it from the shop.",
                        "&8An EconomyShopGUI re-import would bring it back.",
                        "",
                        "&eClick to confirm"),
                event -> openDelete(player, item, shopId, shopPage));

        menu.button(31, Items.of(Material.BARRIER, "&cBack"), event -> openShop(player, shopId, shopPage));
        menu.button(35, Items.of(Material.OAK_DOOR, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    private void openDelete(Player player, ShopService.ShopItem item, String shopId, int shopPage) {
        Menu menu = new Menu("&8Delete entry", 3);
        menu.frame();
        menu.button(13, describe(item));
        menu.button(11, Items.of(Material.LIME_CONCRETE, "&aKeep it"), event -> openItem(player, item.key(), shopId, shopPage));
        menu.button(15, Items.of(Material.RED_CONCRETE, "&cDelete"), event -> {
            if (plugin.shops().deleteItem(shopId, item.key())) {
                Msg.success(player, "Entry deleted.");
            } else {
                Msg.error(player, "That entry was already gone.");
            }
            openShop(player, shopId, shopPage);
        });
        menu.open(player);
    }

    /* ---------------------------------------------------------- settings -- */

    private void openSettings(Player player, String shopId, int shopPage) {
        ShopService.Shop shop = plugin.shops().shop(shopId);
        if (shop == null) {
            openShopList(player, 0);
            return;
        }
        String permission = plugin.shops().permissionFor(shopId);
        boolean open = permission == null;

        Menu menu = new Menu("&8Shop settings", 3);
        menu.frame();
        menu.button(13, Items.of(Material.NAME_TAG, "&6" + shop.display(),
                "&7Access: " + (open ? "&aopen to everyone" : "&c" + permission),
                "&7Shop id: &f" + shop.id(),
                "",
                open ? "&7Anyone with the shop command may use it."
                        : "&7Only holders of that permission may use it."));

        menu.button(11, Items.of(Material.WRITABLE_BOOK, "&bSet access permission",
                        "&7Type the permission that unlocks this shop.",
                        "&8Shop ids: " + ShopService.nodeOf(shop.id()),
                        "",
                        "&eClick to type a permission"),
                event -> plugin.chatPrompts().request(player,
                        "&7Which permission unlocks &f" + shop.display() + "&7? &8(type &fnone &8to open it)",
                        answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                openSettings(player, shopId, shopPage);
                                return;
                            }
                            if (answer.isBlank() || answer.equalsIgnoreCase("none")
                                    || answer.equalsIgnoreCase("clear")) {
                                plugin.shops().setShopPermission(shopId, null);
                                Msg.success(player, "The shop is open to everyone again.");
                            } else {
                                plugin.shops().setShopPermission(shopId, answer);
                                Msg.success(player, "Only holders of " + answer + " can use this shop now.");
                            }
                            openSettings(player, shopId, shopPage);
                        }));

        if (!open) {
            menu.button(15, Items.of(Material.LIME_DYE, "&aOpen it to everyone",
                            "&7Clears the permission.",
                            "",
                            "&eClick to clear"),
                    event -> {
                        plugin.shops().setShopPermission(shopId, null);
                        Msg.success(player, "The shop is open to everyone again.");
                        openSettings(player, shopId, shopPage);
                    });
        } else {
            menu.button(15, Items.of(Material.GRAY_DYE, "&7Already open to everyone",
                    "&7Use the button on the left to lock it."));
        }

        menu.button(22, Items.of(Material.BARRIER, "&cBack"), event -> openShop(player, shopId, shopPage));
        menu.open(player);
    }

    /* ----------------------------------------------------------- helpers -- */

    private void promptPrice(Player player, ShopService.ShopItem item, boolean buying,
                             String shopId, int shopPage) {
        String what = buying ? "buy" : "sell";
        plugin.chatPrompts().request(player,
                "&7What should one &f" + what + " &7cost? &8(type &foff &8to disable it)", answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Cancelled.");
                        openItem(player, item.key(), shopId, shopPage);
                        return;
                    }
                    if (answer.equalsIgnoreCase("off") || answer.equalsIgnoreCase("none")) {
                        plugin.shops().updateItem(buying
                                ? ShopService.withBuyPrice(item, -1.0)
                                : ShopService.withSellPrice(item, -1.0));
                        Msg.success(player, "The " + what + " price was removed.");
                        openItem(player, item.key(), shopId, shopPage);
                        return;
                    }
                    Double price = Targets.parseDouble(answer);
                    if (price == null || price < 0.0) {
                        Msg.error(player, "That is not a valid price.");
                        openItem(player, item.key(), shopId, shopPage);
                        return;
                    }
                    plugin.shops().updateItem(buying
                            ? ShopService.withBuyPrice(item, price)
                            : ShopService.withSellPrice(item, price));
                    Msg.success(player, "The " + what + " price is now "
                            + plugin.economy().format(price) + ".");
                    openItem(player, item.key(), shopId, shopPage);
                });
    }

    /** The entry with its prices written into the lore, so the menu reads itself. */
    private ItemStack describe(ShopService.ShopItem item) {
        ItemStack display = item.display().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.lore() == null
                    ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(Msg.component("&7Amount: &f" + item.unit() + "x"));
            lore.add(Msg.component(item.buyable()
                    ? "&7Buy: &6" + plugin.economy().format(item.buyPrice())
                    : "&cNo buy price"));
            lore.add(Msg.component(item.sellable()
                    ? "&7Sell: &6" + plugin.economy().format(item.sellPrice())
                    : "&cNo sell price"));
            if (!item.buyable() && !item.sellable()) {
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(Msg.component("&cNeeds a price before players can use it"));
            }
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(Msg.component("&eClick to edit"));
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    /** A friendly name for an entry, used in messages. */
    static String nameOf(ShopService.ShopItem item) {
        ItemMeta meta = item.display().getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
        }
        return item.material().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
