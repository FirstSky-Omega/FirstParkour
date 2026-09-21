package fr.firstsky.firstparkour.gui;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
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

public class ThemeMenu implements Listener {

    private final FirstParkour plugin;
    static final String THEME_TITLE = "&8✦ &6Thèmes de Blocs &8✦";

    // Ordre et matériaux d'icônes des thèmes
    private static final Object[][] THEMES = {
        { BlockTheme.DEFAULT, "&7Défaut",      Material.STONE,         "&7Blocs selon la difficulté" },
        { BlockTheme.NETHER,  "&cNether",       Material.NETHERRACK,    "&7Ambiance enfer" },
        { BlockTheme.END,     "&5The End",      Material.END_STONE,     "&7Blocs de l'End" },
        { BlockTheme.OCEAN,   "&9Océan",        Material.PRISMARINE,    "&7Fond sous-marin" },
        { BlockTheme.WINTER,  "&bHiver",        Material.PACKED_ICE,    "&7Glace et neige" },
        { BlockTheme.DESERT,  "&eDésert",       Material.SANDSTONE,     "&7Sable et grès" },
        { BlockTheme.SKY,     "&fNuages",       Material.WHITE_WOOL,    "&7Au-dessus des nuages" },
        { BlockTheme.JUNGLE,  "&aJungle",       Material.JUNGLE_LOG,    "&7Végétation tropicale" },
    };

    public ThemeMenu(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtil.color(THEME_TITLE));

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());
        BlockTheme current = data != null ? data.getTheme() : BlockTheme.DEFAULT;

        // Fond
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Bouton retour
        inv.setItem(18, buildItem(Material.ARROW, "&7← Retour", List.of("&7Retour au menu principal")));

        // Thèmes sur les slots du milieu
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < THEMES.length && i < slots.length; i++) {
            BlockTheme theme   = (BlockTheme) THEMES[i][0];
            String name        = (String) THEMES[i][1];
            Material mat       = (Material) THEMES[i][2];
            String description = (String) THEMES[i][3];

            boolean selected = theme == current;
            String border = selected ? "&a✔ &r" : "&7";
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtil.color(description));
            lore.add("");
            if (selected) lore.add(MessageUtil.color("&a✔ Thème actif"));
            else lore.add(MessageUtil.color("&eCliquez pour sélectionner"));

            ItemStack item = buildItem(mat, border + name, lore);

            // Encadrement vert si sélectionné
            if (selected) {
                var meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(MessageUtil.color("&a✔ " + name));
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(slots[i], item);
        }

        // Slot supplémentaire pour le 8ème thème
        if (THEMES.length == 8) {
            BlockTheme theme   = (BlockTheme) THEMES[7][0];
            String name        = (String) THEMES[7][1];
            Material mat       = (Material) THEMES[7][2];
            String description = (String) THEMES[7][3];
            boolean selected = theme == current;
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtil.color(description));
            lore.add("");
            lore.add(selected ? MessageUtil.color("&a✔ Thème actif") : MessageUtil.color("&eCliquez pour sélectionner"));
            inv.setItem(4, buildItem(mat, (selected ? "&a✔ " : "&7") + name, lore));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (!event.getView().getTitle().equals(MessageUtil.color(THEME_TITLE))) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 18) { // Retour
            plugin.getParkourMenu().open(player);
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < Math.min(THEMES.length, slots.length); i++) {
            if (slot == slots[i]) {
                selectTheme(player, (BlockTheme) THEMES[i][0], (String) THEMES[i][1]);
                return;
            }
        }
        if (THEMES.length == 8 && slot == 4) {
            selectTheme(player, (BlockTheme) THEMES[7][0], (String) THEMES[7][1]);
        }
    }

    private void selectTheme(Player player, BlockTheme theme, String name) {
        plugin.getParkourManager().setTheme(player, theme);
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        MessageUtil.send(player, prefix + "&aThème &6" + name + " &asélectionné !");
        open(player); // Rafraîchit le menu
    }

    private ItemStack buildItem(Material mat, String name, List<String> loreRaw) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(MessageUtil.color(name));
        List<String> lore = new ArrayList<>();
        for (String l : loreRaw) lore.add(MessageUtil.color(l));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
