package io.sniperjohnny.github.better_admin_commands.util;

import io.sniperjohnny.github.better_admin_commands.commands.admin.Time_Command;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class PersonalDisplay {

    public record TimePreset(String id, String display, Material icon, long ticks) {
    }

    public static final List<TimePreset> TIME_PRESETS = List.of(
            new TimePreset("day", "&eDay", Material.SUNFLOWER, 1000L),
            new TimePreset("noon", "&6Noon", Material.SUNFLOWER, 6000L),
            new TimePreset("sunset", "&cSunset", Material.ORANGE_DYE, 12000L),
            new TimePreset("night", "&9Night", Material.CLOCK, 13000L),
            new TimePreset("midnight", "&1Midnight", Material.CLOCK, 18000L),
            new TimePreset("sunrise", "&dSunrise", Material.PINK_DYE, 23000L));

    private PersonalDisplay() {
    }

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

    public static boolean isReset(String value) {
        if (value == null) {
            return false;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        return key.equals("reset") || key.equals("off") || key.equals("server");
    }

    public static Long ticksOf(String value) {
        TimePreset preset = timePreset(value);
        return preset != null ? preset.ticks() : Time_Command.parseTicks(value == null ? "" : value.trim());
    }

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

    public static WeatherType currentWeather(Player player) {
        return player.getPlayerWeather();
    }
}
