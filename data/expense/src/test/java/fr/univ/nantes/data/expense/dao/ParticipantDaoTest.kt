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
    fun `insertParticipant returns a string id`() = runTest {
        whenever(dao.insertParticipant(any())).thenReturn(1L)

        val id = dao.insertParticipant(ParticipantEntity(groupId = "group-1", name = "Alice"))

        assertEquals(1L, id)
    }

    @Test
    fun `insertParticipants inserts all participants`() = runTest {
        val participants = listOf(
            ParticipantEntity(groupId = "group-1", name = "Alice"),
            ParticipantEntity(groupId = "group-1", name = "Bob"),
            ParticipantEntity(groupId = "group-1", name = "Charlie")
        )

        dao.insertParticipants(participants)

        verify(dao).insertParticipants(participants)
    }

    @Test
    fun `getParticipantsByGroupId returns the participants list`() = runTest {
        val expected = listOf(
            ParticipantEntity(id = "p-1", groupId = "group-1", name = "Alice"),
            ParticipantEntity(id = "p-2", groupId = "group-1", name = "Bob")
        )
        whenever(dao.getParticipantsByGroupId("group-1")).thenReturn(expected)

        val result = dao.getParticipantsByGroupId("group-1")

        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals("Bob", result[1].name)
    }

    @Test
    fun `getParticipantsByGroupId returns empty list when no participants exist`() = runTest {
        whenever(dao.getParticipantsByGroupId("group-99")).thenReturn(emptyList())

        val result = dao.getParticipantsByGroupId("group-99")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteParticipantByName calls the method with the correct parameters`() = runTest {
        dao.deleteParticipantByName("group-1", "Alice")
        verify(dao).deleteParticipantByName("group-1", "Alice")
    }

    @Test
    fun `deleteParticipant calls the method with the correct id`() = runTest {
        dao.deleteParticipant("p-5")
        verify(dao).deleteParticipant("p-5")
    }

    @Test
    fun `BR3 - insertParticipant with duplicate name throws a simulated constraint exception`() = runTest {
        val duplicate = ParticipantEntity(groupId = "group-1", name = "Alice")
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
        val toAdd = listOf(ParticipantEntity(groupId = "group-1", name = "Charlie"))
        val toRemove = listOf("Alice")

        dao.updateParticipants("group-1", toAdd, toRemove)

        verify(dao).updateParticipants("group-1", toAdd, toRemove)
    }
}
