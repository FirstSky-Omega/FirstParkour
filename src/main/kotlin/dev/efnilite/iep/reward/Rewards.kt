package dev.efnilite.iep.reward

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Config

object Rewards {

    val enabled = Config.REWARDS.getBoolean("enabled")
    val scoreRewards = getRewards("score")
    val intervalRewards = getRewards("interval")
    val oneTimeRewards = getRewards("one-time")

    private fun getRewards(path: String): Map<Int, Set<Reward>> {
        if (!enabled) {
            return emptyMap()
        }

        val rewards = mutableMapOf<Int, Set<Reward>>()
        for (score in Config.REWARDS.getPaths(path)) {
            val fullPath = "$path.$score"

            try {
                val parsedScore = score.toInt()

                if (parsedScore < 1) {
                    IEP.logging.error("Invalid score $score in rewards")
                    continue
                }

                rewards[parsedScore] = Config.REWARDS.getStringList(fullPath)
                    .map { Reward(it) }
                    .toSet()
            } catch (ex: NumberFormatException) {
                IEP.logging.error("Invalid score $score in rewards")
            }
        }

        return rewards
    }

}