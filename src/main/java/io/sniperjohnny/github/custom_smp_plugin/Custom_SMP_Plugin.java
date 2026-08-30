package io.sniperjohnny.github.custom_smp_plugin;

import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.commands.Enchant_Command;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.commands.Gamemode_Command;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.commands.Fly_Command;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.commands.Smite_Command;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.commands.Unban_Command;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.listeners.Join_listener;
import io.sniperjohnny.github.custom_smp_plugin.quality_of_life.listeners.Leave_Listener;
import io.sniperjohnny.github.custom_smp_plugin.pvp_logic.commands.Revive_beacon_recipe_commands;
import io.sniperjohnny.github.custom_smp_plugin.pvp_logic.commands.Revive_Command;
import io.sniperjohnny.github.custom_smp_plugin.pvp_logic.commands.See_Lives_Command;
import io.sniperjohnny.github.custom_smp_plugin.pvp_logic.listeners.Inventory_Listener_pvp;
import io.sniperjohnny.github.custom_smp_plugin.pvp_logic.listeners.Kill_Listener;
import io.sniperjohnny.github.custom_smp_plugin.yaml.YamlPlayerCreator;
import io.sniperjohnny.github.custom_smp_plugin.yaml.YamlSaveDataCreator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Function;

public final class Custom_SMP_Plugin extends JavaPlugin implements Listener {
    private static Custom_SMP_Plugin instance = null;
    
    private YamlPlayerCreator yamlPlayerCreator;
    private YamlSaveDataCreator yamlSaveDataCreator;

    public YamlPlayerCreator getYamlPlayerCreator() {
        return yamlPlayerCreator;
    }

    public YamlSaveDataCreator getYamlSaveDataCreator() {
        return yamlSaveDataCreator;
    }

    public void savePlayerData(UUID playerUUID, String key, Object value) {
        yamlSaveDataCreator.saveData(playerUUID.toString(), key, value);
    }

    public void savePlayerData(Player player, String key, Object value) {
        yamlSaveDataCreator.saveData(player.getUniqueId().toString(), key, value);
    }

    public Object getPlayerData(UUID playerUUID, String key) {
        return yamlSaveDataCreator.getData(playerUUID.toString(), key);
    }

    public Object getPlayerData(Player player, String key) {
        return yamlSaveDataCreator.getData(player.getUniqueId().toString(), key);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        yamlPlayerCreator.createPlayerFileIfNotExists(e.getPlayer());
    }

    @Override
    public void onEnable() {

        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize YAML creators with configurable defaults
        int defaultLives = getConfig().getInt("pvp.default_lives", 3);
        int defaultKills = getConfig().getInt("pvp.default_kills", 0);
        
        this.yamlPlayerCreator = new YamlPlayerCreator.Builder(this)
            .playersFolder("players")
            .defaultLives(defaultLives)
            .defaultKills(defaultKills)
            .build();
        
        this.yamlSaveDataCreator = new YamlSaveDataCreator.Builder(this)
            .dataFolder("players")
            .saveLogger((fileName, key) -> getLogger().fine("Saved " + key + " for " + fileName))
            .errorHandler(e -> getLogger().severe("Failed to save player data: " + e.getMessage()))
            .build();

        // Register Events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Join_listener(this), this);
        getServer().getPluginManager().registerEvents(new Kill_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Inventory_Listener_pvp(), this);
        getServer().getPluginManager().registerEvents(new Leave_Listener(this), this);

        // Register Commands
        getCommand("smite").setExecutor(new Smite_Command());
        getCommand("revive").setExecutor(new Revive_Command());
        getCommand("revivebeaconrecipe").setExecutor(new Revive_beacon_recipe_commands());
        getCommand("lives").setExecutor(new See_Lives_Command(this));
        if(this.getConfig().getBoolean("qol.revamp_normal_commands")) {
            getCommand("gm").setExecutor(new Gamemode_Command());
            getCommand("unban").setExecutor(new Unban_Command());
            getCommand("enchant").setExecutor(new Enchant_Command());
            getCommand("fly").setExecutor(new Fly_Command());
        }

        getLogger().info("Custom_SMP_Plugin_started");
    }

    @Override
    public void onDisable() {
        // Save all player data when server stops
        for (Player player : getServer().getOnlinePlayers()) {
            // Ensure player file exists and save any pending data
            yamlPlayerCreator.createPlayerFileIfNotExists(player);
            // Note: YamlSaveDataCreator saves immediately, so no additional saving needed
            // for data stored through it. For YamlPlayerCreator, we ensure the file exists.
        }
        
        getLogger().info("Custom_SMP_Plugin_disabled");
    }

    public static Custom_SMP_Plugin get_Instance() {
        return instance;
    }
}