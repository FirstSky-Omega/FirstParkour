package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.foundation.util.VoidGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class World {

    private static String name;
    private static org.bukkit.World world;

    /**
     * Creates a new parkour world and sets all according settings in it.
     *
     * <p><b>Paper 26 + delete-on-reload pitfall:</b> Bukkit#createWorld() with a custom
     * generator only honors that generator on FIRST creation. If something else (a
     * stray bukkit.yml entry, Multiverse, or a leftover world dir from a crashed
     * shutdown) already loaded the parkour world by the time IP enables, the
     * generator passed in WorldCreator is silently ignored — Bukkit returns the
     * already-loaded world unchanged, with whatever generator (often vanilla)
     * created it. The result was the bug players reported: "after restart the
     * parkour world is no longer void, it's a vanilla world."</p>
     *
     * <p>We now defensively:
     * <ol>
     *   <li>Force-unload (without save) any in-memory copy of our name.</li>
     *   <li>Delete the world dir on disk if delete-on-reload is true, retrying
     *       once for Windows file-lock cases.</li>
     *   <li>Create fresh with our VoidGenerator — guaranteed first-load path.</li>
     *   <li>Verify post-create that the result is actually void; log a loud error
     *       if not, with a manual cleanup hint.</li>
     * </ol>
     */
    public static void create() {
        name = Config.CONFIG.getString("world.name");

        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        // Idempotent: if onLoad() already created the world, just re-apply settings.
        if (world != null) {
            setup();
            return;
        }

        boolean deleteOnReload = Config.CONFIG.getBoolean("world.delete-on-reload");

        // Step 1: if Bukkit has already loaded this world (e.g. from bukkit.yml or
        // Multiverse), unload it so we can re-create with our generator. The
        // unload() call won't save (we pass false) so we don't pollute the dir.
        org.bukkit.World existing = Bukkit.getWorld(name);
        if (existing != null) {
            IP.logging().warn("Parkour world '%s' was already loaded by something else; unloading it so IP can apply its void generator.".formatted(name));
            existing.getPlayers().forEach(p -> p.kick(Component.text("Server is restarting")));
            boolean unloaded = Bukkit.unloadWorld(existing, false);
            if (!unloaded) {
                IP.logging().error("Could not unload '%s'. Another plugin is keeping it loaded; the world will be left as-is and may NOT be void.".formatted(name));
            }
        }

        // Step 2: if configured to wipe-and-respawn, delete the world dir from disk
        // so the next createWorld is a brand-new void world. The retry handles
        // Windows file-lock races where Paper's chunk save threads still own the
        // region files for a tick or two after unloadWorld returns.
        if (deleteOnReload) {
            deleteWorldOnDisk(2);
        }

        createWorld();
        if (world == null) {
            IP.logging().error("Parkour world '%s' is null after createWorld() — IP will not work this session.".formatted(name));
            return;
        }
        setup();

        verifyIsVoidOrWarn();
    }

    private static void createWorld() {
        IP.log("Creating Spigot world");

        try {
            WorldCreator creator = new WorldCreator(name)
                    .generateStructures(false)
                    .type(WorldType.NORMAL)
                    .generator(VoidGenerator.getGenerator()) // to fix No keys in MapLayer etc.
                    .environment(org.bukkit.World.Environment.NORMAL);

            world = Bukkit.createWorld(creator);
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to create the parkour world", "delete the parkour world folder and restart the server", ex);
        }
    }

    private static void setup() {
        IP.log("Initializing world rules");

        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10_000_000);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1000000);
        world.setAutoSave(false);
    }

    /**
     * Sample a few blocks at spawn — if any of them are non-air, our void generator
     * wasn't actually applied (someone else's generator filled the chunk with
     * terrain). We log a loud diagnostic so the operator can intervene rather than
     * silently shipping a broken parkour world.
     */
    private static void verifyIsVoidOrWarn() {
        try {
            Location spawn = world.getSpawnLocation();
            // Force-load the spawn chunk and look at a 4x4x4 cube of blocks across
            // the typical "ground" Y. If anything is non-AIR we lost.
            int sx = spawn.getBlockX();
            int sz = spawn.getBlockZ();
            for (int y : new int[]{60, 64, 70, 100}) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Material m = world.getBlockAt(sx + dx, y, sz + dz).getType();
                        if (m != Material.AIR) {
                            IP.logging().error("Parkour world '%s' is NOT void — block at (%d,%d,%d) is %s. The on-disk world was loaded with a non-IP generator. Set 'world.delete-on-reload: true' in config.yml, then stop the server and manually delete the '%s/' directory before restarting.".formatted(name, sx + dx, y, sz + dz, m, name));
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            IP.logging().stack("Failed to verify parkour world is void", t);
        }
    }

    /**
     * Deletes the world.
     */
    public static void delete() {
        if (!Config.CONFIG.getBoolean("world.delete-on-reload") || !Config.CONFIG.getBoolean("joining")) {
            return;
        }
        IP.log("Deleting world");

        if (world != null) {
            world.getPlayers().forEach(player -> player.kick(Component.text("Server is restarting")));
            // Unload first so chunk save threads stop touching the region files.
            Bukkit.unloadWorld(world, false);
            world = null;
        }

        deleteWorldOnDisk(2);
    }

    /**
     * Removes the world directory from disk. Retries up to {@code attempts} times with
     * a short sleep between, because on Windows the kernel may still hold a region-file
     * lock for a beat after unloadWorld(false) returns (Paper's chunk IO threads aren't
     * guaranteed to be fully drained synchronously).
     */
    private static void deleteWorldOnDisk(int attempts) {
        File file = new File(name);
        if (!file.exists()) return;

        IP.log("Deleting Spigot world directory '%s' (%d attempt%s)".formatted(name, attempts, attempts == 1 ? "" : "s"));

        for (int attempt = 1; attempt <= attempts; attempt++) {
            boolean allGone = true;
            try (Stream<Path> files = Files.walk(file.toPath())) {
                // Reverse order so children delete before parents.
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ex) {
                        allGone = false;
                    }
                }
            } catch (IOException ex) {
                allGone = false;
            }

            if (!file.exists() || allGone) return;

            if (attempt < attempts) {
                IP.logging().warn("Could not fully delete '%s/' (attempt %d). Retrying after 500ms — Paper async chunk IO may still hold file locks.".formatted(name, attempt));
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }

        if (file.exists()) {
            IP.logging().error("Failed to delete '%s/' after %d attempts. Stop the server cleanly and remove the directory manually before next startup, or the world may load with the wrong generator.".formatted(name, attempts));
        }
    }

    /**
     * @return the name of the parkour world.
     */
    public static String getName() {
        return name;
    }

    /**
     * @return the Bukkit world wherein IP is currently active.
     */
    public static org.bukkit.World getWorld() {
        return world;
    }
}
