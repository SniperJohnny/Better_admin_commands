package io.sniperjohnny.github.better_admin_commands.gui;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Small helper for building the display items used in menus. Every string may use
 * legacy {@code &} colour codes, exactly like chat messages.
 */
public final class Items {

    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private Items() {
    }

    /** An item with a display name and optional lore lines. */
    public static ItemStack of(Material material, String name, String... lore) {
        return of(material, 1, name, Arrays.asList(lore));
    }

    /** An item with a display name and a lore list. */
    public static ItemStack of(Material material, String name, List<String> lore) {
        return of(material, 1, name, lore);
    }

    /** An item with a display name and a lore list. */
    public static ItemStack of(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.component(name));
            if (lore != null && !lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> lines = new ArrayList<>(lore.size());
                for (String line : lore) {
                    lines.add(Msg.component(line));
                }
                meta.lore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** A decorative background pane. */
    public static ItemStack filler() {
        return of(FILLER, " ");
    }

    /**
     * A player head with a display name and optional lore.
     *
     * @param owner the skin to show, or {@code null} for an unskinned head (used
     *              when a name is known but no account has been cached for it)
     */
    public static ItemStack head(UUID owner, String name, String... lore) {
        return head(owner, name, Arrays.asList(lore));
    }

    /** A player head with a display name and a lore list. */
    public static ItemStack head(UUID owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (owner != null && meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }
        meta.displayName(Msg.component(name));
        if (lore != null && !lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(Msg.component(line));
            }
            meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** A previous/next page arrow, or a disabled placeholder when there is no page. */
    public static ItemStack arrow(boolean previous, boolean enabled) {
        if (!enabled) {
            return of(FILLER, " ");
        }
        return of(Material.ARROW, previous ? "&ePrevious page" : "&eNext page");
    }
}
