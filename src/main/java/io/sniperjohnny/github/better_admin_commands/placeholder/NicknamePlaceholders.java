package io.sniperjohnny.github.better_admin_commands.placeholder;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class NicknamePlaceholders extends PlaceholderExpansion {

    private final Better_Admin_Commands plugin;

    public NicknamePlaceholders(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "betteradmincommands";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        String nickname = plugin.preferences().nickname(player.getUniqueId());
        String realName = player.getName() == null ? "" : player.getName();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "nickname" -> nickname == null
                    ? realName
                    : plugin.preferences().nicknameDisplay(player.getUniqueId());
            case "nickname_raw" -> nickname == null ? realName : nickname;
            case "nick_prefix" -> nickname == null ? "" : plugin.preferences().nicknamePrefix(player.getUniqueId());
            default -> null;
        };
    }
}
