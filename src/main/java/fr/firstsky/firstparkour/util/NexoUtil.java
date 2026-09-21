package fr.firstsky.firstparkour.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire pour charger des items Nexo avec fallback vanilla.
 * Toutes les méthodes sont null-safe et ne lèvent jamais d'exception.
 */
public final class NexoUtil {

    private static final boolean NEXO_ENABLED =
            Bukkit.getPluginManager().isPluginEnabled("Nexo");

    private NexoUtil() {}

    /**
     * Retourne un ItemStack Nexo si l'id est valide et Nexo est actif,
     * sinon retourne un item vanilla de type {@code fallback}.
     * Le nom et le lore restent ceux qui seront appliqués juste après.
     */
    public static ItemStack get(String nexoId, Material fallback) {
        if (NEXO_ENABLED && nexoId != null && !nexoId.isBlank()) {
            try {
                var builder = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId);
                if (builder != null) return builder.build();
            } catch (Throwable ignored) {}
        }
        return new ItemStack(fallback);
    }

    /**
     * Construit un item complet (Nexo ou vanilla) avec nom et lore colorés.
     */
    public static ItemStack build(String nexoId, Material fallback, String name, List<String> lore) {
        ItemStack item = get(nexoId, fallback);
        applyMeta(item, name, lore);
        return item;
    }

    /** Applique nom + lore sur un item existant (couleurs & + gérées). */
    public static void applyMeta(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (name != null) meta.setDisplayName(MessageUtil.color(name));
        if (lore != null) {
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(MessageUtil.color(l));
            meta.setLore(colored);
        }
        item.setItemMeta(meta);
    }

    public static boolean isNexoEnabled() { return NEXO_ENABLED; }
}
