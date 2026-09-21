package fr.firstsky.firstparkour.model;

public enum BlockTheme {
    DEFAULT("default"),
    NETHER("nether"),
    END("end"),
    OCEAN("ocean"),
    WINTER("winter"),
    DESERT("desert"),
    SKY("sky"),
    JUNGLE("jungle");

    private final String key;

    BlockTheme(String key) { this.key = key; }

    public String getKey() { return key; }

    public static BlockTheme fromKey(String key) {
        if (key == null) return DEFAULT;
        for (BlockTheme t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return DEFAULT;
    }
}
