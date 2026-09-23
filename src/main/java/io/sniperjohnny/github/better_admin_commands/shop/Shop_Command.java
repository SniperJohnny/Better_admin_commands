package io.sniperjohnny.github.better_admin_commands.shop;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The shop. {@code /shop} opens a GUI: a list of shops (or the only shop
 * directly), the items of a page, and one detail view per item where the amount
 * and the trade are chosen. The contents come from
 * {@link EconomyShopGuiImporter}, so this is also what replaces EconomyShopGUI.
 *
 * <p>{@code /shop import} (for staff) re-reads the EconomyShopGUI files, and
 * {@code /shop search <text>} jumps straight to the search results.</p>
 */
public class Shop_Command implements TabExecutor {

    /** The amounts offered as one-click buttons. Anything else goes through chat. */
    private static final int[] QUANTITIES = {1, 8, 16, 32, 64};

    private final Better_Admin_Commands plugin;

    public Shop_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (!plugin.shops().enabled()) {
            Msg.error(player, "The shop is disabled.");
            return true;
        }
        if (args.length >= 1) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (action.equals("import") || action.equals("reload")) {
                importNow(player);
                return true;
            }
            if (action.equals("edit")) {
                editNow(player);
                return true;
            }
            if (action.equals("search")) {
                String query = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                        : "";
                openSearch(player, query, 0);
                return true;
            }
            ShopService.Shop named = byName(args[0]);
            if (named != null) {
                openShop(player, named.id(), 0);
                return true;
            }
            Msg.error(player, "There is no shop called " + args[0] + ".");
        }
        openMain(player);
        return true;
    }

    /** Re-reads the EconomyShopGUI files and swaps the shop over. Staff only. */
    private void importNow(Player player) {
        if (!player.hasPermission("betteradmincommands.shop.admin")) {
            Msg.noPermission(player);
            return;
        }
        player.closeInventory();
        Msg.send(player, "&7Importing the EconomyShopGUI shops...");
        plugin.importShops().whenComplete((summary, failure) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (failure != null) {
                        Msg.error(player, "The import failed: " + failure.getMessage());
                        return;
                    }
                    Msg.send(player, summary);
                    openMain(player);
                }));
    }

    private ShopService.Shop byName(String name) {
        for (ShopService.Shop shop : plugin.shops().shops()) {
            if (shop.id().equalsIgnoreCase(name) || shop.display().equalsIgnoreCase(name)) {
                return shop;
            }
        }
        return null;
    }

    /** Opens the in-game editor. Staff only. */
    private void editNow(Player player) {
        if (!player.hasPermission("betteradmincommands.shop.admin")) {
            Msg.noPermission(player);
            return;
        }
        plugin.shopEditor().openShopList(player, 0);
    }

    /* -------------------------------------------------------- shop list --- */

    void openMain(Player player) {
        openMain(player, 0);
    }

    /** The ordinary shop entry point, also used by the editor's "back to the shop". */
    void openMain(Player player, int page) {
        List<ShopService.Shop> shops = plugin.shops().visibleShops(player);
        if (shops.isEmpty()) {
            if (plugin.shops().shopCount() > 0) {
                Msg.error(player, "You do not have access to any shop.");
            } else {
                Msg.error(player, "No shop has been imported yet.");
                if (player.hasPermission("betteradmincommands.shop.admin")) {
                    Msg.send(player, "&7Put your EconomyShopGUI shop files in place and run &f/shop import&7.");
                }
            }
            return;
        }
        if (shops.size() == 1 && plugin.shops().canAccess(player, shops.get(0).id())) {
            openShop(player, shops.get(0).id(), 0);
            return;
        }

        int rows = plugin.shops().guiRows();
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(shops.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Shop", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < shops.size(); index++) {
            ShopService.Shop shop = shops.get(start + index);
            int count = plugin.shops().itemsOf(shop.id()).size();
            boolean allowed = plugin.shops().canAccess(player, shop.id());

            List<String> lore = new ArrayList<>();
            lore.add("&7Items: &f" + count);
            lore.add("&7Pages: &f" + plugin.shops().pages(shop.id()));
            lore.add("");
            if (allowed) {
                lore.add("&eClick to open");
            } else {
                lore.add("&cYou do not have access to this shop");
                String permission = plugin.shops().permissionFor(shop.id());
                if (permission != null) {
                    lore.add("&8Needs " + permission);
                }
            }
            menu.button(index, Items.of(allowed ? shop.icon().getType() : Material.GRAY_DYE,
                            (allowed ? "&6" : "&7") + shop.display(), lore),
                    event -> {
                        if (!plugin.shops().canAccess(player, shop.id())) {
                            Msg.error(player, "You do not have access to that shop.");
                            return;
                        }
                        openShop(player, shop.id(), 0);
                    });
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openMain(player, current - 1));
        }
        if (plugin.shops().searchEnabled()) {
            menu.button(nav + 2, Items.of(Material.COMPASS, "&bSearch",
                            "&7Look for an item across every shop.",
                            "", "&eClick to search"),
                    event -> promptSearch(player, () -> openMain(player, current)));
        }
        menu.button(nav + 4, Items.of(Material.SUNFLOWER, "&6Your balance",
                "&7You have &f" + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId()))
                        + "&7."));
        if (player.hasPermission("betteradmincommands.shop.admin")) {
            menu.button(nav + 3, Items.of(Material.WRITABLE_BOOK, "&bEdit shop",
                            "&7Change prices, amounts and shop access",
                            "&7without touching a single file.",
                            "", "&eClick to edit"),
                    event -> editNow(player));
            menu.button(nav + 5, Items.of(Material.HOPPER, "&7Re-import shops",
                            "&7Reads the EconomyShopGUI files again.",
                            "&cThis overwrites in-game edits.",
                            "", "&eClick to import"),
                    event -> importNow(player));
        }
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 7, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openMain(player, current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------------ shop ---- */

    private void openShop(Player player, String shopId, int page) {
        ShopService.Shop shop = plugin.shops().shop(shopId);
        if (shop == null) {
            openMain(player);
            return;
        }
        if (!plugin.shops().canAccess(player, shopId)) {
            Msg.error(player, "You do not have access to that shop.");
            openMain(player);
            return;
        }
        int rows = shop.rows() > 0 ? Math.max(3, Math.min(6, shop.rows())) : plugin.shops().guiRows();
        int pages = plugin.shops().pages(shopId);
        int current = Math.max(1, Math.min(page, pages));

        Menu menu = new Menu("&8Shop &8» &f" + shop.display(), rows);
        menu.frame();

        Map<Integer, ShopService.ShopItem> items = plugin.shops().page(shopId, current);
        for (Map.Entry<Integer, ShopService.ShopItem> entry : items.entrySet()) {
            ShopService.ShopItem item = entry.getValue();
            menu.button(entry.getKey(), describe(item, "&eClick to buy or sell"),
                    event -> openItem(player, item.key(), shopId, current));
        }

        int nav = (rows - 1) * 9;
        if (current > 1) {
            menu.button(nav, Items.arrow(true, true), event -> openShop(player, shopId, current - 1));
        }
        if (plugin.shops().searchEnabled()) {
            menu.button(nav + 2, Items.of(Material.COMPASS, "&bSearch",
                            "&7Look for an item across every shop.",
                            "", "&eClick to search"),
                    event -> promptSearch(player, () -> openShop(player, shopId, current)));
        }
        if (plugin.shops().shopCount() > 1) {
            menu.button(nav + 3, Items.of(Material.CHEST, "&6All shops"), event -> openMain(player));
        }
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + current + "&7/&f" + pages,
                "&7Items on this page: &f" + items.size()));
        if (current < pages) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openShop(player, shopId, current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------------ item ---- */

    private void openItem(Player player, String itemKey, String shopId, int shopPage) {
        ShopService.ShopItem item = plugin.shops().findItem(itemKey);
        if (item == null) {
            Msg.error(player, "That item is no longer in the shop.");
            openShop(player, shopId, shopPage);
            return;
        }
        if (!plugin.shops().canAccess(player, item.shop())) {
            Msg.error(player, "You do not have access to that shop.");
            openMain(player);
            return;
        }
        Menu menu = new Menu("&8Shop &8» &f" + pretty(item.display()), 4);
        menu.frame();
        menu.button(13, describe(item, ""));
        menu.button(31, Items.of(Material.BARRIER, "&cBack"), event -> openShop(player, shopId, shopPage));

        if (item.buyable()) {
            int slot = 9;
            for (int quantity : QUANTITIES) {
                if (slot > 12) {
                    break;
                }
                double total = item.buyPrice() * quantity;
                menu.button(slot++, Items.of(Material.EMERALD, "&aBuy &f" + quantity + "x",
                                "&7You get: &f" + (item.unit() * quantity) + "x " + pretty(item.display()),
                                "&7Price: &6" + plugin.economy().format(total),
                                "",
                                "&eClick to buy"),
                        event -> buy(player, item, quantity, shopId, shopPage));
            }
            menu.button(14, Items.of(Material.EMERALD, "&aBuy &f" + QUANTITIES[QUANTITIES.length - 1] + "x",
                            "&7Price: &6" + plugin.economy().format(item.buyPrice() * QUANTITIES[QUANTITIES.length - 1]),
                            "", "&eClick to buy"),
                    event -> buy(player, item, QUANTITIES[QUANTITIES.length - 1], shopId, shopPage));
            menu.button(22, Items.of(Material.NAME_TAG, "&aBuy a custom amount",
                            "&7Type the amount in chat.",
                            "", "&eClick to enter an amount"),
                    event -> promptAmount(player, "buy", item, shopId, shopPage));
        } else {
            menu.button(22, Items.of(Material.GRAY_DYE, "&7Not for sale",
                    "&7This item can only be sold here."));
        }

        if (item.sellable() && plugin.shops().sellingEnabled()) {
            menu.button(15, Items.of(Material.GOLD_INGOT, "&6Sell &f1x",
                            "&7You give: &f" + item.unit() + "x " + pretty(item.display()),
                            "&7You get: &6" + plugin.economy().format(item.sellPrice()),
                            "", "&eClick to sell"),
                    event -> sell(player, item, 1, shopId, shopPage));
            menu.button(16, Items.of(Material.GOLD_INGOT, "&6Sell &f8x",
                            "&7You get: &6" + plugin.economy().format(item.sellPrice() * 8),
                            "", "&eClick to sell"),
                    event -> sell(player, item, 8, shopId, shopPage));
            menu.button(17, Items.of(Material.GOLD_BLOCK, "&6Sell everything",
                            "&7Sells every &f" + pretty(item.display()) + " &7you carry.",
                            "", "&eClick to sell"),
                    event -> sellEverything(player, item, shopId, shopPage));
        }

        menu.button(35, Items.of(Material.OAK_DOOR, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    /* ---------------------------------------------------------- search ---- */

    private void promptSearch(Player player, Runnable onCancel) {
        plugin.chatPrompts().request(player,
                "&7What are you looking for? &8(part of an item name)", answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Search cancelled.");
                        onCancel.run();
                        return;
                    }
                    openSearch(player, answer, 0);
                });
    }

    private void openSearch(Player player, String query, int page) {
        // Items of a locked shop stay out of the results entirely, so search cannot
        // be used to peek at or buy from a shop the player may not open.
        List<ShopService.ShopItem> results = new ArrayList<>();
        for (ShopService.ShopItem found : plugin.shops().search(query)) {
            if (plugin.shops().canAccess(player, found.shop())) {
                results.add(found);
            }
        }
        int rows = plugin.shops().guiRows();
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(results.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Shop &8» &fSearch: " + query, rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < results.size(); index++) {
            ShopService.ShopItem item = results.get(start + index);
            ShopService.Shop shop = plugin.shops().shop(item.shop());
            menu.button(index, describe(item, "&eClick to open &8(from "
                            + (shop == null ? item.shop() : shop.display()) + ")"),
                    event -> openItem(player, item.key(), item.shop(), item.page()));
        }
        if (results.isEmpty()) {
            menu.button(rows / 2 * 9 + 4, Items.of(Material.GRAY_DYE, "&7Nothing found",
                    "&7No item matches &f" + query + "&7."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openSearch(player, query, current - 1));
        }
        menu.button(nav + 2, Items.of(Material.COMPASS, "&bSearch again"), event ->
                promptSearch(player, () -> openSearch(player, query, current)));
        menu.button(nav + 4, Items.of(Material.BARRIER, "&cBack"), event -> openMain(player));
        menu.button(nav + 6, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                "&7Results: &f" + results.size()));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openSearch(player, query, current + 1));
        }
        menu.open(player);
    }

    /* ------------------------------------------------------------ trade --- */

    private void promptAmount(Player player, String mode, ShopService.ShopItem item,
                              String shopId, int shopPage) {
        plugin.chatPrompts().request(player,
                "&7How many times do you want to " + mode + " &f" + pretty(item.display()) + "&7?", answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        Msg.send(player, "&7Cancelled.");
                        return;
                    }
                    Integer amount = Targets.parseInt(answer);
                    if (amount == null || amount <= 0 || amount > 10_000) {
                        Msg.error(player, "That is not a valid amount.");
                        return;
                    }
                    if (mode.equals("buy")) {
                        buy(player, item, amount, shopId, shopPage);
                    } else {
                        sell(player, item, amount, shopId, shopPage);
                    }
                });
    }

    private void buy(Player player, ShopService.ShopItem item, int quantity, String shopId, int shopPage) {
        ShopService.TradeResult result = plugin.shops().buy(player, item, quantity);
        switch (result) {
            case SUCCESS -> {
                int pieces = item.unit() * quantity;
                Msg.success(player, "You bought " + pieces + "x " + pretty(item.display()) + " &afor &6"
                        + plugin.economy().format(item.buyPrice() * quantity) + "&a.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
            }
            case NO_MONEY -> Msg.error(player, "You cannot afford that.");
            case NO_SPACE -> Msg.error(player, "Make room in your inventory first.");
            case NOT_BUYABLE -> Msg.error(player, "That item is not for sale.");
            case NO_ACCESS -> Msg.error(player, "You do not have access to that shop.");
            default -> Msg.error(player, "That did not work.");
        }
        openItem(player, item.key(), shopId, shopPage);
    }

    private void sell(Player player, ShopService.ShopItem item, int quantity, String shopId, int shopPage) {
        ShopService.TradeResult result = plugin.shops().sell(player, item, quantity);
        switch (result) {
            case SUCCESS -> {
                Msg.success(player, "You sold " + pretty(item.display()) + " &afor &6"
                        + plugin.economy().format(item.sellPrice() * quantity) + "&a.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
            }
            case NOTHING_TO_SELL -> Msg.error(player, "You do not carry any " + pretty(item.display()) + ".");
            case NOT_SELLABLE -> Msg.error(player, "That item cannot be sold here.");
            case NO_ACCESS -> Msg.error(player, "You do not have access to that shop.");
            default -> Msg.error(player, "That did not work.");
        }
        openItem(player, item.key(), shopId, shopPage);
    }

    private void sellEverything(Player player, ShopService.ShopItem item, String shopId, int shopPage) {
        if (!plugin.shops().canAccess(player, item.shop())) {
            Msg.error(player, "You do not have access to that shop.");
            openMain(player);
            return;
        }
        int units = plugin.shops().sellEverything(player, item);
        if (units <= 0) {
            Msg.error(player, "You do not carry any " + pretty(item.display()) + ".");
        } else {
            Msg.success(player, "You sold every " + pretty(item.display()) + " for &6"
                    + plugin.economy().format(item.sellPrice() * units) + "&a.");
        }
        openItem(player, item.key(), shopId, shopPage);
    }

    /* ---------------------------------------------------------- helpers --- */

    /** The item with its trade details written into the lore. */
    private ItemStack describe(ShopService.ShopItem item, String footer) {
        ItemStack display = item.display().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.lore() == null
                    ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(Msg.component("&7Amount: &f" + item.unit() + "x"));
            if (item.buyable()) {
                lore.add(Msg.component("&7Buy: &6" + plugin.economy().format(item.buyPrice())
                        + " &7each"));
            }
            if (item.sellable() && plugin.shops().sellingEnabled()) {
                lore.add(Msg.component("&7Sell: &6" + plugin.economy().format(item.sellPrice())
                        + " &7each"));
            }
            if (!footer.isBlank()) {
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(Msg.component(footer));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    /** A friendly item name, preferring a custom display name. */
    private static String pretty(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        String[] parts = item.getType().name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("betteradmincommands.shop.admin")) {
                options.add("import");
                options.add("edit");
            }
            options.add("search");
            for (ShopService.Shop shop : plugin.shops().shops()) {
                options.add(shop.id());
            }
            return Targets.completeFrom(args[0], options);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
