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

    public void loadPlayerAsync(Player player) {
        FoliaUtil.runAsync(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayer(player.getUniqueId(), player.getName());
            playerDataCache.put(player.getUniqueId(), data);
        });
    }

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
        session.setTheme(data.getTheme());

        float yaw = player.getLocation().getYaw();
        session.setCurrentAngle(Math.toRadians(yaw));

        Location startLoc = player.getLocation().getBlock().getLocation();
        session.addBlock(startLoc);
        session.setLastLandedBlock(startLoc);

        sessions.put(player.getUniqueId(), session);

        if (plugin.getConfig().getBoolean("parkour.force-adventure", true)) {
            previousGameModes.put(player.getUniqueId(), player.getGameMode());
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(GameMode.ADVENTURE));
        }

        int blocksAhead = plugin.getConfig().getInt("parkour.blocks-ahead", 3);
        for (int i = 0; i < blocksAhead; i++) placeNextBlock(session);

        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());
        String msg = plugin.getConfig().getString("messages.prefix", "")
                + plugin.getConfig().getString("messages.start", "&aDémarré !")
                        .replace("{difficulty}", MessageUtil.color(diffName));
        FoliaUtil.runForEntity(plugin, player, () -> MessageUtil.send(player, msg));
    }

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
            boolean inDuel = plugin.getDuelManager().isInDuel(player);
            String duelPart = "";
            if (inDuel) {
                UUID opponentUuid = plugin.getDuelManager().getDuel(player).getOpponent(player.getUniqueId());
                Player opponent = plugin.getServer().getPlayer(opponentUuid);
                if (opponent != null) {
                    ParkourSession opSession = getSession(opponent);
                    int opScore = opSession != null ? opSession.getScore() : 0;
                    duelPart = " &8| &c⚔ " + opponent.getName() + ": &e" + opScore;
                }
            }
            String msg = (plugin.getConfig().getString("messages.score-actionbar",
                            "&6Score: &e{score} &7| &6Record: &e{best}")
                    .replace("{score}", String.valueOf(session.getScore()))
                    .replace("{best}", String.valueOf(session.getPersonalBest())))
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

    public void stopSession(Player player, boolean sendMessage) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.setActive(false);

        for (Location loc : session.getAllBlocks()) {
            FoliaUtil.runAtLocation(plugin, loc, () -> loc.getBlock().setType(Material.AIR));
        }

        GameMode prev = previousGameModes.remove(player.getUniqueId());
        if (prev != null) {
            FoliaUtil.runForEntity(plugin, player, () -> player.setGameMode(prev));
        }

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

    /**
     * Vérifie si le joueur est tombé.
     * Si en duel, notifie le DuelManager (qui arrête le gagnant).
     * Si solo, arrête la session directement.
     */
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

    /** Met à jour le thème du joueur en live */
    public void setTheme(Player player, BlockTheme theme) {
        PlayerData data = playerDataCache.get(player.getUniqueId());
        if (data != null) data.setTheme(theme);

        ParkourSession session = getSession(player);
        if (session != null) session.setTheme(theme);

        if (data != null) {
            FoliaUtil.runAsync(plugin, () -> plugin.getDatabaseManager().savePlayer(data));
        }
    }

    private void placeNextBlock(ParkourSession session) {
        Location next = generator.generateNext(session);
        if (next == null) return;
        Material mat = generator.getRandomMaterial(session.getDifficulty(), session.getTheme());
        session.addBlock(next);
        FoliaUtil.runAtLocation(plugin, next, () -> next.getBlock().setType(mat));
    }

    public Collection<ParkourSession> getAllSessions() { return sessions.values(); }
    public PlayerData getPlayerData(UUID uuid) { return playerDataCache.get(uuid); }
}
