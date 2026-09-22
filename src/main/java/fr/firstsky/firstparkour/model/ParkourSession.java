package fr.firstsky.firstparkour.model;

import org.bukkit.Location;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ParkourSession {

    private final UUID playerUuid;
    private final Difficulty difficulty;
    private int score;
    private int personalBest;
    /** PB au démarrage de la session — ne change pas. */
    private final int initialPersonalBest;
    /** Vrai dès que le score dépasse initialPersonalBest (annoncé une seule fois). */
    private boolean hasBeatenRecord = false;
    private final Deque<Location> activeBlocks = new ArrayDeque<>();
    /** Index O(1) pour isParkourBlock — clé = "x,y,z" */
    private final Set<String> blockKeys = new HashSet<>();
    /** Index de génération de chaque bloc — pour bloquer les sauts en arrière */
    private final Map<String, Integer> blockIndices = new HashMap<>();
    /** Prochain index à attribuer lors de addBlock() */
    private int nextBlockIndex = 0;
    /** Index du dernier bloc qui a accordé un score (−1 = aucun) */
    private int lastScoredIndex = -1;
    /** Y minimum des blocs actifs — mis à jour dans addBlock / pollOldestIfNeeded */
    private int minBlockY = Integer.MAX_VALUE;

    private Location lastLandedBlock;
    private double currentAngle;
    private final long startTime;
    private boolean active;
    private final int historySize;
    private BlockTheme theme = BlockTheme.DEFAULT;

    public ParkourSession(UUID playerUuid, Difficulty difficulty, int personalBest, int historySize) {
        this.playerUuid = playerUuid;
        this.difficulty = difficulty;
        this.score = 0;
        this.personalBest = personalBest;
        this.initialPersonalBest = personalBest;
        this.historySize = historySize;
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.currentAngle = 0.0;
    }

    public void addBlock(Location loc) {
        Location c = loc.clone();
        activeBlocks.addLast(c);
        String key = blockKey(c);
        blockKeys.add(key);
        blockIndices.put(key, nextBlockIndex++);
        if (c.getBlockY() < minBlockY) minBlockY = c.getBlockY();
    }

    /** Retourne le bloc à supprimer si l'historique est trop long, sinon null */
    public Location pollOldestIfNeeded() {
        if (activeBlocks.size() > historySize) {
            Location removed = activeBlocks.pollFirst();
            String key = blockKey(removed);
            blockKeys.remove(key);
            blockIndices.remove(key);
            if (removed.getBlockY() == minBlockY) {
                minBlockY = activeBlocks.stream()
                        .mapToInt(Location::getBlockY)
                        .min().orElse(Integer.MAX_VALUE);
            }
            return removed;
        }
        return null;
    }

    /** O(1) grâce au HashSet. */
    public boolean isParkourBlock(Location loc) {
        return blockKeys.contains(blockKey(loc));
    }

    /**
     * Vrai si ce bloc est en avant du dernier bloc scoré (jamais accordé de score).
     * Empêche de scorer en sautant en arrière.
     */
    public boolean isNewBlock(Location loc) {
        Integer idx = blockIndices.get(blockKey(loc));
        return idx != null && idx > lastScoredIndex;
    }

    /** Marque ce bloc comme scoré. Appeler juste avant d'incrémenter le score. */
    public void markScored(Location loc) {
        Integer idx = blockIndices.get(blockKey(loc));
        if (idx != null) lastScoredIndex = idx;
    }

    public boolean isLastLandedBlock(Location loc) {
        if (lastLandedBlock == null) return false;
        return lastLandedBlock.getBlockX() == loc.getBlockX()
                && lastLandedBlock.getBlockY() == loc.getBlockY()
                && lastLandedBlock.getBlockZ() == loc.getBlockZ();
    }

    public void setLastLandedBlock(Location loc) {
        this.lastLandedBlock = loc.clone();
    }

    public Location getLastBlock() {
        return activeBlocks.isEmpty() ? null : activeBlocks.peekLast();
    }

    public List<Location> getAllBlocks() {
        return new ArrayList<>(activeBlocks);
    }

    public void incrementScore() {
        score++;
        if (score > personalBest) personalBest = score;
    }

    public void setScore(int score) {
        this.score = score;
        if (score > personalBest) personalBest = score;
    }

    private static String blockKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    /**
     * Retourne vrai la première fois que le score dépasse le PB initial.
     * Appels suivants retournent faux (annonce une seule fois par session).
     */
    public boolean checkAndMarkRecordBeaten() {
        if (!hasBeatenRecord && score > initialPersonalBest) {
            hasBeatenRecord = true;
            return true;
        }
        return false;
    }

    public boolean hasBeatenRecord() { return hasBeatenRecord; }
    public int getInitialPersonalBest() { return initialPersonalBest; }

    public UUID getPlayerUuid() { return playerUuid; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getScore() { return score; }
    public int getPersonalBest() { return personalBest; }
    public double getCurrentAngle() { return currentAngle; }
    public void setCurrentAngle(double angle) { this.currentAngle = angle; }
    public long getStartTime() { return startTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public BlockTheme getTheme() { return theme; }
    public void setTheme(BlockTheme theme) { this.theme = theme != null ? theme : BlockTheme.DEFAULT; }
    /** Y minimum des blocs actifs (sans copie de collection). */
    public int getMinBlockY() { return minBlockY; }
}
