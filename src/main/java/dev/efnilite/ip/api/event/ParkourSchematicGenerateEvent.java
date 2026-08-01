package dev.efnilite.ip.api.event;

import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.foundation.event.EventWrapper;
import dev.efnilite.ip.foundation.schematic.Schematic;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourSchematicGenerateEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final Schematic schematic;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourSchematicGenerateEvent(Schematic schematic, ParkourGenerator generator, ParkourPlayer player) {
        this.schematic = schematic;
        this.generator = generator;
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
