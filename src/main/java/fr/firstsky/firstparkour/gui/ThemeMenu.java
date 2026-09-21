package fr.firstsky.firstparkour.gui;

import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
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

import java.util.ArrayList;
import java.util.List;

public class ThemeMenu implements Listener {

    private final FirstParkour plugin;

    static final String THEME_TITLE_KEY = "gui.theme-title";
    static final String DEFAULT_THEME_TITLE = "&8✦ &6Thèmes de Blocs &8✦";

    /** { themeKey, displayName, fallbackMaterial, description } */
    private static final Object[][] THEMES = {
        { "default", "&7Défaut",   Material.STONE,       "&7Blocs selon la difficulté" },
        { "nether",  "&cNether",   Material.NETHERRACK,  "&7Ambiance Nether" },
        { "end",     "&5The End",  Material.END_STONE,   "&7Blocs de l'End" },
        { "ocean",   "&9Océan",    Material.PRISMARINE,  "&7Fond sous-marin" },
        { "winter",  "&bHiver",    Material.PACKED_ICE,  "&7Glace et neige" },
        { "desert",  "&eDésert",   Material.SANDSTONE,   "&7Sable et grès" },
        { "sky",     "&fNuages",   Material.WHITE_WOOL,  "&7Au-dessus des nuages" },
        { "jungle",  "&aJungle",   Material.JUNGLE_LOG,  "&7Végétation tropicale" },
    };

    // Slots des 8 thèmes
    private static final int[] THEME_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 4 };

    public ThemeMenu(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String rawTitle = themeTitle();
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtil.color(rawTitle));

        PlayerData data = plugin.getParkourManager().getPlayerData(player.getUniqueId());
        BlockTheme current = data != null ? data.getTheme() : BlockTheme.DEFAULT;

        // Fond
        String fillerNexo = plugin.getConfig().getString("gui.nexo-items.filler", "");
        ItemStack filler = NexoUtil.build(fillerNexo, Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Bouton retour
        String backNexo = plugin.getConfig().getString("gui.nexo-items.back", "");
        inv.setItem(18, NexoUtil.build(backNexo, Material.ARROW, "&7← Retour", List.of("&7Retour au menu principal")));

        // Thèmes
        for (int i = 0; i < THEMES.length; i++) {
            String themeKey  = (String) THEMES[i][0];
            String name      = (String) THEMES[i][1];
            Material mat     = (Material) THEMES[i][2];
            String desc      = (String) THEMES[i][3];

            BlockTheme theme = BlockTheme.fromKey(themeKey);
            boolean selected = theme == current;

            // Nexo item configuré pour ce thème
            String nexoId = plugin.getConfig().getString("gui.nexo-items.theme-" + themeKey, "");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(desc);
            lore.add("");
            lore.add(selected ? "&a✔ Thème actif" : "&eCliquez pour sélectionner");

            String displayName = (selected ? "&a✔ " : "&7") + name;
            ItemStack item = NexoUtil.build(nexoId, mat, displayName, lore);
            inv.setItem(THEME_SLOTS[i], item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (!event.getView().getTitle().equals(MessageUtil.color(themeTitle()))) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 18) { // Retour
            plugin.getParkourMenu().open(player);
            return;
        }

        for (int i = 0; i < THEMES.length; i++) {
            if (slot == THEME_SLOTS[i]) {
                BlockTheme theme = BlockTheme.fromKey((String) THEMES[i][0]);
                String name = (String) THEMES[i][1];
                plugin.getParkourManager().setTheme(player, theme);
                String prefix = plugin.getConfig().getString("messages.prefix", "");
                MessageUtil.send(player, prefix + "&aThème &6" + name + " &asélectionné !");
                open(player);
                return;
            }
        }
    }

    private String themeTitle() {
        return plugin.getConfig().getString(THEME_TITLE_KEY, DEFAULT_THEME_TITLE);
    }
}
