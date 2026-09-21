package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.model.ParkourSession;
import fr.firstsky.firstparkour.util.BlockGenerator;
import fr.firstsky.firstparkour.util.FoliaUtil;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ParkourManager {

    private final FirstParkour plugin;
    private final BlockGenerator generator;
    private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode>  previousGameModes  = new ConcurrentHashMap<>();
    private final Map<UUID, Location>  savedLocations     = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> playerDataCache   = new ConcurrentHashMap<>();

    public ParkourManager(FirstParkour plugin) {
        this.plugin = plugin;
        this.generator = new BlockGenerator(plugin);
    }

    // ──────────────────────────────────────────────
    //  Chargement / déchargement joueur
    // ──────────────────────────────────────────────

    public void loadPlayerAsync(Player player) {
        FoliaUtil.runAsync(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayer(player.getUniqueId(), player.getName());
            playerDataCache.put(player.getUniqueId(), data);
        });
    }

    public void unloadPlayer(Player player) {
        savedLocations.remove(player.getUniqueId());
        stopSession(player, false);
        PlayerData data = playerDataCache.remove(player.getUniqueId());
        if (data != null) {
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }
    }

    // ──────────────────────────────────────────────
    //  Session
    // ──────────────────────────────────────────────

    public boolean isPlaying(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Démarre une session parkour.
     * Si un monde de parkour est configuré, téléporte le joueur avant d'initialiser.
     */
    public void startSession(Player player, Difficulty difficulty) {
        if (isPlaying(player)) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.already-playing", "Déjà en jeu !"));
            return;
        }

        PlayerData data = playerDataCache.computeIfAbsent(player.getUniqueId(),
                id -> plugin.getDatabaseManager().loadPlayer(id, player.getName()));

        // Sauvegarde gamemode avant de changer
        if (plugin.getConfig().getBoolean("parkour.force-adventure", true)) {
            previousGameModes.put(player.getUniqueId(), player.getGameMode());
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(GameMode.ADVENTURE));
        }

        Location parkourSpawn = getParkourSpawn(player);

        if (parkourSpawn != null) {
            // Sauvegarde la position d'origine AVANT la téléportation
            savedLocations.put(player.getUniqueId(), player.getLocation().clone());
            // Téléporte puis initialise
            FoliaUtil.teleport(plugin, player, parkourSpawn,
                    () -> initSession(player, difficulty, data));
        } else {
            // Pas de monde configuré : démarrage sur place
            initSession(player, difficulty, data);
        }
    }

    /** Initialise la session une fois le joueur positionné au bon endroit. */
    private void initSession(Player player, Difficulty difficulty, PlayerData data) {
        int personalBest = data.getBestScore(difficulty);
        int historySize  = plugin.getConfig().getInt("parkour.history-size", 12);

        ParkourSession session = new ParkourSession(
                player.getUniqueId(), difficulty, personalBest, historySize);

        float yaw = player.getLocation().getYaw();
        session.setCurrentAngle(Math.toRadians(yaw));
        session.setTheme(data.getTheme());

        Location startLoc = player.getLocation().getBlock().getLocation();
        session.addBlock(startLoc);
        session.setLastLandedBlock(startLoc);

        sessions.put(player.getUniqueId(), session);

        int blocksAhead = plugin.getConfig().getInt("parkour.blocks-ahead", 3);
        for (int i = 0; i < blocksAhead; i++) placeNextBlock(session);

        String diffName = plugin.getConfig().getString(
                "difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());
        String msg = plugin.getConfig().getString("messages.prefix", "")
                + plugin.getConfig().getString("messages.start", "&aDémarré !")
                .replace("{difficulty}", MessageUtil.color(diffName));
        MessageUtil.send(player, msg);
    }

    // ──────────────────────────────────────────────
    //  Atterrissage
    // ──────────────────────────────────────────────

    public void onPlayerLand(Player player, ParkourSession session, Location landedOn) {
        session.setLastLandedBlock(landedOn);
        session.incrementScore();

        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) data.updateBestScore(session.getDifficulty(), session.getScore());

        boolean newRecord = session.getScore() == session.getPersonalBest() && session.getScore() > 1;

        placeNextBlock(session);

        Location toRemove = session.pollOldestIfNeeded();
        if (toRemove != null) {
            FoliaUtil.runAtLocation(plugin, toRemove, () -> toRemove.getBlock().setType(Material.AIR));
        }

        if (plugin.getConfig().getBoolean("parkour.action-bar", true)) {
            String duelPart = buildDuelPart(player, session);
            String msg = plugin.getConfig().getString("messages.score-actionbar",
                            "&6Score: &e{score} &7| &6Record: &e{best}")
                    .replace("{score}", String.valueOf(session.getScore()))
                    .replace("{best}", String.valueOf(session.getPersonalBest()))
                    + duelPart;
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.sendActionBar(player, msg));
        }

        if (newRecord) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.new-record", "&6RECORD!")
                    .replace("{score}", String.valueOf(session.getScore()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
        }
    }

    // ──────────────────────────────────────────────
    //  Arrêt de session
    // ──────────────────────────────────────────────

    public void stopSession(Player player, boolean sendMessage) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);

        // Supprime les blocs
        for (Location loc : session.getAllBlocks()) {
            FoliaUtil.runAtLocation(plugin, loc, () -> loc.getBlock().setType(Material.AIR));
        }

        // Restaure le gamemode
        GameMode prev = previousGameModes.remove(player.getUniqueId());
        if (prev != null) {
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(prev));
        }

        // Téléporte le joueur à sa position d'origine
        Location origin = savedLocations.remove(player.getUniqueId());
        if (origin != null) {
            FoliaUtil.teleport(plugin, player, origin, null);
        }

        // Sauvegarde le score
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            data.updateBestScore(session.getDifficulty(), session.getScore());
            data.addJumps(session.getScore());
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }

        plugin.getLeaderboardManager().forceRefresh();

        if (sendMessage) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.stop", "&cArrêté. Score: &6{score}")
                    .replace("{score}", String.valueOf(session.getScore()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
        }
    }

    public void stopAllSessions() {
        for (UUID uuid : sessions.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) stopSession(p, false);
        }
        sessions.clear();
    }

    // ──────────────────────────────────────────────
    //  Détection de chute
    // ──────────────────────────────────────────────

    public void checkFall(Player player) {
        ParkourSession session = getSession(player);
        if (session == null) return;

        int lowestBlockY = session.getAllBlocks().stream()
                .mapToInt(Location::getBlockY)
                .min().orElse(player.getLocation().getBlockY());

        if (player.getLocation().getBlockY() < lowestBlockY - 5) {
            int score = session.getScore();
            boolean inDuel = plugin.getDuelManager().isInDuel(player);

            stopSession(player, false);

            if (inDuel) {
                plugin.getDuelManager().onDuelEnd(player, score);
            } else {
                String msg = plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString("messages.fall", "&cTombé ! Score: &6{score}")
                        .replace("{score}", String.valueOf(score));
                FoliaUtil.runForEntity(plugin, player, () -> {
                    MessageUtil.send(player, msg);
                    MessageUtil.sendTitle(player, "&c✗", "&7Score: &6" + score, 5, 40, 10);
                });
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Thème
    // ──────────────────────────────────────────────

    public void setTheme(Player player, BlockTheme theme) {
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) data.setTheme(theme);

        ParkourSession session = getSession(player);
        if (session != null) session.setTheme(theme);

        if (data != null) {
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }
    }

    // ──────────────────────────────────────────────
    //  Monde de parkour
    // ──────────────────────────────────────────────

    /**
     * Retourne le spawn du monde de parkour configuré, ou null si aucun monde n'est défini.
     * Si le monde n'existe pas, log un avertissement et retourne null.
     */
    public Location getParkourSpawn(Player player) {
        String worldName = plugin.getConfig().getString("parkour.world", "");
        if (worldName == null || worldName.isBlank()) return null;

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.world-not-found",
                            "&cMonde &e{world} &cintrouvable !")
                    .replace("{world}", worldName);
            MessageUtil.send(player, msg);
            plugin.getLogger().log(Level.WARNING,
                    "Monde de parkour '" + worldName + "' introuvable. Démarrage sur place.");
            return null;
        }

        double x     = plugin.getConfig().getDouble("parkour.spawn.x", 0);
        double y     = plugin.getConfig().getDouble("parkour.spawn.y", 100);
        double z     = plugin.getConfig().getDouble("parkour.spawn.z", 0);
        float  yaw   = (float) plugin.getConfig().getDouble("parkour.spawn.yaw", 0);
        float  pitch = (float) plugin.getConfig().getDouble("parkour.spawn.pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    // ──────────────────────────────────────────────
    //  Utilitaires privés
    // ──────────────────────────────────────────────

    private void placeNextBlock(ParkourSession session) {
        Location next = generator.generateNext(session);
        if (next == null) return;
        Material mat = generator.getRandomMaterial(session.getDifficulty(), session.getTheme());
        session.addBlock(next);
        FoliaUtil.runAtLocation(plugin, next, () -> next.getBlock().setType(mat));
    }

    private String buildDuelPart(Player player, ParkourSession session) {
        if (!plugin.getDuelManager().isInDuel(player)) return "";
        UUID opUuid = plugin.getDuelManager().getDuel(player).getOpponent(player.getUniqueId());
        Player opponent = plugin.getServer().getPlayer(opUuid);
        if (opponent == null) return "";
        ParkourSession opSession = getSession(opponent);
        int opScore = opSession != null ? opSession.getScore() : 0;
        return " &8| &c⚔ " + opponent.getName() + ": &e" + opScore;
    }

    public Collection<ParkourSession> getAllSessions() { return sessions.values(); }
    public PlayerData getPlayerData(UUID uuid) { return playerDataCache.get(uuid); }
}
