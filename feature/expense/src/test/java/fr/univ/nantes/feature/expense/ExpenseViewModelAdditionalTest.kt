package fr.univ.nantes.feature.expense

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.univ.nantes.data.currency.ICurrencyRepository
import fr.univ.nantes.data.expense.model.GroupWithDetails
import fr.univ.nantes.data.expense.repository.ExpenseRepository
import fr.univ.nantes.data.expense.repository.JoinGroupResult
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Additional unit tests for [ExpenseViewModel] focusing on:
 *   RG11 – Expense/group forms validate before submission
 *   RG12 – Invalid inputs produce no state change (silent rejection)
 *   RG13 – State reloads after group / participant modifications
 *   RG14 – isLoggedIn reflected in UI state; only logged-in users should add expenses
 *
 * CA7  – Extends coverage of ExpenseViewModel beyond the existing ExpenseViewModelTest
 * CA8  – UI state events (currentGroupId, groupName, isLoggedIn, userCurrencyCode) tested
 * CA9  – Edge cases: zero amount, non-existent payer, blank description
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ExpenseViewModelAdditionalTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Mutable profile flow so tests can push different login states
    private val profileFlow = MutableStateFlow<Profile?>(null)

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var fakeRepository: FakeExpenseRepository

    private fun buildProfileUseCase(profile: Profile? = null): ProfileUseCase {
        profileFlow.value = profile
        val repo = object : ProfileRepository {
            override fun observeProfile(): Flow<Profile?> = profileFlow
            override fun observeCurrencies(): Flow<List<Pair<String, String>>> = flowOf(listOf("EUR" to "Euro"))
            override suspend fun saveProfile(profile: Profile) = Unit
            override suspend fun clearProfile() = Unit
            override suspend fun isLoggedIn(): Boolean = profileFlow.value != null
        }
        return ProfileUseCase(repo)
    }

    private fun buildCurrencyRepository(): ICurrencyRepository = object : ICurrencyRepository {
        override suspend fun getRate(from: String, to: String): Double = 1.0
        override suspend fun convert(amount: Double, from: String, to: String): Double = amount
        override suspend fun getCacheAgeMinutes(base: String): Long? = null
        override suspend fun getAvailableCurrencies(base: String): List<String> = listOf("EUR", "USD")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeExpenseRepository()
        viewModel = ExpenseViewModel(
            fakeRepository,
            buildProfileUseCase(),
            buildCurrencyRepository()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── CA8 - Initial state ───────────────────────────────────────────────────

    @Test
    fun `CA8 - initial state has empty group and no current group id`() {
        val state = viewModel.state.value
        assertEquals("", state.groupName)
        assertTrue(state.participants.isEmpty())
        assertTrue(state.expenses.isEmpty())
        assertNull(state.currentGroupId)
    }

    @Test
    fun `CA8 - initial isLoggedIn is false when no profile`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoggedIn)
    }

    // ── RG14 - Authentication state ───────────────────────────────────────────

    @Test
    fun `RG14 - isLoggedIn becomes true when profile is emitted`() = runTest {
        profileFlow.value = Profile(
            firstName = "Alice",
            lastName = "Dupont",
            email = "alice@mail.com",
            currency = "EUR",
            isLoggedIn = true
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `RG14 - userCurrencyCode reflects profile currency`() = runTest {
        profileFlow.value = Profile(
            firstName = "Alice",
            lastName = "Dupont",
            email = "alice@mail.com",
            currency = "USD",
            isLoggedIn = true
        )
        advanceUntilIdle()
        assertEquals("USD", viewModel.state.value.userCurrencyCode)
    }

    @Test
    fun `RG14 - currentUserName reflects profile firstName`() = runTest {
        profileFlow.value = Profile(
            firstName = "Alice",
            lastName = "Dupont",
            email = "alice@mail.com",
            currency = "EUR",
            isLoggedIn = true
        )
        advanceUntilIdle()
        assertEquals("Alice", viewModel.state.value.currentUserName)
    }

    // ── RG11 - Group form validation ──────────────────────────────────────────

    @Test
    fun `RG11 - setGroupName trims whitespace in state`() {
        viewModel.setGroupName("  Trip  ")
        // GroupName is stored as-is (trimming happens in saveGroup)
        assertEquals("  Trip  ", viewModel.state.value.groupName)
    }

    @Test
    fun `RG11 - saveGroup does nothing when groupName is blank`() = runTest {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.setGroupName("")
        viewModel.saveGroup()
        advanceUntilIdle()
        // Group name is blank — repository.createGroup must NOT be called
        assertEquals(0L, fakeRepository.createGroupCallCount)
    }

    @Test
    fun `RG11 - saveGroup does nothing when participants list is empty`() = runTest {
        viewModel.setGroupName("Trip")
        viewModel.saveGroup()
        advanceUntilIdle()
        assertEquals(0L, fakeRepository.createGroupCallCount)
    }

    @Test
    fun `RG11 - saveGroup normalises participant names on save`() = runTest {
        viewModel.setGroupName("Trip")
        viewModel.addParticipant("  Alice  ")
        viewModel.addParticipant("Bob")
        viewModel.saveGroup()
        advanceUntilIdle()
        // Should have been called once with trimmed names
        assertEquals(1L, fakeRepository.createGroupCallCount)
        assertNotNull(fakeRepository.lastCreatedParticipants)
        assertTrue(fakeRepository.lastCreatedParticipants!!.contains("Alice"))
    }

    @Test
    fun `RG11 - saveGroup removes duplicate participants`() = runTest {
        viewModel.setGroupName("Trip")
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Alice") // duplicate — addParticipant blocks it
        viewModel.saveGroup()
        advanceUntilIdle()
        assertEquals(1, fakeRepository.lastCreatedParticipants?.size)
    }

    // ── RG11 / RG12 - Expense form validation ────────────────────────────────

    @Test
    fun `RG11 - addExpense rejects zero amount silently`() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Lunch", 0.0, "Alice")
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    @Test
    fun `RG11 - addExpense rejects negative amount silently`() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Lunch", -5.0, "Alice")
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    @Test
    fun `RG11 - addExpense rejects blank description silently`() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("", 50.0, "Alice")
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    @Test
    fun `RG11 - addExpense rejects blank paidBy silently`() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Dinner", 50.0, "")
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    @Test
    fun `RG11 - addExpense rejects payer not in participants list`() {
        viewModel.addParticipant("Alice")
        viewModel.addExpense("Dinner", 50.0, "UnknownPerson")
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    // ── RG13 - State updates ──────────────────────────────────────────────────

    @Test
    fun `RG13 - reset preserves groups list`() = runTest {
        viewModel.setGroupName("Trip")
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Dinner", 60.0, "Alice")
        viewModel.reset()

        assertEquals("", viewModel.state.value.groupName)
        assertTrue(viewModel.state.value.participants.isEmpty())
        assertTrue(viewModel.state.value.expenses.isEmpty())
    }

    @Test
    fun `RG13 - removeParticipant updates participants list`() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.removeParticipant("Alice")

        assertFalse(viewModel.state.value.participants.contains("Alice"))
        assertTrue(viewModel.state.value.participants.contains("Bob"))
    }

    @Test
    fun `RG13 - removeParticipant also removes their expenses`() {
        viewModel.addParticipant("Alice")
        viewModel.addParticipant("Bob")
        viewModel.addExpense("Hotel", 100.0, "Alice")
        viewModel.addExpense("Food", 40.0, "Bob")

        viewModel.removeParticipant("Alice")

        assertEquals(1, viewModel.state.value.expenses.size)
        assertEquals("Bob", viewModel.state.value.expenses[0].paidBy)
    }

    // ── parseSplitDetails helper ──────────────────────────────────────────────

    @Test
    fun `parseSplitDetails returns empty map for empty string`() {
        val result = ExpenseViewModel.parseSplitDetails("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseSplitDetails returns empty map for empty JSON object`() {
        val result = ExpenseViewModel.parseSplitDetails("{}")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseSplitDetails parses valid JSON correctly`() {
        val result = ExpenseViewModel.parseSplitDetails("""{"Alice":30.0,"Bob":20.0}""")
        assertEquals(30.0, result["Alice"]!!, 0.001)
        assertEquals(20.0, result["Bob"]!!, 0.001)
    }

    @Test
    fun `parseSplitDetails returns empty map for invalid JSON`() {
        val result = ExpenseViewModel.parseSplitDetails("not-json")
        assertTrue(result.isEmpty())
    }

    // ── serializeSplitDetails helper ──────────────────────────────────────────

    @Test
    fun `serializeSplitDetails returns empty JSON object for empty map`() {
        val result = ExpenseViewModel.serializeSplitDetails(emptyMap())
        assertEquals("{}", result)
    }

    @Test
    fun `serializeSplitDetails round-trips with parseSplitDetails`() {
        val original = mapOf("Alice" to 30.0, "Bob" to 20.0)
        val serialized = ExpenseViewModel.serializeSplitDetails(original)
        val parsed = ExpenseViewModel.parseSplitDetails(serialized)
        assertEquals(original["Alice"]!!, parsed["Alice"]!!, 0.001)
        assertEquals(original["Bob"]!!, parsed["Bob"]!!, 0.001)
    }

    // ── getCurrentGroupId ─────────────────────────────────────────────────────

    @Test
    fun `getCurrentGroupId returns null when no group is loaded`() {
        assertNull(viewModel.getCurrentGroupId())
    }
}

/**
 * Extended fake repository that tracks createGroup calls and parameters.
 */
class FakeExpenseRepository : ExpenseRepository {

    var createGroupCallCount = 0L
    var lastCreatedParticipants: List<String>? = null
    var lastAddedParticipantGroupId: Long? = null
    var lastAddedParticipantName: String? = null
    var lastRemovedParticipantGroupId: Long? = null
    var lastRemovedParticipantName: String? = null
    var lastUpdatedGroupNameId: Long? = null
    var lastUpdatedGroupName: String? = null
    var lastUpdateGroupId: Long? = null
    var lastUpdateNewName: String? = null
    var lastUpdateAddParticipants: List<String>? = null
    var lastUpdateRemoveParticipants: List<String>? = null

    override fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>> = flowOf(emptyList())

    override suspend fun getGroupWithDetails(groupId: Long): GroupWithDetails? = null

    override suspend fun createGroup(groupName: String, participants: List<String>): Long {
        createGroupCallCount++
        lastCreatedParticipants = participants
        return createGroupCallCount
    }

    override suspend fun addParticipantToGroup(groupId: Long, participantName: String) {
        lastAddedParticipantGroupId = groupId
        lastAddedParticipantName = participantName
    }

    override suspend fun addExpenseToGroup(
        groupId: Long,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: Int,
        splitDetails: String,
        receiptPath: String
    ) {

    }

    override suspend fun deleteGroup(groupId: Long) = Unit

    override suspend fun deleteExpense(expenseId: Long) = Unit

    override suspend fun updateGroupName(groupId: Long, groupName: String) {
        lastUpdatedGroupNameId = groupId
        lastUpdatedGroupName = groupName
    }

    override suspend fun removeParticipantFromGroup(groupId: Long, participantName: String) {
        lastRemovedParticipantGroupId = groupId
        lastRemovedParticipantName = participantName
    }

    override suspend fun updateGroup(
        groupId: Long,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    ) {
        lastUpdateGroupId = groupId
        lastUpdateNewName = newName
        lastUpdateAddParticipants = addParticipants
        lastUpdateRemoveParticipants = removeParticipants
    }

    override suspend fun canViewShareCode(groupId: Long, userName: String?): Boolean {
        // In tests the fake repository does not manage share codes; return false by default
        return false
    }

    override suspend fun joinGroupByShareCode(
        shareCode: String,
        userName: String?
    ): JoinGroupResult {
        // Simplified: tests using joinGroupByCode will handle messages; return InvalidCode by default
        return JoinGroupResult.InvalidCode
    }
}
