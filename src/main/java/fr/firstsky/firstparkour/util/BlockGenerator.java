package fr.firstsky.firstparkour.util;

import fr.firstsky.firstparkour.FirstParkour;
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

    /**
     * Génère la prochaine location de bloc depuis la dernière position de la session.
     * Retourne null si la session n'a aucun bloc de référence.
     */
    public Location generateNext(ParkourSession session) {
        Location last = session.getLastBlock();
        if (last == null) return null;

        ConfigurationSection diff = getDiffSection(session.getDifficulty());
        if (diff == null) return null;

        int minDist = diff.getInt("min-distance", 2);
        int maxDist = diff.getInt("max-distance", 3);
        int minH = diff.getInt("min-height", -1);
        int maxH = diff.getInt("max-height", 1);
        double spread = diff.getDouble("angle-spread", 30.0);

        int distance = minDist + random.nextInt(maxDist - minDist + 1);

        double deviation = (random.nextDouble() * 2.0 - 1.0) * Math.toRadians(spread);
        double newAngle = session.getCurrentAngle() + deviation;
        session.setCurrentAngle(newAngle);

        int dx = (int) Math.round(Math.sin(newAngle) * distance);
        int dz = (int) Math.round(Math.cos(newAngle) * distance);

        // Garantie de distance minimale sur l'axe horizontal
        if (Math.abs(dx) + Math.abs(dz) < 2) {
            if (dx == 0 && dz == 0) dz = distance;
        }

        int dy = minH + random.nextInt(maxH - minH + 1);
        int newY = last.getBlockY() + dy;
        int minWorld = last.getWorld().getMinHeight() + 5;
        int maxWorld = last.getWorld().getMaxHeight() - 5;
        newY = Math.max(minWorld, Math.min(maxWorld, newY));

        return new Location(last.getWorld(),
                last.getBlockX() + dx,
                newY,
                last.getBlockZ() + dz);
    }

    public Material getRandomMaterial(Difficulty difficulty) {
        ConfigurationSection diff = getDiffSection(difficulty);
        if (diff == null) return Material.STONE;

        List<String> blockNames = diff.getStringList("blocks");
        if (blockNames.isEmpty()) return Material.STONE;

        List<Material> materials = new ArrayList<>();
        for (String name : blockNames) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (materials.isEmpty()) return Material.STONE;
        return materials.get(random.nextInt(materials.size()));
    }

    private ConfigurationSection getDiffSection(Difficulty difficulty) {
        return plugin.getConfig().getConfigurationSection("difficulties." + difficulty.getKey());
    }
}
