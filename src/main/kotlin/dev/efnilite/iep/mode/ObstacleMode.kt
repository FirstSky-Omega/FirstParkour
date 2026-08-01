package dev.efnilite.iep.mode

import dev.efnilite.iep.generator.ObstacleGenerator
import dev.efnilite.iep.leaderboard.Leaderboard

object ObstacleMode : Mode {

    override val name = "obstacle"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = ObstacleGenerator()

}