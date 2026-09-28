package io.sniperjohnny.github.better_admin_commands.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class Theme {

    private static final Material BORDER = Material.BLACK_STAINED_GLASS_PANE;

    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private Theme() {
    }

    public static ItemStack border() {
        return Items.of(BORDER, " ");
    }

    public static ItemStack filler() {
        return Items.of(FILLER, " ");
    }

    public static String title(String text) {
        return "&8" + text;
    }

    public static String heading(String section) {
        return "&8" + section;
    }

    public static ItemStack info(Material material, String name, String... lore) {
        return Items.of(material, name, lore);
    }

    public static ItemStack back() {
        return Items.of(Material.ARROW, "&cBack", "&7Return to the previous screen.");
    }

    public static ItemStack close() {
        return Items.of(Material.BARRIER, "&cClose", "&7Close this window.");
    }
}
