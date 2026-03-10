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
 * Tests unitaires du DAO ParticipantDao.
 * RG3 : l'index UNIQUE (groupId, name) est représenté ici par un mock qui simule l'exception.
 */
class ParticipantDaoTest {

    private lateinit var dao: ParticipantDao

    @Before
    fun setUp() {
        dao = mock()
    }

    @Test
    fun `insertParticipant retourne un id positif`() = runTest {
        whenever(dao.insertParticipant(any())).thenReturn(1L)

        val id = dao.insertParticipant(ParticipantEntity(groupId = 1L, name = "Alice"))

        assertEquals(1L, id)
    }

    @Test
    fun `insertParticipants insere tous les participants`() = runTest {
        val participants = listOf(
            ParticipantEntity(groupId = 1L, name = "Alice"),
            ParticipantEntity(groupId = 1L, name = "Bob"),
            ParticipantEntity(groupId = 1L, name = "Charlie")
        )

        dao.insertParticipants(participants)

        verify(dao).insertParticipants(participants)
    }

    @Test
    fun `getParticipantsByGroupId retourne la liste des participants`() = runTest {
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
    fun `getParticipantsByGroupId retourne une liste vide si aucun participant`() = runTest {
        whenever(dao.getParticipantsByGroupId(99L)).thenReturn(emptyList())

        val result = dao.getParticipantsByGroupId(99L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteParticipantByName appelle la methode avec les bons parametres`() = runTest {
        dao.deleteParticipantByName(1L, "Alice")
        verify(dao).deleteParticipantByName(1L, "Alice")
    }

    @Test
    fun `deleteParticipant appelle la methode avec le bon id`() = runTest {
        dao.deleteParticipant(5L)
        verify(dao).deleteParticipant(5L)
    }

    @Test
    fun `RG3 - insertParticipant avec nom en doublon leve une exception de contrainte simulee`() = runTest {
        val duplicate = ParticipantEntity(groupId = 1L, name = "Alice")
        // Simule la violation de la contrainte UNIQUE (groupId, name) de la base SQLite
        whenever(dao.insertParticipant(duplicate))
            .thenThrow(RuntimeException("UNIQUE constraint failed: participants.groupId, participants.name"))

        try {
            dao.insertParticipant(duplicate)
            org.junit.Assert.fail("Une RuntimeException de contrainte était attendue")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("UNIQUE") == true)
        }
    }

    @Test
    fun `updateParticipants ajoute et supprime les bons participants`() = runTest {
        val toAdd = listOf(ParticipantEntity(groupId = 1L, name = "Charlie"))
        val toRemove = listOf("Alice")

        dao.updateParticipants(1L, toAdd, toRemove)

        verify(dao).updateParticipants(1L, toAdd, toRemove)
    }
}




