package dev.efnilite.ip.foundation.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for handling cooldowns. Cooldowns are stored by key (and optionally by player).
 * The keys are a way of identifying which action to use in the cooldown.
 * When a player performs action A, action B and then action B again, the system needs to be able to differentiate action A and B,
 * since a player can be on cooldown for multiple things.
 * <p>Using the {@link #canPerform(String, long)} or {@link #canPerform(Player, String, long)} methods allows you to check
 * whether a player can perform the action specified in the arguments. These methods return true when the player
 * is allowed to perform the action, and false when the player is not. When the player is allowed to perform the action,
 * the last execution time of the player is automatically updated to now. The {@code cooldown} argument is used to specify
 * the cooldown for the provided key. Code example:</p>
 * <blockquote><code>if (!canPerform(player, "right click wand", 1000)) { return; }</code></blockquote>
 * <p>In the above example, the {@code return} statement will be executed when the player has performed the action {@code "right click wand"}
 * in the last 1000 ms (1 second). If the action was last performed more than 1000 ms ago, the {@code return} statement will not be called.</p>
 */
public class Cooldowns {

    private static final Map<String, Long> EXECUTION_TIMES = new HashMap<>();

    /**
     * Returns whether the provided action {@code key} can be performed with the provided cooldown {@code cooldown}.
     * <strong>This method is only recommended to be used with global actions</strong>, e.g. requiring no player.
     * Returns:
     * <ul>
     *     <li>{@code true} - When the action with the provided key can be performed.
     *     The last execution was more than {@code cooldown} milliseconds ago.</li>
     *     <li>{@code false} - When the action with the provided key cannot be performed.
     *     The last execution was less than {@code cooldown} milliseconds ago.</li>
     * </ul>
     *
     * @param key      The key by which the system can differentiate actions.
     * @param cooldown The cooldown in milliseconds.
     * @return true if the action {@code key} was more than {@code cooldown} ms ago, false if not.
     */
    public static boolean canPerform(@NotNull String key, long cooldown) {
        Objects.requireNonNull(key);

        Long lastTime = EXECUTION_TIMES.get(key);

        // if no value has been previously registered, player is executing for the first time, so return true
        if (lastTime == null) {
            EXECUTION_TIMES.put(key, System.currentTimeMillis());
            return true;
        }

        // get the difference between now and the last time the command was executed
        long dt = System.currentTimeMillis() - lastTime;

        // if the last time execution difference is higher than cooldown, allow execution
        if (dt > cooldown) {
            EXECUTION_TIMES.put(key, System.currentTimeMillis());
            return true;
        }

        return false;
    }

    /**
     * Returns whether the provided action {@code key}, belonging to player {@code player},  can be performed
     * with the provided cooldown {@code cooldown}. Returns:
     * <ul>
     *     <li>{@code true} - When the action with the provided player's key can be performed.
     *     The last execution was more than {@code cooldown} milliseconds ago.</li>
     *     <li>{@code false} - When the action with the provided player's key cannot be performed.
     *     The last execution was less than {@code cooldown} milliseconds ago.</li>
     * </ul>
     *
     * @param key      The key by which the system can differentiate actions.
     * @param cooldown The cooldown in milliseconds.
     * @return true if the action {@code key} was more than {@code cooldown} ms ago, false if not.
     */
    public static boolean canPerform(@NotNull Player player, @NotNull String key, long cooldown) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(key);

        return canPerform(player.getUniqueId() + "-" + key, cooldown);
    }

    /**
     * Returns the last registered execution time (in ms) of the specified string key. Can be null.
     * <strong>This method is only recommended to be used with global actions</strong>, e.g. requiring no player.
     *
     * @param key The key by which the system can differentiate actions.
     * @return the last execution time (in ms) of the global action. Can be null.
     */
    @Nullable
    public static Long getLastExecutionTime(@NotNull String key) {
        Objects.requireNonNull(key);

        return EXECUTION_TIMES.get(key);
    }

    /**
     * Returns the last registered execution time (in ms) of the specified string key that's associated to the provided player.
     * Can be null.
     *
     * @param player The player.
     * @param key    The key by which the system can differentiate actions.
     * @return the last execution time (in ms) of the player. Can be null.
     */
    @Nullable
    public static Long getLastExecutionTime(@NotNull Player player, @NotNull String key) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(key);

        return EXECUTION_TIMES.get(player.getUniqueId() + "-" + key);
    }

}