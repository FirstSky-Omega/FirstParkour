package fr.firstsky.firstparkour.model;

import java.util.UUID;

public class ActiveDuel {

    private final UUID player1;
    private final UUID player2;
    private final Difficulty difficulty;
    private boolean ended;

    public ActiveDuel(UUID player1, UUID player2, Difficulty difficulty) {
        this.player1 = player1;
        this.player2 = player2;
        this.difficulty = difficulty;
        this.ended = false;
    }

    public UUID getOpponent(UUID player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }

    public boolean isEnded() { return ended; }
    public void setEnded(boolean ended) { this.ended = ended; }

    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public Difficulty getDifficulty() { return difficulty; }
}
