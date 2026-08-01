package dev.efnilite.iep.world

import dev.efnilite.iep.generator.Generator
import org.bukkit.util.Vector
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.TestOnly
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Divides the world into sections.
 */
object Divider {

    private val sections = mutableMapOf<Generator, Int>()

    val size = Vector(100_000, 250, 100_000)
    val generators: Set<Generator>
        get() = sections.keys

    /**
     * Adds a generator to the divider.
     * @param generator The generator to add.
     * @return The section the generator was added to.
     */
    @Synchronized
    fun add(generator: Generator): Vector {
        val missing = (0..sections.size)
            .first { it !in sections.values }

        sections[generator] = missing

        return toLocation(generator)
    }

    /**
     * Returns the center position of the generator.
     * @param generator The generator to get the position of.
     * @return The center position of the generator.
     */
    private fun toLocation(generator: Generator): Vector {
        val idx = sections[generator]!!

        val (x, z) = spiralAt(idx)

        return Vector(x * size.x, size.y, z * size.z)
    }

    /**
     * Removes a generator from the divider.
     * @param generator The generator to remove.
     */
    fun remove(generator: Generator) {
        sections.remove(generator)
    }

    /**
     * Clears the divider.
     */
    @TestOnly
    fun clear() {
        sections.clear()
    }

    @TestOnly
    fun toIndex(generator: Generator): Int {
        return sections[generator]!!
    }

    // todo remove magic code
    // https://math.stackexchange.com/a/163101
    @Contract(pure = true)
    private fun spiralAt(n: Int): Pair<Int, Int> {
        require(n >= 0) { "Invalid n bound: $n" }

        var n = n
        n++ // one-index
        val k = ceil((sqrt(n.toDouble()) - 1) / 2).toInt()
        var t = 2 * k + 1
        var m = t * t

        t--
        m -= if (n > m - t) {
            return k - (m - n) to -k
        } else {
            t
        }

        m -= if (n > m - t) {
            return -k to -k + (m - n)
        } else {
            t
        }

        return if (n > m - t) {
            -k + (m - n) to k
        } else {
            k to k - (m - n - t)
        }
    }
}