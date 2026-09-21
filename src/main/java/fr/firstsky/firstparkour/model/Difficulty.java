package fr.firstsky.firstparkour.model;

public enum Difficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String key;

    Difficulty(String key) { this.key = key; }

    public String getKey() { return key; }

    /** Accepte les noms anglais ET français (facile, normal, difficile). */
    public static Difficulty fromKey(String key) {
        if (key == null) return null;
        return switch (key.toLowerCase()) {
            case "easy",   "facile"    -> EASY;
            case "medium", "normal"    -> MEDIUM;
            case "hard",   "difficile" -> HARD;
            default -> null;
        };
    }

    public String getScoreColumn() {
        return switch (this) {
            case EASY   -> "best_score_easy";
            case MEDIUM -> "best_score_medium";
            case HARD   -> "best_score_hard";
        };
    }
}
