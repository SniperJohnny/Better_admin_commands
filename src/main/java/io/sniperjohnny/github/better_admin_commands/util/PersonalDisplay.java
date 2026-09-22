package io.sniperjohnny.github.better_admin_commands.util;

import io.sniperjohnny.github.better_admin_commands.commands.admin.Time_Command;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * The values {@code /ptime} and {@code /pweather} accept, in one place.
 *
 * <p>Both have a command form and a menu, and a preset list that drifts between
 * the two would be confusing (a time the menu offers but the command rejects),
 * so the accepted values and the way they are applied live here.</p>
 */
public final class PersonalDisplay {

    /** One {@code /ptime} preset. */
    public record TimePreset(String id, String display, Material icon, long ticks) {
    }

    /** The presets offered by {@code /ptime} and its menu, in clock order. */
    public static final List<TimePreset> TIME_PRESETS = List.of(
            new TimePreset("day", "&eDay", Material.SUNFLOWER, 1000L),
            new TimePreset("noon", "&6Noon", Material.SUNFLOWER, 6000L),
            new TimePreset("sunset", "&cSunset", Material.ORANGE_DYE, 12000L),
            new TimePreset("night", "&9Night", Material.CLOCK, 13000L),
            new TimePreset("midnight", "&1Midnight", Material.CLOCK, 18000L),
            new TimePreset("sunrise", "&dSunrise", Material.PINK_DYE, 23000L));

    private PersonalDisplay() {
    }

    /** The preset with that id, or {@code null} when the id is not a preset. */
    public static TimePreset timePreset(String id) {
        if (id == null) {
            return null;
        }
        for (TimePreset preset : TIME_PRESETS) {
            if (preset.id().equalsIgnoreCase(id)) {
                return preset;
            }
        }
        return null;
    }

    /** Whether a value means "go back to the server's own time/weather". */
    public static boolean isReset(String value) {
        if (value == null) {
            return false;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        return key.equals("reset") || key.equals("off") || key.equals("server");
    }

    /**
     * The tick count a {@code /ptime} value stands for, or {@code null} when the
     * value is neither a preset nor a number.
     */
    public static Long ticksOf(String value) {
        TimePreset preset = timePreset(value);
        return preset != null ? preset.ticks() : Time_Command.parseTicks(value == null ? "" : value.trim());
    }

    /**
     * Applies a {@code /ptime} value: {@code reset}, a preset id or a tick count.
     *
     * @return whether the value was understood
     */
    public static boolean applyTime(Player player, String value) {
        if (isReset(value)) {
            player.resetPlayerTime();
            return true;
        }
        Long ticks = ticksOf(value);
        if (ticks == null) {
            return false;
        }
        player.setPlayerTime(ticks, false);
        return true;
    }

    /**
     * Applies a {@code /pweather} value: {@code reset}, {@code sun} or {@code rain}.
     *
     * @return whether the value was understood
     */
    public static boolean applyWeather(Player player, String value) {
        if (isReset(value)) {
            player.resetPlayerWeather();
            return true;
        }
        switch (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)) {
            case "sun", "clear" -> player.setPlayerWeather(WeatherType.CLEAR);
            case "rain", "storm", "downfall" -> player.setPlayerWeather(WeatherType.DOWNFALL);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Whether the player currently sees weather of their own. */
    public static WeatherType currentWeather(Player player) {
        return player.getPlayerWeather();
    }
}
