package io.sniperjohnny.github.better_admin_commands.kit;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The kit menu behind {@code /kit}. Every kit is a button that hands the kit
 * out; kits on cooldown or without permission are shown in grey with the reason,
 * so a player can tell the difference between "not yet" and "not for me".
 */
public class Kit_Gui {

    private final Better_Admin_Commands plugin;

    public Kit_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<String> names = new ArrayList<>(plugin.kits().names());
        if (names.isEmpty()) {
            Msg.error(player, "No kits are configured.");
            return;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("kit-gui.rows", 3)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(names.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Kits", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < names.size(); index++) {
            String name = names.get(start + index);
            String permission = plugin.kits().permission(name);
            boolean allowed = permission == null || permission.isBlank() || player.hasPermission(permission);
            long remaining = plugin.kits().remainingMillis(player, name);

            List<String> lore = new ArrayList<>();
            List<ItemStack> contents = plugin.kits().items(name);
            lore.add("&7Contains: &f" + contents.size() + " item(s)");
            if (!contents.isEmpty()) {
                StringBuilder preview = new StringBuilder("&8");
                for (int item = 0; item < Math.min(4, contents.size()); item++) {
                    if (item > 0) {
                        preview.append("&8, ");
                    }
                    preview.append("&8").append(contents.get(item).getAmount()).append("x ")
                            .append(pretty(contents.get(item)));
                }
                if (contents.size() > 4) {
                    preview.append("&8 ...");
                }
                lore.add(preview.toString());
            }
            long cooldown = plugin.kits().cooldownMillis(name);
            if (cooldown > 0) {
                lore.add("&7Cooldown: &f" + Targets.formatDuration(cooldown / 1000L));
            }
            lore.add("");
            if (!allowed) {
                lore.add("&cYou do not have permission for this kit");
            } else if (remaining > 0) {
                lore.add("&cReady again in &f" + Targets.formatDuration(remaining / 1000L));
            } else {
                lore.add("&eClick to claim");
            }

            Material icon = plugin.kits().icon(name);
            if (icon == null) {
                icon = Material.matchMaterial(String.valueOf(
                        plugin.getConfig().getString("kit-gui.icon", "CHEST")).trim().toUpperCase(Locale.ROOT));
            }
            if (icon == null || icon.isAir()) {
                icon = Material.CHEST;
            }
            if (!allowed || remaining > 0) {
                icon = Material.GRAY_DYE;
            }
            menu.button(index, Items.of(icon, (allowed && remaining <= 0 ? "&6" : "&7") + prettify(name), lore),
                    event -> claim(player, name, current));
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

    /**
     * Claims a kit and reports the outcome.
     *
     * @param returnPage the page to reopen afterwards, or -1 to leave the menu closed
     */
    public void claim(Player player, String name, int returnPage) {
        KitManager.Claim claim = plugin.kits().claim(player, name);
        switch (claim.result()) {
            case SUCCESS -> Msg.success(player, "You received the kit " + name + ".");
            case UNKNOWN -> Msg.error(player, "There is no kit called " + name + ".");
            case NO_PERMISSION -> Msg.noPermission(player);
            case NO_ITEMS -> Msg.error(player, "This kit does not contain any items.");
            case COOLDOWN -> Msg.error(player, "You have to wait "
                    + Targets.formatDuration(claim.remainingMillis() / 1000L)
                    + " before using this kit again.");
        }
        if (returnPage >= 0) {
            open(player, returnPage);
        }
    }

    private static String pretty(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName());
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
