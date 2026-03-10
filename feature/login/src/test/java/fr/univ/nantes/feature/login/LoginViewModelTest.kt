package fr.univ.nantes.feature.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import fr.univ.nantes.domain.login.User
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [LoginViewModel].
 *
 * Robolectric is required because [LoginViewModel] uses [android.util.Patterns.EMAIL_ADDRESS].
 *
 * Business rules covered:
 *   RG11 – Forms validate before submission (email format, password length, names in register mode)
 *   RG12 – Error messages are displayed correctly for each failure case
 *   RG13 – State reloads after currency list is fetched
 *   RG14 – Authentication is required before accessing features
 *
 * CA7  – 75 %+ ViewModel coverage
 * CA8  – UI state changes (isLoading, errorMessage, isRegister) tested
 * CA9  – User errors handled correctly (NotExistingException, WrongPasswordException, AlreadyExistsException)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var profileUseCase: ProfileUseCase
    private lateinit var viewModel: LoginViewModel

    private val validUser = User(username = "Alice", email = "alice@mail.com")

    // Fake ProfileRepository that returns an empty currencies flow by default
    private fun buildProfileUseCase(
        currencies: List<Pair<String, String>> = listOf("EUR" to "Euro", "USD" to "US Dollar"),
    ): ProfileUseCase {
        val repo = object : ProfileRepository {
            override fun observeProfile(): Flow<Profile?> = flowOf(null)
            override fun observeCurrencies(): Flow<List<Pair<String, String>>> = flowOf(currencies)
            override suspend fun saveProfile(profile: Profile) = Unit
            override suspend fun clearProfile() = Unit
            override suspend fun isLoggedIn(): Boolean = false
        }
        return ProfileUseCase(repo)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mock()
        profileUseCase = buildProfileUseCase()
        viewModel = LoginViewModel(loginUseCase, profileUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty fields and login mode`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isRegister)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `CA13 - currencies are loaded on init`() = runTest {
        advanceUntilIdle()
        val currencies = viewModel.uiState.value.currencies
        assertTrue(currencies.isNotEmpty())
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    @Test
    fun `setEmail updates email in state and clears error`() {
        viewModel.setEmail("test@mail.com")
        assertEquals("test@mail.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `setPassword updates password in state and clears error`() {
        viewModel.setPassword("secret")
        assertEquals("secret", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `setFirstName updates firstName in state`() {
        viewModel.setFirstName("Alice")
        assertEquals("Alice", viewModel.uiState.value.firstName)
    }

    @Test
    fun `setLastName updates lastName in state`() {
        viewModel.setLastName("Dupont")
        assertEquals("Dupont", viewModel.uiState.value.lastName)
    }

    @Test
    fun `setCurrency updates currency in state`() {
        viewModel.setCurrency("USD")
        assertEquals("USD", viewModel.uiState.value.currency)
    }

    // ── CA8 - toggleMode ──────────────────────────────────────────────────────

    @Test
    fun `CA8 - toggleMode switches from login to register mode`() {
        assertFalse(viewModel.uiState.value.isRegister)
        viewModel.toggleMode()
        assertTrue(viewModel.uiState.value.isRegister)
    }

    @Test
    fun `CA8 - toggleMode clears errorMessage`() = runTest {
        // Trigger a validation error first
        viewModel.setEmail("bad")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.toggleMode()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `CA8 - toggleMode back to login mode from register`() {
        viewModel.toggleMode() // → register
        viewModel.toggleMode() // → login
        assertFalse(viewModel.uiState.value.isRegister)
    }

    // ── RG11 - Validation in login mode ──────────────────────────────────────

    @Test
    fun `RG11 - submit with blank email shows error and does not call useCase`() = runTest {
        viewModel.setEmail("")
        viewModel.setPassword("password")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        verify(loginUseCase, never()).authenticateUser(any(), any())
    }

    @Test
    fun `RG11 - submit with invalid email format shows error`() = runTest {
        viewModel.setEmail("not-an-email")
        viewModel.setPassword("password")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        verify(loginUseCase, never()).authenticateUser(any(), any())
    }

    @Test
    fun `RG11 - submit with password shorter than 4 chars shows error`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("abc")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        verify(loginUseCase, never()).authenticateUser(any(), any())
    }

    // ── RG11 - Validation in register mode ───────────────────────────────────

    @Test
    fun `RG11 - register with blank firstName shows error`() = runTest {
        viewModel.toggleMode()
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        viewModel.setFirstName("")
        viewModel.setLastName("Dupont")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        verify(loginUseCase, never()).registerUser(any(), any(), any(), any(), any())
    }

    @Test
    fun `RG11 - register with blank lastName shows error`() = runTest {
        viewModel.toggleMode()
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        viewModel.setFirstName("Alice")
        viewModel.setLastName("")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)
        verify(loginUseCase, never()).registerUser(any(), any(), any(), any(), any())
    }

    // ── CA8 - isLoading state ─────────────────────────────────────────────────

    @Test
    fun `CA8 - isLoading is false after successful login`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        whenever(loginUseCase.authenticateUser("alice@mail.com", "password")).thenReturn(validUser)

        viewModel.submit {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `CA8 - isLoading is false after failed login`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        doAnswer { throw LoginException.WrongPasswordException }
            .whenever(loginUseCase).authenticateUser("alice@mail.com", "password")

        viewModel.submit {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ── CA6/CA9 - Error propagation ───────────────────────────────────────────

    @Test
    fun `CA9 - RG12 - NotExistingException sets error message`() = runTest {
        viewModel.setEmail("unknown@mail.com")
        viewModel.setPassword("password")
        doAnswer { throw LoginException.NotExistingException }
            .whenever(loginUseCase).authenticateUser("unknown@mail.com", "password")

        viewModel.submit {}
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `CA9 - RG12 - WrongPasswordException sets error message`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("wrongpass")
        doAnswer { throw LoginException.WrongPasswordException }
            .whenever(loginUseCase).authenticateUser("alice@mail.com", "wrongpass")

        viewModel.submit {}
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `CA9 - RG12 - AlreadyExistsException sets error message in register mode`() = runTest {
        viewModel.toggleMode()
        viewModel.setEmail("existing@mail.com")
        viewModel.setPassword("password")
        viewModel.setFirstName("Alice")
        viewModel.setLastName("Dupont")
        doAnswer { throw LoginException.AlreadyExistsException }
            .whenever(loginUseCase).registerUser("Alice", "Dupont", "existing@mail.com", "password", "EUR")

        viewModel.submit {}
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    // ── RG14 - Successful authentication ─────────────────────────────────────

    @Test
    fun `RG14 - successful login calls onSuccess with username`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        whenever(loginUseCase.authenticateUser("alice@mail.com", "password")).thenReturn(validUser)

        var receivedUsername: String? = null
        viewModel.submit { username -> receivedUsername = username }
        advanceUntilIdle()

        assertEquals("Alice", receivedUsername)
    }

    @Test
    fun `RG14 - successful register calls onSuccess with username`() = runTest {
        viewModel.toggleMode()
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("password")
        viewModel.setFirstName("Alice")
        viewModel.setLastName("Dupont")
        whenever(
            loginUseCase.registerUser("Alice", "Dupont", "alice@mail.com", "password", "EUR"),
        ).thenReturn(validUser)

        var receivedUsername: String? = null
        viewModel.submit { username -> receivedUsername = username }
        advanceUntilIdle()

        assertEquals("Alice", receivedUsername)
    }

    @Test
    fun `RG14 - failed login does not call onSuccess`() = runTest {
        viewModel.setEmail("alice@mail.com")
        viewModel.setPassword("wrongpass")
        doAnswer { throw LoginException.WrongPasswordException }
            .whenever(loginUseCase).authenticateUser("alice@mail.com", "wrongpass")

        var successCalled = false
        viewModel.submit { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
    }

    // ── Error cleared on field change ─────────────────────────────────────────

    @Test
    fun `error is cleared when email changes after an error`() = runTest {
        viewModel.setEmail("bad")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.setEmail("new@mail.com")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `error is cleared when password changes after an error`() = runTest {
        viewModel.setEmail("bad")
        viewModel.submit {}
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.setPassword("newpass")
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

