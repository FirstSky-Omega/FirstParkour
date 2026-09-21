package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.DailyEntry;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.util.FoliaUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DailyChallengeManager {

    private final FirstParkour plugin;
    private volatile List<DailyEntry> dailyTop = new ArrayList<>();
    private volatile LocalDate currentDate = LocalDate.now();
    private volatile Difficulty todayDifficulty;
    private ScheduledTask resetTask;

    public DailyChallengeManager(FirstParkour plugin) {
        this.plugin = plugin;
        this.todayDifficulty = computeTodayDifficulty();
    }

    public void start() {
        FoliaUtil.runAsyncRepeating(plugin, this::refresh, 0, 60_000);
        scheduleNextReset();
    }

    private void refresh() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
            todayDifficulty = computeTodayDifficulty();
        }
        dailyTop = plugin.getDatabaseManager().getDailyLeaderboard(today, 10);
    }

    private void scheduleNextReset() {
        if (resetTask != null) resetTask.cancel();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        long delayMs = Duration.between(LocalDateTime.now(), midnight).toMillis();
        resetTask = FoliaUtil.runAsyncDelayedCancellable(plugin, () -> {
            currentDate = LocalDate.now();
            todayDifficulty = computeTodayDifficulty();
            refresh();
            scheduleNextReset();
        }, Math.max(delayMs, 1000));
    }

    public void stop() {
        if (resetTask != null) resetTask.cancel();
    }

    private Difficulty computeTodayDifficulty() {
        String cfg = plugin.getConfig().getString("daily.difficulty", "RANDOM");
        if ("RANDOM".equalsIgnoreCase(cfg)) {
            Difficulty[] values = Difficulty.values();
            return values[LocalDate.now().getDayOfYear() % values.length];
        }
        Difficulty d = Difficulty.fromKey(cfg);
        return d != null ? d : Difficulty.EASY;
    }

    /** Enregistre le score du jour si meilleur. Le refresh périodique (60 s) se charge de la mise à jour du cache. */
    public void recordIfBetter(UUID uuid, String name, int score, Difficulty difficulty) {
        if (!difficulty.equals(todayDifficulty)) return;
        FoliaUtil.runAsync(plugin, () ->
                plugin.getDatabaseManager().saveDailyScore(uuid, name, score, currentDate, difficulty));
    }

    public Difficulty getTodayDifficulty()   { return todayDifficulty; }
    public LocalDate  getCurrentDate()       { return currentDate; }
    public List<DailyEntry> getDailyTop()   { return Collections.unmodifiableList(dailyTop); }

    public int getDailyRank(UUID uuid) {
        List<DailyEntry> top = dailyTop;
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).uuid().equals(uuid)) return i + 1;
        }
        return -1;
    }

    public int getDailyScore(UUID uuid) {
        List<DailyEntry> top = dailyTop;
        for (DailyEntry e : top) {
            if (e.uuid().equals(uuid)) return e.score();
        }
        return 0;
    }
}
