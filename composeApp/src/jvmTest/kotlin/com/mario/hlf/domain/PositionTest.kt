package hundirlaflota.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PositionTest {

    @Test
    fun `position inside board returns true`() {
        val size = BoardSize(10)
        assertTrue(Position(0, 0).isInside(size))
        assertTrue(Position(9, 9).isInside(size))
    }

    @Test
    fun `position outside board returns false`() {
        val size = BoardSize(10)
        assertFalse(Position(-1, 0).isInside(size))
        assertFalse(Position(10, 0).isInside(size))
        assertFalse(Position(0, 10).isInside(size))
    }
}
