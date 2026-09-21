package fr.firstsky.firstparkour.command;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ParkourCommand implements CommandExecutor, TabCompleter {

    private final FirstParkour plugin;

    public ParkourCommand(FirstParkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        // /parkour → ouvre le menu
        if (args.length == 0) {
            if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
            plugin.getParkourMenu().open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /parkour arreter ─────────────────────────────────────────────
            case "arreter", "stop" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                if (!plugin.getParkourManager().isPlaying(player)) {
                    MessageUtil.send(player, prefix + plugin.getConfig().getString("messages.not-playing")); return true;
                }
                if (plugin.getDuelManager().isInDuel(player)) {
                    int score = plugin.getParkourManager().getSession(player).getScore();
                    plugin.getParkourManager().stopSession(player, false);
                    plugin.getDuelManager().onDuelEnd(player, score);
                } else {
                    plugin.getParkourManager().stopSession(player, true);
                }
            }

            // ── /parkour stats ───────────────────────────────────────────────
            case "stats" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());
                if (data == null) { MessageUtil.send(player, prefix + "&7Données en chargement..."); return true; }
                MessageUtil.send(player, plugin.getConfig().getString("messages.stats-header"));
                MessageUtil.send(player, plugin.getConfig().getString("messages.stats-easy")
                        .replace("{score}", String.valueOf(data.getBestScoreEasy())));
                MessageUtil.send(player, plugin.getConfig().getString("messages.stats-medium")
                        .replace("{score}", String.valueOf(data.getBestScoreMedium())));
                MessageUtil.send(player, plugin.getConfig().getString("messages.stats-hard")
                        .replace("{score}", String.valueOf(data.getBestScoreHard())));
                MessageUtil.send(player, plugin.getConfig().getString("messages.stats-jumps")
                        .replace("{jumps}", String.valueOf(data.getTotalJumps())));
                MessageUtil.send(player, prefix + "&7Thème actif: &b" + data.getTheme().getKey());
            }

            // ── /parkour classement [facile|normal|difficile] ─────────────────
            case "classement", "top" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                Difficulty diff = Difficulty.EASY;
                if (args.length >= 2) { Difficulty p = Difficulty.fromKey(args[1]); if (p != null) diff = p; }
                String diffName = plugin.getConfig().getString(
                        "difficulties." + diff.getKey() + ".display-name", diff.getKey());
                MessageUtil.send(player, plugin.getConfig().getString("messages.top-header")
                        .replace("{difficulty}", MessageUtil.color(diffName)));
                var top = plugin.getLeaderboardManager().getTop(diff);
                if (top.isEmpty()) {
                    MessageUtil.send(player, plugin.getConfig().getString("messages.top-empty"));
                } else {
                    for (int i = 0; i < top.size(); i++) {
                        var e = top.get(i);
                        MessageUtil.send(player, plugin.getConfig().getString("messages.top-line")
                                .replace("{rank}", String.valueOf(i + 1))
                                .replace("{name}", e.getName())
                                .replace("{score}", String.valueOf(e.getBestScore(diff))));
                    }
                }
            }

            // ── /parkour duel <joueur|accepter|refuser> [difficulté] ──────────
            case "duel" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                if (args.length < 2) {
                    MessageUtil.send(player, prefix + "&7Usage:");
                    MessageUtil.send(player, prefix + "&e  /parkour duel <joueur> [facile|normal|difficile]");
                    MessageUtil.send(player, prefix + "&e  /parkour duel accepter");
                    MessageUtil.send(player, prefix + "&e  /parkour duel refuser");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "accepter", "accept" -> plugin.getDuelManager().acceptInvite(player);
                    case "refuser",  "decline" -> plugin.getDuelManager().declineInvite(player);
                    default -> {
                        Player target = plugin.getServer().getPlayer(args[1]);
                        if (target == null) {
                            MessageUtil.send(player, prefix + "&cJoueur introuvable ou hors-ligne.");
                            return true;
                        }
                        Difficulty diff = Difficulty.EASY;
                        if (args.length >= 3) { Difficulty p = Difficulty.fromKey(args[2]); if (p != null) diff = p; }
                        plugin.getDuelManager().sendInvite(player, target, diff);
                    }
                }
            }

            // ── /parkour themes ───────────────────────────────────────────────
            case "themes", "theme" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                plugin.getThemeMenu().open(player);
            }

            // ── /parkour defi ──────────────────────────────────────────────────
            case "defi", "daily" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                var dcm = plugin.getDailyChallengeManager();
                String diffName = plugin.getConfig().getString(
                        "difficulties." + dcm.getTodayDifficulty().getKey() + ".display-name",
                        dcm.getTodayDifficulty().getKey());
                MessageUtil.send(player, prefix + "&6✦ Défi du jour &8— &7Difficulté : " + MessageUtil.color(diffName));
                var top = dcm.getDailyTop();
                if (top.isEmpty()) {
                    MessageUtil.send(player, prefix + "&7Aucun score encore.");
                } else {
                    for (int i = 0; i < top.size(); i++) {
                        var e = top.get(i);
                        MessageUtil.send(player, plugin.getConfig().getString("messages.top-line",
                                        "&7#{rank} &e{name} &7- &6{score} blocs")
                                .replace("{rank}", String.valueOf(i + 1))
                                .replace("{name}", e.name())
                                .replace("{score}", String.valueOf(e.score())));
                    }
                }
                int myRank  = dcm.getDailyRank(player.getUniqueId());
                int myScore = dcm.getDailyScore(player.getUniqueId());
                if (myRank > 0) {
                    MessageUtil.send(player, prefix + "&7Votre position : &e#" + myRank
                            + " &7(&6" + myScore + " blocs&7)");
                }
            }

            // ── /parkour spectater <joueur> ────────────────────────────────────
            case "spectater", "spectate", "spec" -> {
                if (!player.hasPermission("firstparkour.use")) { noPerms(player, prefix); return true; }
                if (args.length < 2) {
                    if (plugin.getSpectatorManager().isSpectating(player)) {
                        plugin.getSpectatorManager().stopSpectating(player);
                    } else {
                        MessageUtil.send(player, prefix + "&7Usage : &e/parkour spectater <joueur>");
                    }
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    MessageUtil.send(player, prefix + "&cJoueur introuvable ou hors-ligne.");
                    return true;
                }
                plugin.getSpectatorManager().startSpectating(player, target);
            }

            // ── Admin ─────────────────────────────────────────────────────────
            case "setspawn", "definirespawn" -> {
                if (!player.hasPermission("firstparkour.admin")) { noPerms(player, prefix); return true; }
                var loc = player.getLocation();
                plugin.getConfig().set("parkour.world",       loc.getWorld().getName());
                plugin.getConfig().set("parkour.spawn.x",     loc.getX());
                plugin.getConfig().set("parkour.spawn.y",     loc.getY());
                plugin.getConfig().set("parkour.spawn.z",     loc.getZ());
                plugin.getConfig().set("parkour.spawn.yaw",   (double) loc.getYaw());
                plugin.getConfig().set("parkour.spawn.pitch", (double) loc.getPitch());
                plugin.saveConfig();
                MessageUtil.send(player, prefix + plugin.getConfig()
                        .getString("messages.spawn-set", "&aSpawn défini !")
                        .replace("{world}", loc.getWorld().getName()));
            }

            case "recharger", "reload" -> {
                if (!player.hasPermission("firstparkour.admin")) { noPerms(player, prefix); return true; }
                plugin.reloadConfig();
                MessageUtil.send(player, prefix + plugin.getConfig().getString("messages.reload"));
            }

            default -> plugin.getParkourMenu().open(player);
        }

        return true;
    }

    private void noPerms(Player player, String prefix) {
        MessageUtil.send(player, prefix + plugin.getConfig().getString(
                "messages.no-permission", "&cPermission insuffisante."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                     @NotNull String label, @NotNull String[] args) {
        String current = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("arreter");
            subs.add("stats");
            subs.add("classement");
            subs.add("duel");
            subs.add("themes");
            subs.add("defi");
            subs.add("spectater");
            if (sender.hasPermission("firstparkour.admin")) {
                subs.add("definirespawn");
                subs.add("recharger");
            }
            return filter(subs, current);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "classement", "top" -> filter(DIFFICULTIES, current);
                case "duel", "spectater", "spectate", "spec" -> {
                    List<String> names = new ArrayList<>(List.of("accepter", "refuser"));
                    plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .forEach(names::add);
                    yield filter(names, current);
                }
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("duel")) {
            return filter(DIFFICULTIES, current);
        }

        return List.of();
    }

    private static final List<String> DIFFICULTIES = List.of("facile", "normal", "difficile");

    private static List<String> filter(List<String> options, String prefix) {
        if (prefix.isEmpty()) return new ArrayList<>(options);
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(prefix)) result.add(s);
        }
        return result;
    }
}
