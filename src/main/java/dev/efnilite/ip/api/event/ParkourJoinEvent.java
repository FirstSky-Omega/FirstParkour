package dev.efnilite.ip.api.event;

import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.foundation.event.EventWrapper;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a player joins a session. Read-only.
 */
public class ParkourJoinEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourUser player;

    public ParkourJoinEvent(ParkourUser player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
