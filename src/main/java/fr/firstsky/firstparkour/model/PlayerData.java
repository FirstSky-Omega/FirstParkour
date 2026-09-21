package fr.firstsky.firstparkour.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private int bestScoreEasy;
    private int bestScoreMedium;
    private int bestScoreHard;
    private long totalJumps;

    public PlayerData(UUID uuid, String name, int bestEasy, int bestMedium, int bestHard, long totalJumps) {
        this.uuid = uuid;
        this.name = name;
        this.bestScoreEasy = bestEasy;
        this.bestScoreMedium = bestMedium;
        this.bestScoreHard = bestHard;
        this.totalJumps = totalJumps;
    }

    public int getBestScore(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> bestScoreEasy;
            case MEDIUM -> bestScoreMedium;
            case HARD -> bestScoreHard;
        };
    }

    public void updateBestScore(Difficulty difficulty, int score) {
        switch (difficulty) {
            case EASY -> { if (score > bestScoreEasy) bestScoreEasy = score; }
            case MEDIUM -> { if (score > bestScoreMedium) bestScoreMedium = score; }
            case HARD -> { if (score > bestScoreHard) bestScoreHard = score; }
        }
    }

    public void addJumps(int n) {
        totalJumps += n;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBestScoreEasy() { return bestScoreEasy; }
    public int getBestScoreMedium() { return bestScoreMedium; }
    public int getBestScoreHard() { return bestScoreHard; }
    public long getTotalJumps() { return totalJumps; }
}
