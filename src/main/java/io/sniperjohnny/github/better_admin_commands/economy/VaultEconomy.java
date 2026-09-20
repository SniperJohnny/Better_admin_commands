package io.sniperjohnny.github.better_admin_commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

/**
 * Exposes the MySQL backed balance store through the Vault economy API so any
 * Vault aware plugin can use it as the server economy.
 */
public class VaultEconomy implements Economy {

    private final Better_Admin_Commands plugin;
    private final EconomyService service;

    private final String currencyNameSingular;
    private final String currencyNamePlural;

    public VaultEconomy(Better_Admin_Commands plugin, EconomyService service) {
        this.plugin = plugin;
        this.service = service;
        this.currencyNameSingular = plugin.getConfig().getString("economy.currency-name-singular", "Dollar");
        this.currencyNamePlural = plugin.getConfig().getString("economy.currency-name-plural", "Dollars");
    }

    /* ------------------------------------------------------------ metadata -- */

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled() && service != null;
    }

    @Override
    public String getName() {
        return "Better_Admin_Commands";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return service.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return currencyNamePlural;
    }

    @Override
    public String currencyNameSingular() {
        return currencyNameSingular;
    }

    /* ------------------------------------------------------------ accounts -- */

    private UUID uuid(OfflinePlayer player) {
        return player == null ? null : player.getUniqueId();
    }

    private EconomyResponse unsupported(String message) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, message);
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return service.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return service.hasAccount(player);
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return service.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return service.getBalance(player);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return service.has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return service.has(uuid(player), amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    /* ------------------------------------------------------- transactions --- */

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "No player given");
        }
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE, "Cannot withdraw negative amounts");
        }
        UUID uuid = uuid(player);
        if (!service.withdraw(uuid, amount)) {
            return new EconomyResponse(0, service.getBalance(uuid), ResponseType.FAILURE, "Insufficient funds");
        }
        service.saveAsync();
        return new EconomyResponse(amount, service.getBalance(uuid), ResponseType.SUCCESS, null);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "No player given");
        }
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE, "Cannot deposit negative amounts");
        }
        UUID uuid = uuid(player);
        service.ensureAccount(uuid, player.getName() == null ? uuid.toString() : player.getName());
        double updated = service.deposit(uuid, amount);
        service.saveAsync();
        return new EconomyResponse(amount, updated, ResponseType.SUCCESS, null);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    /* -------------------------------------------------------------- banks --- */

    @Override
    @Deprecated
    public EconomyResponse createBank(String name, String player) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return unsupported("This economy does not support banks");
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String name, String playerName) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return unsupported("This economy does not support banks");
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String name, String playerName) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return unsupported("This economy does not support banks");
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    /* ------------------------------------------------------ create account -- */

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        UUID uuid = uuid(player);
        if (service.hasAccount(uuid)) {
            return false;
        }
        service.ensureAccount(uuid, player.getName() == null ? uuid.toString() : player.getName());
        service.saveAsync();
        return true;
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
