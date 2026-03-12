package fr.univ.nantes.data.expense.repository

import fr.univ.nantes.data.expense.dao.ExpenseDao
import fr.univ.nantes.data.expense.dao.ExpenseGroupDao
import fr.univ.nantes.data.expense.dao.ParticipantDao
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExpenseRepositoryImplTest {

    private lateinit var groupDao: ExpenseGroupDao
    private lateinit var participantDao: ParticipantDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        groupDao = mock()
        participantDao = mock()
        expenseDao = mock()
        repository = ExpenseRepositoryImpl(groupDao, participantDao, expenseDao)
    }

    // ── createGroup ────────────────────────────────────────────────────────────

    @Test
    fun `createGroup with 2 valid members returns the group id`() = runTest {
        whenever(groupDao.getGroupByShareCode(any())).thenReturn(null)
        whenever(groupDao.insertGroup(any())).thenReturn("uuid-1")
        val groupCaptor = argumentCaptor<ExpenseGroupEntity>()

        val id = repository.createGroup("Holidays", listOf("Alice", "Bob"))

        assertEquals("uuid-1", id)
        verify(groupDao).insertGroup(groupCaptor.capture())
        assertEquals("Holidays", groupCaptor.firstValue.groupName)
        assertTrue(groupCaptor.firstValue.shareCode.isNotBlank())
        assertEquals(6, groupCaptor.firstValue.shareCode.length)
        assertTrue(groupCaptor.firstValue.shareCode.all { it.isUpperCase() || it.isDigit() })
        verify(participantDao).insertParticipants(any())
    }

    @Test
    fun `createGroup inserts exactly the provided participants`() = runTest {
        whenever(groupDao.insertGroup(any())).thenReturn("uuid-42")
        val captor = argumentCaptor<List<ParticipantEntity>>()

        repository.createGroup("Trip", listOf("Alice", "Bob", "Charlie"))

        verify(participantDao).insertParticipants(captor.capture())
        val inserted = captor.firstValue
        assertEquals(3, inserted.size)
        assertTrue(inserted.all { it.groupId == "uuid-42" })
        assertEquals(listOf("Alice", "Bob", "Charlie"), inserted.map { it.name })
    }

    // ── BR1 ───────────────────────────────────────────────────────────────────

    @Test
    fun `BR1 - createGroup with 0 members throws NotEnoughMembersException`() = runTest {
        try {
            repository.createGroup("Solo", emptyList())
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.NotEnoughMembersException) {
            assertNotNull(e)
        }
        verify(groupDao, never()).insertGroup(any())
    }

    @Test
    fun `BR1 - createGroup with 1 member throws NotEnoughMembersException`() = runTest {
        try {
            repository.createGroup("Solo", listOf("Alice"))
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.NotEnoughMembersException) {
            assertNotNull(e)
        }
    }

    // ── BR2 ───────────────────────────────────────────────────────────────────

    @Test
    fun `BR2 - createGroup with an empty name throws EmptyMemberNameException`() = runTest {
        try {
            repository.createGroup("Group", listOf("Alice", ""))
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR2 - createGroup with a blank name throws EmptyMemberNameException`() = runTest {
        try {
            repository.createGroup("Group", listOf("Alice", "   "))
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR2 - addParticipantToGroup with empty name throws EmptyMemberNameException`() = runTest {
        try {
            repository.addParticipantToGroup("group-1", "")
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
        verify(participantDao, never()).insertParticipant(any())
    }

    // ── BR3 ───────────────────────────────────────────────────────────────────

    @Test
    fun `BR3 - createGroup with duplicate names throws DuplicateMemberNameException`() = runTest {
        try {
            repository.createGroup("Group", listOf("Alice", "Bob", "Alice"))
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
    }

    @Test
    fun `BR3 - addParticipantToGroup with existing name throws DuplicateMemberNameException`() = runTest {
        whenever(participantDao.getParticipantsByGroupId("group-1")).thenReturn(
            listOf(ParticipantEntity(id = "p-1", groupId = "group-1", name = "Alice"))
        )

        try {
            repository.addParticipantToGroup("group-1", "Alice")
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).insertParticipant(any())
    }

    @Test
    fun `BR3 - addParticipantToGroup with unique name inserts the participant`() = runTest {
        whenever(participantDao.getParticipantsByGroupId("group-1")).thenReturn(
            listOf(ParticipantEntity(id = "p-1", groupId = "group-1", name = "Alice"))
        )

        repository.addParticipantToGroup("group-1", "Bob")

        verify(participantDao).insertParticipant(ParticipantEntity(groupId = "group-1", name = "Bob"))
    }

    // ── BR4 ───────────────────────────────────────────────────────────────────

    @Test
    fun `BR4 - addExpenseToGroup with zero amount throws InvalidAmountException`() = runTest {
        try {
            repository.addExpenseToGroup("group-1", "Dinner", 0.0, "Alice")
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.InvalidAmountException) {
            assertNotNull(e)
        }
        verify(expenseDao, never()).insertExpense(any())
    }

    @Test
    fun `BR4 - addExpenseToGroup with negative amount throws InvalidAmountException`() = runTest {
        try {
            repository.addExpenseToGroup("group-1", "Dinner", -5.0, "Alice")
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.InvalidAmountException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR4 - addExpenseToGroup with positive amount inserts the expense`() = runTest {
        val captor = argumentCaptor<ExpenseEntity>()

        repository.addExpenseToGroup("group-1", "Dinner", 42.50, "Alice")

        verify(expenseDao).insertExpense(captor.capture())
        assertEquals(42.50, captor.firstValue.amount, 0.001)
        assertEquals("Dinner", captor.firstValue.description)
        assertEquals("Alice", captor.firstValue.paidBy)
    }

    // ── BR5 ───────────────────────────────────────────────────────────────────

    @Test
    fun `BR5 - removeParticipantFromGroup with own expenses throws MemberHasExpensesException`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(
            listOf(
                ExpenseEntity(id = "exp-1", groupId = "group-1", description = "Taxi",
                    amount = 20.0, paidBy = "Alice")
            )
        )

        try {
            repository.removeParticipantFromGroup("group-1", "Alice")
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.MemberHasExpensesException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).deleteParticipantByName(any(), any())
    }

    @Test
    fun `BR5 - removeParticipantFromGroup without own expenses removes the member`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(
            listOf(
                ExpenseEntity(id = "exp-1", groupId = "group-1", description = "Taxi",
                    amount = 20.0, paidBy = "Bob")
            )
        )

        repository.removeParticipantFromGroup("group-1", "Alice")

        verify(participantDao).deleteParticipantByName("group-1", "Alice")
    }

    @Test
    fun `BR5 - removeParticipantFromGroup with no expenses at all removes the member`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(emptyList())

        repository.removeParticipantFromGroup("group-1", "Alice")

        verify(participantDao).deleteParticipantByName("group-1", "Alice")
    }

    // ── updateGroup ────────────────────────────────────────────────────────────

    @Test
    fun `updateGroup with new name updates the group name`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(emptyList())

        repository.updateGroup("group-1", "New Name", emptyList(), emptyList())

        verify(groupDao).updateGroupName("group-1", "New Name")
    }

    @Test
    fun `updateGroup without new name does not update the group name`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(emptyList())

        repository.updateGroup("group-1", null, emptyList(), emptyList())

        verify(groupDao, never()).updateGroupName(any(), any())
    }

    @Test
    fun `BR5 - updateGroup removing member with expenses throws MemberHasExpensesException`() = runTest {
        whenever(expenseDao.getExpensesByGroupId("group-1")).thenReturn(
            listOf(
                ExpenseEntity(id = "exp-1", groupId = "group-1", description = "Hotel",
                    amount = 100.0, paidBy = "Alice")
            )
        )

        try {
            repository.updateGroup("group-1", null, emptyList(), listOf("Alice"))
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.MemberHasExpensesException) {
            assertTrue(e.message?.contains("Alice") == true)
        }
        verify(participantDao, never()).updateParticipants(any(), any(), any())
    }

    @Test
    fun `BR2 - updateGroup with blank new member name throws EmptyMemberNameException`() = runTest {
        try {
            repository.updateGroup("group-1", null, listOf(""), emptyList())
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.EmptyMemberNameException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR3 - updateGroup with duplicate names in additions throws DuplicateMemberNameException`() = runTest {
        try {
            repository.updateGroup("group-1", null, listOf("Charlie", "Charlie"), emptyList())
            fail("Exception expected")
        } catch (e: ExpenseBusinessException.DuplicateMemberNameException) {
            assertTrue(e.message?.contains("Charlie") == true)
        }
    }

    // ── deleteGroup / deleteExpense ────────────────────────────────────────────

    @Test
    fun `deleteGroup calls dao with the correct id`() = runTest {
        repository.deleteGroup("group-5")
        verify(groupDao).deleteGroup("group-5")
    }

    @Test
    fun `deleteExpense calls dao with the correct id`() = runTest {
        repository.deleteExpense("exp-7")
        verify(expenseDao).deleteExpense("exp-7")
    }

    @Test
    fun `updateGroupName calls dao with the correct parameters`() = runTest {
        repository.updateGroupName("group-3", "Updated")
        verify(groupDao).updateGroupName("group-3", "Updated")
    }

    // ── getGroupWithDetails ────────────────────────────────────────────────────

    @Test
    fun `getGroupWithDetails returns null when group does not exist`() = runTest {
        whenever(groupDao.getGroupWithDetails("uuid-99")).thenReturn(null)

        val result = repository.getGroupWithDetails("uuid-99")

        assertNull(result)
    }

    @Test
    fun `getGroupWithDetails returns the group with its details`() = runTest {
        val group = ExpenseGroupEntity(id = "uuid-1", groupName = "Holidays", shareCode = "HOL123")
        val expected = GroupWithDetails(group = group, participants = emptyList(), expenses = emptyList())
        whenever(groupDao.getGroupWithDetails("uuid-1")).thenReturn(expected)

        val result = repository.getGroupWithDetails("uuid-1")

        assertEquals(expected, result)
    }
}
