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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

        // Ne jamais appeler loadPlayer sur le thread principal — si le cache n'est pas prêt, on attend
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data == null) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.prefix", "")
                    + "&7Données en chargement, réessayez dans un instant.");
            return;
        }

        // Sauvegarde gamemode avant de changer
        if (plugin.getConfig().getBoolean("parkour.force-adventure", true)) {
            previousGameModes.put(player.getUniqueId(), player.getGameMode());
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(GameMode.ADVENTURE));
        }

        String configuredWorld = plugin.getConfig().getString("parkour.world", "");
        Location parkourSpawn = getParkourSpawn(player);

        if (!configuredWorld.isBlank() && parkourSpawn == null) {
            // Monde configuré mais introuvable — message déjà envoyé par getParkourSpawn
            return;
        }

        if (parkourSpawn != null) {
            // Sauvegarde la position d'origine AVANT la téléportation
            savedLocations.put(player.getUniqueId(), player.getLocation().clone());
            // Téléporte puis initialise
            FoliaUtil.teleport(plugin, player, parkourSpawn,
                    () -> initSession(player, difficulty, data));
        } else {
            // Aucun monde configuré : démarrage sur place
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

        // Bloc sous les pieds du joueur — c'est là qu'il doit se tenir
        Location startLoc = player.getLocation().subtract(0, 1, 0).getBlock().getLocation();
        Material startMat = generator.getRandomMaterial(difficulty, session.getTheme());
        FoliaUtil.runAtLocation(plugin, startLoc, () -> startLoc.getBlock().setType(startMat));
        session.addBlock(startLoc);
        session.markScored(startLoc);
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
        session.markScored(landedOn);
        session.setLastLandedBlock(landedOn);
        session.incrementScore();

        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) data.updateBestScore(session.getDifficulty(), session.getScore());

        // Nouveau record : annoncé une seule fois, au moment où le score dépasse le PB initial
        boolean justBeatRecord = session.checkAndMarkRecordBeaten();

        placeNextBlock(session);

        Location toRemove = session.pollOldestIfNeeded();
        if (toRemove != null) {
            FoliaUtil.runAtLocation(plugin, toRemove, () -> toRemove.getBlock().setType(Material.AIR));
        }

        // Sons & particules
        plugin.getSoundManager().playLand(player);
        if (justBeatRecord) plugin.getSoundManager().playRecord(player);

        // Récompenses palier
        plugin.getRewardManager().checkMilestone(player, session.getScore());

        // Défi quotidien
        plugin.getDailyChallengeManager().recordIfBetter(
                player.getUniqueId(), player.getName(), session.getScore(), session.getDifficulty());

        if (plugin.getConfig().getBoolean("parkour.action-bar", true)) {
            String duelPart = buildDuelPart(player, session);
            String msg = plugin.getConfig().getString("messages.score-actionbar",
                            "&6Score: &e{score} &7| &6Record: &e{best}")
                    .replace("{score}", String.valueOf(session.getScore()))
                    .replace("{best}", String.valueOf(session.getPersonalBest()))
                    + duelPart;
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.sendActionBar(player, msg));
        }

        if (justBeatRecord) {
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
        stopSession(player, sendMessage, null);
    }

    /**
     * Stoppe la session. Si destination est non null, TP le joueur là-bas
     * au lieu de le renvoyer à sa position d'origine (utile après une chute).
     */
    public void stopSession(Player player, boolean sendMessage, Location destination) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);

        // Supprime les blocs — groupés par chunk pour minimiser les appels au region-scheduler
        Map<Long, List<Location>> byChunk = new HashMap<>();
        for (Location loc : session.getAllBlocks()) {
            long key = ((long) (loc.getBlockX() >> 4) << 32) | ((loc.getBlockZ() >> 4) & 0xFFFFFFFFL);
            byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(loc);
        }
        for (List<Location> chunkBlocks : byChunk.values()) {
            Location anchor = chunkBlocks.get(0);
            FoliaUtil.runAtLocation(plugin, anchor, () -> {
                for (Location bloc : chunkBlocks) bloc.getBlock().setType(Material.AIR);
            });
        }

        // Restaure le gamemode
        GameMode prev = previousGameModes.remove(player.getUniqueId());
        if (prev != null) {
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(prev));
        }

        // Téléporte vers la destination (spawn parkour si chute, origine sinon)
        Location origin = savedLocations.remove(player.getUniqueId());
        Location dest = destination != null ? destination : origin;
        if (dest != null) {
            FoliaUtil.teleport(plugin, player, dest, null);
        }

        // Sauvegarde le score
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            data.updateBestScore(session.getDifficulty(), session.getScore());
            data.addJumps(session.getScore());
            FoliaUtil.runAsync(plugin, () -> {
                plugin.getDatabaseManager().savePlayer(data);
                plugin.getLeaderboardManager().forceRefresh();
            });
        }

        if (sendMessage) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.stop", "&cArrêté. Score: &6{score}")
                    .replace("{score}", String.valueOf(session.getScore()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
        }
    }

    public void stopAllSessions() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
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

        // getMinBlockY() = O(1), pas de copie de collection
        int lowestBlockY = session.getMinBlockY();

        if (player.getLocation().getBlockY() < lowestBlockY - 5) {
            int score = session.getScore();
            boolean isNewRecord = session.hasBeatenRecord();
            boolean inDuel = plugin.getDuelManager().isInDuel(player);

            plugin.getSoundManager().playFall(player);
            if (isNewRecord) plugin.getSoundManager().playRecord(player);
            // Après une chute : renvoyer au spawn parkour (pas à l'île d'origine)
            stopSession(player, false, getParkourSpawnSilent());

            if (inDuel) {
                plugin.getDuelManager().onDuelEnd(player, score);
            } else {
                String fallMsg = plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString("messages.fall", "&cTombé ! Score: &6{score}")
                        .replace("{score}", String.valueOf(score));
                String subtitle = isNewRecord
                        ? MessageUtil.color(plugin.getConfig()
                                .getString("messages.new-record", "&6✦ NOUVEAU RECORD !")
                                .replace("{score}", String.valueOf(score)))
                        : "&7Score: &6" + score;
                FoliaUtil.runForEntity(plugin, player, () -> {
                    MessageUtil.send(player, fallMsg);
                    MessageUtil.sendTitle(player, "&c✗", subtitle, 5, 60, 15);
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
            String loaded = plugin.getServer().getWorlds().stream()
                    .map(World::getName).reduce((a, b) -> a + ", " + b).orElse("aucun");
            plugin.getLogger().log(Level.WARNING,
                    "Monde '" + worldName + "' introuvable. Mondes charges: " + loaded);
            return null;
        }

        double x     = plugin.getConfig().getDouble("parkour.spawn.x", 0);
        double y     = plugin.getConfig().getDouble("parkour.spawn.y", 100);
        double z     = plugin.getConfig().getDouble("parkour.spawn.z", 0);
        float  yaw   = (float) plugin.getConfig().getDouble("parkour.spawn.yaw", 0);
        float  pitch = (float) plugin.getConfig().getDouble("parkour.spawn.pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Retourne le spawn du monde parkour configuré sans envoyer de message, ou null. */
    private Location getParkourSpawnSilent() {
        String worldName = plugin.getConfig().getString("parkour.world", "");
        if (worldName == null || worldName.isBlank()) return null;
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;
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

    /** Sauvegarde les données sans stopper la session (pour déconnexion en duel). */
    public void saveDataOnly(Player player) {
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }
    }

    /** Restaure la session après reconnexion en duel (sans appel DB sur le thread principal). */
    public void restoreSessionForDuel(Player player, Difficulty difficulty, int savedScore) {
        PlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null) {
            doRestore(player, difficulty, cached, savedScore);
        } else {
            FoliaUtil.runAsync(plugin, () -> {
                PlayerData loaded = plugin.getDatabaseManager().loadPlayer(
                        player.getUniqueId(), player.getName());
                playerDataCache.put(player.getUniqueId(), loaded);
                doRestore(player, difficulty, loaded, savedScore);
            });
        }
    }

    private void doRestore(Player player, Difficulty difficulty, PlayerData data, int savedScore) {
        if (plugin.getConfig().getBoolean("parkour.force-adventure", true)) {
            previousGameModes.put(player.getUniqueId(), player.getGameMode());
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(GameMode.ADVENTURE));
        }
        Location parkourSpawn = getParkourSpawn(player);
        if (parkourSpawn != null) {
            savedLocations.put(player.getUniqueId(), player.getLocation().clone());
            FoliaUtil.teleport(plugin, player, parkourSpawn,
                    () -> initRestoredSession(player, difficulty, data, savedScore));
        } else {
            initRestoredSession(player, difficulty, data, savedScore);
        }
    }

    private void initRestoredSession(Player player, Difficulty difficulty, PlayerData data, int savedScore) {
        int personalBest = data.getBestScore(difficulty);
        int historySize  = plugin.getConfig().getInt("parkour.history-size", 12);

        ParkourSession session = new ParkourSession(player.getUniqueId(), difficulty, personalBest, historySize);
        session.setScore(savedScore);
        float yaw = player.getLocation().getYaw();
        session.setCurrentAngle(Math.toRadians(yaw));
        session.setTheme(data.getTheme());

        Location startLoc = player.getLocation().subtract(0, 1, 0).getBlock().getLocation();
        Material startMat = generator.getRandomMaterial(difficulty, session.getTheme());
        FoliaUtil.runAtLocation(plugin, startLoc, () -> startLoc.getBlock().setType(startMat));
        session.addBlock(startLoc);
        session.markScored(startLoc);
        session.setLastLandedBlock(startLoc);

        sessions.put(player.getUniqueId(), session);

        int blocksAhead = plugin.getConfig().getInt("parkour.blocks-ahead", 3);
        for (int i = 0; i < blocksAhead; i++) placeNextBlock(session);

        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = prefix + plugin.getConfig().getString("messages.duel-reconnect",
                "&aReconnecté ! Score restauré: &6{score}").replace("{score}", String.valueOf(savedScore));
        FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
    }

    public Collection<ParkourSession> getAllSessions() { return sessions.values(); }
    public PlayerData getPlayerData(UUID uuid) { return playerDataCache.get(uuid); }
    public ParkourSession getSessionByUuid(UUID uuid) { return sessions.get(uuid); }
}
