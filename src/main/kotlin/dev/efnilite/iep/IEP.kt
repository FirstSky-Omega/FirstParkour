package dev.efnilite.iep

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.efnilite.ip.IP
import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.hook.ChunkyHook
import dev.efnilite.iep.hook.PapiHook
import dev.efnilite.iep.mode.*
import dev.efnilite.iep.style.IncrementalStyle
import dev.efnilite.iep.style.RandomStyle
import dev.efnilite.iep.style.Style
import dev.efnilite.iep.world.Divider
import dev.efnilite.iep.world.World
import dev.efnilite.ip.foundation.schematic.Schematics
import dev.efnilite.ip.foundation.util.Logging
import dev.efnilite.ip.foundation.util.Task
import org.bukkit.Material
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object IEP {

    lateinit var instance: IP
        private set

    val dataFolder: File
        get() = instance.dataFolder.resolve("elytra")

    val logging: Logging
        get() = IP.logging()

    fun enable(plugin: IP) {
        instance = plugin
        stopping = false
        modes.clear()
        styles.clear()

        plugin.registerListener(Events)
        plugin.registerCommand("eparkour", Command)

        saveFile("schematics/spawn-island")

        World.create()
        Locales.init()
        Files.list(dataFolder.toPath().resolve("schematics")).use { files ->
            Schematics.addFromFiles(plugin, *files.map { it.toFile() }.toList().toTypedArray())
        }

        registerStyle("styles.random") { name, data -> RandomStyle(name, data) }
        registerStyle("styles.incremental") { name, data -> IncrementalStyle(name, data) }

        registerMode(DefaultMode, false)
        registerMode(SpeedDemonMode)
        registerMode(MinSpeedMode)
        registerMode(TimeTrialMode)
        registerMode(CloseMode)
        registerMode(ObstacleMode)

        if (plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            log("Registered PlaceholderAPI Hook")
            papiHook = PapiHook
            PapiHook.register()
        }
        if (plugin.server.pluginManager.isPluginEnabled("Chunky")) {
            chunkyHook = ChunkyHook
//            ChunkyHook.init() todo
        }
        if (plugin.server.pluginManager.isPluginEnabled("Vault")) {
            log("Registered Vault Hook")
        }
        if (Config.CONFIG.getBoolean("proxy.enabled")) {
            log("Registered BungeeCord Hook")
            plugin.server.messenger.registerOutgoingPluginChannel(plugin, "BungeeCord")
        }
        
        Task.create(plugin)
            .async()
            .repeat(5 * 60 * 20)
            .delay(5 * 60 * 20)
            .execute {
                log("Saving all leaderboards")
                modes.forEach { it.leaderboard.save() }
            }
            .run()

    }

    fun saveFile(path: String) {
        val file = dataFolder.resolve(path)

        if (!file.exists()) {
            instance.saveResource("elytra/$path", false)
        }
    }

    private fun registerStyle(path: String, fn: (name: String, data: List<Material>) -> Style) {
        Config.CONFIG.getPaths(path).forEach { name ->
            registerStyle(
                fn.invoke(name, Config.CONFIG.getStringList("$path.$name")
                    .map {
                        try {
                            return@map Material.getMaterial(it.uppercase())!!
                        } catch (_: NullPointerException) {
                            logging.error("Invalid material in style $path.$name: $it")
                            return@map Material.STONE
                        }
                    })
            )
        }
    }

    fun disable() {
        stopping = true

        try {
            papiHook?.unregister()
            papiHook = null

            for (generator in HashSet(Divider.generators)) {
                generator.player.leave(urgent = true)
            }

            getModes().forEach { it.leaderboard.save() }

            World.delete()
        } catch (_: Exception) {
            // for no class found errors if nobody has joined yet
        }
    }

    var stopping = false
        private set
    var papiHook: PapiHook? = null
    var chunkyHook: ChunkyHook? = null

    val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun log(message: String) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] $message")
        }
    }

    private val modes: MutableList<Mode> = mutableListOf()

    fun registerMode(mode: Mode, checkExists: Boolean = true) {
        if (!Config.CONFIG.getBoolean("mode-settings.${mode.name.replace(" ", "-")}.enabled") && checkExists) {
            return
        }

        log("Registered mode ${mode.name}")

        modes += mode
    }

    fun getMode(name: String): Mode? = modes.firstOrNull { it.name == name }

    fun getModes() = modes.toList()

    private val styles: MutableList<Style> = mutableListOf()

    fun registerStyle(style: Style) {
        log("Registered style ${style.name()}")

        styles += style
    }

    fun getStyle(name: String) = styles.firstOrNull { it.name() == name } ?: styles.first()

    fun getStyles() = styles.toList()
}
