package fr.firstsky.firstparkour.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Utilitaires Folia-compatible pour les schedulers. */
public final class FoliaUtil {

    private FoliaUtil() {}

    public static void runAsync(Plugin plugin, Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    public static void runAsyncDelayed(Plugin plugin, Runnable task, long delayMs) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, t -> task.run(), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void runAsyncRepeating(Plugin plugin, Runnable task, long initialDelayMs, long periodMs) {
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, loc, task);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        entity.getScheduler().execute(plugin, task, null, 1L);
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    /**
     * Téléporte un joueur de façon Folia-compatible.
     * afterTeleport est exécuté sur l'entity-scheduler du joueur après la téléportation.
     */
    public static void teleport(Plugin plugin, org.bukkit.entity.Player player,
                                Location destination, Runnable afterTeleport) {
        player.teleportAsync(destination).thenAccept(success -> {
            if (success && afterTeleport != null) {
                player.getScheduler().execute(plugin, afterTeleport, null, 1L);
            }
        });
    }
}
