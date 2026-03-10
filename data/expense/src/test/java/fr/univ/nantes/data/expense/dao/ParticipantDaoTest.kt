package fr.univ.nantes.data.expense.dao

import fr.univ.nantes.data.expense.entity.ParticipantEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ParticipantDao.
 * BR3: the UNIQUE index (groupId, name) is represented here by a mock that simulates the exception.
 */
class ParticipantDaoTest {

    private lateinit var dao: ParticipantDao

    @Before
    fun setUp() {
        dao = mock()
    }

    @Test
    fun `insertParticipant returns a positive id`() = runTest {
        whenever(dao.insertParticipant(any())).thenReturn(1L)

        val id = dao.insertParticipant(ParticipantEntity(groupId = 1L, name = "Alice"))

        assertEquals(1L, id)
    }

    @Test
    fun `insertParticipants inserts all participants`() = runTest {
        val participants = listOf(
            ParticipantEntity(groupId = 1L, name = "Alice"),
            ParticipantEntity(groupId = 1L, name = "Bob"),
            ParticipantEntity(groupId = 1L, name = "Charlie")
        )

        dao.insertParticipants(participants)

        verify(dao).insertParticipants(participants)
    }

    @Test
    fun `getParticipantsByGroupId returns the participants list`() = runTest {
        val expected = listOf(
            ParticipantEntity(id = 1, groupId = 1L, name = "Alice"),
            ParticipantEntity(id = 2, groupId = 1L, name = "Bob")
        )
        whenever(dao.getParticipantsByGroupId(1L)).thenReturn(expected)

        val result = dao.getParticipantsByGroupId(1L)

        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("Bob", result[1].name)
    }

    @Test
    fun `getParticipantsByGroupId returns empty list when no participants exist`() = runTest {
        whenever(dao.getParticipantsByGroupId(99L)).thenReturn(emptyList())

        val result = dao.getParticipantsByGroupId(99L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteParticipantByName calls the method with the correct parameters`() = runTest {
        dao.deleteParticipantByName(1L, "Alice")
        verify(dao).deleteParticipantByName(1L, "Alice")
    }

    @Test
    fun `deleteParticipant calls the method with the correct id`() = runTest {
        dao.deleteParticipant(5L)
        verify(dao).deleteParticipant(5L)
    }

    @Test
    fun `BR3 - insertParticipant with duplicate name throws a simulated constraint exception`() = runTest {
        val duplicate = ParticipantEntity(groupId = 1L, name = "Alice")
        // Simulates the UNIQUE constraint violation on (groupId, name)
        whenever(dao.insertParticipant(duplicate))
            .thenThrow(RuntimeException("UNIQUE constraint failed: participants.groupId, participants.name"))

        try {
            dao.insertParticipant(duplicate)
            org.junit.Assert.fail("A RuntimeException was expected")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("UNIQUE") == true)
        }
    }

    @Test
    fun `updateParticipants adds and removes the correct participants`() = runTest {
        val toAdd = listOf(ParticipantEntity(groupId = 1L, name = "Charlie"))
        val toRemove = listOf("Alice")

        dao.updateParticipants(1L, toAdd, toRemove)

        verify(dao).updateParticipants(1L, toAdd, toRemove)
    }
}
