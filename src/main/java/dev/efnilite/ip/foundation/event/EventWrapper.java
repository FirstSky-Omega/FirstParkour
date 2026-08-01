package dev.efnilite.ip.foundation.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * A class for removing unnecessary methods that are the same across all Event classes,
 * making the actual Event classes cleaner.
 *
 * @author Efnilite
 */
public abstract class EventWrapper extends Event {

    protected boolean cancelled;

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return cancelled;
    }

}
