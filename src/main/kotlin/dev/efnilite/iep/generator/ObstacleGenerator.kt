package dev.efnilite.iep.generator

import dev.efnilite.iep.generator.section.PointType
import dev.efnilite.iep.generator.section.Section
import org.bukkit.Material
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

private enum class Obstacle {

    PILLARS {
        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            val points = mutableListOf<Vector>()
            val range = -radius..radius

            for (dz in range.filter { it % 3 == 0 }) {
                for (dy in range) {
                    val new = center.clone().add(Vector(0, dy, dz))

                    if (new.distance(center) <= radius) {
                        points += new
                    }
                }
            }

            return points
        }
    },
    LINES {
        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            val points = mutableListOf<Vector>()
            val range = -radius..radius

            for (dy in range.filter { it % 3 == 0 }) {
                for (dz in range) {
                    val new = center.clone().add(Vector(0, dy, dz))

                    if (new.distance(center) <= radius) {
                        points += new
                    }
                }
            }

            return points
        }
    },
    HOLE_IN_WALL {
        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            val random = { (-2..2).random() }
            val offset = center.clone().add(Vector(0, random(), random()))

            val radius2 = radius * radius
            val points = mutableListOf<Vector>()
            for (dy in -radius + 1..radius) {
                for (dz in -radius + 1..radius) {
                    val point = center.clone().add(Vector(0, dy, dz))

                    if (point.distanceSquared(center) <= radius2 &&
                        point.distanceSquared(offset) >= 2) {
                        points += point
                    }
                }
            }

            return points
        }
    },
    CIRCLE {
        fun get(center: Vector, radius: Int) = PointType.CIRCLE.getPoints(center, radius)

        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            return (0..radius).filter { it > 0 && it % 2 == 0 }.flatMap { get(center, it) }
        }
    },
    DIAMOND {
        fun get(center: Vector, radius: Int): List<Vector> {
            val points = mutableListOf<Vector>()
            val newCenter = center.clone().add(Vector(0, -radius + 2, 0))
            val r = radius - 2
            val range = 0..2 * r

            for (dy in range) {
                if (dy < r) {
                    points += newCenter.clone().add(Vector(0, dy, -dy))
                    points += newCenter.clone().add(Vector(0, dy, dy))
                    continue
                }

                val newDz = 2 * r - dy
                points += newCenter.clone().add(Vector(0, dy, -newDz))
                points += newCenter.clone().add(Vector(0, dy, newDz))
            }

            return points
        }

        override fun getPoints(center: Vector, radius: Int): List<Vector> {
            return (0..radius).filter { it % 3 == 2 }.flatMap { get(center, it) }
        }
    };

    abstract fun getPoints(center: Vector, radius: Int): List<Vector>

}

/**
 * Obstacle parkour variant.
 *
 * Memory-leak fix: the original implementation placed REAL blocks via `block.type =
 * Material.YELLOW_CONCRETE`, which dirtied the chunk (lighting + save markers), kept it
 * pinned in Paper's chunk holder, and meant a 2 h run could drag in tens of thousands of
 * dirty chunks. We now spoof obstacle blocks the same way the rest of the parkour does:
 * client-side block packets via the parent Generator's [blockChanger]. The server world
 * itself never sees an obstacle block, so chunks unload on Paper's own schedule.
 *
 * The [obstacles] map now stores Vector positions instead of Block references for the
 * same reason — a Vector is 24 bytes and pins nothing, whereas Block#getChunk() forced
 * a chunk load.
 */
class ObstacleGenerator : Generator() {

    private val obstacles = mutableMapOf<Int, MutableList<Vector>>()

    override fun generate(waitForDisplay: CompletableFuture<Void>) {
        super.generate(waitForDisplay)

        val (idx, section) = sections.maxBy { it.key }

        generateObstacle(idx, section, 1)
        generateObstacle(idx, section, 3)
    }

    private fun generateObstacle(idx: Int, section: Section, knotIdx: Int) {
        val obstacle = Obstacle.entries.random()
        val points = obstacle.getPoints(section.getKnot(knotIdx), settings.radius)
        if (points.isEmpty()) return

        // Render client-side rather than mutating the world. Same visual, no chunk dirty.
        blockChangerInstance().sendNow(player.player, points, Material.YELLOW_CONCRETE)

        obstacles.getOrPut(idx) { mutableListOf() }.addAll(points)
    }

    override fun clear(idx: Int, section: Section) {
        super.clear(idx, section)

        val list = obstacles.remove(idx) ?: return
        // Undo the spoofed obstacles — also client-side, no chunk reload.
        blockChangerInstance().sendNow(player.player, list, Material.AIR)
    }

    // Generator.blockChanger is private; ObstacleGenerator lives in the same package, so
    // we declare it accessible via reflection-free package-private helper added in Generator.
    private fun blockChangerInstance() = getBlockChanger()
}
