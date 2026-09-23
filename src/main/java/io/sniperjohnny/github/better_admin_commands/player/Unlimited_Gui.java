package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The unlimited-items menu behind {@code /unlimited}: a switch that shows the
 * current state instead of a command whose effect you have to remember, and -
 * for staff - who else has it turned on.
 */
public class Unlimited_Gui {

    private final Better_Admin_Commands plugin;

    public Unlimited_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        boolean enabled = plugin.unlimited().isUnlimited(player);
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("unlimited.gui-rows", 4)));
        Menu menu = new Menu("&8Unlimited items", rows);
        menu.frame();

        menu.button(13, enabled
                        ? Items.of(Material.LIME_DYE, "&aEnabled",
                                "&7Placing blocks and using items does",
                                "&7not use them up.",
                                "",
                                "&eClick to turn it off")
                        : Items.of(Material.GRAY_DYE, "&7Disabled",
                                "&7Blocks and items are used normally.",
                                "",
                                "&eClick to turn it on"),
                event -> {
                    boolean now = plugin.unlimited().toggle(player);
                    Msg.success(player, now
                            ? "Unlimited items enabled - placed blocks and eaten items are kept."
                            : "Unlimited items disabled.");
                    open(player);
                });

        if (player.hasPermission("betteradmincommands.unlimited.list")) {
            menu.button(15, Items.of(Material.PLAYER_HEAD, "&bWho has it on",
                            "&7Show every player using unlimited items.",
                            "",
                            "&eClick to show"),
                    event -> showActive(player));
        }
        menu.button(rows * 9 - 5, Items.of(Material.RED_DYE, "&cTurn it off",
                        "&7Stops the mode for you.",
                        "",
                        "&eClick to disable"),
                event -> {
                    plugin.unlimited().clear(player);
                    Msg.success(player, "Unlimited items disabled.");
                    open(player);
                });
        menu.button(rows * 9 - 1, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    /** Lists everyone with the mode on, so staff can see it at a glance. */
    private void showActive(Player player) {
        List<String> names = plugin.unlimited().activePlayers();
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("unlimited.gui-rows", 4)));
        int pageSize = (rows - 1) * 9;
        Menu menu = new Menu("&8Unlimited items &7(" + names.size() + ")", rows);
        menu.frame();

        for (int index = 0; index < pageSize && index < names.size(); index++) {
            String name = names.get(index);
            Player online = plugin.getServer().getPlayerExact(name);
            menu.button(index, Items.head(online == null ? null : online.getUniqueId(),
                    "&f" + name,
                    "&7Unlimited items is &aon&7."));
        }
        if (names.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.GRAY_DYE, "&7Nobody has it on",
                    "&7No online player is using unlimited items."));
        }

        int nav = (rows - 1) * 9;
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Players: &f" + names.size()));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cBack"), event -> open(player));
        menu.open(player);
    }
}
