import dev.efnilite.iep.generator.section.Section
import org.bukkit.util.Vector
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SectionTest {

    private lateinit var section: Section

    @BeforeEach
    fun setup() {
        section = Section(Vector(0, 0, 0), Random(0))
    }

    @Test
    fun testEnds() {
        val start = section.beginning
        val end = section.end

        assert(start == Vector(0, 0, 0))
    }

    @Test
    fun testPoints() {
        val start = section.beginning
        val end = section.end

        assert(start == section.getPoint(0))
        assert(end == section.getPoint((end.x - start.x).toInt()))
    }

}