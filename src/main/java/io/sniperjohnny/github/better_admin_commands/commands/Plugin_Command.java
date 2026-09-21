package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.backup.BackupService;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plugin management command: {@code /betteradmincommands reload|backup|info}.
 */
public class Plugin_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Plugin_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            info(sender, command);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload", "rl" -> reload(sender);
            case "backup" -> runBackup(sender);
            case "reconnect", "rc" -> reconnect(sender);
            case "database", "db" -> diagnose(sender);
            case "disable", "off" -> setEnabled(sender, false);
            case "enable", "on" -> setEnabled(sender, true);
            case "info" -> info(sender, command);
            default -> {
                Msg.error(sender, "Unknown subcommand. Use /" + plugin.rootLabel()
                        + " <reload|backup|reconnect|database|info|disable|enable>.");
                Msg.usage(sender, command);
            }
        }
        return true;
    }

    /**
     * Reloads every file the plugin reads at runtime. New options in
     * config.yml are added while the values already in the file are kept.
     */
    private void reload(CommandSender sender) {
        int added = plugin.reloadAll();
        Msg.success(sender, "config.yml, spawn.yml, warps.yml and jails.yml were reloaded"
                + (added > 0 ? " (" + added + " new option" + (added == 1 ? "" : "s") + " added)." : "."));
        Msg.send(sender, "&7Note: database, economy and skin settings need a full restart.");
    }

    /**
     * Switches the plugin's features off or on without unloading it. The
     * management command keeps working, so disabling can be undone in-game.
     */
    private void setEnabled(CommandSender sender, boolean enable) {
        boolean currentlyDisabled = plugin.isPluginDisabled();
        if (enable && !currentlyDisabled) {
            Msg.error(sender, "The plugin is already enabled.");
            return;
        }
        if (!enable && currentlyDisabled) {
            Msg.error(sender, "The plugin is already disabled.");
            return;
        }
        plugin.setPluginDisabled(!enable);
        if (enable) {
            Msg.success(sender, "The plugin is enabled again - all commands and listeners are active.");
        } else {
            Msg.success(sender, "The plugin is disabled: the listeners and every command except /"
                    + plugin.rootLabel() + " are switched off and the data was saved.");
            Msg.send(sender, "&7Use /" + plugin.rootLabel() + " enable to switch it back on.");
        }
    }

    /**
     * Walks through the database connection step by step, so a problem can be
     * narrowed down to the address, the port, the login or the tables.
     */
    private void diagnose(CommandSender sender) {
        Msg.send(sender, "&7Checking the database connection, this can take a moment...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Database.Check> checks = plugin.database().diagnose();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Msg.raw(sender, "&6Database check");
                for (Database.Check check : checks) {
                    Msg.raw(sender, (check.ok() ? " &aOK   &7" : " &cFAIL &7")
                            + check.label() + ": &f" + check.detail());
                }
                Msg.raw(sender, " &7The settings live in the 'database' section of config.yml. "
                        + "Use /" + plugin.rootLabel() + " reconnect afterwards.");
            });
        });
    }

    private void info(CommandSender sender, Command command) {
        Msg.raw(sender, "&6Better_Admin_Commands &7v" + plugin.getDescription().getVersion());
        Msg.raw(sender, " &7State: " + (plugin.isPluginDisabled() ? "&cdisabled" : "&aenabled")
                + " &7(command: &f/" + plugin.rootLabel() + "&7)");
        Msg.raw(sender, " &7Economy: &f" + (plugin.getConfig().getBoolean("economy.enabled", true) ? "enabled" : "disabled")
                + " &7(Vault: " + (plugin.getServer().getPluginManager().getPlugin("Vault") != null ? "installed" : "missing") + ")");
        Msg.raw(sender, " &7Database: &f" + (plugin.database().isAvailable()
                ? "connected" : "unavailable (using local safe files)"));
        Msg.raw(sender, " &7Target: &f" + plugin.database().describe());
        Msg.raw(sender, " &7Tables are prefixed with &f"
                + plugin.getConfig().getString("database.table-prefix", "bac_"));
        Msg.raw(sender, " &7Warps: &f" + plugin.warps().size()
                + " &7Spawn set: &f" + plugin.spawns().hasSpawn());
        List<String> unbound = plugin.unboundCommands();
        if (!unbound.isEmpty()) {
            Msg.raw(sender, " &cNot taking effect (owned by another plugin?): &f"
                    + String.join(", ", unbound));
        }
        if (sender instanceof Player player) {
            String nickname = plugin.preferences().nickname(player.getUniqueId());
            Msg.raw(sender, " &7Your nickname: &f" + (nickname == null ? "none set" : nickname));
        }
        Msg.raw(sender, " &7Subcommands: &freload&7, &fbackup&7, &freconnect&7, &fdatabase&7, "
                + "&finfo&7, &fdisable&7, &fenable");
    }

    private void reconnect(CommandSender sender) {
        Msg.send(sender, "&7Attempting to reconnect to the database...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean connected = plugin.reconnectDatabase();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (connected) {
                    Msg.success(sender, "Database connection is up and the local safe files were synced.");
                } else {
                    Msg.error(sender, "Still unable to reach the database. "
                            + "Check the 'database' section in config.yml.");
                }
            });
        });
    }

    private void runBackup(CommandSender sender) {
        Msg.send(sender, "&7Starting a database backup, this can take a moment...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BackupService.BackupResult result = plugin.backups().backup();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Msg.success(sender, "Backup finished: " + result.totalRows() + " row(s) written.");
                    StringBuilder counts = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : result.rowCounts().entrySet()) {
                        if (counts.length() > 0) {
                            counts.append("&7, &f");
                        }
                        counts.append(entry.getKey()).append(" &8(").append(entry.getValue()).append(")");
                    }
                    Msg.raw(sender, " &7" + counts);
                    Msg.raw(sender, " &7Folder: &f" + result.folder().getAbsolutePath());
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Backup failed: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.error(sender, "Backup failed: " + e.getMessage()));
            } catch (IOException e) {
                plugin.getLogger().severe("Backup failed: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.error(sender, "Backup failed, could not write the files: " + e.getMessage()));
            }
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "reload", "backup", "reconnect", "database", "info",
                    "disable", "enable");
        }
        return Collections.emptyList();
    }
}
