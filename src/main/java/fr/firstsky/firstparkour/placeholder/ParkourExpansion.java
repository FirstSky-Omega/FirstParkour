package fr.firstsky.firstparkour.placeholder;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.model.ParkourSession;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders disponibles :
 *
 * %firstparkour_score%                      → score actuel (0 si non en jeu)
 * %firstparkour_best_easy%                  → record facile
 * %firstparkour_best_medium%                → record normal
 * %firstparkour_best_hard%                  → record difficile
 * %firstparkour_playing%                    → true/false
 * %firstparkour_difficulty%                 → difficulté en cours
 * %firstparkour_total_jumps%                → total de sauts
 *
 * %firstparkour_top_easy_1_name%            → nom du 1er du classement facile
 * %firstparkour_top_easy_1_score%           → score du 1er du classement facile
 * (easy/medium/hard, rang 1-10)
 */
public class ParkourExpansion extends PlaceholderExpansion {

    private final FirstParkour plugin;

    public ParkourExpansion(FirstParkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "firstparkour"; }

    @Override
    public @NotNull String getAuthor() { return "FirstSky"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // Score actuel
        if (params.equals("score")) {
            ParkourSession s = plugin.getParkourManager().getSession(player);
            return String.valueOf(s != null ? s.getScore() : 0);
        }

        // En train de jouer
        if (params.equals("playing")) {
            return String.valueOf(plugin.getParkourManager().isPlaying(player));
        }

        // Difficulté en cours
        if (params.equals("difficulty")) {
            ParkourSession s = plugin.getParkourManager().getSession(player);
            if (s == null) return "—";
            return plugin.getConfig().getString(
                    "difficulties." + s.getDifficulty().getKey() + ".display-name",
                    s.getDifficulty().getKey());
        }

        // Total sauts
        if (params.equals("total_jumps")) {
            PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());
            return data != null ? String.valueOf(data.getTotalJumps()) : "0";
        }

        // Records personnels
        if (params.startsWith("best_")) {
            String key = params.substring(5);
            Difficulty diff = Difficulty.fromKey(key);
            if (diff == null) return "0";
            PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());
            return data != null ? String.valueOf(data.getBestScore(diff)) : "0";
        }

        // Classement: top_<difficulty>_<rank>_name|score
        if (params.startsWith("top_")) {
            // Exemple: top_easy_1_name
            String[] parts = params.split("_");
            if (parts.length < 4) return "";
            Difficulty diff = Difficulty.fromKey(parts[1]);
            if (diff == null) return "";
            int rank;
            try { rank = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return ""; }
            String field = parts[3]; // name ou score

            var entry = plugin.getLeaderboardManager().getByRank(diff, rank);
            if (entry == null) return field.equals("name") ? "—" : "0";

            return field.equals("name") ? entry.getName() : String.valueOf(entry.getBestScore(diff));
        }

        return null;
    }
}
