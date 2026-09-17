package dev.efnilite.ip;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.api.ServerIntegration;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.hologram.HologramManager;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.PAPIHook;
import dev.efnilite.ip.integration.IntegrationLoader;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.mode.SpectatorMode;
import dev.efnilite.ip.migration.LegacyDataMigrator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.world.Divider;
import dev.efnilite.ip.world.World;
import dev.efnilite.iep.IEP;
import dev.efnilite.ipp.IPP;
import dev.efnilite.ip.foundation.ParkourPlugin;
import dev.efnilite.ip.foundation.inventory.Menu;
import dev.efnilite.ip.foundation.util.Logging;
import dev.efnilite.ip.foundation.util.UpdateChecker;
import dev.efnilite.ip.foundation.util.VoidGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public final class IP extends ParkourPlugin {

    public static final String NAME = "<#FF6464><bold>Infinite Parkour<reset>";
    public static final String PREFIX = NAME + " <dark_gray>» <gray>";

    private static Logging logging;
    private static IP instance;
    public static boolean plusEnabled;
    public static boolean elytraEnabled;
    private static Command parkourCommand;
    private List<ServerIntegration> serverIntegrations = List.of();

    public static Command getParkourCommand() {
        return parkourCommand;
    }

    @Nullable
    private static PAPIHook placeholderHook;

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] " + message);
        }
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }

    /**
     * @return This plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return logging;
    }

    /**
     * @return The plugin instance.
     */
    public static IP getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    @Override
    public void onLoad() {
        instance = this;
        logging = new Logging(this);
    }

    @Override
    public void enable() {

        // ----- Configurations -----

        LegacyDataMigrator.migrate(this);
        Config.reload(true);

        // ----- Registry -----

        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());

        Modes.init();
        Menu.init(this);

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging.info("Registered Holographic Displays hook");
            HoloHook.init();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Registered PlaceholderAPI hook");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            logging.info("Registered BungeeCord hook");
        }

        // ----- Worlds -----

        if (Config.CONFIG.getBoolean("joining")) {
            World.create();
        }

        // ----- Events -----

        registerListener(new Events());
        parkourCommand = new Command();
        registerCommand("parkour", parkourCommand);
        registerListener(new TabCompleteListener());

        try {
            IPP.enable(this);
            plusEnabled = true;
        } catch (Throwable t) {
            logging.stack("Failed to enable the former IPPlus modes", t);
        }

        try {
            IEP.INSTANCE.enable(this);
            elytraEnabled = true;
        } catch (Throwable t) {
            logging.stack("Failed to enable elytra parkour", t);
        }

        serverIntegrations = IntegrationLoader.enable(this);

        // Built-in hologram leaderboards — replaces DH/HD softdepend on Paper 26.
        // Uses TextDisplay + Interaction (Display API) so there's no armor-stand
        // entity holding chunks open. Initialized after Events so PAPI placeholder
        // resolution is wired before any refresh tick fires.
        try {
            HologramManager.enable();
        } catch (Throwable t) {
            logging.stack("Failed to enable the hologram subsystem — continuing without holograms", t);
        }

        UpdateChecker.check(this, 136046);
    }

    /**
     * Exposes the built-in void {@link ChunkGenerator} to the rest of the server. Users can
     * create void worlds with e.g. {@code /mv create hub normal -g IP} or by setting
     * {@code generator: IP} in bukkit.yml, without installing a separate generator plugin.
     *
     * <p>Returns a generator that produces empty chunks (no noise, no surface, no caves,
     * no bedrock, no decorations, no mobs, no structures). Same implementation IP uses
     * for its own parkour world.</p>
     *
     * @param worldName the name of the world being generated (informational; ignored)
     * @param id        the generator id token from {@code -g IP:<id>}; ignored — we
     *                  always produce a void world regardless of token.
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return VoidGenerator.getGenerator();
    }

    @Override
    public void disable() {
        // Tear down the hologram subsystem FIRST. Its entities are setPersistent(false)
        // and won't survive in the world file even if we skip this, but doing it
        // explicitly here means the chunk tickets are released cleanly and a /reload
        // (which calls disable→enable on the same JVM) starts from a clean slate.
        try {
            HologramManager.disable();
        } catch (Throwable t) {
            logging.stack("Error while disabling the hologram subsystem", t);
        }

        try {
            IntegrationLoader.disable(this, serverIntegrations);
            serverIntegrations = List.of();

            if (placeholderHook != null) {
                placeholderHook.unregister();
                placeholderHook = null;
            }

            if (elytraEnabled) {
                try {
                    IEP.INSTANCE.disable();
                } catch (Throwable t) {
                    logging.stack("Error while disabling elytra parkour", t);
                }
            }

            if (plusEnabled) {
                try {
                    IPP.disable();
                } catch (Throwable t) {
                    logging.stack("Error while disabling the former IPPlus modes", t);
                }
            }

            for (ParkourUser user : ParkourUser.getUsers()) {
                try {
                    ParkourUser.leave(user);
                } catch (Throwable t) {
                    logging.stack("Error while leaving user on disable", t);
                }
            }

            // write all IP gamemodes
            Modes.DEFAULT.getLeaderboard().write(false);

            Storage.close();
            World.delete();
        } catch (Throwable ignored) {

        } finally {
            // Defensive clear so static state never survives a failed disable
            // (e.g. /reload) and leak Sessions / Players across plugin lifecycles.
            Divider.sections.clear();
            Command.selections.clear();
        }
    }
}
