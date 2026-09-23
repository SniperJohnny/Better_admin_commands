package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The richest-players menu behind {@code /baltop}. One head per player, with the
 * rank in front of the name, paged, plus a button showing where the viewer
 * stands so they do not have to page through to find themselves.
 */
public class Baltop_Gui {

    /** How many players the leaderboard looks at. */
    private static final int LIMIT = 200;

    private final Better_Admin_Commands plugin;

    public Baltop_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<EconomyService.BalanceEntry> entries = plugin.economy().top(LIMIT);
        if (entries.isEmpty()) {
            Msg.error(player, "There is no economy data yet.");
            return;
        }

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("baltop.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Richest players", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < entries.size(); index++) {
            int rank = start + index + 1;
            EconomyService.BalanceEntry entry = entries.get(start + index);
            boolean self = entry.uuid().equals(player.getUniqueId());

            List<String> lore = new ArrayList<>();
            lore.add("&7Rank: &f#" + rank);
            lore.add("&7Balance: &a" + plugin.economy().format(entry.balance()));
            if (self) {
                lore.add("");
                lore.add("&eThat is you!");
            }
            menu.button(index, Items.head(entry.uuid(),
                    medal(rank) + (self ? "&f" : "&7") + entry.name(), lore));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        menu.button(nav + 2, ownRank(entries, player));
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                "&7Showing the top &f" + entries.size()));
        menu.button(nav + 6, Items.of(Material.SUNFLOWER, "&6Your balance",
                        "&7See your own balance and send money.",
                        "",
                        "&eClick to open"),
                event -> plugin.balanceGui().open(player));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    /** The button that says where the viewer stands, even when they are far down. */
    private org.bukkit.inventory.ItemStack ownRank(List<EconomyService.BalanceEntry> entries, Player player) {
        int rank = 0;
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).uuid().equals(player.getUniqueId())) {
                rank = index + 1;
                break;
            }
        }
        double balance = plugin.economy().getBalance(player.getUniqueId());
        if (rank == 0) {
            return Items.of(Material.GRAY_DYE, "&7You are not on the list",
                    "&7Your balance: &a" + plugin.economy().format(balance),
                    "&8Only the top " + entries.size() + " are shown");
        }
        return Items.of(Material.GOLD_INGOT, "&6You are &f#" + rank,
                "&7Your balance: &a" + plugin.economy().format(balance));
    }

    /** A colour cue for the top three places. */
    private static String medal(int rank) {
        return switch (rank) {
            case 1 -> "&6#1 ";
            case 2 -> "&7#2 ";
            case 3 -> "&c#3 ";
            default -> "&8#" + rank + " ";
        };
    }
}
