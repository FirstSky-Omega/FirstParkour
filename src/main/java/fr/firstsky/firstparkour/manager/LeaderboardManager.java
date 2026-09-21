package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.util.FoliaUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LeaderboardManager {

    private final FirstParkour plugin;
    private final Map<Difficulty, List<PlayerData>> cache = new EnumMap<>(Difficulty.class);

    public LeaderboardManager(FirstParkour plugin) {
        this.plugin = plugin;
        for (Difficulty d : Difficulty.values()) cache.put(d, new ArrayList<>());
    }

    public void startRefreshTask() {
        long period = plugin.getConfig().getLong("parkour.leaderboard-refresh", 60) * 1000L;
        FoliaUtil.runAsyncRepeating(plugin, this::refreshAll, 0, period);
    }

    private void refreshAll() {
        for (Difficulty d : Difficulty.values()) {
            List<PlayerData> top = plugin.getDatabaseManager().getLeaderboard(d, 10);
            cache.put(d, top);
        }
    }

    public List<PlayerData> getTop(Difficulty difficulty) {
        return Collections.unmodifiableList(cache.getOrDefault(difficulty, List.of()));
    }

    /** Rang 1-based, retourne null si hors classement */
    public PlayerData getByRank(Difficulty difficulty, int rank) {
        List<PlayerData> top = cache.getOrDefault(difficulty, List.of());
        if (rank < 1 || rank > top.size()) return null;
        return top.get(rank - 1);
    }

    /**
     * Rang du joueur dans le classement, 1-based.
     * Basé sur le cache local (top 10 par difficulté).
     * Retourne -1 si le joueur n'est pas dans le top mis en cache.
     */
    public int getRank(java.util.UUID uuid, Difficulty difficulty) {
        List<PlayerData> top = cache.getOrDefault(difficulty, List.of());
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getUuid().equals(uuid)) return i + 1;
        }
        return -1;
    }

    /** Force un refresh immédiat en async */
    public void forceRefresh() {
        FoliaUtil.runAsync(plugin, this::refreshAll);
    }
}
