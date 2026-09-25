package com.pennywiseai.tracker.ui.icons

import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which rows get a working arrow in the category manager.
 *
 * The two rules that are easy to get wrong: the ends of a *section* are the ends
 * of the list the arrow may move within (a row at the bottom of "Needs" must not
 * move down into "Wants"), and a sub-category never moves on its own because it
 * is rendered under its parent.
 */
class CategoryMoveStateTest {

    private fun category(name: String, id: Long, parentId: Long? = null) = CategoryEntity(
        id = id,
        name = name,
        color = "#FFFFFF",
        parentId = parentId
    )

    @Test
    fun `first row of a section only moves down`() {
        val state = CategoryMoveState.from(
            groupedForDisplay(listOf(category("Groceries", 1), category("Fuel", 2)))
        )
        assertFalse(1L in state.canMoveUp)
        assertTrue(1L in state.canMoveDown)
        assertTrue(2L in state.canMoveUp)
        assertFalse(2L in state.canMoveDown)
    }

    @Test
    fun `a section border is an end of the movable range`() {
        // Two groups that both land in NEEDS/WANTS beyond the mapping above:
        // "Groceries" is NEEDS, "Food & Dining" is WANTS.
        val state = CategoryMoveState.from(
            groupedForDisplay(listOf(category("Groceries", 1), category("Food & Dining", 2)))
        )
        assertFalse("Groceries must not move down into Wants", 1L in state.canMoveDown)
        assertFalse("Food & Dining must not move up into Needs", 2L in state.canMoveUp)
    }

    @Test
    fun `rows inside a section can move both ways`() {
        val state = CategoryMoveState.from(
            groupedForDisplay(
                listOf(
                    category("Groceries", 1),
                    category("Fuel", 2),
                    category("Healthcare", 3)
                )
            )
        )
        assertTrue(2L in state.canMoveUp)
        assertTrue(2L in state.canMoveDown)
    }

    @Test
    fun `sub-categories are not movable`() {
        val parent = category("Groceries", 1)
        val child = category("Coffee", 2, parentId = 1L)
        val another = category("Fuel", 3)
        val state = CategoryMoveState.from(groupedForDisplay(listOf(parent, child, another)))

        assertFalse(2L in state.canMoveUp)
        assertFalse(2L in state.canMoveDown)
        // The child doesn't block the row after it either.
        assertTrue(3L in state.canMoveUp)
    }

    @Test
    fun `a section whose only top-level row has a child offers no move`() {
        val state = CategoryMoveState.from(
            groupedForDisplay(
                listOf(category("Groceries", 1), category("Coffee", 2, parentId = 1L))
            )
        )
        assertEquals(CategoryMoveState.Empty, state)
    }

    @Test
    fun `an empty list has nothing to move`() {
        assertEquals(CategoryMoveState.Empty, CategoryMoveState.from(groupedForDisplay(emptyList())))
    }
}
