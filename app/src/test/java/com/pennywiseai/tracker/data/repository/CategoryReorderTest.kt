package com.pennywiseai.tracker.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The arithmetic behind the up/down arrows in the category manager.
 *
 * The write that follows it — renumbering a section 0..n-1 and flushing it in one
 * transaction — is deliberately not what these cover: what can actually go wrong
 * is picking the wrong neighbour, or "moving" a row that is already at the end of
 * its section and having the list come back shuffled.
 */
class CategoryReorderTest {

    @Test
    fun `moving up swaps with the row above`() {
        assertEquals(
            listOf(2L, 1L, 3L),
            reorderedSection(listOf(1L, 2L, 3L), categoryId = 2L, up = true)
        )
    }

    @Test
    fun `moving down swaps with the row below`() {
        assertEquals(
            listOf(1L, 3L, 2L),
            reorderedSection(listOf(1L, 2L, 3L), categoryId = 2L, up = false)
        )
    }

    @Test
    fun `the first row has nowhere to move up to`() {
        assertNull(reorderedSection(listOf(1L, 2L, 3L), categoryId = 1L, up = true))
    }

    @Test
    fun `the last row has nowhere to move down to`() {
        assertNull(reorderedSection(listOf(1L, 2L, 3L), categoryId = 3L, up = false))
    }

    @Test
    fun `a row that is not in the section moves nothing`() {
        assertNull(reorderedSection(listOf(1L, 2L, 3L), categoryId = 99L, up = true))
        assertNull(reorderedSection(listOf(1L, 2L, 3L), categoryId = 99L, up = false))
    }

    @Test
    fun `a section of one is already at both ends`() {
        assertNull(reorderedSection(listOf(7L), categoryId = 7L, up = true))
        assertNull(reorderedSection(listOf(7L), categoryId = 7L, up = false))
    }

    @Test
    fun `an empty section moves nothing`() {
        assertNull(reorderedSection(emptyList(), categoryId = 1L, up = true))
    }

    @Test
    fun `the result keeps every id exactly once`() {
        val section = listOf(5L, 6L, 7L, 8L)
        val moved = reorderedSection(section, categoryId = 6L, up = false)!!
        assertEquals(section.toSet(), moved.toSet())
        assertEquals(section.sorted(), moved.sorted())
    }
}
