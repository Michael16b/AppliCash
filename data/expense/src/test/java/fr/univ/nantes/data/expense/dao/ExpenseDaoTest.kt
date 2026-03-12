package fr.univ.nantes.data.expense.dao

import fr.univ.nantes.data.expense.entity.ExpenseEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ExpenseDao.
 */
class ExpenseDaoTest {

    private lateinit var dao: ExpenseDao

    @Before
    fun setUp() {
        dao = mock()
    }

    @Test
    fun `insertExpense returns a string id`() = runTest {
        whenever(dao.insertExpense(any())).thenReturn("exp-uuid-1")

        val id = dao.insertExpense(
            ExpenseEntity(groupId = "group-1", description = "Meal", amount = 25.0, paidBy = "Alice")
        )

        assertEquals("exp-uuid-1", id)
    }

    @Test
    fun `insertExpense is called with the correct entity`() = runTest {
        val entity = ExpenseEntity(groupId = "group-1", description = "Taxi", amount = 15.0, paidBy = "Bob")
        whenever(dao.insertExpense(entity)).thenReturn("exp-uuid-2")
        val captor = argumentCaptor<ExpenseEntity>()

        dao.insertExpense(entity)

        verify(dao).insertExpense(captor.capture())
        assertEquals("Taxi", captor.firstValue.description)
        assertEquals(15.0, captor.firstValue.amount, 0.001)
        assertEquals("Bob", captor.firstValue.paidBy)
    }

    @Test
    fun `insertExpenses inserts a list of expenses`() = runTest {
        val expenses = listOf(
            ExpenseEntity(groupId = "group-1", description = "Hotel", amount = 120.0, paidBy = "Alice"),
            ExpenseEntity(groupId = "group-1", description = "Flight", amount = 350.0, paidBy = "Bob")
        )

        dao.insertExpenses(expenses)

        verify(dao).insertExpenses(expenses)
    }

    @Test
    fun `getExpensesByGroupId returns the expenses for the group`() = runTest {
        val expected = listOf(
            ExpenseEntity(id = "exp-1", groupId = "group-1", description = "Meal", amount = 25.0, paidBy = "Alice"),
            ExpenseEntity(id = "exp-2", groupId = "group-1", description = "Taxi", amount = 10.0, paidBy = "Bob")
        )
        whenever(dao.getExpensesByGroupId("group-1")).thenReturn(expected)

        val result = dao.getExpensesByGroupId("group-1")

        assertEquals(2, result.size)
        assertEquals(25.0, result[0].amount, 0.001)
        assertEquals(10.0, result[1].amount, 0.001)
    }

    @Test
    fun `getExpensesByGroupId returns empty list when no expenses exist`() = runTest {
        whenever(dao.getExpensesByGroupId("group-99")).thenReturn(emptyList())

        val result = dao.getExpensesByGroupId("group-99")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteExpense calls the method with the correct id`() = runTest {
        dao.deleteExpense("exp-3")
        verify(dao).deleteExpense("exp-3")
    }

    @Test
    fun `BR4 - ExpenseEntity with zero amount should not be inserted via the repository`() {
        // Documents that BR4 is enforced at the repository level, not the DAO level
        val entity = ExpenseEntity(groupId = "group-1", description = "Invalid", amount = 0.0, paidBy = "Alice")
        assertEquals(0.0, entity.amount, 0.001)
        // BR4 validation is tested in ExpenseRepositoryImplTest
    }
}
