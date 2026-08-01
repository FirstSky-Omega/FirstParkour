package dev.efnilite.iep.config

import dev.efnilite.iep.IEP
import com.tchristofferson.configupdater.ConfigUpdater
import org.bukkit.configuration.file.YamlConfiguration

/**
 * Per-file ignored-section lists — passing a section name to ConfigUpdater that doesn't
 * exist in the target file blows up with "Ignored sections must be a ConfigurationSection
 * not a value!" (the lookup returns null, which is not a Map). The previous shared list
 * meant updating rewards.yml tried to ignore `styles` and updating config.yml tried to
 * ignore `score`/`interval`/`one-time`, and IEP failed to enable on Paper 26.
 */
enum class Config(file: String, private val ignoredSections: List<String>) {

    CONFIG("config.yml", listOf("styles")),
    REWARDS("rewards.yml", listOf("score", "interval", "one-time"));

    private val config: YamlConfiguration

    init {
        IEP.saveFile(file)

        val target = IEP.dataFolder.resolve(file)

        // ConfigUpdater throws "Ignored sections must be a ConfigurationSection not a value!"
        // if any listed path doesn't exist in the target file (lookup returns null which
        // isn't a Map). On Paper 26 this kept IEP from enabling at all. Pre-filter
        // against the on-disk YAML so we only pass paths that are actually present as
        // sections — handles users with stale custom configs too.
        val existing = YamlConfiguration.loadConfiguration(target)
        val present = ignoredSections.filter { existing.isConfigurationSection(it) }

        ConfigUpdater.update(IEP.instance, "elytra/$file", target, present)

        config = YamlConfiguration.loadConfiguration(target)
    }

    /**
     * Returns a boolean from the file.
     */
    fun getBoolean(path: String): Boolean = config.getBoolean(path)

    /**
     * Returns an integer from the file.
     */
    fun getInt(path: String, bounds: (Int) -> Boolean = { true }): Int {
        val value = config.getInt(path)

        if (!bounds.invoke(value)) {
            IEP.logging.error("Value $value at path $path is invalid")
        }

        return value
    }

    /**
     * Returns a string from the file.
     */
    fun getString(path: String, bounds: (String) -> Boolean = { true }): String {
        val value = config.getString(path)!!

        if (!bounds.invoke(value)) {
            IEP.logging.error("Value $value at path $path is invalid")
        }

        return value
    }

    /**
     * Returns a string list from the file.
     */
    fun getStringList(path: String, bounds: (List<String>) -> Boolean = { true }): List<String> {
        val value = config.getStringList(path)

        if (!bounds.invoke(value)) {
            IEP.logging.error("Value $value at path $path is invalid")
        }

        return value
    }

    /**
     * Returns a double from the file.
     */
    fun getDouble(path: String, bounds: (Double) -> Boolean = { true }): Double {
        val value = config.getDouble(path)

        if (!bounds.invoke(value)) {
            IEP.logging.error("Value $value at path $path is invalid")
        }

        return value
    }

    fun getPaths(path: String): Set<String> = config.getConfigurationSection(path)?.getKeys(false) ?: emptySet()
}
