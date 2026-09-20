package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.backup.BackupService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
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
            case "reload", "rl" -> {
                plugin.reloadConfig();
                Msg.setPrefix(plugin.getConfig().getString("messages.prefix", "&8[&6BetterAdmin&8] &r"));
                plugin.spawns().load();
                plugin.warps().load();
                Msg.success(sender, "Configuration, spawn.yml and warps.yml were reloaded.");
                sender.sendMessage(Msg.color("&7Note: database and economy settings need a full restart."));
            }
            case "backup" -> runBackup(sender);
            case "info" -> info(sender, command);
            default -> {
                Msg.error(sender, "Unknown subcommand. Use /betteradmincommands <reload|backup|info>.");
                Msg.usage(sender, command);
            }
        }
        return true;
    }

    private void info(CommandSender sender, Command command) {
        Msg.raw(sender, "&6Better_Admin_Commands &7v" + plugin.getDescription().getVersion());
        Msg.raw(sender, " &7Economy: &f" + (plugin.getConfig().getBoolean("economy.enabled", true) ? "enabled" : "disabled")
                + " &7(Vault: " + (plugin.getServer().getPluginManager().getPlugin("Vault") != null ? "installed" : "missing") + ")");
        Msg.raw(sender, " &7Tables are prefixed with &f"
                + plugin.getConfig().getString("database.table-prefix", "bac_"));
        Msg.raw(sender, " &7Warps: &f" + plugin.warps().size()
                + " &7Spawn set: &f" + plugin.spawns().hasSpawn());
        Msg.raw(sender, " &7Subcommands: &freload&7, &fbackup&7, &finfo");
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
            return Targets.completeFrom(args[0], "reload", "backup", "info");
        }
        return Collections.emptyList();
    }
}
