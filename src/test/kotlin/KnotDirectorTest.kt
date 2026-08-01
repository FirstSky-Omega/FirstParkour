import dev.efnilite.iep.generator.section.KnotDirector
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KnotDirectorTest {

    @Test
    fun testReproducability() {
        val random = Random(0)

        val dir = KnotDirector(random)

        val a = (0..9).map { dir.nextOffset() }

        val random1 = Random(0)

        val dir1 = KnotDirector(random1)

        val b = (0..9).map { dir1.nextOffset() }

        assert(a == b)
    }
}