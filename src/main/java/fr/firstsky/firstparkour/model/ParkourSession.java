package fr.firstsky.firstparkour.model;

import org.bukkit.Location;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class ParkourSession {

    private final UUID playerUuid;
    private final Difficulty difficulty;
    private int score;
    private int personalBest;
    private final Deque<Location> activeBlocks = new ArrayDeque<>();
    private Location lastLandedBlock;
    private double currentAngle;
    private final long startTime;
    private boolean active;
    private final int historySize;

    public ParkourSession(UUID playerUuid, Difficulty difficulty, int personalBest, int historySize) {
        this.playerUuid = playerUuid;
        this.difficulty = difficulty;
        this.score = 0;
        this.personalBest = personalBest;
        this.historySize = historySize;
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.currentAngle = 0.0;
    }

    public void addBlock(Location loc) {
        activeBlocks.addLast(loc.clone());
    }

    /** Retourne le bloc à supprimer si l'historique est trop long, sinon null */
    public Location pollOldestIfNeeded() {
        if (activeBlocks.size() > historySize) {
            return activeBlocks.pollFirst();
        }
        return null;
    }

    public boolean isParkourBlock(Location loc) {
        for (Location b : activeBlocks) {
            if (b.getBlockX() == loc.getBlockX()
                    && b.getBlockY() == loc.getBlockY()
                    && b.getBlockZ() == loc.getBlockZ()
                    && b.getWorld() != null
                    && b.getWorld().equals(loc.getWorld())) {
                return true;
            }
        }
        return false;
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

    public UUID getPlayerUuid() { return playerUuid; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getScore() { return score; }
    public int getPersonalBest() { return personalBest; }
    public double getCurrentAngle() { return currentAngle; }
    public void setCurrentAngle(double angle) { this.currentAngle = angle; }
    public long getStartTime() { return startTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
