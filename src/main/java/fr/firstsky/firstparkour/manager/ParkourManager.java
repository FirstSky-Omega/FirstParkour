package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.model.ParkourSession;
import fr.firstsky.firstparkour.util.BlockGenerator;
import fr.firstsky.firstparkour.util.FoliaUtil;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourManager {

    private final FirstParkour plugin;
    private final BlockGenerator generator;
    private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode> previousGameModes = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    public ParkourManager(FirstParkour plugin) {
        this.plugin = plugin;
        this.generator = new BlockGenerator(plugin);
    }

    /** Charge les données du joueur en async (à appeler à la connexion) */
    public void loadPlayerAsync(Player player) {
        FoliaUtil.runAsync(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayer(player.getUniqueId(), player.getName());
            playerDataCache.put(player.getUniqueId(), data);
        });
    }

    /** Sauvegarde et retire les données du joueur (à la déconnexion) */
    public void unloadPlayer(Player player) {
        stopSession(player, false);
        PlayerData data = playerDataCache.remove(player.getUniqueId());
        if (data != null) {
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }
    }

    public boolean isPlaying(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /** Démarre une session parkour pour un joueur */
    public void startSession(Player player, Difficulty difficulty) {
        if (isPlaying(player)) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.already-playing", "Déjà en jeu !"));
            return;
        }

        PlayerData data = playerDataCache.computeIfAbsent(player.getUniqueId(),
                id -> plugin.getDatabaseManager().loadPlayer(id, player.getName()));

        int personalBest = data.getBestScore(difficulty);
        int historySize = plugin.getConfig().getInt("parkour.history-size", 12);

        ParkourSession session = new ParkourSession(player.getUniqueId(), difficulty, personalBest, historySize);

        // Initialise l'angle sur la direction du joueur (yaw → radians)
        float yaw = player.getLocation().getYaw();
        session.setCurrentAngle(Math.toRadians(yaw));

        // Bloc de départ = bloc sous les pieds du joueur
        Location startLoc = player.getLocation().getBlock().getLocation();
        session.addBlock(startLoc);
        session.setLastLandedBlock(startLoc);

        sessions.put(player.getUniqueId(), session);

        // Sauvegarde le gamemode et force ADVENTURE si configuré
        if (plugin.getConfig().getBoolean("parkour.force-adventure", true)) {
            previousGameModes.put(player.getUniqueId(), player.getGameMode());
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(GameMode.ADVENTURE));
        }

        // Pré-génère les blocs initiaux
        int blocksAhead = plugin.getConfig().getInt("parkour.blocks-ahead", 3);
        for (int i = 0; i < blocksAhead; i++) {
            placeNextBlock(session);
        }

        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());
        String msg = plugin.getConfig().getString("messages.prefix", "")
                + plugin.getConfig().getString("messages.start", "&aDémarré !")
                        .replace("{difficulty}", MessageUtil.color(diffName));
        FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
    }

    /** Appelé quand le joueur atterrit sur un bloc de parkour */
    public void onPlayerLand(Player player, ParkourSession session, Location landedOn) {
        session.setLastLandedBlock(landedOn);
        session.incrementScore();

        // Met à jour les données en cache
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            data.updateBestScore(session.getDifficulty(), session.getScore());
        }

        // Nouveau record ?
        boolean newRecord = session.getScore() > session.getPersonalBest() - 1
                && session.getScore() == session.getPersonalBest();

        // Génère le prochain bloc
        placeNextBlock(session);

        // Supprime l'ancien bloc si nécessaire
        Location toRemove = session.pollOldestIfNeeded();
        if (toRemove != null) {
            FoliaUtil.runAtLocation(plugin, toRemove, () -> toRemove.getBlock().setType(Material.AIR));
        }

        // Met à jour l'action bar
        if (plugin.getConfig().getBoolean("parkour.action-bar", true)) {
            String msg = plugin.getConfig().getString("messages.score-actionbar",
                            "&6Score: &e{score} &7| &6Record: &e{best}")
                    .replace("{score}", String.valueOf(session.getScore()))
                    .replace("{best}", String.valueOf(session.getPersonalBest()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.sendActionBar(player, msg));
        }

        if (newRecord && session.getScore() > 1) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.new-record", "&6RECORD!")
                            .replace("{score}", String.valueOf(session.getScore()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
        }
    }

    /** Arrête la session parkour du joueur */
    public void stopSession(Player player, boolean sendMessage) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        session.setActive(false);

        // Supprime tous les blocs actifs
        for (Location loc : session.getAllBlocks()) {
            FoliaUtil.runAtLocation(plugin, loc, () -> loc.getBlock().setType(Material.AIR));
        }

        // Restaure le gamemode
        GameMode prev = previousGameModes.remove(player.getUniqueId());
        if (prev != null) {
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(prev));
        }

        // Sauvegarde le score
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) {
            data.updateBestScore(session.getDifficulty(), session.getScore());
            data.addJumps(session.getScore());
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }

        // Rafraîchit le classement
        plugin.getLeaderboardManager().forceRefresh();

        if (sendMessage) {
            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.stop", "&cArrêté. Score: &6{score}")
                            .replace("{score}", String.valueOf(session.getScore()));
            FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
        }
    }

    /** Arrête toutes les sessions (appelé à la désactivation du plugin) */
    public void stopAllSessions() {
        for (UUID uuid : sessions.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) stopSession(p, false);
        }
        sessions.clear();
    }

    /** Vérifie si un joueur est tombé (Y trop bas par rapport au dernier bloc) */
    public void checkFall(Player player) {
        ParkourSession session = getSession(player);
        if (session == null) return;

        Location lastBlock = session.getLastBlock();
        if (lastBlock == null) return;

        int playerY = player.getLocation().getBlockY();
        int lowestBlockY = session.getAllBlocks().stream()
                .mapToInt(Location::getBlockY)
                .min()
                .orElse(lastBlock.getBlockY());

        if (playerY < lowestBlockY - 5) {
            // Le joueur est tombé
            int score = session.getScore();
            stopSession(player, false);

            String msg = plugin.getConfig().getString("messages.prefix", "")
                    + plugin.getConfig().getString("messages.fall", "&cTombé ! Score: &6{score}")
                            .replace("{score}", String.valueOf(score));
            FoliaUtil.runForEntity(plugin, player, () -> {
                MessageUtil.send(player, msg);
                MessageUtil.sendTitle(player, "&c✗", "&7Score: &6" + score, 5, 40, 10);
            });
        }
    }

    private void placeNextBlock(ParkourSession session) {
        Location next = generator.generateNext(session);
        if (next == null) return;

        Material mat = generator.getRandomMaterial(session.getDifficulty());
        session.addBlock(next);

        FoliaUtil.runAtLocation(plugin, next, () -> next.getBlock().setType(mat));
    }

    public Collection<ParkourSession> getAllSessions() {
        return sessions.values();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }
}
