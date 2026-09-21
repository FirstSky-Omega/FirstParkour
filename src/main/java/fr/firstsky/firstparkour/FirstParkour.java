package fr.firstsky.firstparkour;

import fr.firstsky.firstparkour.command.ParkourCommand;
import fr.firstsky.firstparkour.database.DatabaseManager;
import fr.firstsky.firstparkour.gui.ParkourMenu;
import fr.firstsky.firstparkour.gui.ThemeMenu;
import fr.firstsky.firstparkour.listener.ParkourListener;
import fr.firstsky.firstparkour.manager.DuelManager;
import fr.firstsky.firstparkour.manager.LeaderboardManager;
import fr.firstsky.firstparkour.manager.ParkourManager;
import fr.firstsky.firstparkour.placeholder.ParkourExpansion;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class FirstParkour extends JavaPlugin {

    private static FirstParkour instance;

    private DatabaseManager databaseManager;
    private ParkourManager parkourManager;
    private DuelManager duelManager;
    private LeaderboardManager leaderboardManager;
    private ParkourMenu parkourMenu;
    private ThemeMenu themeMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        parkourManager = new ParkourManager(this);
        duelManager = new DuelManager(this);
        leaderboardManager = new LeaderboardManager(this);
        leaderboardManager.startRefreshTask();

        parkourMenu = new ParkourMenu(this);
        themeMenu = new ThemeMenu(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new ParkourListener(this), this);
        pm.registerEvents(parkourMenu, this);
        pm.registerEvents(themeMenu, this);

        var cmd = Objects.requireNonNull(getCommand("parkour"));
        var executor = new ParkourCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new ParkourExpansion(this).register();
            getLogger().info("PlaceholderAPI trouvé — expansion enregistrée.");
        }
        if (pm.isPluginEnabled("Nexo")) {
            getLogger().info("Nexo trouvé — items custom activés.");
        }

        getLogger().info("FirstParkour activé !");
    }

    @Override
    public void onDisable() {
        if (parkourManager != null) parkourManager.stopAllSessions();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("FirstParkour désactivé.");
    }

    public static FirstParkour getInstance() { return instance; }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ParkourManager getParkourManager() { return parkourManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public ParkourMenu getParkourMenu() { return parkourMenu; }
    public ThemeMenu getThemeMenu() { return themeMenu; }
}
