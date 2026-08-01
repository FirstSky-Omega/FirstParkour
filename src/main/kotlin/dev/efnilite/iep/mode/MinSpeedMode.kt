package dev.efnilite.iep.mode

import dev.efnilite.iep.generator.MinSpeedGenerator
import dev.efnilite.iep.leaderboard.Leaderboard

object MinSpeedMode : Mode {

    override val name = "min speed"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = MinSpeedGenerator()
}