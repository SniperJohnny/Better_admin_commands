package io.sniperjohnny.github.better_admin_commands.warp;

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
 * The warp menu behind {@code /warps}. Every warp is a button that teleports on
 * click; warps the player may not use are shown greyed out so they can see what
 * exists without being able to jump there.
 *
 * <p>Icons come from {@code warps.gui-icon} with an optional per-warp override
 * in {@code warps.gui-icons.<name>}.</p>
 */
public class Warp_Gui {

    private final Better_Admin_Commands plugin;

    public Warp_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<String> names = new ArrayList<>(plugin.warps().names());
        if (names.isEmpty()) {
            Msg.error(player, "There are no warps yet.");
            return;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("warps.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(names.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Warps &7(" + names.size() + ")", rows);
        menu.fillEmpty(Items.filler());

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < names.size(); index++) {
            String name = names.get(start + index);
            Location warp = plugin.warps().get(name);
            if (warp == null) {
                continue;
            }
            boolean allowed = player.hasPermission("betteradmincommands.warp." + name.toLowerCase(Locale.ROOT));
            List<String> lore = new ArrayList<>();
            lore.add("&7World: &f" + warp.getWorld().getName());
            lore.add(String.format(Locale.ROOT, "&7At: &f%.0f, %.0f, %.0f",
                    warp.getX(), warp.getY(), warp.getZ()));
            lore.add("");
            lore.add(allowed ? "&eClick to teleport" : "&cYou do not have permission for this warp");

            Material icon = iconFor(name, allowed);
            menu.button(index, Items.of(icon, (allowed ? "&6" : "&7") + prettify(name), lore),
                    event -> {
                        if (!player.hasPermission("betteradmincommands.warp." + name.toLowerCase(Locale.ROOT))) {
                            Msg.noPermission(player);
                            return;
                        }
                        player.closeInventory();
                        plugin.teleports().requestTeleport(player, warp);
                        Msg.success(player, "Teleported to warp " + name + ".");
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

    private Material iconFor(String name, boolean allowed) {
        String configured = plugin.getConfig().getString("warps.gui-icons." + name.toLowerCase(Locale.ROOT));
        if (configured == null || configured.isBlank()) {
            configured = plugin.getConfig().getString("warps.gui-icon", "COMPASS");
        }
        Material material = configured == null ? null : Material.matchMaterial(configured.trim().toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            material = Material.COMPASS;
        }
        return allowed ? material : Material.GRAY_DYE;
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
