package dev.efnilite.ip.hologram;

import dev.efnilite.ip.IP;
import org.bukkit.NamespacedKey;

/**
 * Centralized {@link NamespacedKey}s used to tag our Display / Interaction entities so we
 * can identify them on chunk-load events (cleanup of leftovers) without keeping a
 * permanent in-memory entity registry.
 *
 * <p>Why PDC tags instead of just a HashMap&lt;UUID,Entity&gt;? On a crashed-server restart
 * we have no in-memory registry, but the chunk still has zombie entities. PDC survives
 * across server restarts and lets us recognize "ours" vs "someone else's hologram" even
 * when the World handed us back an Entity for the first time.</p>
 */
public final class HologramKeys {

    private HologramKeys() {}

    /** Marks an entity as managed by IP's hologram subsystem. Value = config id (string). */
    public static final NamespacedKey HOLOGRAM_ID = new NamespacedKey(IP.getPlugin(), "hologram_id");

    /** Sub-role of the entity inside a hologram. Value = "text" | "icon" | "interaction". */
    public static final NamespacedKey HOLOGRAM_ROLE = new NamespacedKey(IP.getPlugin(), "hologram_role");

    /** Numeric index when a hologram has multiple icons. Value = int (stringified). */
    public static final NamespacedKey HOLOGRAM_INDEX = new NamespacedKey(IP.getPlugin(), "hologram_index");

    public static final String ROLE_TEXT = "text";
    public static final String ROLE_ICON = "icon";
    public static final String ROLE_INTERACTION = "interaction";
}
