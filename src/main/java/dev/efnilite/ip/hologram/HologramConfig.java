package dev.efnilite.ip.hologram;

import dev.efnilite.ip.IP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses {@code plugins/IP/holograms.yml} into the structured form the runtime uses.
 *
 * <p>Top-level YAML shape:
 * <pre>
 * holograms:
 *   my-id:
 *     enabled: true
 *     location: 'world:0.5:80.5:0.5:0'   # world:x:y:z:yaw
 *     update-interval-ticks: 20
 *     interaction:
 *       width: 1.6
 *       height: 1.4
 *       offset-y: -1.4                   # interaction box centered just below the text
 *     pages:
 *       - lines:                         # one MiniMessage string per visible line
 *           - '&lt;gradient:#1fe7ff:#57fde7&gt;经典模式排行榜&lt;/gradient&gt;'
 *           - '1. %witp_player_rank_default_1% - %witp_score_rank_default_1%'
 *         icons:                         # optional ItemDisplay decorations
 *           - material: GOLDEN_BOOTS
 *             enchanted: true
 *             offset: '0.0,1.2,0.0'      # x,y,z relative to hologram anchor
 *             scale: 0.6
 * </pre>
 *
 * <p>A missing or unparseable hologram is skipped with a warning rather than crashing
 * the plugin enable() — a typo in one entry should never disable the entire leaderboard
 * subsystem.</p>
 */
public final class HologramConfig {

    public record IconSpec(Material material, boolean enchanted, double dx, double dy, double dz, float scale) {}

    public record Page(List<String> lines, List<IconSpec> icons) {}

    public record InteractionSpec(float width, float height, double offsetY) {}

    public record Entry(String id, Location location, long updateIntervalTicks, List<Page> pages, InteractionSpec interaction) {}

    private HologramConfig() {}

    /** Loads and validates the YAML; returns a map of id -> entry. */
    public static Map<String, Entry> load(File file) {
        if (!file.exists()) {
            IP.log("holograms.yml does not exist — skipping built-in hologram subsystem");
            return Map.of();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("holograms");
        if (root == null) {
            return Map.of();
        }

        Map<String, Entry> out = new LinkedHashMap<>();
        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;
                if (!sec.getBoolean("enabled", true)) continue;

                Entry entry = parseEntry(id, sec);
                if (entry != null) out.put(id, entry);
            } catch (Throwable t) {
                IP.logging().stack("Failed to parse hologram '%s' in holograms.yml — skipping".formatted(id), t);
            }
        }
        return out;
    }

    @Nullable
    private static Entry parseEntry(String id, ConfigurationSection sec) {
        Location location = parseLocation(sec.getString("location"));
        if (location == null) {
            IP.logging().warn("Hologram '%s' has an invalid 'location' — skipping".formatted(id));
            return null;
        }

        long interval = Math.max(1L, sec.getLong("update-interval-ticks", 20L));

        List<Page> pages = new ArrayList<>();
        List<Map<?, ?>> rawPages = sec.getMapList("pages");
        for (Map<?, ?> rawPage : rawPages) {
            pages.add(parsePage(rawPage));
        }
        if (pages.isEmpty()) {
            IP.logging().warn("Hologram '%s' has no 'pages' — skipping".formatted(id));
            return null;
        }

        ConfigurationSection inter = sec.getConfigurationSection("interaction");
        float w = inter == null ? 1.6f : (float) inter.getDouble("width", 1.6);
        float h = inter == null ? 1.4f : (float) inter.getDouble("height", 1.4);
        double oy = inter == null ? -1.4 : inter.getDouble("offset-y", -1.4);
        InteractionSpec spec = new InteractionSpec(Math.max(0.1f, w), Math.max(0.1f, h), oy);

        return new Entry(id, location, interval, pages, spec);
    }

    @SuppressWarnings("unchecked")
    private static Page parsePage(Map<?, ?> raw) {
        List<String> lines = new ArrayList<>();
        Object linesObj = raw.get("lines");
        if (linesObj instanceof List<?> list) {
            for (Object l : list) lines.add(String.valueOf(l));
        }

        List<IconSpec> icons = new ArrayList<>();
        Object iconsObj = raw.get("icons");
        if (iconsObj instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                IconSpec spec = parseIcon((Map<String, Object>) m);
                if (spec != null) icons.add(spec);
            }
        }

        return new Page(List.copyOf(lines), List.copyOf(icons));
    }

    @Nullable
    private static IconSpec parseIcon(Map<String, Object> m) {
        Object mat = m.get("material");
        if (mat == null) return null;
        Material material;
        try {
            material = Material.valueOf(String.valueOf(mat).toUpperCase());
        } catch (IllegalArgumentException ex) {
            IP.logging().warn("Unknown icon material '%s'".formatted(mat));
            return null;
        }
        boolean enchanted = m.get("enchanted") instanceof Boolean b ? b : false;

        double dx = 0, dy = 0, dz = 0;
        Object off = m.get("offset");
        if (off instanceof String s) {
            String[] parts = s.split("[,:\\s]+");
            if (parts.length >= 3) {
                try {
                    dx = Double.parseDouble(parts[0]);
                    dy = Double.parseDouble(parts[1]);
                    dz = Double.parseDouble(parts[2]);
                } catch (NumberFormatException ignored) {}
            }
        }
        float scale = m.get("scale") instanceof Number n ? n.floatValue() : 0.5f;
        return new IconSpec(material, enchanted, dx, dy, dz, Math.max(0.05f, scale));
    }

    /**
     * Parses "world:x:y:z" or "world:x:y:z:yaw" into a {@link Location}. Returns null if
     * the world doesn't exist (which is a legitimate case during plugin enable — the
     * parkour world may not be created yet — and we'll re-try later).
     */
    @Nullable
    public static Location parseLocation(@Nullable String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(":");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0f;
            return new Location(world, x, y, z, yaw, 0f);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
