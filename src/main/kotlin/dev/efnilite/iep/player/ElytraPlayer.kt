package dev.efnilite.iep.player

import com.google.common.io.ByteStreams
import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.leaderboard.Score.Companion.pretty
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.reward.Reward
import dev.efnilite.iep.storage.Storage
import dev.efnilite.iep.world.Divider
import dev.efnilite.iep.world.World
import fr.mrmicky.fastboard.FastBoard
import dev.efnilite.ip.foundation.util.Task
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.ChannelNotRegisteredException
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

/**
 * Class for wrapping players.
 *
 * Paper 26 port notes:
 *  - Replaced PaperLib.teleportAsync with Player.teleportAsync (native since 1.15).
 *  - Replaced Bungee Chat ActionBar path with Adventure Player.sendActionBar(Component);
 *    Bungee Chat is gone on Paper 26.
 *  - FastBoard is now always cleaned up on leave (`switchMode = true` used to skip
 *    `board.delete()`, which leaked one scoreboard objective per mode-switch — over a
 *    long session the player ended up with hundreds of zombie objectives in their packet
 *    history and Paper kept their packet listeners pinned).
 */
class ElytraPlayer(val player: Player, private val data: PreviousData = PreviousData(player)) {

    val position
        get() = player.location.toVector()

    val name = player.name
    val uuid = player.uniqueId

    private var boardTitle = ""
    private var boardLines = listOf<String>()
    private val board = FastBoard(player)

    /**
     * Joins a [Mode].
     */
    fun join(mode: Mode) {
        IEP.log("Creating generator for ${player.name} with mode ${mode.name}")

        val generator = mode.getGenerator()

        val at = Divider.add(generator)

        generator.add(this)

        data.setup(at).thenRun { generator.start(mode, at, mode.pointType) }
    }

    /**
     * Leaves the current mode. The FastBoard is deleted unconditionally — previously
     * it was kept across mode switches, which leaked a scoreboard objective per switch.
     *
     * <b>Ordering matters.</b> The Generator's tick task references this.board. If we
     * delete the board first, then cancel the task, a pending mid-tick can fire one
     * more time and throw "FastBoard is deleted". So the order is:
     *   1. find the generator without throwing (firstOrNull)
     *   2. call remove() — cancels the tick task synchronously
     *   3. data.reset() restores inventory/teleport
     *   4. delete the board last
     * The defensive isDeleted check in updateBoard/sendActionBar covers the residual
     * case where double-leave or a mode-switch race fires anyway.
     */
    fun leave(switchMode: Boolean = false, urgent: Boolean = false) {
        val gen = Divider.generators.firstOrNull { it.player == this }
        if (gen != null) {
            try {
                gen.remove(this)
            } catch (t: Throwable) {
                IEP.logging.stack("Error while removing ${player.name} from their generator", t)
            }
        }

        if (!switchMode && Config.CONFIG.getBoolean("proxy.enabled")) {
            sendToServer(Config.CONFIG.getString("proxy.return-server"))
            return
        }

        data.reset(switchMode, urgent)

        // Always delete — even on mode switch. The next ElytraPlayer for this Player
        // will instantiate its own FastBoard via the field initializer; keeping the
        // old one alive just leaks packet listeners.
        try {
            if (!board.isDeleted) board.delete()
        } catch (_: Throwable) {
            // FastBoard.delete() can throw if the player has already disconnected;
            // we don't care.
        }
    }

    private fun sendToServer(server: String) {
        IEP.log("Sending ${player.name} to proxy server $server")

        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(server)

        try {
            player.sendPluginMessage(IEP.instance, "BungeeCord", out.toByteArray())

            IEP.log("Sent ${player.name} to proxy server $server")
        } catch (ex: ChannelNotRegisteredException) {
            IEP.logging.stack("$server is not registered on proxy", ex)
            player.kick(Component.text("$server is not registered on proxy"))
        }
    }

    /**
     * Teleports the player using Paper's native async API (was PaperLib).
     * @param vector The vector to teleport to.
     */
    fun teleport(vector: Vector): CompletableFuture<Boolean> {
        return player.teleportAsync(vector.toLocation(World.world))
    }

    /**
     * Teleports the player using Paper's native async API.
     * @param location The location to teleport to.
     */
    fun teleport(location: Location): CompletableFuture<Boolean> {
        return player.teleportAsync(location)
    }

    /**
     * Sends a message to the player.
     * @param message The message to send.
     */
    fun send(message: String) {
        player.sendMessage(message)
    }

