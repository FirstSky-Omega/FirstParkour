import dev.efnilite.iep.style.IncrementalStyle
import org.bukkit.Material
import org.junit.jupiter.api.Test

class IncrementalStyleTest {

    @Test
    fun testNext() {
        val style = IncrementalStyle("test", listOf(Material.STONE, Material.DIRT, Material.GRASS_BLOCK))

        assert(Material.STONE == style.next())
        assert(Material.DIRT == style.next())
        assert(Material.GRASS_BLOCK == style.next())
        assert(Material.STONE == style.next())
    }

}