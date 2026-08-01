package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.foundation.event.EventWrapper;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a player falls. Read-only.
 */
public class ParkourFallEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourPlayer player;

    public ParkourFallEvent(ParkourPlayer player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