    /**
     * Sends an action bar message via Adventure. Paper 26 dropped Bungee Chat support,
     * so the old `player.spigot().sendMessage(...)` / `TextComponent.fromLegacy(...)` path
     * crashes with NoClassDefFoundError; we go straight through MiniMessage.
     *
     * Guarded against post-quit ticks — Player#sendActionBar throws if the player has
     * disconnected and Paper's Bukkit handle was already invalidated.
     */
    fun sendActionBar(message: String) {
        if (!player.isOnline) return
        try {
            player.sendActionBar(MiniMessage.miniMessage().deserialize(message))
        } catch (_: Throwable) {
            // Player went offline between isOnline check and the actual send. Swallow.
        }
    }

    /**
     * Updates the player's board.
     *
     * Guarded against a stale tick that fires after [leave] already deleted the board.
     * Cancelling the Bukkit task in Generator.remove() should prevent this, but in
     * practice we still see "FastBoard is deleted" exceptions on double-leave (quit
     * event + world-change event firing back-to-back) and during mode-switch where a
     * new ElytraPlayer is constructed while the old generator is still draining its
     * final tick. Skipping the update is harmless — board.delete() has already pulled
     * the scoreboard from the player's screen.
     */
    fun updateBoard(score: Double, time: String, seed: Int) {
        if (board.isDeleted || !player.isOnline) return

        if (boardTitle.isEmpty()) {
            updateBoardValues()
        }

        try {
            board.updateTitle(boardTitle)
            board.updateLines(boardLines.map { updateLine(it, score, time, seed) })
        } catch (_: IllegalStateException) {
            // Lost the race: another thread/event deleted the board between our
            // isDeleted check and the actual send. Harmless — next tick won't fire
            // either (the cancelled task gets pulled from the scheduler).
        }
    }

    // saves 6% performance!
    private fun updateBoardValues() {
        boardTitle = Locales.getString(this, "scoreboard.title")
        boardLines = Locales.getStringList(this, "scoreboard.lines")
    }

    private fun updateLine(line: String, score: Double, time: String, seed: Int): String {
        val local = line.replace("%score%", score.pretty())
            .replace("%high-score%", getGenerator().getHighScore().score.pretty())
            .replace("%time%", time)
            .replace("%seed%", seed.toString())

        return if (IEP.papiHook != null) {
            IEP.papiHook!!.replace(player, local)
        } else {
            local
        }
    }

    /**
     * Saves the player's settings.
     * @param settings The settings to save.
     */
    fun save(settings: Settings) {
        IEP.log("Saving settings for ${player.name}")

        updateBoardValues()
        player.setPlayerTime(settings.time.toLong(), false)

        if (IEP.stopping) {
            Storage.save(uuid, settings)
            return
        }

        Task.create(IEP.instance)
            .async()
            .execute { Storage.save(uuid, settings) }
            .run()
    }

    /**
     * Loads the player's settings.
     * @return The player's settings.
     */
    fun load(): Settings {
        IEP.log("Loading settings for $name")

        Storage.init(uuid)

        return Storage.load(uuid) ?: DEFAULT_SETTINGS
    }

    /**
     * Adds a reward to the player's settings.
     */
    fun addReward(mode: Mode, reward: Reward) {
        val set = data.leaveRewards[mode] ?: mutableSetOf()

        set += reward

        data.leaveRewards[mode] = set
    }

    /**
     * @param permission The permission to check.
     * @return If the player has the permission.
     */
    fun hasPermission(permission: String): Boolean {
        if (Config.CONFIG.getBoolean("permissions")) {
            return player.hasPermission(permission)
        }

        return true
    }

    /**
     * Returns the generator the player is in.
     */
    fun getGenerator() = Divider.generators.first { it.player == this }

    companion object {

        val DEFAULT_SETTINGS
            get() = Settings(locale = Config.CONFIG.getString("settings.locale.default"),
                metric = Config.CONFIG.getBoolean("settings.metric.default"),
                style = Config.CONFIG.getString("settings.style.default"),
                radius = Config.CONFIG.getInt("settings.radius.default") { it in 3..6 },
                time = Config.CONFIG.getInt("settings.time.default") { it in 0..<24000 },
                seed = Config.CONFIG.getInt("settings.seed.default") { it in -1..1_000_000 },
                info = Config.CONFIG.getBoolean("settings.info.default"),
                fall = Config.CONFIG.getBoolean("settings.fall.default"),
                rewards = mutableSetOf())

        fun Player.asElytraPlayer(): ElytraPlayer? {
            return Divider.generators.map { it.player }.firstOrNull { it.player == this }
        }
    }
}
