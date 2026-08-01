package dev.efnilite.iep.generator

class SpeedDemonGenerator : Generator() {

    private var maxSpeedSoFar = 0.0

    override fun getScore() = maxSpeedSoFar

    override fun tick() {
        super.tick()

        if (shouldScore()) {
            val speed = getSpeed(player)

            maxSpeedSoFar = maxOf(maxSpeedSoFar, speed)
        }
    }

    override fun reset(resetReason: ResetReason, regenerate: Boolean, s: Int, overrideSeedSettings: Boolean) {
        super.reset(resetReason, regenerate, s, overrideSeedSettings)

        maxSpeedSoFar = 0.0
    }
}