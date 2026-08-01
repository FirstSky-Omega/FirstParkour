package dev.efnilite.iep.style

import org.bukkit.Material
import org.jetbrains.annotations.Contract
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * Represents a style where each block is selected randomly.
 */
data class RandomStyle(val name: String, val data: List<Material>,
                       val random: Random = ThreadLocalRandom.current().asKotlinRandom()) : Style {

    @Contract(pure = true)
    override fun next() = data.random(random)

    override fun name() = name

}