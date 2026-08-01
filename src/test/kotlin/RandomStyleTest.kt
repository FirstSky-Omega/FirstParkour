import dev.efnilite.iep.style.RandomStyle
import org.bukkit.Material
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RandomStyleTest {

    @Test
    fun testNext() {
        val style = RandomStyle("test", listOf(Material.STONE, Material.DIRT, Material.GRASS_BLOCK), Random(0))

        assert(Material.STONE == style.next())
        assert(Material.STONE == style.next())
        assert(Material.STONE == style.next())
        assert(Material.STONE == style.next())
        assert(Material.STONE == style.next())
        assert(Material.GRASS_BLOCK == style.next())
        assert(Material.STONE == style.next())
        assert(Material.DIRT == style.next())
    }

}