package io.sniperjohnny.github.better_admin_commands.home;

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
import java.util.Map;

/**
 * The home menu behind {@code /homes}. Left-clicking a home teleports, and
 * right-clicking opens a confirmation before the home is deleted, so a misclick
 * cannot lose a home.
 */
public class Home_Gui {

    private final Better_Admin_Commands plugin;

    public Home_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        Map<String, Location> homes = plugin.homes().homesOf(player.getUniqueId());
        if (homes.isEmpty()) {
            Msg.error(player, "You have no homes yet. Use /sethome to create one.");
            return;
        }
        List<Map.Entry<String, Location>> entries = new ArrayList<>(homes.entrySet());

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("homes.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Your homes &7(" + entries.size() + "/" + plugin.homes().limitFor(player) + ")",
                rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < entries.size(); index++) {
            Map.Entry<String, Location> entry = entries.get(start + index);
            Location home = entry.getValue();
            menu.button(index, Items.of(icon(), "&6" + prettify(entry.getKey()),
                            "&7World: &f" + home.getWorld().getName(),
                            String.format(Locale.ROOT, "&7At: &f%.0f, %.0f, %.0f",
                                    home.getX(), home.getY(), home.getZ()),
                            "",
                            "&eLeft-click to teleport",
                            "&cRight-click to delete"),
                    event -> {
                        if (event.isRightClick()) {
                            openDelete(player, entry.getKey(), current);
                            return;
                        }
                        player.closeInventory();
                        plugin.teleports().requestTeleport(player, home);
                        Msg.success(player, "Teleported to your home " + entry.getKey() + ".");
                    });
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        menu.button(nav + 6, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    private void openDelete(Player player, String name, int returnPage) {
        Location home = plugin.homes().get(player.getUniqueId(), name);
        Menu menu = new Menu("&8Delete home", 3);
        menu.fillEmpty(Items.filler());
        if (home != null) {
            menu.button(13, Items.of(icon(), "&6" + prettify(name),
                    "&7World: &f" + home.getWorld().getName()));
        }
        menu.button(11, Items.of(Material.LIME_CONCRETE, "&aKeep it"), event -> open(player, returnPage));
        menu.button(15, Items.of(Material.RED_CONCRETE, "&cDelete &f" + prettify(name),
                        "&7This cannot be undone.",
                        "", "&eClick to delete"),
                event -> {
                    if (plugin.homes().delete(player.getUniqueId(), name)) {
                        Msg.success(player, "Deleted your home " + name + ".");
                    } else {
                        Msg.error(player, "That home is already gone.");
                    }
                    open(player, returnPage);
                });
        menu.open(player);
    }

    private Material icon() {
        Material material = Material.matchMaterial(
                String.valueOf(plugin.getConfig().getString("homes.gui-icon", "RED_BED"))
                        .trim().toUpperCase(Locale.ROOT));
        return material == null || material.isAir() ? Material.RED_BED : material;
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
