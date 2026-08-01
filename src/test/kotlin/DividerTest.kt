import dev.efnilite.iep.generator.Generator
import dev.efnilite.iep.world.Divider
import org.bukkit.util.Vector
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DividerTest {

    @AfterEach
    fun clear() {
        Divider.clear()
    }

    @Test
    fun testAdd() {
        val gen1 = Generator()
        val gen2 = Generator()
        val gen3 = Generator()

        Divider.add(gen1)
        Divider.add(gen2)
        Divider.add(gen3)

        assert(0 == Divider.toIndex(gen1))
        assert(1 == Divider.toIndex(gen2))
        assert(2 == Divider.toIndex(gen3))
    }

    @Test
    fun testRemoveAndUsePreviousSpaces() {
        val gen1 = Generator()

        Divider.add(gen1)
        Divider.add(Generator())
        Divider.add(Generator())

        Divider.remove(gen1)

        val gen2 = Generator()
        val gen3 = Generator()

        Divider.add(gen2)
        Divider.add(gen3)

        assert(0 == Divider.toIndex(gen2))
        assert(3 == Divider.toIndex(gen3))
    }

    @Test
    fun testToLocation() {
        val pos1 = Divider.add(Generator())
        val pos2 = Divider.add(Generator())
        val pos3 = Divider.add(Generator())
        val pos4 = Divider.add(Generator())

        val x = Divider.size.x.toInt()
        val y = Divider.size.y.toInt()
        val z = Divider.size.z.toInt()

        assert(Vector(0, y, 0) == pos1)
        assert(Vector(x * 1, y, 0) == pos2)
        assert(Vector(x * 1, y, z * 1) == pos3)
        assert(Vector(0, y, z * 1) == pos4)
    }
}