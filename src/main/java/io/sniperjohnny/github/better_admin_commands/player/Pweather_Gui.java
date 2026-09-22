package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.PersonalDisplay;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

/**
 * The personal weather menu behind {@code /pweather}: always sun, always rain,
 * or back to whatever the server has. Values are applied through
 * {@link PersonalDisplay}, the same code {@code /pweather} uses.
 */
public class Pweather_Gui {

    private final Better_Admin_Commands plugin;

    public Pweather_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        WeatherType current = PersonalDisplay.currentWeather(player);
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("pweather.gui-rows", 3)));
        Menu menu = new Menu("&8Your personal weather", rows);
        menu.fillEmpty(Items.filler());

        String state = current == null
                ? "&7You see the server's own weather."
                : (current == WeatherType.CLEAR ? "&7You always see &esun&7." : "&7You always see &brain&7.");
        menu.button(13, Items.of(Material.CLOCK, "&6Current setting", state));

        menu.button(10, Items.of(Material.SUNFLOWER, "&eSun",
                        "&7Always see clear weather.",
                        "",
                        "&eClick to apply"),
                event -> apply(player, "sun"));
        menu.button(12, Items.of(Material.WATER_BUCKET, "&bRain",
                        "&7Always see rain.",
                        "",
                        "&eClick to apply"),
                event -> apply(player, "rain"));
        menu.button(16, Items.of(Material.RED_DYE, "&cReset",
                        "&7Follow the server's weather again.",
                        "",
                        "&eClick to reset"),
                event -> apply(player, "reset"));
        menu.button(rows * 9 - 1, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    private void apply(Player player, String value) {
        if (!PersonalDisplay.applyWeather(player, value)) {
            Msg.error(player, "Use &freset&c, &fsun&c or &frain&c.");
        } else if (PersonalDisplay.isReset(value)) {
            Msg.success(player, "Your personal weather was reset to the server weather.");
        } else if (value.equals("sun")) {
            Msg.success(player, "You now always see clear weather.");
        } else {
            Msg.success(player, "You now always see rain.");
        }
        open(player);
    }
}
