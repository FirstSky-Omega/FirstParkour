package dev.efnilite.iep.generator.section

import dev.efnilite.ip.foundation.util.Probs
import org.bukkit.util.Vector
import org.jetbrains.annotations.Contract
import kotlin.random.Random
import kotlin.random.asJavaRandom

/**
 * Picks per-knot offsets for the spline-based parkour pipe.
 *
 * The Y bias is parameterised: default is -15 (gentle descent, original IEP behavior).
 * Pass a positive value (e.g. +15) to make the pipe climb instead — used by
 * [dev.efnilite.iep.generator.Generator] when the previous section ended too low so the
 * next section bends upward rather than relying on the old clone-and-teleport trick.
 */
class KnotDirector(
    private val random: Random,
    private val verticalBias: Int = DEFAULT_VERTICAL_BIAS,
) {

    /**
     * Returns a random offset vector. Y component is centered on [verticalBias] so the
     * caller controls whether this section descends, ascends, or hovers.
     */
    @Contract(pure = true)
    fun nextOffset(): Vector {
        val dx = nextOffset(75, 15)
        val dy = nextOffset(verticalBias, 3)
        val dz = nextOffset(0, 35)

        return Vector(dx, dy, dz)
    }

    /**
     * Returns a random normally distributed value.
     */
    @Contract(pure = true)
    private fun nextOffset(mean: Int, sd: Int): Int {
        val distribution = ((mean - 2 * sd)..(mean + 2 * sd))
            .associateWith { Probs.normalpdf(mean.toDouble(), sd.toDouble(), it.toDouble()) }

        return Probs.random(distribution, random.asJavaRandom())
    }

    companion object {
        const val DEFAULT_VERTICAL_BIAS = -15
        const val ASCENDING_VERTICAL_BIAS = 15
    }
}
