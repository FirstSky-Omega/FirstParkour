package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.ActiveDuel;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.DuelInvite;
import fr.firstsky.firstparkour.util.FoliaUtil;
import fr.firstsky.firstparkour.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final FirstParkour plugin;

    /** Invitations en attente, clé = UUID du joueur invité */
    private final Map<UUID, DuelInvite> pendingInvites = new ConcurrentHashMap<>();

    /** Duels actifs, clé = UUID d'un des deux joueurs */
    private final Map<UUID, ActiveDuel> activeDuels = new ConcurrentHashMap<>();

    public DuelManager(FirstParkour plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────
    //  Invitation
    // ──────────────────────────────────────────────

    public void sendInvite(Player sender, Player target, Difficulty difficulty) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (sender.equals(target)) {
            MessageUtil.send(sender, prefix + "&cVous ne pouvez pas vous inviter vous-même.");
            return;
        }
        if (plugin.getParkourManager().isPlaying(sender)) {
            MessageUtil.send(sender, prefix + "&cTerminez votre parkour avant de défier quelqu'un.");
            return;
        }
        if (plugin.getParkourManager().isPlaying(target)) {
            MessageUtil.send(sender, prefix + "&c" + target.getName() + " est déjà en train de jouer.");
            return;
        }
        if (isInDuel(sender)) {
            MessageUtil.send(sender, prefix + "&cVous êtes déjà en duel.");
            return;
        }
        if (isInDuel(target)) {
            MessageUtil.send(sender, prefix + "&c" + target.getName() + " est déjà en duel.");
            return;
        }

        pendingInvites.put(target.getUniqueId(), new DuelInvite(sender.getUniqueId(), target.getUniqueId(), difficulty));

        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());
        MessageUtil.send(sender, prefix + "&aInvitation de duel envoyée à &e" + target.getName() + " &a(difficulté : " + MessageUtil.color(diffName) + "&a).");

        // Message cliquable au target
        String header = MessageUtil.color(prefix + "&6" + sender.getName() + " &evous défie en parkour &7(" + MessageUtil.color(diffName) + "&7) !");
        Component acceptBtn = LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&a[✔ ACCEPTER]")
                .clickEvent(ClickEvent.runCommand("/parkour duel accept"))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour accepter")));
        Component refuseBtn = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &c[✘ REFUSER]")
                .clickEvent(ClickEvent.runCommand("/parkour duel decline"))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour refuser")));

        FoliaUtil.runForEntity(plugin, target, () -> {
            target.sendMessage(header);
            target.sendMessage(acceptBtn.append(refuseBtn));
            target.sendMessage(MessageUtil.color("&7(expire dans 30 secondes)"));
        });

        // Expire automatiquement après 30 s
        FoliaUtil.runAsyncDelayed(plugin, () -> {
            DuelInvite invite = pendingInvites.get(target.getUniqueId());
            if (invite != null && invite.isExpired()) {
                pendingInvites.remove(target.getUniqueId());
                Player s = plugin.getServer().getPlayer(invite.getSender());
                Player t = plugin.getServer().getPlayer(invite.getReceiver());
                if (s != null) FoliaUtil.runForEntity(plugin, s, () ->
                        MessageUtil.send(s, prefix + "&7L'invitation pour &e" + target.getName() + " &7a expiré."));
                if (t != null) FoliaUtil.runForEntity(plugin, t, () ->
                        MessageUtil.send(t, prefix + "&7L'invitation de duel de &e" + sender.getName() + " &7a expiré."));
            }
        }, 31_000);
    }

    public void acceptInvite(Player receiver) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        DuelInvite invite = pendingInvites.remove(receiver.getUniqueId());

        if (invite == null || invite.isExpired()) {
            MessageUtil.send(receiver, prefix + "&cAucune invitation de duel en attente.");
            return;
        }

        Player sender = plugin.getServer().getPlayer(invite.getSender());
        if (sender == null || !sender.isOnline()) {
            MessageUtil.send(receiver, prefix + "&cL'adversaire n'est plus connecté.");
            return;
        }

        startDuel(sender, receiver, invite.getDifficulty());
    }

    public void declineInvite(Player receiver) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        DuelInvite invite = pendingInvites.remove(receiver.getUniqueId());

        if (invite == null) {
            MessageUtil.send(receiver, prefix + "&cAucune invitation de duel en attente.");
            return;
        }

        MessageUtil.send(receiver, prefix + "&7Invitation refusée.");
        Player sender = plugin.getServer().getPlayer(invite.getSender());
        if (sender != null) {
            FoliaUtil.runForEntity(plugin, sender, () ->
                    MessageUtil.send(sender, prefix + "&c" + receiver.getName() + " a refusé votre invitation de duel."));
        }
    }

    // ──────────────────────────────────────────────
    //  Démarrage du duel
    // ──────────────────────────────────────────────

    private void startDuel(Player p1, Player p2, Difficulty difficulty) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());

        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), difficulty);
        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        String announce = prefix + "&6⚔ DUEL &7| &e" + p1.getName() + " &7vs &e" + p2.getName()
                + " &7| &7Difficulté : " + MessageUtil.color(diffName);

        FoliaUtil.runForEntity(plugin, p1, () -> {
            MessageUtil.send(p1, announce);
            MessageUtil.sendTitle(p1, "&6⚔ DUEL !", "&7vs &e" + p2.getName(), 10, 40, 10);
        });
        FoliaUtil.runForEntity(plugin, p2, () -> {
            MessageUtil.send(p2, announce);
            MessageUtil.sendTitle(p2, "&6⚔ DUEL !", "&7vs &e" + p1.getName(), 10, 40, 10);
        });

        // Compte à rebours 3s puis démarre les sessions
        FoliaUtil.runAsyncDelayed(plugin, () -> {
            countdown(p1, p2, 3, difficulty);
        }, 1_200); // petite pause avant le countdown
    }

    private void countdown(Player p1, Player p2, int count, Difficulty difficulty) {
        if (count <= 0) {
            // Démarre les deux sessions simultanément
            FoliaUtil.runForEntity(plugin, p1, () ->
                    plugin.getParkourManager().startSession(p1, difficulty));
            FoliaUtil.runForEntity(plugin, p2, () ->
                    plugin.getParkourManager().startSession(p2, difficulty));
            return;
        }
        String countStr = count == 1 ? "&c" + count : count == 2 ? "&e" + count : "&a" + count;
        FoliaUtil.runForEntity(plugin, p1, () -> MessageUtil.sendTitle(p1, countStr, "", 0, 18, 2));
        FoliaUtil.runForEntity(plugin, p2, () -> MessageUtil.sendTitle(p2, countStr, "", 0, 18, 2));

        FoliaUtil.runAsyncDelayed(plugin, () -> countdown(p1, p2, count - 1, difficulty), 1_000);
    }

    // ──────────────────────────────────────────────
    //  Fin de duel
    // ──────────────────────────────────────────────

    /**
     * Appelé depuis ParkourManager quand un joueur en duel tombe ou arrête.
     * loserScore = score du perdant au moment de sa chute.
     */
    public void onDuelEnd(Player loser, int loserScore) {
        ActiveDuel duel = activeDuels.remove(loser.getUniqueId());
        if (duel == null || duel.isEnded()) return;
        duel.setEnded(true);

        UUID winnerUuid = duel.getOpponent(loser.getUniqueId());
        if (winnerUuid == null) return;

        activeDuels.remove(winnerUuid);
        Player winner = plugin.getServer().getPlayer(winnerUuid);

        int winnerScore = 0;
        if (winner != null) {
            var session = plugin.getParkourManager().getSession(winner);
            if (session != null) winnerScore = session.getScore();
            // Arrête la session du gagnant
            plugin.getParkourManager().stopSession(winner, false);
        }

        final int finalWinnerScore = winnerScore;
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        String loserName = loser.getName();
        String winnerName = winner != null ? winner.getName() : "?";

        // Message perdant
        FoliaUtil.runForEntity(plugin, loser, () -> {
            MessageUtil.sendTitle(loser, "&c✗ DÉFAITE", "&7Score: &c" + loserScore, 10, 60, 15);
            MessageUtil.send(loser, prefix + "&c✗ Vous avez perdu le duel contre &e" + winnerName
                    + "&c ! &7Votre score : &c" + loserScore + " &7| Le sien : &6" + finalWinnerScore);
        });

        // Message gagnant
        if (winner != null) {
            FoliaUtil.runForEntity(plugin, winner, () -> {
                MessageUtil.sendTitle(winner, "&a✔ VICTOIRE !", "&7Score: &6" + finalWinnerScore, 10, 60, 15);
                MessageUtil.send(winner, prefix + "&a✔ Vous remportez le duel contre &e" + loserName
                        + "&a ! &7Votre score : &6" + finalWinnerScore + " &7| Le sien : &c" + loserScore);
            });
        }
    }

    // ──────────────────────────────────────────────
    //  Utilitaires
    // ──────────────────────────────────────────────

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    public ActiveDuel getDuel(Player player) {
        return activeDuels.get(player.getUniqueId());
    }

    /** Nettoie les données du joueur à la déconnexion */
    public void onPlayerQuit(Player player) {
        pendingInvites.remove(player.getUniqueId());
        ActiveDuel duel = activeDuels.get(player.getUniqueId());
        if (duel != null && !duel.isEnded()) {
            // Le joueur déconnecté perd le duel
            onDuelEnd(player, 0);
        }
    }
}
