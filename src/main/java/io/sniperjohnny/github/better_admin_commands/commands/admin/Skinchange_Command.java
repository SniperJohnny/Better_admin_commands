package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.skin.SkinService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Puts the skin of a premium account on the player running the command.
 *
 * <ul>
 *   <li>{@code /skinchange <username>} - uses the skin of that account</li>
 *   <li>{@code /skinchange <uuid>} - same, but the account is named by its UUID</li>
 *   <li>{@code /skinchange off} - back to your own skin</li>
 * </ul>
 *
 * A name is resolved to its UUID at Mojang first and the skin is then read from
 * that UUID, exactly like sending the UUID straight away would. The lookup runs
 * off the main thread and the borrowed skin is stored, so it is applied again on
 * every join.
 */
public class Skinchange_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Skinchange_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        String input = args[0];
        if (isReset(input)) {
            resetSkin(sender, player);
            return true;
        }

        // A pasted texture: /skinchange value <base64> <signature>. Useful when a
        // premium lookup comes back without a signed skin.
        if (input.equalsIgnoreCase("value") || input.equalsIgnoreCase("texture")) {
            if (args.length < 3) {
                Msg.error(sender, "Usage: /skinchange value <texture-value> <signature> - get both from "
                        + "a site like NameMC, or from another server's skin file.");
                return true;
            }
            SkinService.Skin pasted = plugin.skins().byTexture(args[1], args[2], "the pasted texture");
            if (pasted == null) {
                Msg.error(sender, "That texture is incomplete - both the value and the signature are needed.");
                return true;
            }
            plugin.skins().apply(player, pasted);
            plugin.skins().remember(player, pasted);
            Msg.success(sender, "Your skin is now the pasted texture.");
            return true;
        }

        // A UUID is looked up as it is, a username is resolved to its UUID first.
        UUID uuid = parseUuid(input);
        if (uuid == null && !input.matches("[A-Za-z0-9_]{1,16}")) {
            Msg.error(sender, "Give a Minecraft username or a UUID, or 'off' to use your own skin again.");
            return true;
        }

        Msg.send(sender, "&7Looking up the skin of &f" + input + "&7...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            CompletableFuture<SkinService.Skin> lookup = uuid == null
                    ? plugin.skins().byUsername(input)
                    : plugin.skins().byUuid(uuid);
            lookup.whenComplete((skin, error) -> plugin.getServer().getScheduler().runTask(plugin,
                    () -> report(sender, player, input, skin, error)));
        });
        return true;
    }

    /** Applies a borrowed skin, or explains why it could not be applied. */
    private void report(CommandSender sender, Player player, String input, SkinService.Skin skin,
                        Throwable error) {
        if (error != null) {
            // The message already says what went wrong, including the HTTP status.
            Msg.error(sender, describe(error));
            plugin.getLogger().warning("Skin lookup for " + input + " failed: " + describe(error));
            return;
        }
        if (!player.isOnline()) {
            Msg.error(sender, "You are no longer online.");
            return;
        }
        if (skin == null) {
            Msg.error(sender, "Mojang does not know a premium account with the name or UUID "
                    + input + ". Check the spelling, or that the account is a Java Edition one. "
                    + "You can also paste a texture with /skinchange value <value> <signature>.");
            return;
        }

        plugin.skins().apply(player, skin);
        plugin.skins().remember(player, skin);
        Msg.success(sender, "Your skin is now the one of " + skin.source() + ".");
    }

    /** Puts the player's own skin back by asking Mojang for it again. */
    private void resetSkin(CommandSender sender, Player player) {
        Msg.send(sender, "&7Restoring your own skin...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.skins().byUuid(player.getUniqueId()).whenComplete((skin, error) ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (!player.isOnline()) {
                                Msg.error(sender, "You are no longer online.");
                                return;
                            }
                            if (error != null) {
                                // Leave the stored skin alone, Mojang might just be unreachable.
                                Msg.error(sender, describe(error));
                                return;
                            }
                            plugin.skins().forget(player.getUniqueId());
                            if (skin == null) {
                                // No premium skin to restore (offline mode account),
                                // so just drop the borrowed one.
                                plugin.skins().clear(player);
                                Msg.success(sender, "The borrowed skin was removed.");
                                return;
                            }
                            plugin.skins().apply(player, skin);
                            Msg.success(sender, "Your own skin is back.");
                        })));
    }

    private static boolean isReset(String value) {
        return value.equalsIgnoreCase("off") || value.equalsIgnoreCase("reset")
                || value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("own");
    }

    /**
     * Reads a UUID that is written with or without dashes. Returns {@code null}
     * when the input is not a UUID at all, which is the signal that the argument
     * should be treated as a username instead.
     */
    private static UUID parseUuid(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.matches("[0-9a-fA-F]{32}")) {
            trimmed = trimmed.replaceFirst(
                    "(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
        }
        if (!trimmed.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
                + "-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** A readable reason for a failed lookup. */
    private static String describe(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
        return cause == null || cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off", "value");
        }
        return Collections.emptyList();
    }
}
