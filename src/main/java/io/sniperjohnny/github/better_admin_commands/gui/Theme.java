package io.sniperjohnny.github.better_admin_commands.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The shared look of the menus: a dark border, a lighter inner background and
 * consistent headings, so every window of the plugin feels like one piece
 * instead of a pile of separate screens.
 */
public final class Theme {

    /** Outer ring of every window. */
    private static final Material BORDER = Material.BLACK_STAINED_GLASS_PANE;
    /** Background between the buttons. */
    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private Theme() {
    }

    /** The item used for the outer ring. */
    public static ItemStack border() {
        return Items.of(BORDER, " ");
    }

    /** The item used for the inner background. */
    public static ItemStack filler() {
        return Items.of(FILLER, " ");
    }

    /** A window title: dark grey, so the buttons carry the colour. */
    public static String title(String text) {
        return "&8" + text;
    }

    /** A heading shown inside a window, e.g. {@code "&8Auction House &8» &fBrowse"}. */
    public static String heading(String section) {
        return "&8" + section;
    }

    /** A button that only displays information. */
    public static ItemStack info(Material material, String name, String... lore) {
        return Items.of(material, name, lore);
    }

    /** The "go back" button used in sub screens. */
    public static ItemStack back() {
        return Items.of(Material.ARROW, "&cBack", "&7Return to the previous screen.");
    }

    /** The "close" button. */
    public static ItemStack close() {
        return Items.of(Material.BARRIER, "&cClose", "&7Close this window.");
    }
}
