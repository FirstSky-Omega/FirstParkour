package dev.efnilite.ip.foundation.schematic;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Schematics {

    private static final Map<Plugin, Map<String, Schematic>> cache = new HashMap<>();

    public static void addFromFiles(@NotNull Plugin plugin, @NotNull File... files) throws IOException, ClassNotFoundException {
        Map<String, Schematic> current = cache.getOrDefault(plugin, new HashMap<>());

        for (File file : files) {
            current.put(file.getName(), Schematic.load(file, plugin));
        }

        cache.put(plugin, current);

        plugin.getLogger().info("Loaded all schematics!");
    }

    public static Schematic getSchematic(@NotNull Plugin plugin, @NotNull String schematicName) {
        return cache.get(plugin).get(schematicName);
    }

    public static Set<String> getSchematicNames(@NotNull Plugin plugin) {
        return cache.get(plugin).keySet();
    }

    public static Collection<Schematic> getSchematics(@NotNull Plugin plugin) {
        return cache.get(plugin).values();
    }
}