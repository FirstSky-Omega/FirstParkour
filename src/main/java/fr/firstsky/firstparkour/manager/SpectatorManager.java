package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.ActiveDuel;
import fr.firstsky.firstparkour.model.ParkourSession;
import fr.firstsky.firstparkour.util.FoliaUtil;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorManager {

    private final FirstParkour plugin;
    /** spectateur UUID → UUID d'un des deux duellistes */
    private final Map<UUID, UUID> spectators       = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode>  prevModes   = new ConcurrentHashMap<>();
    private final Map<UUID, Location>  prevLocations = new ConcurrentHashMap<>();

    public SpectatorManager(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FoliaUtil.runAsyncRepeating(plugin, this::tickActionBars, 0, 1_000);
    }

    // ──────────────────────────────────────────────
    //  Entrer / Quitter
    // ──────────────────────────────────────────────

    public void startSpectating(Player spectator, Player target) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        ActiveDuel duel = plugin.getDuelManager().getDuel(target);
        if (duel == null) {
            MessageUtil.send(spectator, prefix + "&cCe joueur n'est pas en duel.");
            return;
        }
        if (isSpectating(spectator)) {
            MessageUtil.send(spectator, prefix + "&cVous spectez déjà un duel.");
            return;
        }

        prevLocations.put(spectator.getUniqueId(), spectator.getLocation().clone());
        prevModes.put(spectator.getUniqueId(), spectator.getGameMode());
        spectators.put(spectator.getUniqueId(), target.getUniqueId());

        Location spawn = plugin.getParkourManager().getParkourSpawn(spectator);
        Location dest  = spawn != null ? spawn : target.getLocation().clone();

        FoliaUtil.teleport(plugin, spectator, dest, () -> {
            spectator.setGameMode(GameMode.SPECTATOR);

            Player p1 = plugin.getServer().getPlayer(duel.getPlayer1());
            Player p2 = plugin.getServer().getPlayer(duel.getPlayer2());
            String name1 = p1 != null ? p1.getName() : "?";
            String name2 = p2 != null ? p2.getName() : "?";

            String msg = plugin.getConfig().getString("messages.spectator-join",
                    "&7Vous regardez le duel de &e{player1} &7vs &e{player2}&7.")
                    .replace("{player1}", name1).replace("{player2}", name2);
            MessageUtil.send(spectator, prefix + msg);
        });
    }

    public void stopSpectating(Player spectator) {
        UUID uuid = spectator.getUniqueId();
        if (!spectators.containsKey(uuid)) return;

        spectators.remove(uuid);
        GameMode prev  = prevModes.remove(uuid);
        Location origin = prevLocations.remove(uuid);

        Location dest = origin != null ? origin : spectator.getWorld().getSpawnLocation();
        FoliaUtil.teleport(plugin, spectator, dest, () -> {
            if (prev != null) spectator.setGameMode(prev);
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            MessageUtil.send(spectator, prefix + plugin.getConfig().getString(
                    "messages.spectator-quit", "&7Vous avez quitté le mode spectateur."));
        });
    }

    /** Appelé à la fin d'un duel pour renvoyer tous les spectateurs. */
    public void onDuelEnd(ActiveDuel duel) {
        for (UUID specUuid : new ArrayList<>(spectators.keySet())) {
            UUID watched = spectators.get(specUuid);
            if (duel.getPlayer1().equals(watched) || duel.getPlayer2().equals(watched)) {
                Player spec = plugin.getServer().getPlayer(specUuid);
                if (spec != null) stopSpectating(spec);
                else {
                    spectators.remove(specUuid);
                    prevModes.remove(specUuid);
                    prevLocations.remove(specUuid);
                }
            }
        }
    }

    public void onPlayerQuit(Player player) {
        spectators.remove(player.getUniqueId());
        prevModes.remove(player.getUniqueId());
        prevLocations.remove(player.getUniqueId());
    }

    public boolean isSpectating(Player player) {
        return spectators.containsKey(player.getUniqueId());
    }

    // ──────────────────────────────────────────────
    //  Action bar globale
    // ──────────────────────────────────────────────

    private void tickActionBars() {
        for (UUID specUuid : new ArrayList<>(spectators.keySet())) {
            Player spectator = plugin.getServer().getPlayer(specUuid);
            if (spectator == null || !spectator.isOnline()) {
                spectators.remove(specUuid);
                prevModes.remove(specUuid);
                prevLocations.remove(specUuid);
                continue;
            }
            UUID watchedUuid = spectators.get(specUuid);
            Player watched = plugin.getServer().getPlayer(watchedUuid);
            if (watched == null) continue;

            ActiveDuel duel = plugin.getDuelManager().getDuel(watched);
            if (duel == null) {
                stopSpectating(spectator);
                continue;
            }

            Player p1 = plugin.getServer().getPlayer(duel.getPlayer1());
            Player p2 = plugin.getServer().getPlayer(duel.getPlayer2());
            int s1 = score(duel.getPlayer1());
            int s2 = score(duel.getPlayer2());
            String n1 = p1 != null ? p1.getName() : "?";
            String n2 = p2 != null ? p2.getName() : "?";

            String bar = "&6⚔ Spectateur &8| &e" + n1 + " &7" + s1 + " &8vs &e" + n2 + " &7" + s2;
            FoliaUtil.runForEntity(plugin, spectator, () -> MessageUtil.sendActionBar(spectator, bar));
        }
    }

    private int score(UUID uuid) {
        Player p = plugin.getServer().getPlayer(uuid);
        if (p == null) return 0;
        ParkourSession session = plugin.getParkourManager().getSession(p);
        return session != null ? session.getScore() : 0;
    }
}
