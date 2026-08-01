package dev.efnilite.iep.generator

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.leaderboard.Score.Companion.pretty
import kotlin.math.ceil
import kotlin.math.max

class MinSpeedGenerator : Generator() {

    private var startX = 0
    private var maxSpeed = 0.0
    private var ticksTooSlow = 0

    override fun getScore(): Double {
        if (startX == 0) return 0.0

        return max(0.0, player.position.x - startX)
    }

    override fun tick() {
        super.tick()

        val speed = getSpeed(player)

        maxSpeed = maxOf(maxSpeed, speed)

        if (speed > MIN_SPEED && startX == 0) {
            startX = player.position.blockX
        }

        if (MIN_SPEED > speed && maxSpeed > MIN_SPEED) {
            ticksTooSlow++
        }

        if (ticksTooSlow == 6) {
            reset(ResetReason.SPEED)
        }

        player.sendActionBar("${getProgressBar(speed)}${getAboveBar(speed)} <reset><dark_gray>| " +
                "<gray>${convertSpeed(speed, MIN_SPEED)}")
    }

    private fun convertSpeed(a: Double, b: Double): String {
        return if (settings.metric) {
            "${a.pretty()}/${b.pretty()} km/h"
        } else {
            "${(a * 2.236936).pretty()}/${(b * 2.236936).pretty()} mph"
        }
    }

    private fun getAboveBar(speed: Double): String {
        val barAmount = ceil((speed - MIN_SPEED) / INCREMENTS).toInt()

        return (0..<barAmount).joinToString("") { "<green><bold>|" }
    }

    private fun getProgressBar(t: Double): String {
        return (0..<ACTIONBAR_LENGTH)
            .map { if (it * INCREMENTS < t) return@map "<red>|" else return@map "<reset><dark_gray>|" }
            .joinToString("") { it }
    }

    override fun reset(resetReason: ResetReason, regenerate: Boolean, s: Int, overrideSeedSettings: Boolean) {
        super.reset(resetReason, regenerate, s, overrideSeedSettings)

        maxSpeed = 0.0
        ticksTooSlow = 0
        startX = 0
    }

    // The old resetPlayerHeight() teleport was replaced in Generator with a firework
    // boost on entering an ascending section — see Generator.applyAscendingBoost.
    // Hook the same counter reset onto that new path so a player getting lifted
    // doesn't immediately fail the "too slow" check while their velocity rebuilds.
    override fun onAscendingBoost() {
        ticksTooSlow = 0
    }

    companion object {
        val MIN_SPEED = Config.CONFIG.getDouble("mode-settings.min-speed.min-speed") { it > 0 }
        val ACTIONBAR_LENGTH = Config.CONFIG.getInt("mode-settings.min-speed.actionbar-length") { it > 0 }
        val INCREMENTS = MIN_SPEED / ACTIONBAR_LENGTH
    }
}