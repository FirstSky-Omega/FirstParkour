package dev.efnilite.iep.generator.section

import org.bukkit.util.Vector
import org.jetbrains.annotations.Contract
import kotlin.math.floor

enum class PointType(val heightOffset: Int) {

    CIRCLE(-1) {
        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            if (radius == 0) {
                return listOf(center)
            }

            val blocks = mutableListOf<Vector>()
            val centerX = floor(center.x) + 0.5
            val centerY = floor(center.y) + 0.5
            val centerZ = floor(center.z) + 0.5

            var y = radius
            var z = 0
            var d = 3 - 2 * radius

            while (z <= y) {
                blocks += Vector(centerX, centerY + y, centerZ + z)
                blocks += Vector(centerX, centerY + y, centerZ - z)
                blocks += Vector(centerX, centerY - y, centerZ + z)
                blocks += Vector(centerX, centerY - y, centerZ - z)
                blocks += Vector(centerX, centerY + z, centerZ + y)
                blocks += Vector(centerX, centerY + z, centerZ - y)
                blocks += Vector(centerX, centerY - z, centerZ + y)
                blocks += Vector(centerX, centerY - z, centerZ - y)

                if (d < 0) {
                    d += 4 * z + 6
                } else {
                    d += 4 * (z - y) + 10
                    y--
                }
                z++
            }

            return blocks
        }
    },
    FLAT(-5) {
        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            val x = center.x
            val y = center.y
            val z = center.z

            return listOf(
                Vector(x, y, z - 1),
                Vector(x, y, z),
                Vector(x, y, z + 1)
            )
        }
    };

    @Contract(pure = true)
    abstract fun getPoints(center: Vector, radius: Int): List<Vector>

}