package fr.firstsky.firstparkour.gui;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.util.MessageUtil;
import fr.firstsky.firstparkour.util.NexoUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ParkourMenu implements Listener {

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    private static final Holder HOLDER = new Holder();

    private final FirstParkour plugin;

    static final String MENU_TITLE_KEY = "gui.title";
    static final String DEFAULT_TITLE  = "&8✦ &6FirstParkour &8✦";

    private String cachedTitle;
    private int cachedRows;
    private int slotEasy, slotMedium, slotHard, slotThemes, slotStats, slotClose, slotGlobal, slotDaily;

    public ParkourMenu(FirstParkour plugin) {
        this.plugin = plugin;
        reloadCache();
    }

    /** Appelé à l'init et sur /parkour recharger. */
    public void reloadCache() {
        cachedTitle = MessageUtil.color(plugin.getConfig().getString(MENU_TITLE_KEY, DEFAULT_TITLE));
        cachedRows  = plugin.getConfig().getInt("gui.rows", 3);
        slotEasy    = plugin.getConfig().getInt("gui.easy-slot",    11);
        slotMedium  = plugin.getConfig().getInt("gui.medium-slot",  13);
        slotHard    = plugin.getConfig().getInt("gui.hard-slot",    15);
        slotThemes  = plugin.getConfig().getInt("gui.themes-slot",  20);
        slotStats   = plugin.getConfig().getInt("gui.stats-slot",   24);
        slotClose   = plugin.getConfig().getInt("gui.close-slot",   26);
        slotGlobal  = plugin.getConfig().getInt("gui.global-slot",  22);
        slotDaily   = plugin.getConfig().getInt("gui.daily-slot",    4);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(HOLDER, cachedRows * 9, cachedTitle);

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());

        // ── Fond ─────────────────────────────────────────────────────────────
        ItemStack filler = NexoUtil.build(
                nid("filler"), Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // ── Difficultés ───────────────────────────────────────────────────────
        inv.setItem(slotEasy,   diffItem(Difficulty.EASY, data));
        inv.setItem(slotMedium, diffItem(Difficulty.MEDIUM, data));
        inv.setItem(slotHard,   diffItem(Difficulty.HARD, data));

        // ── Thèmes ────────────────────────────────────────────────────────────
        String themeNexo = nid("themes");
        ItemStack themeBtn = NexoUtil.build(themeNexo, Material.PAINTING,
                "&#00E5FF✦ Thèmes de blocs",
                List.of("", "&#96A6B8Personnalisez l'apparence", "&#96A6B8de vos blocs de parkour.", "", "&#FFD700▶ Cliquez pour ouvrir"));
        inv.setItem(slotThemes, themeBtn);

        // ── Stats ────────────────────────────────────────────────────────────
        List<String> statsLore;
        if (data == null) {
            statsLore = List.of("&#96A6B8Chargement...");
        } else {
            statsLore = List.of(
                    "",
                    "&#A8FF78Facile    &7: &a" + data.getBestScoreEasy() + " blocs",
                    "&#FFD700Normal    &7: &e" + data.getBestScoreMedium() + " blocs",
                    "&#FF6B81Difficile &7: &c" + data.getBestScoreHard() + " blocs",
                    "",
                    "&#07B9FBTotal sauts &7: &f" + data.getTotalJumps(),
                    "&#00E5FFThème actif &7: &f" + data.getTheme().getKey()
            );
        }
        inv.setItem(slotStats,
                NexoUtil.build(nid("stats"), Material.BOOK, "&#FFD700✦ Vos Statistiques", statsLore));

        // ── Classement global ─────────────────────────────────────────────────
        var globalTop = plugin.getLeaderboardManager().getGlobalTop(3);
        List<String> globalLore = new ArrayList<>();
        globalLore.add("");
        if (globalTop.isEmpty()) {
            globalLore.add("&#96A6B8Aucun score enregistré.");
        } else {
            String[] medals = {"&#FFD700#1", "&#C0C0C0#2", "&#CD7F32#3"};
            for (int i = 0; i < globalTop.size(); i++) {
                var pd = globalTop.get(i);
                int best = Math.max(pd.getBestScoreEasy(), Math.max(pd.getBestScoreMedium(), pd.getBestScoreHard()));
                globalLore.add(medals[i] + " &f" + pd.getName() + " &#7A8C99— &#FFA500" + best + " blocs");
            }
        }
        globalLore.add("");
        globalLore.add("&#FFD700▶ Cliquez pour le classement complet");
        inv.setItem(slotGlobal,
                NexoUtil.build(nid("global"), Material.NETHER_STAR, "&#FFD700★ Classement Global", globalLore));

        // ── Défi quotidien ────────────────────────────────────────────────────
        var dcm = plugin.getDailyChallengeManager();
        String dailyDiffName = plugin.getConfig().getString(
                "difficulties." + dcm.getTodayDifficulty().getKey() + ".display-name",
                dcm.getTodayDifficulty().getKey());
        int myDailyRank  = dcm.getDailyRank(player.getUniqueId());
        int myDailyScore = dcm.getDailyScore(player.getUniqueId());
        List<String> dailyLore = new ArrayList<>();
        dailyLore.add("");
        dailyLore.add("&#96A6B8Mode du jour &7: " + MessageUtil.color(dailyDiffName));
        dailyLore.add(myDailyRank > 0
                ? "&#96A6B8Votre position &7: &#FFD700#" + myDailyRank + " &7(&f" + myDailyScore + " blocs&7)"
                : "&#96A6B8Tu n'as pas encore participé.");
        dailyLore.add("");
        dailyLore.add("&#FFD700▶ Cliquez pour voir le classement");
        inv.setItem(slotDaily,
                NexoUtil.build(nid("daily"), Material.CLOCK, "&#FFA500⏱ Défi du Jour", dailyLore));

        // ── Fermer ────────────────────────────────────────────────────────────
        inv.setItem(slotClose,
                NexoUtil.build(nid("close"), Material.BARRIER, "&#FF4757✖ Fermer", List.of()));

        player.openInventory(inv);
    }

    private ItemStack diffItem(Difficulty difficulty, PlayerData data) {
        String key  = difficulty.getKey();
        String nexo = plugin.getConfig().getString("difficulties." + key + ".nexo-item", "");
        String name = plugin.getConfig().getString("difficulties." + key + ".display-name", key);
        Material mat = switch (difficulty) {
            case EASY   -> Material.LIME_TERRACOTTA;
            case MEDIUM -> Material.YELLOW_TERRACOTTA;
            case HARD   -> Material.RED_TERRACOTTA;
        };
        int best = data != null ? data.getBestScore(difficulty) : 0;
        return NexoUtil.build(nexo, mat, name,
                List.of("", "&#96A6B8Cliquez pour &#FFD700choisir cette difficulté", "", "&#96A6B8Votre record &7: &#FFD700" + best + " &#96A6B8blocs"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (!(event.getInventory().getHolder() instanceof Holder)) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        player.closeInventory();

        if      (slot == slotEasy)   plugin.getParkourManager().startSession(player, Difficulty.EASY);
        else if (slot == slotMedium) plugin.getParkourManager().startSession(player, Difficulty.MEDIUM);
        else if (slot == slotHard)   plugin.getParkourManager().startSession(player, Difficulty.HARD);
        else if (slot == slotThemes) plugin.getThemeMenu().open(player);
        else if (slot == slotGlobal) {
            // Affiche le classement global en chat
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            MessageUtil.send(player, prefix + "&#FFD700★ &#FFC200Classement Global &#FFD700★");
            var top = plugin.getLeaderboardManager().getGlobalTop(10);
            if (top.isEmpty()) {
                MessageUtil.send(player, "  &#96A6B8Aucun score enregistré pour le moment.");
            }
            for (int i = 0; i < top.size(); i++) {
                var pd = top.get(i);
                int best = Math.max(pd.getBestScoreEasy(), Math.max(pd.getBestScoreMedium(), pd.getBestScoreHard()));
                MessageUtil.send(player, plugin.getConfig().getString("messages.top-line",
                                "  &#FFD700&l#{rank} &f{name} &#7A8C99— &#FFA500{score} &6blocs")
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{name}", pd.getName())
                        .replace("{score}", String.valueOf(best)));
            }
        }
        else if (slot == slotDaily) {
            player.performCommand("parkour defi");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Retourne l'id Nexo configuré pour un bouton du menu principal. */
    private String nid(String button) {
        return plugin.getConfig().getString("gui.nexo-items." + button, "");
    }
}
