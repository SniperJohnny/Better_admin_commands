package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.PersonalDisplay;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The personal time menu behind {@code /ptime}: one button per preset, plus a
 * reset and a custom tick count typed in chat.
 *
 * <p>The presets and the way a value is applied come from
 * {@link PersonalDisplay}, so the menu and the command always accept exactly the
 * same values.</p>
 */
public class Ptime_Gui {

    private final Better_Admin_Commands plugin;

    public Ptime_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<PersonalDisplay.TimePreset> presets = PersonalDisplay.TIME_PRESETS;
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("ptime.gui-rows", 3)));
        Menu menu = new Menu("&8Your personal time", rows);
        menu.frame();

        long offset = player.getPlayerTimeOffset();
        boolean custom = offset != 0L;
        menu.button(13, Items.of(Material.CLOCK, "&6Current setting",
                custom
                        ? "&7Your time is offset by &f" + offset + " ticks&7."
                        : "&7You see the server's own time.",
                "",
                custom ? "&7Use &fReset &7to follow the server again." : "&7Pick a preset below."));

        // The presets sit in the middle row, one button each.
        int slot = 9;
        for (PersonalDisplay.TimePreset preset : presets) {
            if (slot > 17) {
                break;
            }
            menu.button(slot++, Items.of(preset.icon(), preset.display(),
                            "&7Set your time to &f" + preset.id() + "&7.",
                            "&8" + preset.ticks() + " ticks",
                            "",
                            "&eClick to apply"),
                    event -> apply(player, preset.id()));
        }

        long offset = player.getPlayerTimeOffset();
        boolean custom = offset != 0L;
        menu.button(22, Items.of(Material.CLOCK, "&bPick a tick value",
                        "&7Set an exact time, 0-24000, with a slider.",
                        "",
                        "&eClick to pick"),
                event -> plugin.dialogs().slider(player, "Personal time",
                        "&7Which tick value do you want? &8(0-24000)",
                        "Ticks", 0f, 24000f, 500f,
                        custom ? Math.floorMod(offset, 24000L) : 12000f, answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                open(player);
                                return;
                            }
                            apply(player, answer);
                        }));

        menu.button(rows * 9 - 5, Items.of(Material.RED_DYE, "&cReset",
                        "&7Follow the server's time again.",
                        "",
                        "&eClick to reset"),
                event -> apply(player, "reset"));
        menu.button(rows * 9 - 1, Items.of(Material.BARRIER, "&cClose"), event -> player.closeInventory());
        menu.open(player);
    }

    private void apply(Player player, String value) {
        if (!PersonalDisplay.applyTime(player, value)) {
            Msg.error(player, "Use &freset&c, a preset or a tick value.");
        } else if (PersonalDisplay.isReset(value)) {
            Msg.success(player, "Your personal time was reset to the server time.");
        } else {
            Msg.success(player, "Your personal time is now " + value + ".");
        }
        open(player);
    }
}
