package dev.efnilite.iep.hook

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object VaultHook {

    private val economy = if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
        Bukkit.getServicesManager().getRegistration(Economy::class.java)?.provider
    } else {
        null
    }

    /**
     * Deposits amount to the bal of player.
     *
     * @param player The player.
     * @param amount The amount.
     */
    fun give(player: Player, amount: Double) {
        economy?.depositPlayer(player, amount)
    }
}