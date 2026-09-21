package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.util.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {

    private final FirstParkour plugin;

    public SoundManager(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void playLand(Player player) {
        play(player, "land", "BLOCK_NOTE_BLOCK_PLING", 1.0f, 1.2f, "HAPPY_VILLAGER", 8);
    }

    public void playRecord(Player player) {
        play(player, "record", "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.0f, "TOTEM_OF_UNDYING", 30);
    }

    public void playFall(Player player) {
        play(player, "fall", "ENTITY_VILLAGER_HURT", 1.0f, 0.8f, "SMOKE_NORMAL", 15);
    }

    public void playMilestone(Player player) {
        play(player, "milestone", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f, "FIREWORK", 20);
    }

    private void play(Player player, String key, String defSound, float defVol, float defPitch,
                      String defParticle, int defCount) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;

        String soundName = plugin.getConfig().getString("sounds." + key, defSound);
        float volume = (float) plugin.getConfig().getDouble("sounds." + key + "-volume", defVol);
        float pitch  = (float) plugin.getConfig().getDouble("sounds." + key + "-pitch",  defPitch);
        try {
            Sound sound = Sound.valueOf(soundName);
            FoliaUtil.runForEntity(plugin, player,
                    () -> player.playSound(player.getLocation(), sound, volume, pitch));
        } catch (IllegalArgumentException ignored) {}

        if (!plugin.getConfig().getBoolean("particles.enabled", true)) return;
        String particleName = plugin.getConfig().getString("particles." + key + ".type", defParticle);
        int count = plugin.getConfig().getInt("particles." + key + ".count", defCount);
        try {
            Particle particle = Particle.valueOf(particleName);
            FoliaUtil.runForEntity(plugin, player, () -> {
                Location loc = player.getLocation().add(0, 0.5, 0);
                player.spawnParticle(particle, loc, count, 0.3, 0.3, 0.3);
            });
        } catch (IllegalArgumentException ignored) {}
    }
}
