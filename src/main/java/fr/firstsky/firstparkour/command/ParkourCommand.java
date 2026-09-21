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

            // ── Admin ─────────────────────────────────────────────────────────
            case "setspawn", "definirespawn" -> {
                if (!player.hasPermission("firstparkour.admin")) { noPerms(player, prefix); return true; }
                var loc = player.getLocation();
                plugin.getConfig().set("spawn.world", loc.getWorld().getName());
                plugin.getConfig().set("spawn.x", loc.getX());
                plugin.getConfig().set("spawn.y", loc.getY());
                plugin.getConfig().set("spawn.z", loc.getZ());
                plugin.getConfig().set("spawn.yaw", loc.getYaw());
                plugin.getConfig().set("spawn.pitch", loc.getPitch());
                plugin.saveConfig();
                MessageUtil.send(player, prefix + plugin.getConfig().getString("messages.spawn-set"));
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
        if (args.length == 1) {
            return List.of("arreter", "stats", "classement", "duel", "themes",
                           "definirespawn", "recharger");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "classement", "top" -> List.of("facile", "normal", "difficile");
                case "duel" -> {
                    List<String> names = new ArrayList<>(List.of("accepter", "refuser"));
                    plugin.getServer().getOnlinePlayers().forEach(p -> names.add(p.getName()));
                    yield names;
                }
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duel")) {
            return List.of("facile", "normal", "difficile");
        }
        return List.of();
    }
}
