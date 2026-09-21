package fr.firstsky.firstparkour.gui;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;
import fr.firstsky.firstparkour.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ParkourMenu implements Listener {

    private final FirstParkour plugin;
    static final String MENU_TITLE = "&8✦ &6FirstParkour &8✦";

    public ParkourMenu(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String rawTitle = plugin.getConfig().getString("gui.title", MENU_TITLE);
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        Inventory inv = Bukkit.createInventory(null, rows * 9, MessageUtil.color(rawTitle));

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());

        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        inv.setItem(plugin.getConfig().getInt("gui.easy-slot", 11),
                buildDifficultyItem(Difficulty.EASY, data, Material.LIME_TERRACOTTA));
        inv.setItem(plugin.getConfig().getInt("gui.medium-slot", 13),
                buildDifficultyItem(Difficulty.MEDIUM, data, Material.YELLOW_TERRACOTTA));
        inv.setItem(plugin.getConfig().getInt("gui.hard-slot", 15),
                buildDifficultyItem(Difficulty.HARD, data, Material.RED_TERRACOTTA));

        // Bouton thèmes (slot 20)
        inv.setItem(20, buildItem(Material.PAINTING, "&bThèmes de blocs",
                List.of("", "&7Personnalisez l'apparence", "&7de vos blocs de parkour.", "", "&eCliquez pour ouvrir")));

        // Bouton stats (slot 24)
        inv.setItem(plugin.getConfig().getInt("gui.stats-slot", 24), buildStatsItem(data));

        // Bouton fermer (slot 26)
        inv.setItem(plugin.getConfig().getInt("gui.close-slot", 26),
                buildItem(Material.BARRIER, "&cFermer", List.of()));

        player.openInventory(inv);
    }

    private ItemStack buildDifficultyItem(Difficulty difficulty, PlayerData data, Material fallbackMat) {
        String nexoId = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".nexo-item", "");
        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());

        ItemStack item = tryNexoItem(nexoId, fallbackMat);
        int best = data != null ? data.getBestScore(difficulty) : 0;
        setMeta(item, MessageUtil.color(diffName), List.of(
                "",
                "&7Cliquez pour &6choisir cette difficulté",
                "",
                "&7Votre record: &e" + best + " blocs"
        ));
        return item;
    }

    private ItemStack buildStatsItem(PlayerData data) {
        return buildItem(Material.BOOK, "&6Vos Statistiques",
                data == null ? List.of("&7Chargement...") : List.of(
                        "",
                        "&7Facile:     &a" + data.getBestScoreEasy() + " blocs",
                        "&7Normal:     &e" + data.getBestScoreMedium() + " blocs",
                        "&7Difficile:  &c" + data.getBestScoreHard() + " blocs",
                        "",
                        "&7Total sauts: &6" + data.getTotalJumps(),
                        "&7Thème: &b" + (data.getTheme() != null ? data.getTheme().getKey() : "défaut")
                ));
    }

    private ItemStack tryNexoItem(String nexoId, Material fallback) {
        if (nexoId != null && !nexoId.isEmpty()
                && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            try {
                var builder = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId);
                if (builder != null) return builder.build();
            } catch (Throwable ignored) {}
        }
        return new ItemStack(fallback);
    }

    private ItemStack buildItem(Material mat, String name, List<String> loreRaw) {
        ItemStack item = new ItemStack(mat);
        setMeta(item, MessageUtil.color(name), loreRaw);
        return item;
    }

    private void setMeta(ItemStack item, String name, List<String> loreRaw) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(name);
        List<String> colored = new ArrayList<>();
        for (String l : loreRaw) colored.add(MessageUtil.color(l));
        meta.setLore(colored);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = MessageUtil.color(plugin.getConfig().getString("gui.title", MENU_TITLE));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        player.closeInventory();

        int easySlot   = plugin.getConfig().getInt("gui.easy-slot", 11);
        int medSlot    = plugin.getConfig().getInt("gui.medium-slot", 13);
        int hardSlot   = plugin.getConfig().getInt("gui.hard-slot", 15);
        int themeSlot  = 20;

        if      (slot == easySlot)  plugin.getParkourManager().startSession(player, Difficulty.EASY);
        else if (slot == medSlot)   plugin.getParkourManager().startSession(player, Difficulty.MEDIUM);
        else if (slot == hardSlot)  plugin.getParkourManager().startSession(player, Difficulty.HARD);
        else if (slot == themeSlot) plugin.getThemeMenu().open(player);
    }
}
