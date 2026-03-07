package fr.univ.nantes.feature.expense

import fr.univ.nantes.data.expense.repository.ExpenseRepository
import fr.univ.nantes.data.expense.model.GroupWithDetails
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Fake repository for testing that does not interact with the database
 */
class FakeExpenseRepository : ExpenseRepository {
    override fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>> {
        return flowOf(emptyList())
    }

    override suspend fun getGroupWithDetails(groupId: Long): GroupWithDetails? {
        return null
    }

    override suspend fun createGroup(groupName: String, participants: List<String>): Long {
        return 1L
    }

    override suspend fun addParticipantToGroup(groupId: Long, participantName: String) {
        // No-op for testing
    }

    override suspend fun addExpenseToGroup(
        groupId: Long,
        description: String,
        amount: Double,
        paidBy: String
    ) {
        // No-op for testing
    }

    override suspend fun deleteGroup(groupId: Long) {
        // No-op for testing
    }

    override suspend fun deleteExpense(expenseId: Long) {
        // No-op for testing
    }
}

/**
 * Unit tests for ExpenseViewModel business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var fakeRepository: ExpenseRepository
    private lateinit var fakeProfileUseCase: ProfileUseCase
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        fakeRepository = FakeExpenseRepository()
        viewModel = ExpenseViewModel(fakeRepository, fakeProfileUseCase)
    }

    @org.junit.After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setGroupName_updatesState() {
        viewModel.setGroupName("Test Group")
        assertEquals("Test Group", viewModel.state.value.groupName)
    }

    @Test
    fun addParticipant_addsValidParticipant() {
        viewModel.addParticipant("Alice")
        assertTrue(viewModel.state.value.participants.contains("Alice"))
    }

    @Test
    fun addParticipant_ignoresBlankName() {
        viewModel.addParticipant("")
        viewModel.addParticipant("   ")
        assertTrue(viewModel.state.value.participants.isEmpty())
    }

    @Test
    fun addParticipant_preventsDuplicates() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Alice")
        assertEquals(1, viewModel.state.value.participants.size)
    }

    @Test
    fun removeParticipant_removesExistingParticipant() {
        viewModel.addParticipant("Alice")
        viewModel.removeParticipant("Alice")
        assertFalse(viewModel.state.value.participants.contains("Alice"))
    }

    @Test
    fun removeParticipant_removesExpensesPaidByParticipant() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 50.0, "Alice")
        viewModel.addExpense("Lunch", 30.0, "Bob")

        assertEquals(2, viewModel.state.value.expenses.size)

        viewModel.removeParticipant("Alice")

        assertEquals(1, viewModel.state.value.expenses.size)
        assertEquals("Lunch", viewModel.state.value.expenses[0].description)
        assertEquals("Bob", viewModel.state.value.expenses[0].paidBy)
    }

    @Test
    fun addExpense_addsValidExpense() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Dinner", 50.0, "Alice")
        assertEquals(1, viewModel.state.value.expenses.size)
        assertEquals("Dinner", viewModel.state.value.expenses[0].description)
        assertEquals(50.0, viewModel.state.value.expenses[0].amount, 0.01)
        assertEquals("Alice", viewModel.state.value.expenses[0].paidBy)
    }

    @Test
    fun addExpense_rejectsInvalidExpense() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("", 50.0, "Alice")
        viewModel.addExpense("Dinner", 0.0, "Alice")
        viewModel.addExpense("Dinner", -10.0, "Alice")
        viewModel.addExpense("Dinner", 50.0, "")
        assertEquals(0, viewModel.state.value.expenses.size)
    }

    @Test
    fun addExpense_rejectsNonExistentParticipant() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Dinner", 50.0, "Bob")
        assertEquals(0, viewModel.state.value.expenses.size)
    }

    @Test
    fun calculateBalances_equalExpenses() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")
        viewModel.addExpense("Lunch", 60.0, "Bob")

        val balances = viewModel.calculateBalances()
        assertEquals(2, balances.size)
        balances.forEach { balance ->
            assertEquals(0.0, balance.amount, 0.01)
        }
    }

    @Test
    fun calculateBalances_unequalExpenses() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")

        val balances = viewModel.calculateBalances()
        val aliceBalance = balances.find { it.participant == "Alice" }
        val bobBalance = balances.find { it.participant == "Bob" }

        assertNotNull(aliceBalance)
        assertNotNull(bobBalance)
        assertEquals(30.0, aliceBalance!!.amount, 0.01)
        assertEquals(-30.0, bobBalance!!.amount, 0.01)
    }

    @Test
    fun calculateBalances_multipleExpenses() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addParticipant("Charlie")
        viewModel.addExpense("Dinner", 90.0, "Alice")
        viewModel.addExpense("Lunch", 60.0, "Bob")

        val balances = viewModel.calculateBalances()
        assertEquals(3, balances.size)

        val total = 150.0
        val share = total / 3
        val aliceBalance = balances.find { it.participant == "Alice" }
        val bobBalance = balances.find { it.participant == "Bob" }
        val charlieBalance = balances.find { it.participant == "Charlie" }

        assertNotNull(aliceBalance)
        assertNotNull(bobBalance)
        assertNotNull(charlieBalance)
        assertEquals(90.0 - share, aliceBalance!!.amount, 0.01)
        assertEquals(60.0 - share, bobBalance!!.amount, 0.01)
        assertEquals(-share, charlieBalance!!.amount, 0.01)
    }

    @Test
    fun calculateBalances_noParticipants() {
        val balances = viewModel.calculateBalances()
        assertTrue(balances.isEmpty())
    }

    @Test
    fun calculateReimbursements_simpleCase() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")

        val reimbursements = viewModel.calculateReimbursements()
        assertEquals(1, reimbursements.size)
        assertEquals("Bob", reimbursements[0].from)
        assertEquals("Alice", reimbursements[0].to)
        assertEquals(30.0, reimbursements[0].amount, 0.01)
    }

    @Test
    fun calculateReimbursements_noReimbursementsNeeded() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")
        viewModel.addExpense("Lunch", 60.0, "Bob")

        val reimbursements = viewModel.calculateReimbursements()
        assertTrue(reimbursements.isEmpty())
    }

    @Test
    fun calculateReimbursements_multipleParticipants() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addParticipant("Charlie")
        viewModel.addExpense("Dinner", 90.0, "Alice")

        val reimbursements = viewModel.calculateReimbursements()
        val totalReimbursements = reimbursements.sumOf { it.amount }
        assertEquals(60.0, totalReimbursements, 0.01)
    }

    @Test
    fun reset_clearsAllState() {
        viewModel.setGroupName("Test Group")
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")

        viewModel.reset()

        assertEquals("", viewModel.state.value.groupName)
        assertTrue(viewModel.state.value.participants.isEmpty())
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }
}
