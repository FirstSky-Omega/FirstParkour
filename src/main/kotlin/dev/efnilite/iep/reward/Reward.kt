package dev.efnilite.iep.reward

import dev.efnilite.iep.IEP
import dev.efnilite.iep.hook.VaultHook
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.ip.foundation.util.Strings
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * String representation of a reward.
 *
 * Format: <give-time>||<mode>||<command>||<value>
 */
data class Reward(val string: String) {

    fun execute(player: Player, currentMode: Mode) {
        val parts = string.split("||", limit = 4)

        val time = parts[0].lowercase()

        if (time == "leave") {
            player.asElytraPlayer()?.addReward(currentMode, Reward("now||${parts[1]}||${parts[2]}||${parts[3]}"))
            return
        }

        val modeName = parts[1].lowercase()

        if (modeName != "all") {
            val mode = IEP.getMode(modeName)

            if (mode == null) {
                IEP.logging.error("Invalid mode $currentMode in rewards")
                return
            }

            if (mode != currentMode) {
                return
            }
        }

        val command = parts[2].lowercase()
        val value = parts[3].replace("%player%", player.name)

        when (command) {
            "send" -> player.sendMessage(Strings.colour(value))
            "player command" -> player.performCommand(value)
            "console command" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value)
            "vault" -> {
                try {
                    val amount = value.toDouble()
                    VaultHook.give(player, amount)
                } catch (e: NumberFormatException) {
                    IEP.logging.error("Invalid numerical value $value in rewards")
                }
            }

            else -> IEP.logging.error("Invalid command $command in rewards")
        }
    }
}