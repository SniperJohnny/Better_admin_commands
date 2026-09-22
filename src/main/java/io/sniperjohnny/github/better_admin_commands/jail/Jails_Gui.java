package io.sniperjohnny.github.better_admin_commands.jail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The jail list behind {@code /jails}. Every cell is a button that teleports
 * staff to it, which is the whole point of the list - checking a cell, or
 * fetching whoever is in it.
 *
 * <p>Deleting a cell is deliberately not offered here: {@code /deljail} stays
 * the only way, so a misclick in a menu cannot remove a cell that someone is
 * currently sitting in.</p>
 */
public class Jails_Gui {

    private final Better_Admin_Commands plugin;

    public Jails_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<String> names = new ArrayList<>(plugin.jails().names());
        if (names.isEmpty()) {
            Msg.error(player, "There are no jails yet. Create one with /setjail <name>.");
            return;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("jails.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(names.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Jails &7(" + names.size() + ")", rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < names.size(); index++) {
            String name = names.get(start + index);
            Location cell = plugin.jails().get(name);
            if (cell == null) {
                continue;
            }
            menu.button(index, Items.of(icon(name), "&6" + prettify(name),
                            "&7World: &f" + cell.getWorld().getName(),
                            String.format(Locale.ROOT, "&7At: &f%.0f, %.0f, %.0f",
                                    cell.getX(), cell.getY(), cell.getZ()),
                            "",
                            "&eClick to teleport to this cell"),
                    event -> {
                        player.closeInventory();
                        plugin.teleports().requestTeleport(player, cell);
                        Msg.success(player, "Teleported to the jail cell " + name + ".");
                    });
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        if (player.hasPermission("betteradmincommands.setjail")) {
            menu.button(nav + 2, Items.of(Material.IRON_BARS, "&aCreate a cell here",
                            "&7Saves your current position as a cell.",
                            "&7Use &f/setjail <name> &7to give it a name.",
                            "",
                            "&eType &f/setjail <name>&e to add one"));
        }
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    private Material icon(String name) {
        Material material = Material.matchMaterial(String.valueOf(
                plugin.getConfig().getString("jails.gui-icon", "IRON_BARS")).trim().toUpperCase(Locale.ROOT));
        return material == null || material.isAir() ? Material.IRON_BARS : material;
    }

    private static String prettify(String raw) {
        String[] parts = raw.replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
