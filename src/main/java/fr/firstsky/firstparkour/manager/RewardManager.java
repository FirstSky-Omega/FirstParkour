package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.util.FoliaUtil;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardManager {

    private final FirstParkour plugin;

    public RewardManager(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void checkMilestone(Player player, int score) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) return;

        ConfigurationSection milestones = plugin.getConfig().getConfigurationSection("rewards.milestones");
        if (milestones == null) return;

        String key = String.valueOf(score);
        if (!milestones.contains(key)) return;

        String msg = milestones.getString(key + ".message", "");
        if (!msg.isEmpty()) {
            String colored = MessageUtil.color(plugin.getConfig().getString("messages.prefix", "") + msg);
            FoliaUtil.runForEntity(plugin, player, () -> player.sendMessage(colored));
        }

        List<String> commands = milestones.getStringList(key + ".commands");
        if (!commands.isEmpty()) {
            String playerName = player.getName();
            FoliaUtil.runGlobal(plugin, () -> {
                for (String cmd : commands) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            cmd.replace("{player}", playerName));
                }
            });
        }

        plugin.getSoundManager().playMilestone(player);
    }
}
