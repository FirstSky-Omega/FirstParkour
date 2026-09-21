package fr.firstsky.firstparkour.util;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockGenerator {

    private final FirstParkour plugin;
    private final Random random = new Random();

    public BlockGenerator(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public Location generateNext(ParkourSession session) {
        Location last = session.getLastBlock();
        if (last == null) return null;

        ConfigurationSection diff = plugin.getConfig()
                .getConfigurationSection("difficulties." + session.getDifficulty().getKey());
        if (diff == null) return null;

        int minDist = diff.getInt("min-distance", 2);
        int maxDist = diff.getInt("max-distance", 3);
        int minH    = diff.getInt("min-height", -1);
        int maxH    = diff.getInt("max-height", 1);
        double spread = diff.getDouble("angle-spread", 30.0);

        int distance = minDist + random.nextInt(maxDist - minDist + 1);

        double deviation = (random.nextDouble() * 2.0 - 1.0) * Math.toRadians(spread);
        double newAngle = session.getCurrentAngle() + deviation;
        session.setCurrentAngle(newAngle);

        int dx = (int) Math.round(Math.sin(newAngle) * distance);
        int dz = (int) Math.round(Math.cos(newAngle) * distance);
        if (Math.abs(dx) + Math.abs(dz) < 2) {
            if (dx == 0 && dz == 0) dz = distance;
        }

        int dy = minH + random.nextInt(maxH - minH + 1);
        int newY = last.getBlockY() + dy;
        int minWorld = last.getWorld().getMinHeight() + 5;
        int maxWorld = last.getWorld().getMaxHeight() - 5;
        newY = Math.max(minWorld, Math.min(maxWorld, newY));

        return new Location(last.getWorld(),
                last.getBlockX() + dx, newY, last.getBlockZ() + dz);
    }

    /**
     * Récupère un matériau aléatoire selon le thème du joueur.
     * Si le thème est DEFAULT, utilise les blocs de la difficulté.
     */
    public Material getRandomMaterial(Difficulty difficulty, BlockTheme theme) {
        List<String> blockNames;

        if (theme != null && theme != BlockTheme.DEFAULT) {
            ConfigurationSection themeSec = plugin.getConfig()
                    .getConfigurationSection("themes." + theme.getKey());
            if (themeSec != null) {
                blockNames = themeSec.getStringList("blocks");
                if (!blockNames.isEmpty()) {
                    return pickMaterial(blockNames);
                }
            }
        }

        // Fallback : blocs de la difficulté
        ConfigurationSection diff = plugin.getConfig()
                .getConfigurationSection("difficulties." + difficulty.getKey());
        if (diff == null) return Material.STONE;
        blockNames = diff.getStringList("blocks");
        return pickMaterial(blockNames);
    }

    private Material pickMaterial(List<String> names) {
        List<Material> mats = new ArrayList<>();
        for (String name : names) {
            try { mats.add(Material.valueOf(name.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        if (mats.isEmpty()) return Material.STONE;
        return mats.get(random.nextInt(mats.size()));
    }
}
