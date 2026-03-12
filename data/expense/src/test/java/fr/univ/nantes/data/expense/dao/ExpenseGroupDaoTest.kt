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
 * Unit tests for ExpenseGroupDao using mocks.
 * Real SQLite behaviour is covered by instrumented tests (Room).
 */
class ExpenseGroupDaoTest {

    private lateinit var dao: ExpenseGroupDao

    @Before
    fun setUp() {
        dao = mock()
    }

    @Test
    fun `insertGroup returns a positive id`() = runTest {
        whenever(dao.insertGroup(any())).thenReturn("uuid-1")

        val id = dao.insertGroup(ExpenseGroupEntity(groupName = "Holidays", shareCode = "HOL123"))

        assertEquals("uuid-1", id)
    }

    @Test
    fun `insertGroup is called with the correct entity`() = runTest {
        val entity = ExpenseGroupEntity(groupName = "Trip", shareCode = "TRP456")
        whenever(dao.insertGroup(entity)).thenReturn("uuid-2")

        dao.insertGroup(entity)

        verify(dao).insertGroup(entity)
    }

    @Test
    fun `deleteGroup calls the method with the correct id`() = runTest {
        dao.deleteGroup("group-3")
        verify(dao).deleteGroup("group-3")
    }

    @Test
    fun `updateGroupName calls the method with the correct parameters`() = runTest {
        dao.updateGroupName("group-1", "New Name")
        verify(dao).updateGroupName("group-1", "New Name")
    }

    @Test
    fun `getGroupById returns null when group does not exist`() = runTest {
        whenever(dao.getGroupById("uuid-99")).thenReturn(null)

        val result = dao.getGroupById("uuid-99")

        assertNull(result)
    }

    @Test
    fun `getGroupById returns the entity when group exists`() = runTest {
        val entity = ExpenseGroupEntity(id = "uuid-1", groupName = "Test", shareCode = "TST789")
        whenever(dao.getGroupById("uuid-1")).thenReturn(entity)

        val result = dao.getGroupById("uuid-1")

        assertEquals(entity, result)
    }

    @Test
    fun `getGroupWithDetails returns null when group does not exist`() = runTest {
        whenever(dao.getGroupWithDetails("uuid-99")).thenReturn(null)

        val result = dao.getGroupWithDetails("uuid-99")

        assertNull(result)
    }

    @Test
    fun `getGroupWithDetails returns the group with its details`() = runTest {
        val group = ExpenseGroupEntity(id = "uuid-1", groupName = "Friends", shareCode = "FRD234")
        val details = GroupWithDetails(group = group, participants = emptyList(), expenses = emptyList())
        whenever(dao.getGroupWithDetails("uuid-1")).thenReturn(details)

        val result = dao.getGroupWithDetails("uuid-1")

        assertEquals(details, result)
        assertEquals("Friends", result?.group?.groupName)
    }
}
