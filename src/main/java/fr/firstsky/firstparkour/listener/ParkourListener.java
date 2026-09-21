package fr.firstsky.firstparkour.listener;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.ParkourSession;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourListener implements Listener {

    private final FirstParkour plugin;
    /** Suit si le joueur était au sol lors du dernier event */
    private final Map<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();

    public ParkourListener(FirstParkour plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;

        var player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ParkourSession session = plugin.getParkourManager().getSession(player);
        if (session == null || !session.isActive()) {
            wasOnGround.remove(uuid);
            return;
        }

        boolean onGround = player.isOnGround();
        boolean prevOnGround = wasOnGround.getOrDefault(uuid, true);
        wasOnGround.put(uuid, onGround);

        // Détection de chute
        plugin.getParkourManager().checkFall(player);

        // Détection d'atterrissage : transition false → true
        if (!prevOnGround && onGround) {
            Block blockBelow = player.getLocation().subtract(0, 0.2, 0).getBlock();
            Location belowLoc = blockBelow.getLocation();

            if (session.isParkourBlock(belowLoc) && !session.isLastLandedBlock(belowLoc)) {
                plugin.getParkourManager().onPlayerLand(player, session, belowLoc);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getParkourManager().loadPlayerAsync(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getParkourManager().unloadPlayer(event.getPlayer());
        wasOnGround.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;
        if (!plugin.getConfig().getBoolean("parkour.disable-damage", true)) return;
        if (plugin.getParkourManager().isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Empêche de casser/placer des blocs pendant le parkour
        var player = event.getPlayer();
        if (!plugin.getParkourManager().isPlaying(player)) return;
        var action = event.getAction();
        if (action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        if (plugin.getParkourManager().isPlaying(player)) {
            plugin.getParkourManager().stopSession(player, false);
            String msg = plugin.getConfig().getString("messages.prefix", "") + "&cParkour arrêté suite à une mort.";
            MessageUtil.send(player, msg);
        }
        wasOnGround.remove(player.getUniqueId());
    }
}
