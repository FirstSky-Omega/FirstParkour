package dev.efnilite.iep.player

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.reward.Reward
import dev.efnilite.iep.world.World
import dev.efnilite.ip.foundation.inventory.Menu
import dev.efnilite.ip.foundation.inventory.item.Item
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

/**
 * Class for storing a player's previous data.
 *
 * Paper 26 port: PaperLib.teleportAsync replaced with native Player.teleportAsync.
 * Equipment setters use the explicit Java methods (setChestplate, setContents) —
 * Kotlin 2.4 refuses to synthesize a property over a getter/setter pair whose
 * nullability annotations disagree, which is the case in Paper 26's PlayerInventory.
 *
 * Inventory mutations are forced onto the main thread via an `onMain` runner.
 * Paper's `teleportAsync(...).thenRun(...)` continuation thread varies — sometimes
 * the chunk-loader thread, sometimes the main thread — and inventory operations on
 * a non-main thread are silently dropped on modern Paper. That was the cause of
 * the "elytra not given on rejoin" symptom users reported: the first join almost
 * always ran setup on main, but a rejoin happened to hit the async continuation
 * path and the setChestplate call no-op'd.
 */
data class PreviousData(private val player: Player) {

    val leaveRewards: MutableMap<Mode, MutableSet<Reward>> = mutableMapOf()

    private val foodLevel = player.foodLevel
    private val saturation = player.saturation
    private val flying = player.isFlying
    private val allowFlight = player.allowFlight

    private val gamemode = player.gameMode
    private val position = player.location
    private val inventoryContents: Array<ItemStack?> = player.inventory.contents
    private val effects: Collection<PotionEffect> = player.activePotionEffects

    /**
     * Teleports the player into the IEP world, then sets up their inventory + state.
     * The inventory setup is guaranteed to run on the main thread regardless of where
     * the teleport future completes — see class doc.
     */
    fun setup(vector: Vector): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        player.teleportAsync(vector.toLocation(World.world)).thenRun {
            onMain {
                applySetup()
                future.complete(true)
            }
        }

        return future
    }

    private fun applySetup() {
        player.gameMode = GameMode.ADVENTURE
        player.fallDistance = 0F

        player.resetPlayerTime()
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        player.foodLevel = 20
        player.saturation = 20F
        player.isFlying = false
        player.allowFlight = false

        // Wipe everything — main inventory, off-hand, AND armor slots. Inventory.clear()
        // does cover armor on Paper 26, but a few setups (e.g. CMI's "vanish kit", some
        // anti-cheat preload, GameStack inventory mirroring) re-populate the chestplate
        // on the next tick. Calling setHelmet/Chestplate/Leggings/Boots(null) explicitly
        // ensures the slot is empty at the exact moment we then setChestplate(elytra) —
        // no race window where something else can put a curse-of-binding item in first.
        player.inventory.clear()
        player.inventory.setHelmet(null)
        player.inventory.setChestplate(null)
        player.inventory.setLeggings(null)
        player.inventory.setBoots(null)

        val elytra = Item(Material.ELYTRA, "").unbreakable().build()
        player.inventory.setChestplate(elytra)

        val items = mutableListOf<ItemStack>()
        items += Locales.getItem(player, "hotbar.play").build()
        items += Locales.getItem(player, "hotbar.settings").build()
        items += Locales.getItem(player, "hotbar.leaderboards").build()
        items += Locales.getItem(player, "hotbar.leave").build()

        Menu.getEvenlyDistributedSlots(items.size).forEachIndexed { index, slot ->
            player.inventory.setItem(slot, items[index])
        }
    }

    /**
     * Resets the player's data.
     */
    fun reset(switchMode: Boolean, urgent: Boolean) {
        if (switchMode) {
            onMain { resetInner() }
            return
        }
        if (urgent) {
            player.teleportAsync(position).thenRun {
                onMain { resetInner() }
            }
            return
        }

        player.teleportAsync(position).thenRun {
            onMain { resetInner() }
        }
    }

    private fun resetInner() {
        player.fallDistance = 0F

        player.gameMode = gamemode
        player.inventory.setContents(inventoryContents)

        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        player.addPotionEffects(effects)

        player.foodLevel = foodLevel
        player.saturation = saturation
        player.allowFlight = allowFlight
        player.isFlying = flying

        player.resetPlayerTime()

        for ((mode, rewards) in leaveRewards) {
            rewards.forEach { it.execute(player, mode) }
        }
    }

    /**
     * Routes [r] to the main server thread. Use for any operation that touches the
     * player's inventory or world state — Paper's teleportAsync continuation runs on
     * whatever thread the chunk loader handed control back on, and inventory writes
     * from that thread are silently dropped.
     */
    private fun onMain(r: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            r()
        } else {
            player.scheduler.run(IEP.instance, { _ -> r() }, null)
        }
    }
}
