package dev.efnilite.ip.world;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.foundation.util.VoidGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        String rawName = Config.CONFIG.getString("world.name");
        // Handle Multiverse "group:worldname" references (e.g. "worlds:parkour" → "parkour").
        // Bukkit.getWorld() and WorldCreator both expect the bare folder/world name.
        name = (rawName != null && rawName.contains(":"))
                ? rawName.substring(rawName.lastIndexOf(':') + 1)
                : rawName;

        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        // Idempotent: if we already have a reference, just re-apply settings.
        if (world != null) {
            setup();
            return;
        }

        // If the server already loaded this world (e.g. from bukkit.yml with generator: FirstParkour
        // after a previous patchBukkitYml() call), use it directly — do NOT unload and recreate,
        // because Folia forbids Bukkit.createWorld() and we don't want to lose a correctly-loaded world.
        org.bukkit.World existing = Bukkit.getWorld(name);
        if (existing != null) {
            world = existing;
            IP.logging().info("Parkour world '%s' found in server world list — using it directly.".formatted(name));
            scheduleSetup();
            return;
        }

        // World not yet loaded — try to create it (works on non-Folia servers).
        if (Config.CONFIG.getBoolean("world.delete-on-reload")) {
            deleteWorldOnDisk(2);
        }

        createWorld();

        // createWorld() either threw (Folia rejected it, patchBukkitYml already called)
        // or returned null. Either way, world is null here.
        if (world == null) {
            patchBukkitYml();
            IP.logging().error("Parkour world '%s' unavailable this session — restart the server to apply the bukkit.yml fix.".formatted(name));
            return;
        }
        scheduleSetup();
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
        } catch (UnsupportedOperationException uoe) {
            // Folia forbids Bukkit.createWorld() from plugin lifecycle methods.
            // Caller (create()) will call patchBukkitYml() since world stays null.
            IP.logging().warn("Folia rejected Bukkit.createWorld() for world '%s'.".formatted(name));
        } catch (Exception ex) {
            IP.logging().stack("Error while trying to create the parkour world", "delete the parkour world folder and restart the server", ex);
        }
    }

    /**
     * Adds the parkour world to bukkit.yml so Folia auto-loads it with the IP void generator
     * on the next server restart. Safe to call from onLoad() — only file I/O, no Bukkit API.
     */
    private static void patchBukkitYml() {
        try {
            File file = new File("bukkit.yml");
            if (!file.exists()) {
                IP.logging().warn("bukkit.yml not found — cannot auto-configure parkour world. " +
                        "Manually add: worlds: { " + name + ": { generator: FirstParkour } }");
                return;
            }

            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            // Already configured — don't add twice.
            if (content.contains(name + ":")) {
                IP.logging().warn("World '%s' already present in bukkit.yml but still not loaded — check your generator entry.".formatted(name));
                return;
            }

            String entry = "  " + name + ":\n    generator: FirstParkour\n";

            if (content.contains("worlds:")) {
                content = content.replaceFirst("worlds:\\s*\n", "worlds:\n" + entry);
            } else {
                if (!content.endsWith("\n")) content += "\n";
                content += "worlds:\n" + entry;
            }

            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            IP.logging().warn("=== FOLIA WORLD FIX ===");
            IP.logging().warn("Parkour world '%s' added to bukkit.yml with generator 'FirstParkour'.".formatted(name));
            IP.logging().warn("RESTART the server once more — the world will load automatically from then on.");
            IP.logging().warn("======================");
        } catch (Exception e) {
            IP.logging().stack("Failed to patch bukkit.yml", e);
            IP.logging().warn("Manual fix: add this to bukkit.yml under 'worlds:': %s: {generator: FirstParkour}".formatted(name));
        }
    }

    // On Folia, setGameRule / setDifficulty / getWorldBorder require the global region thread.
    // During onLoad() and onEnable() we are NOT on that thread, so schedule via GlobalRegionScheduler.
    private static void scheduleSetup() {
        try {
            Bukkit.getServer().getGlobalRegionScheduler().run(IP.getPlugin(), task -> {
                setup();
                verifyIsVoidOrWarn();
            });
        } catch (Exception e) {
            // Fallback for non-Folia servers or if scheduler unavailable during early init
            setup();
            verifyIsVoidOrWarn();
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
     * Called from onEnable() as a last-resort fallback when onLoad() world creation
     * was rejected (e.g. Folia timing restriction). Checks whether the server auto-loaded
     * the world from bukkit.yml and wires up our reference without calling createWorld().
     */
    public static void tryRecover() {
        if (world != null) return;
        if (name == null) name = stripPrefix(Config.CONFIG.getString("world.name"));
        if (!Config.CONFIG.getBoolean("joining")) return;

        world = Bukkit.getWorld(name);
        if (world != null) {
            IP.logging().info("Parkour world '%s' recovered from server world list.".formatted(name));
            scheduleSetup();
        } else {
            IP.logging().error("Parkour world '%s' is still null — parkour is not available this session.".formatted(name));
            IP.logging().error("If the startup log shows '=== FOLIA WORLD FIX ===', a bukkit.yml entry was written — restart once more.");
        }
    }

    /**
     * Called lazily from Divider.add() the first time a session is created. By that
     * point the server is fully running and world managers (Multiverse, Worlds, etc.)
     * have already loaded their worlds, so Bukkit.getWorld() is guaranteed to find
     * any world that is running — regardless of plugin enable order.
     */
    public static synchronized void lazyResolve() {
        if (world != null) return;

        // Always re-resolve from config and strip any "group:worldname" prefix.
        // This guards against tryRecover() having set name to the raw value without stripping.
        String rawName = (name != null) ? name : Config.CONFIG.getString("world.name");
        name = stripPrefix(rawName);

        if (name == null) return;

        world = Bukkit.getWorld(name);
        if (world != null) {
            IP.logging().info("Parkour world '%s' resolved on first session (lazy).".formatted(name));
            scheduleSetup();
        } else {
            IP.logging().error("Parkour world '%s' not found — is the world loaded on this server?".formatted(name));
        }
    }

    private static String stripPrefix(String raw) {
        if (raw == null) return null;
        return raw.contains(":") ? raw.substring(raw.lastIndexOf(':') + 1) : raw;
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
