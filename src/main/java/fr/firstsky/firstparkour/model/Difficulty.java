package fr.firstsky.firstparkour.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public enum Difficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String key;

    Difficulty(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Difficulty fromKey(String key) {
        for (Difficulty d : values()) {
            if (d.key.equalsIgnoreCase(key)) return d;
        }
        return null;
    }

    public String getScoreColumn() {
        return switch (this) {
            case EASY -> "best_score_easy";
            case MEDIUM -> "best_score_medium";
            case HARD -> "best_score_hard";
        };
    }
}
