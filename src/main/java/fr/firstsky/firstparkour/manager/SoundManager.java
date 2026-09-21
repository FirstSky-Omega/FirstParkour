package fr.firstsky.firstparkour.manager;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.util.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private record SoundConfig(Sound sound, float vol, float pitch, Particle particle, int count) {}

    private final FirstParkour plugin;
    private final Map<String, SoundConfig> configs = new HashMap<>();

    public SoundManager(FirstParkour plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Appelé à l'init et sur /parkour recharger. */
    public void reload() {
        configs.clear();
        load("land",      "BLOCK_NOTE_BLOCK_PLING",      1.0f, 1.2f, "HAPPY_VILLAGER",   8);
        load("record",    "UI_TOAST_CHALLENGE_COMPLETE",  1.0f, 1.0f, "TOTEM_OF_UNDYING", 30);
        load("fall",      "ENTITY_VILLAGER_HURT",         1.0f, 0.8f, "SMOKE_NORMAL",     15);
        load("milestone", "ENTITY_PLAYER_LEVELUP",        1.0f, 1.0f, "FIREWORK",         20);
    }

    private void load(String key, String defSound, float defVol, float defPitch,
                      String defParticle, int defCount) {
        Sound sound = null;
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try { sound = Sound.valueOf(plugin.getConfig().getString("sounds." + key, defSound)); }
            catch (IllegalArgumentException ignored) {}
        }
        Particle particle = null;
        if (plugin.getConfig().getBoolean("particles.enabled", true)) {
            try { particle = Particle.valueOf(plugin.getConfig().getString("particles." + key + ".type", defParticle)); }
            catch (IllegalArgumentException ignored) {}
        }
        configs.put(key, new SoundConfig(
                sound,
                (float) plugin.getConfig().getDouble("sounds." + key + "-volume", defVol),
                (float) plugin.getConfig().getDouble("sounds." + key + "-pitch",  defPitch),
                particle,
                plugin.getConfig().getInt("particles." + key + ".count", defCount)));
    }

    public void playLand(Player player)      { play(player, "land"); }
    public void playRecord(Player player)    { play(player, "record"); }
    public void playFall(Player player)      { play(player, "fall"); }
    public void playMilestone(Player player) { play(player, "milestone"); }

    private void play(Player player, String key) {
        SoundConfig cfg = configs.get(key);
        if (cfg == null) return;
        // Un seul appel entity-scheduler pour son + particule
        FoliaUtil.runForEntity(plugin, player, () -> {
            Location loc = player.getLocation();
            if (cfg.sound() != null) {
                player.playSound(loc, cfg.sound(), cfg.vol(), cfg.pitch());
            }
            if (cfg.particle() != null) {
                player.spawnParticle(cfg.particle(), loc.clone().add(0, 0.5, 0),
                        cfg.count(), 0.3, 0.3, 0.3);
            }
        });
    }
}
