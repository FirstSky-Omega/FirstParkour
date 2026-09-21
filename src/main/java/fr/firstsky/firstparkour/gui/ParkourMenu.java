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
    private static final String MENU_TITLE_RAW = "firstparkour_menu";

    public ParkourMenu(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String rawTitle = plugin.getConfig().getString("gui.title", "&8✦ &6FirstParkour &8✦");
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        Inventory inv = Bukkit.createInventory(null, rows * 9, MessageUtil.color(rawTitle));

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());

        // Fond vide
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Bouton Facile
        int easySlot = plugin.getConfig().getInt("gui.easy-slot", 11);
        inv.setItem(easySlot, buildDifficultyItem(Difficulty.EASY, data, Material.LIME_TERRACOTTA));

        // Bouton Normal
        int medSlot = plugin.getConfig().getInt("gui.medium-slot", 13);
        inv.setItem(medSlot, buildDifficultyItem(Difficulty.MEDIUM, data, Material.YELLOW_TERRACOTTA));

        // Bouton Difficile
        int hardSlot = plugin.getConfig().getInt("gui.hard-slot", 15);
        inv.setItem(hardSlot, buildDifficultyItem(Difficulty.HARD, data, Material.RED_TERRACOTTA));

        // Bouton Stats
        int statsSlot = plugin.getConfig().getInt("gui.stats-slot", 24);
        inv.setItem(statsSlot, buildStatsItem(data));

        // Bouton Fermer
        int closeSlot = plugin.getConfig().getInt("gui.close-slot", 26);
        inv.setItem(closeSlot, buildItem(Material.BARRIER, "&cFermer", List.of()));

        player.openInventory(inv);
    }

    private ItemStack buildDifficultyItem(Difficulty difficulty, PlayerData data, Material fallbackMat) {
        String nexoId = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".nexo-item", "");
        String diffName = plugin.getConfig().getString("difficulties." + difficulty.getKey() + ".display-name", difficulty.getKey());

        // Essai avec Nexo si disponible
        ItemStack item = tryNexoItem(nexoId, fallbackMat);

        int best = data != null ? data.getBestScore(difficulty) : 0;
        List<String> lore = List.of(
                "",
                "&7Cliquez pour &6choisir cette difficulté",
                "",
                "&7Votre record: &e" + best + " blocs"
        );
        setMeta(item, MessageUtil.color(diffName), lore);
        return item;
    }

    private ItemStack buildStatsItem(PlayerData data) {
        ItemStack item = buildItem(Material.BOOK, "&6Vos Statistiques",
                data == null ? List.of("&7Chargement...") : List.of(
                        "",
                        "&7Facile:     &a" + data.getBestScoreEasy() + " blocs",
                        "&7Normal:     &e" + data.getBestScoreMedium() + " blocs",
                        "&7Difficile:  &c" + data.getBestScoreHard() + " blocs",
                        "",
                        "&7Total sauts: &6" + data.getTotalJumps()
                ));
        return item;
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

        String title = MessageUtil.color(plugin.getConfig().getString("gui.title", "&8✦ &6FirstParkour &8✦"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);

        int slot = event.getSlot();
        int easySlot = plugin.getConfig().getInt("gui.easy-slot", 11);
        int medSlot = plugin.getConfig().getInt("gui.medium-slot", 13);
        int hardSlot = plugin.getConfig().getInt("gui.hard-slot", 15);
        int closeSlot = plugin.getConfig().getInt("gui.close-slot", 26);

        player.closeInventory();

        if (slot == easySlot) {
            plugin.getParkourManager().startSession(player, Difficulty.EASY);
        } else if (slot == medSlot) {
            plugin.getParkourManager().startSession(player, Difficulty.MEDIUM);
        } else if (slot == hardSlot) {
            plugin.getParkourManager().startSession(player, Difficulty.HARD);
        }
        // closeSlot = fermer seulement (déjà fait par closeInventory())
    }
}
