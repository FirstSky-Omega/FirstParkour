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
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ParkourMenu implements Listener {

    private final FirstParkour plugin;

    // Titre brut (avant colorisation) — utilisé aussi pour identifier le menu
    static final String MENU_TITLE_KEY = "gui.title";
    static final String DEFAULT_TITLE  = "&8✦ &6FirstParkour &8✦";

    public ParkourMenu(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String rawTitle = menuTitle();
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        Inventory inv = Bukkit.createInventory(null, rows * 9, MessageUtil.color(rawTitle));

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());

        // ── Fond ─────────────────────────────────────────────────────────────
        ItemStack filler = NexoUtil.build(
                nid("filler"), Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // ── Difficultés ───────────────────────────────────────────────────────
        inv.setItem(slot("easy-slot", 11), diffItem(Difficulty.EASY, data));
        inv.setItem(slot("medium-slot", 13), diffItem(Difficulty.MEDIUM, data));
        inv.setItem(slot("hard-slot", 15), diffItem(Difficulty.HARD, data));

        // ── Thèmes ────────────────────────────────────────────────────────────
        String themeNexo = nid("themes");
        ItemStack themeBtn = NexoUtil.build(themeNexo, Material.PAINTING,
                "&bThèmes de blocs",
                List.of("", "&7Personnalisez l'apparence", "&7de vos blocs de parkour.", "", "&eCliquez pour ouvrir"));
        inv.setItem(slot("themes-slot", 20), themeBtn);

        // ── Stats ────────────────────────────────────────────────────────────
        List<String> statsLore;
        if (data == null) {
            statsLore = List.of("&7Chargement...");
        } else {
            statsLore = List.of(
                    "",
                    "&7Facile:    &a" + data.getBestScoreEasy() + " blocs",
                    "&7Normal:    &e" + data.getBestScoreMedium() + " blocs",
                    "&7Difficile: &c" + data.getBestScoreHard() + " blocs",
                    "",
                    "&7Total sauts: &6" + data.getTotalJumps(),
                    "&7Thème actif: &b" + data.getTheme().getKey()
            );
        }
        inv.setItem(slot("stats-slot", 24),
                NexoUtil.build(nid("stats"), Material.BOOK, "&6Vos Statistiques", statsLore));

        // ── Fermer ────────────────────────────────────────────────────────────
        inv.setItem(slot("close-slot", 26),
                NexoUtil.build(nid("close"), Material.BARRIER, "&cFermer", List.of()));

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
                List.of("", "&7Cliquez pour &6choisir cette difficulté", "", "&7Votre record: &e" + best + " blocs"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (!event.getView().getTitle().equals(MessageUtil.color(menuTitle()))) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        player.closeInventory();

        if      (slot == slot("easy-slot", 11))   plugin.getParkourManager().startSession(player, Difficulty.EASY);
        else if (slot == slot("medium-slot", 13)) plugin.getParkourManager().startSession(player, Difficulty.MEDIUM);
        else if (slot == slot("hard-slot", 15))   plugin.getParkourManager().startSession(player, Difficulty.HARD);
        else if (slot == slot("themes-slot", 20)) plugin.getThemeMenu().open(player);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String menuTitle() {
        return plugin.getConfig().getString(MENU_TITLE_KEY, DEFAULT_TITLE);
    }

    /** Retourne l'id Nexo configuré pour un bouton du menu principal. */
    private String nid(String button) {
        return plugin.getConfig().getString("gui.nexo-items." + button, "");
    }

    private int slot(String key, int def) {
        return plugin.getConfig().getInt("gui." + key, def);
    }
}
