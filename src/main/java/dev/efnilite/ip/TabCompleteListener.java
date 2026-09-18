package dev.efnilite.ip;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import dev.efnilite.iep.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Set;

public class TabCompleteListener implements Listener {

    private static final Set<String> PARKOUR_ALIASES = Set.of("parkour", "witp", "ip");
    private static final Set<String> ELYTRA_ALIASES = Set.of("eparkour", "iep", "infiniteelytraparkour");

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        String[] parts = buffer.substring(1).split(" ", -1);
        if (parts.length == 0) return;

        String cmdName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        List<String> completions;

        if (PARKOUR_ALIASES.contains(cmdName)) {
            dev.efnilite.ip.Command cmd = IP.getParkourCommand();
            if (cmd == null) return;
            completions = cmd.tabComplete(event.getSender(), args);
        } else if (ELYTRA_ALIASES.contains(cmdName) && IP.elytraEnabled) {
            completions = Command.INSTANCE.tabComplete(event.getSender(), args);
        } else {
            return;
        }

        if (completions != null && !completions.isEmpty()) {
            event.setCompletions(completions);
            event.setHandled(true);
        }
    }
}
