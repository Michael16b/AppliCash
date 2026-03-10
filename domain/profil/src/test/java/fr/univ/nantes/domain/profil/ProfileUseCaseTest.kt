package fr.univ.nantes.domain.profil

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ProfileUseCase].
 *
 * CA6: use cases must delegate correctly and expose the right return types.
 */
class ProfileUseCaseTest {

    private lateinit var repository: ProfileRepository
    private lateinit var useCase: ProfileUseCase

    private val baseProfile = Profile(
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@mail.com",
        currency = "EUR",
        isLoggedIn = true
    )

    @Before
    fun setUp() {
        repository = mock()
        useCase = ProfileUseCase(repository)
    }

    // ── observeProfile ────────────────────────────────────────────────────────

    @Test
    fun `observeProfile delegates to repository and emits the profile`() = runTest {
        whenever(repository.observeProfile()).thenReturn(flowOf(baseProfile))

        val result = useCase.observeProfile().first()

        assertEquals(baseProfile, result)
        verify(repository).observeProfile()
    }

    @Test
    fun `observeProfile emits null when no profile is logged in`() = runTest {
        whenever(repository.observeProfile()).thenReturn(flowOf(null))

        val result = useCase.observeProfile().first()

        assertNull(result)
    }

    // ── observeCurrencies ─────────────────────────────────────────────────────

    @Test
    fun `observeCurrencies delegates to repository and returns currency pairs`() = runTest {
        val currencies = listOf("EUR" to "Euro", "USD" to "US Dollar")
        val currenciesFlow = flowOf(currencies)
        whenever(repository.observeCurrencies()).thenReturn(currenciesFlow)

        val result = useCase.observeCurrencies().first()

        assertEquals(2, result.size)
        assertTrue(result.contains("EUR" to "Euro"))
        verify(repository).observeCurrencies()
    }

    @Test
    fun `observeCurrencies returns empty list when no currencies stored`() = runTest {
        val emptyFlow = flowOf(emptyList<Pair<String, String>>())
        whenever(repository.observeCurrencies()).thenReturn(emptyFlow)

        val result = useCase.observeCurrencies().first()

        assertTrue(result.isEmpty())
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    fun `save delegates to repository saveProfile`() = runTest {
        useCase.save(baseProfile)
        verify(repository).saveProfile(baseProfile)
    }

    @Test
    fun `save passes the exact profile object to repository`() = runTest {
        val profile = Profile(
            firstName = "Bob",
            lastName = "Martin",
            email = "bob@mail.com",
            currency = "USD",
            isLoggedIn = false
        )
        useCase.save(profile)
        verify(repository).saveProfile(profile)
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    fun `clear delegates to repository clearProfile`() = runTest {
        useCase.clear()
        verify(repository).clearProfile()
    }

    // ── isLoggedIn ────────────────────────────────────────────────────────────

    @Test
    fun `isLoggedIn returns true when repository returns true`() = runTest {
        whenever(repository.isLoggedIn()).thenReturn(true)

        assertTrue(useCase.isLoggedIn())
        verify(repository).isLoggedIn()
    }

    @Test
    fun `isLoggedIn returns false when repository returns false`() = runTest {
        whenever(repository.isLoggedIn()).thenReturn(false)

        assertFalse(useCase.isLoggedIn())
    }
}
