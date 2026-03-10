package fr.univ.nantes.feature.profil

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ProfilViewModel].
 *
 * Robolectric is required because [ProfilViewModel] uses [android.util.Patterns.EMAIL_ADDRESS].
 *
 * Business rules covered:
 *   RG11 – Forms validate before submission (firstName, lastName, email format)
 *   RG12 – Field-level error messages displayed correctly
 *   RG13 – UI state reloads / reflects data after profile save
 *   RG14 – shouldRedirectLogin set when no profile is logged in
 *
 * CA7  – 75 %+ ViewModel coverage
 * CA8  – UI state events (saveSuccess, isLoading, shouldRedirectLogin) tested
 * CA9  – Validation errors handled per field
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ProfilViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Mutable flow so individual tests can push different profiles
    private val profileFlow = MutableStateFlow<Profile?>(null)
    private val currenciesFlow = MutableStateFlow(listOf("EUR" to "Euro", "USD" to "US Dollar"))

    private lateinit var fakeRepo: ProfileRepository
    private lateinit var useCase: ProfileUseCase
    private lateinit var viewModel: ProfilViewModel

    private val loggedInProfile = Profile(
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@mail.com",
        currency = "EUR",
        isLoggedIn = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = object : ProfileRepository {
            override fun observeProfile(): Flow<Profile?> = profileFlow
            override fun observeCurrencies(): Flow<List<Pair<String, String>>> = currenciesFlow
            override suspend fun saveProfile(profile: Profile) = Unit
            override suspend fun clearProfile() = Unit
            override suspend fun isLoggedIn(): Boolean = profileFlow.value != null
        }
        useCase = ProfileUseCase(fakeRepo)
        viewModel = ProfilViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has isLoading true`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `initial state has empty fields`() {
        val state = viewModel.uiState.value
        assertEquals("", state.firstName)
        assertEquals("", state.lastName)
        assertEquals("", state.email)
        assertTrue(state.errors.isEmpty())
    }

    // ── RG14 - Profile observation ────────────────────────────────────────────

    @Test
    fun `RG14 - null profile triggers shouldRedirectLogin`() = runTest {
        profileFlow.value = null
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldRedirectLogin)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `RG14 - logged-in profile populates fields and clears redirect`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Alice", state.firstName)
        assertEquals("Dupont", state.lastName)
        assertEquals("alice@mail.com", state.email)
        assertEquals("EUR", state.currency)
        assertTrue(state.isExistingProfile)
        assertFalse(state.isLoading)
        assertFalse(state.shouldRedirectLogin)
    }

    // ── RG13 - State reload after profile change ──────────────────────────────

    @Test
    fun `RG13 - updating profile flow refreshes state`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()
        assertEquals("Alice", viewModel.uiState.value.firstName)

        profileFlow.value = loggedInProfile.copy(firstName = "Alicia")
        advanceUntilIdle()
        assertEquals("Alicia", viewModel.uiState.value.firstName)
    }

    @Test
    fun `RG13 - currencies flow updates available currencies`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currencies.size)

        currenciesFlow.value = listOf("EUR" to "Euro", "USD" to "US Dollar", "GBP" to "Pound")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currencies.size)
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    @Test
    fun `onFirstNameChange updates firstName in state`() {
        viewModel.onFirstNameChange("Bob")
        assertEquals("Bob", viewModel.uiState.value.firstName)
    }

    @Test
    fun `onLastNameChange updates lastName in state`() {
        viewModel.onLastNameChange("Martin")
        assertEquals("Martin", viewModel.uiState.value.lastName)
    }

    @Test
    fun `onEmailChange updates email in state`() {
        viewModel.onEmailChange("bob@mail.com")
        assertEquals("bob@mail.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onCurrencyChange updates currency in state`() {
        viewModel.onCurrencyChange("USD")
        assertEquals("USD", viewModel.uiState.value.currency)
    }

    // ── RG11 / CA9 - Validation ───────────────────────────────────────────────

    @Test
    fun `RG11 - saveProfile with blank firstName sets field error`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onFirstNameChange("")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errors["firstName"])
    }

    @Test
    fun `RG11 - saveProfile with blank lastName sets field error`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onLastNameChange("")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errors["lastName"])
    }

    @Test
    fun `RG11 - saveProfile with blank email sets email error`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onEmailChange("")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errors["email"])
    }

    @Test
    fun `RG11 - saveProfile with invalid email format sets email error`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onEmailChange("not-an-email")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errors["email"])
    }

    @Test
    fun `RG11 - saveProfile with all blank fields sets multiple errors`() = runTest {
        viewModel.onFirstNameChange("")
        viewModel.onLastNameChange("")
        viewModel.onEmailChange("")
        viewModel.saveProfile()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.errors
        assertTrue(errors.containsKey("firstName"))
        assertTrue(errors.containsKey("lastName"))
        assertTrue(errors.containsKey("email"))
    }

    // ── CA8 - saveSuccess state ───────────────────────────────────────────────

    @Test
    fun `CA8 - saveProfile with valid data sets saveSuccess true`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onFirstNameChange("Alice")
        viewModel.onLastNameChange("Dupont")
        viewModel.onEmailChange("alice@mail.com")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `CA8 - clearSuccessMessage resets saveSuccess to false`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.onFirstNameChange("Alice")
        viewModel.onLastNameChange("Dupont")
        viewModel.onEmailChange("alice@mail.com")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        viewModel.clearSuccessMessage()
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    // ── RG12 - Error messages content ─────────────────────────────────────────

    @Test
    fun `RG12 - firstName error message is non-blank`() = runTest {
        viewModel.onFirstNameChange("")
        viewModel.onLastNameChange("Dupont")
        viewModel.onEmailChange("alice@mail.com")
        viewModel.saveProfile()
        advanceUntilIdle()

        val msg = viewModel.uiState.value.errors["firstName"]
        assertNotNull(msg)
        assertTrue(msg!!.isNotBlank())
    }

    @Test
    fun `RG12 - invalid email error message is non-blank`() = runTest {
        viewModel.onFirstNameChange("Alice")
        viewModel.onLastNameChange("Dupont")
        viewModel.onEmailChange("bad-email")
        viewModel.saveProfile()
        advanceUntilIdle()

        val msg = viewModel.uiState.value.errors["email"]
        assertNotNull(msg)
        assertTrue(msg!!.isNotBlank())
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout calls onLogout callback`() = runTest {
        var logoutCalled = false
        viewModel.logout { logoutCalled = true }
        advanceUntilIdle()
        assertTrue(logoutCalled)
    }

    @Test
    fun `logout resets UI state`() = runTest {
        profileFlow.value = loggedInProfile
        advanceUntilIdle()

        viewModel.logout {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isExistingProfile)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `redirectToLogin calls onLogout callback`() = runTest {
        var called = false
        viewModel.redirectToLogin { called = true }
        advanceUntilIdle()
        assertTrue(called)
    }

    @Test
    fun `redirectToLogin clears shouldRedirectLogin flag`() = runTest {
        profileFlow.value = null
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldRedirectLogin)

        viewModel.redirectToLogin {}
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.shouldRedirectLogin)
    }
}
