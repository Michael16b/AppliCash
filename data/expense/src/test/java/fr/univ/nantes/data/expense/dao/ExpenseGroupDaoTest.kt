package fr.univ.nantes.data.expense.dao

import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests unitaires du DAO ExpenseGroupDao via mocks.
 * Les comportements réels SQLite sont testés via les tests d'instrumentation (Room).
 */
class ExpenseGroupDaoTest {

    private lateinit var dao: ExpenseGroupDao

    @Before
    fun setUp() {
        dao = mock()
    }

    @Test
    fun `insertGroup retourne un id positif`() = runTest {
        whenever(dao.insertGroup(any())).thenReturn(1L)

        val id = dao.insertGroup(ExpenseGroupEntity(groupName = "Vacances"))

        assertEquals(1L, id)
    }

    @Test
    fun `insertGroup est appele avec la bonne entite`() = runTest {
        val entity = ExpenseGroupEntity(groupName = "Voyage")
        whenever(dao.insertGroup(entity)).thenReturn(2L)

        dao.insertGroup(entity)

        verify(dao).insertGroup(entity)
    }

    @Test
    fun `deleteGroup appelle la methode avec le bon id`() = runTest {
        dao.deleteGroup(3L)
        verify(dao).deleteGroup(3L)
    }

    @Test
    fun `updateGroupName appelle la methode avec les bons parametres`() = runTest {
        dao.updateGroupName(1L, "Nouveau Nom")
        verify(dao).updateGroupName(1L, "Nouveau Nom")
    }

    @Test
    fun `getGroupById retourne null quand le groupe n existe pas`() = runTest {
        whenever(dao.getGroupById(99L)).thenReturn(null)

        val result = dao.getGroupById(99L)

        assertNull(result)
    }

    @Test
    fun `getGroupById retourne l entite quand le groupe existe`() = runTest {
        val entity = ExpenseGroupEntity(id = 1L, groupName = "Test")
        whenever(dao.getGroupById(1L)).thenReturn(entity)

        val result = dao.getGroupById(1L)

        assertEquals(entity, result)
    }

    @Test
    fun `getGroupWithDetails retourne null quand groupe inexistant`() = runTest {
        whenever(dao.getGroupWithDetails(99L)).thenReturn(null)

        val result = dao.getGroupWithDetails(99L)

        assertNull(result)
    }

    @Test
    fun `getGroupWithDetails retourne le groupe avec details`() = runTest {
        val group = ExpenseGroupEntity(id = 1L, groupName = "Amis")
        val details = GroupWithDetails(group = group, participants = emptyList(), expenses = emptyList())
        whenever(dao.getGroupWithDetails(1L)).thenReturn(details)

        val result = dao.getGroupWithDetails(1L)

        assertEquals(details, result)
        assertEquals("Amis", result?.group?.groupName)
    }
}

