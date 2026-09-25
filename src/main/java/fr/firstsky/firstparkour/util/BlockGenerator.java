package fr.firstsky.firstparkour.util;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.ParkourSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BlockGenerator {

    private record DifficultyConfig(int minDist, int maxDist, int minH, int maxH, double spread) {}

    /** Zone lobby à éviter lors de la génération. */
    private record LobbyZone(boolean enabled, int cx, int cz, int half) {
        boolean contains(int x, int z) {
            return enabled && Math.abs(x - cx) <= half && Math.abs(z - cz) <= half;
        }
    }

    private final FirstParkour plugin;
    // ThreadLocalRandom : thread-safe sous Folia (régions parallèles)

    /** Config de génération pré-calculée par difficulté */
    private final Map<Difficulty, DifficultyConfig> diffConfigs = new EnumMap<>(Difficulty.class);
    /** Matériaux pré-parsés par difficulté */
    private final Map<Difficulty, Material[]> diffMaterials = new EnumMap<>(Difficulty.class);
    /** Matériaux pré-parsés par thème */
    private final Map<BlockTheme, Material[]> themeMaterials = new EnumMap<>(BlockTheme.class);
    /** Zone lobby à ne pas envahir */
    private LobbyZone lobbyZone = new LobbyZone(false, 0, 0, 0);

    public BlockGenerator(FirstParkour plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Appelé à l'initialisation et sur /parkour recharger. */
    public void reload() {
        diffConfigs.clear();
        diffMaterials.clear();
        themeMaterials.clear();

        for (Difficulty d : Difficulty.values()) {
            ConfigurationSection sec = plugin.getConfig()
                    .getConfigurationSection("difficulties." + d.getKey());
            if (sec == null) continue;
            diffConfigs.put(d, new DifficultyConfig(
                    sec.getInt("min-distance", 2), sec.getInt("max-distance", 3),
                    sec.getInt("min-height", -1),  sec.getInt("max-height", 1),
                    sec.getDouble("angle-spread", 30.0)));
            diffMaterials.put(d, parseMaterials(sec.getStringList("blocks")));
        }

        int cx   = plugin.getConfig().getInt("parkour.lobby-zone.center-x", 0);
        int cz   = plugin.getConfig().getInt("parkour.lobby-zone.center-z", 0);
        int half = plugin.getConfig().getInt("parkour.lobby-zone.half-size", 200);
        // Active dès que half-size > 0, indépendamment de la clé "enabled"
        // (évite que saveConfig() écrase la valeur en false)
        lobbyZone = new LobbyZone(half > 0, cx, cz, half);

        for (BlockTheme t : BlockTheme.values()) {
            if (t == BlockTheme.DEFAULT) continue;
            ConfigurationSection sec = plugin.getConfig()
                    .getConfigurationSection("themes." + t.getKey());
            if (sec == null) continue;
            Material[] mats = parseMaterials(sec.getStringList("blocks"));
            if (mats.length > 0) themeMaterials.put(t, mats);
        }
    }

    public boolean isInLobbyZone(Location loc) {
        return lobbyZone.contains(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Retourne une location de bloc (Y-1) juste à l'extérieur de la zone lobby.
     * La direction choisie est celle qui va du centre de la zone vers la position du joueur ;
     * si le joueur est exactement au centre, on utilise preferredAngle.
     */
    public Location getStartOutsideLobby(Location origin, double preferredAngle) {
        double dx = origin.getBlockX() - lobbyZone.cx();
        double dz = origin.getBlockZ() - lobbyZone.cz();
        double angle = (Math.abs(dx) < 0.5 && Math.abs(dz) < 0.5)
                ? preferredAngle
                : Math.atan2(dx, dz);
        double dist = lobbyZone.half() + 5.0;
        int x = lobbyZone.cx() + (int) Math.round(Math.sin(angle) * dist);
        int z = lobbyZone.cz() + (int) Math.round(Math.cos(angle) * dist);
        // Ne pas appeler getHighestBlockYAt ici : (x,z) est dans une région Folia
        // différente de celle du joueur, l'appel serait sur le mauvais thread.
        // Le spawn doit être configuré à une hauteur correcte par /parkour definirespawn.
        return new Location(origin.getWorld(), x, origin.getBlockY() - 1, z);
    }

    public Location generateNext(ParkourSession session) {
        Location last = session.getLastBlock();
        if (last == null) return null;

        DifficultyConfig cfg = diffConfigs.get(session.getDifficulty());
        if (cfg == null) return null;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int dy = cfg.minH() + rng.nextInt(cfg.maxH() - cfg.minH() + 1);

        // En montée, limiter la distance horizontale à minDist pour que le saut reste faisable
        // (sprint-jump à +1 bloc : max ~2 blocs horizontaux en Minecraft)
        int effectiveDist = (dy > 0) ? cfg.minDist() : cfg.minDist() + rng.nextInt(cfg.maxDist() - cfg.minDist() + 1);

        double deviation = (rng.nextDouble() * 2.0 - 1.0) * Math.toRadians(cfg.spread());
        double newAngle = session.getCurrentAngle() + deviation;
        session.setCurrentAngle(newAngle);

        int dx = (int) Math.round(Math.sin(newAngle) * effectiveDist);
        int dz = (int) Math.round(Math.cos(newAngle) * effectiveDist);
        if (Math.abs(dx) + Math.abs(dz) < 2) {
            if (dx == 0 && dz == 0) dz = effectiveDist;
        }

        // Si le bloc candidat tombe dans la zone lobby, on réoriente loin du centre
        if (lobbyZone.contains(last.getBlockX() + dx, last.getBlockZ() + dz)) {
            double awayAngle = Math.atan2(
                    last.getBlockX() - lobbyZone.cx(),
                    last.getBlockZ() - lobbyZone.cz());
            newAngle = awayAngle + (rng.nextDouble() * 2.0 - 1.0) * Math.toRadians(cfg.spread() * 0.5);
            dx = (int) Math.round(Math.sin(newAngle) * effectiveDist);
            dz = (int) Math.round(Math.cos(newAngle) * effectiveDist);
            if (Math.abs(dx) + Math.abs(dz) < 2) dz = effectiveDist;
            session.setCurrentAngle(newAngle);
        }
        int newY = last.getBlockY() + dy;
        int minWorld = last.getWorld().getMinHeight() + 5;
        int maxWorld = last.getWorld().getMaxHeight() - 5;
        newY = Math.max(minWorld, Math.min(maxWorld, newY));

        return new Location(last.getWorld(),
                last.getBlockX() + dx, newY, last.getBlockZ() + dz);
    }

    public Material getRandomMaterial(Difficulty difficulty, BlockTheme theme) {
        if (theme != null && theme != BlockTheme.DEFAULT) {
            Material[] mats = themeMaterials.get(theme);
            if (mats != null && mats.length > 0) return mats[ThreadLocalRandom.current().nextInt(mats.length)];
        }
        Material[] mats = diffMaterials.get(difficulty);
        if (mats == null || mats.length == 0) return Material.STONE;
        return mats[ThreadLocalRandom.current().nextInt(mats.length)];
    }

    private static Material[] parseMaterials(List<String> names) {
        List<Material> mats = new ArrayList<>();
        for (String name : names) {
            try { mats.add(Material.valueOf(name.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        return mats.toArray(new Material[0]);
    }
}
