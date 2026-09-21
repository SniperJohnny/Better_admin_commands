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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

/**
 * Puts the skin of a premium account on a player.
 *
 * <ul>
 *   <li>{@code /skinchange <username>} - your own skin</li>
 *   <li>{@code /skinchange [player] <username>} - someone else, needs
 *       {@code betteradmincommands.skinchange.others}</li>
 *   <li>{@code /skinchange off} - back to your own skin</li>
 * </ul>
 *
 * The name is looked up at Mojang, which is where NameMC gets the skins it
 * displays. The lookup runs off the main thread and the borrowed skin is stored,
 * so it is applied again on every join.
 */
public class Skinchange_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Skinchange_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        // /skinchange <username> for yourself, /skinchange <player> <username>
        // for someone else. The two argument form is used when the first
        // argument names an online player.
        Player target;
        String username;
        Player named = sender.hasPermission("betteradmincommands.skinchange.others")
                ? Targets.onlineOrNull(args[0]) : null;
        if (named != null && args.length >= 2) {
            target = named;
            username = args[1];
        } else {
            if (!(sender instanceof Player self)) {
                Msg.playerOnly(sender);
                return true;
            }
            target = self;
            username = args[0];
        }

        if (isReset(username)) {
            resetSkin(sender, target);
            return true;
        }
        if (!username.matches("[A-Za-z0-9_]{1,16}")) {
            Msg.error(sender, "That is not a valid Minecraft name.");
            return true;
        }

        Msg.send(sender, "&7Looking up the skin of &f" + username + "&7...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.skins().byUsername(username).whenComplete((skin, error) ->
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> report(sender, target, username, skin, error))));
        return true;
    }

    /** Applies a borrowed skin, or explains why it could not be applied. */
    private void report(CommandSender sender, Player target, String username, SkinService.Skin skin,
                        Throwable error) {
        if (error != null) {
            // The message already says what went wrong, including the HTTP status.
            Msg.error(sender, describe(error));
            plugin.getLogger().warning("Skin lookup for " + username + " failed: " + describe(error));
            return;
        }
        if (!target.isOnline()) {
            Msg.error(sender, target.getName() + " is no longer online.");
            return;
        }
        if (skin == null) {
            Msg.error(sender, "Mojang does not know a premium account called " + username
                    + ". Check the spelling, or that the account is a Java Edition one.");
            return;
        }

        plugin.skins().apply(target, skin);
        plugin.skins().remember(target, skin);
        Msg.success(sender, target.equals(sender)
                ? "Your skin is now the one of " + skin.source() + "."
                : target.getName() + " now uses the skin of " + skin.source() + ".");
    }

    /** Puts the player's own skin back by asking Mojang for it again. */
    private void resetSkin(CommandSender sender, Player target) {
        Msg.send(sender, target.equals(sender)
                ? "&7Restoring your own skin..."
                : "&7Restoring the skin of " + target.getName() + "...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.skins().byUuid(target.getUniqueId()).whenComplete((skin, error) ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (!target.isOnline()) {
                                Msg.error(sender, target.getName() + " is no longer online.");
                                return;
                            }
                            if (error != null) {
                                // Leave the stored skin alone, Mojang might just be unreachable.
                                Msg.error(sender, describe(error));
                                return;
                            }
                            plugin.skins().forget(target.getUniqueId());
                            if (skin == null) {
                                // No premium skin to restore (offline mode account),
                                // so just drop the borrowed one.
                                plugin.skins().clear(target);
                                Msg.success(sender, target.equals(sender)
                                        ? "The borrowed skin was removed."
                                        : "The borrowed skin of " + target.getName() + " was removed.");
                                return;
                            }
                            plugin.skins().apply(target, skin);
                            Msg.success(sender, target.equals(sender)
                                    ? "Your own skin is back."
                                    : target.getName() + " has their own skin back.");
                        })));
    }

    private static boolean isReset(String value) {
        return value.equalsIgnoreCase("off") || value.equalsIgnoreCase("reset")
                || value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("own");
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
            if (!sender.hasPermission("betteradmincommands.skinchange.others")) {
                return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off");
            }
            List<String> names = new ArrayList<>(Targets.complete(args[0]));
            names.addAll(Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off"));
            return names;
        }
        if (args.length == 2 && sender.hasPermission("betteradmincommands.skinchange.others")
                && Targets.onlineOrNull(args[0]) != null) {
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), "off");
        }
        return Collections.emptyList();
    }
}
