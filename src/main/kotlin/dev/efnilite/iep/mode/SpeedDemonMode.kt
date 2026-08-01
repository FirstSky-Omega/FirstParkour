package dev.efnilite.iep.mode

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.generator.SpeedDemonGenerator
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score.Companion.pretty

object SpeedDemonMode : Mode {

    override val name = "speed demon"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = SpeedDemonGenerator()

    override fun formatDisplayScore(score: Double): String {
        return if (Config.CONFIG.getBoolean("settings.metric.default")) {
            "${(score * 3.6).pretty()} km/h"
        } else {
            "${(score * 2.23694).pretty()} mph"
        }
    }
}