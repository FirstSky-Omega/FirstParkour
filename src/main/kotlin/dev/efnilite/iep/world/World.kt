package dev.efnilite.iep.world

import dev.efnilite.iep.IEP
import dev.efnilite.ip.IP
import dev.efnilite.ip.foundation.util.VoidGenerator
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Class for handling the IEP elytra parkour world.
 *
 * <h3>Paper 26 "world isn't void after restart" pitfall</h3>
 *
 * Bukkit.createWorld() with a custom generator only honors that generator on FIRST
 * creation. If Multiverse / bukkit.yml / a stale dir from a crashed shutdown caused
 * the IEP world to already be loaded with a vanilla generator, our WorldCreator's
 * generator is silently ignored. The fix mirrors what IP/World.java does:
 *
 *   1. force-unload any pre-existing in-memory copy
 *   2. delete the dir from disk (retries for Windows file-lock races)
 *   3. createWorld() — guaranteed first-load path now → our void generator applies
 *   4. verify post-create that spawn-area blocks are AIR; if not, log loud error
 */
object World {

    private const val NAME = "iep"

    lateinit var world: World

    /**
     * Creates the world.
     */
    fun create() {
        // Idempotent: if onLoad() already created the world, skip.
        if (::world.isInitialized) return

        IP.logging().info("[IEP] Creating world $NAME")

        // Step 1: if Bukkit already loaded our world, unload it so we can override
        // the generator below.
        val existing = Bukkit.getWorld(NAME)
        if (existing != null) {
            IEP.logging.warn("IEP world '$NAME' was already loaded by something else; unloading it so IEP can apply its void generator.")
            val unloaded = Bukkit.unloadWorld(existing, false)
            if (!unloaded) {
                IEP.logging.error("Could not unload '$NAME' — another plugin is keeping it loaded; the world may NOT be void.")
            }
        }

        // Step 2: wipe the on-disk dir so the next createWorld is fresh. IEP always
        // wipes on disable, but a crash shutdown can leave the dir intact.
        deleteWorldOnDisk(attempts = 2)

        world = WorldCreator(NAME)
            .generator(VoidGenerator.getGenerator())
            .type(WorldType.NORMAL)
            .createWorld()!!

        setup()
        verifyIsVoidOrWarn()
    }

    /**
     * Sets all world settings.
     */
    private fun setup() {
        IEP.log("Setting up rules for world $NAME")

        world.setGameRule(GameRule.DO_FIRE_TICK, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(GameRule.DO_TILE_DROPS, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false)
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0)

        world.worldBorder.setCenter(0.0, 0.0)
        world.worldBorder.size = 10_000_000.0
        world.difficulty = Difficulty.PEACEFUL
        world.clearWeatherDuration = 1_000_000
        world.time = 10_000
        world.isAutoSave = false
        world.keepSpawnInMemory = false

        // simulationDistance is safe to keep low — the IEP world has no mobs, and
        // Display entities don't tick. This saves random-tick cycles without breaking
        // the view distance the client needs to see the (client-side) parkour pipe.
        try { world.simulationDistance = 4 } catch (_: Throwable) {}
    }

    private fun verifyIsVoidOrWarn() {
        try {
            val spawn = world.spawnLocation
            val sx = spawn.blockX
            val sz = spawn.blockZ
            for (y in intArrayOf(60, 64, 70, 100)) {
                for (dx in -2..2) {
                    for (dz in -2..2) {
                        val m = world.getBlockAt(sx + dx, y, sz + dz).type
                        if (m != Material.AIR) {
                            IEP.logging.error(
                                "IEP world '$NAME' is NOT void — block at (${sx + dx},$y,${sz + dz}) is $m. " +
                                "An old non-void '$NAME/' directory was loaded with a non-IEP generator. " +
                                "Stop the server, delete the '$NAME/' directory manually, then restart."
                            )
                            return
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            IEP.logging.stack("Failed to verify IEP world is void", t)
        }
    }

    /**
     * Deletes the parkour world.
     */
    fun delete() {
        IEP.log("Deleting world $NAME")
        if (::world.isInitialized) {
            try { Bukkit.unloadWorld(world, false) } catch (_: Throwable) {}
        }
        deleteWorldOnDisk(attempts = 2)
    }

    /**
     * Walks the world directory and deletes everything, with retries — Paper's async
     * chunk IO threads may still hold file handles for a beat after unloadWorld returns
     * (especially on Windows), so a single-pass delete sometimes leaves region files
     * behind. We retry once with a 500ms sleep, then give up loudly.
     */
    private fun deleteWorldOnDisk(attempts: Int) {
        val file = File(NAME)
        if (!file.exists()) return

        IEP.log("Deleting IEP world directory '$NAME' (up to $attempts attempts)")

        for (attempt in 1..attempts) {
            var allGone = true
            try {
                Files.walk(file.toPath()).use { stream ->
                    stream.sorted(Comparator.reverseOrder()).forEach { p ->
                        try {
                            Files.deleteIfExists(p)
                        } catch (_: IOException) {
                            allGone = false
                        }
                    }
                }
            } catch (_: IOException) {
                allGone = false
            }

            if (!file.exists() || allGone) return

            if (attempt < attempts) {
                IEP.logging.warn("Could not fully delete '$NAME/' (attempt $attempt). Retrying after 500ms.")
                try { Thread.sleep(500L) } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }

        if (file.exists()) {
            IEP.logging.error(
                "Failed to delete '$NAME/' after $attempts attempts. " +
                "Stop the server and remove the directory manually before next startup, " +
                "or the world may load with the wrong generator."
            )
        }
    }
}
