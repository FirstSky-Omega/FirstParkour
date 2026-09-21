package fr.firstsky.firstparkour.model;

import java.util.UUID;

public class DuelInvite {

    private final UUID sender;
    private final UUID receiver;
    private final Difficulty difficulty;
    private final long createdAt;
    private static final long TIMEOUT_MS = 30_000; // 30 secondes

    public DuelInvite(UUID sender, UUID receiver, Difficulty difficulty) {
        this.sender = sender;
        this.receiver = receiver;
        this.difficulty = difficulty;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TIMEOUT_MS;
    }

    public UUID getSender() { return sender; }
    public UUID getReceiver() { return receiver; }
    public Difficulty getDifficulty() { return difficulty; }
}
