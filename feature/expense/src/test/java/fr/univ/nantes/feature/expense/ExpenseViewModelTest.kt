package fr.univ.nantes.feature.expense

import fr.univ.nantes.data.currency.ICurrencyRepository
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun fakeProfileUseCase(): ProfileUseCase {
    val fakeRepository = object : ProfileRepository {
        override fun observeProfile(): Flow<Profile?> = flowOf(null)
        override fun observeCurrencies(): Flow<List<Pair<String, String>>> = flowOf(listOf("EUR" to "Euro"))
        override suspend fun saveProfile(profile: Profile) = Unit
        override suspend fun clearProfile() = Unit
        override suspend fun isLoggedIn(): Boolean = false
    }
    return ProfileUseCase(fakeRepository)
}

private fun fakeCurrencyRepository(): ICurrencyRepository = object : ICurrencyRepository {
    override suspend fun getRate(from: String, to: String): Double = 1.0
    override suspend fun convert(amount: Double, from: String, to: String): Double = amount
    override suspend fun getCacheAgeMinutes(base: String): Long? = null
    override suspend fun getAvailableCurrencies(base: String): List<String> = emptyList()
}

/**
 * Unit tests for ExpenseViewModel business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var fakeProfileUseCase: ProfileUseCase
    private lateinit var fakeCurrencyRepo: ICurrencyRepository
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        fakeRepository = FakeExpenseRepository()
        fakeProfileUseCase = fakeProfileUseCase()
        fakeCurrencyRepo = fakeCurrencyRepository()
        viewModel = ExpenseViewModel(fakeRepository, fakeProfileUseCase, fakeCurrencyRepo)
    }

    @After
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
        balances.forEach { balance -> assertEquals(0.0, balance.amount, 0.01) }
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

    @Test
    fun updateGroupName_invokesRepositoryWithCorrectParams() {
        viewModel.updateGroupName(42L, "New Name")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals(42L, fakeRepository.lastUpdatedGroupNameId)
        assertEquals("New Name", fakeRepository.lastUpdatedGroupName)
    }

    @Test
    fun updateGroupName_doesNotUpdateStateWhenCurrentGroupIdDoesNotMatch() {
        viewModel.setGroupName("Old Name")
        viewModel.updateGroupName(42L, "New Name")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals(42L, fakeRepository.lastUpdatedGroupNameId)
        assertEquals("New Name", fakeRepository.lastUpdatedGroupName)
        assertEquals("Old Name", viewModel.state.value.groupName)
    }

    @Test
    fun addParticipantToGroup_invokesRepositoryWithCorrectParams() {
        viewModel.addParticipantToGroup(10L, "Alice")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10L, fakeRepository.lastAddedParticipantGroupId)
        assertEquals("Alice", fakeRepository.lastAddedParticipantName)
    }

    @Test
    fun addParticipantToGroup_trimsParticipantName() {
        viewModel.addParticipantToGroup(10L, "  Alice  ")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Alice", fakeRepository.lastAddedParticipantName)
    }

    @Test
    fun addParticipantToGroup_ignoresBlankName() {
        viewModel.addParticipantToGroup(10L, "   ")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertNull(fakeRepository.lastAddedParticipantName)
    }

    @Test
    fun removeParticipantFromGroup_invokesRepositoryWithCorrectParams() {
        viewModel.removeParticipantFromGroup(10L, "Alice")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10L, fakeRepository.lastRemovedParticipantGroupId)
        assertEquals("Alice", fakeRepository.lastRemovedParticipantName)
    }

    @Test
    fun updateGroup_invokesRepositoryWithCorrectParams() {
        viewModel.updateGroup(
            groupId = 5L,
            newName = "Renamed",
            addParticipants = listOf("Charlie"),
            removeParticipants = listOf("Dave"),
        )
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5L, fakeRepository.lastUpdateGroupId)
        assertEquals("Renamed", fakeRepository.lastUpdateNewName)
        assertEquals(listOf("Charlie"), fakeRepository.lastUpdateAddParticipants)
        assertEquals(listOf("Dave"), fakeRepository.lastUpdateRemoveParticipants)
    }

    @Test
    fun updateGroup_withNullNewName_passesNullToRepository() {
        viewModel.updateGroup(
            groupId = 5L,
            newName = null,
            addParticipants = listOf("Charlie"),
            removeParticipants = emptyList(),
        )
        mainDispatcher.scheduler.advanceUntilIdle()
        assertNull(fakeRepository.lastUpdateNewName)
        assertEquals(listOf("Charlie"), fakeRepository.lastUpdateAddParticipants)
    }
}
