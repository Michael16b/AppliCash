package fr.univ.nantes.data.profil

import fr.univ.nantes.domain.profil.Profile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ProfileRepositoryImpl.
 * Mocks ProfileDao and CurrencyDao.
 */
class ProfileRepositoryImplTest {

    private lateinit var dao: ProfileDao
    private lateinit var currencyDao: CurrencyDao
    private lateinit var repository: ProfileRepositoryImpl

    private val baseEntity = ProfileEntity(
        id = "1",
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@mail.com",
        currency = "EUR",
        password = "hashed_pw",
        isLoggedIn = true
    )

    private val baseProfile = Profile(
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@mail.com",
        currency = "EUR",
        isLoggedIn = true
    )

    @Before
    fun setUp() {
        dao = mock()
        currencyDao = mock()
        repository = ProfileRepositoryImpl(dao, currencyDao)
    }

    // ── observeProfile ────────────────────────────────────────────────────────

    @Test
    fun `observeProfile emits null when no profile is logged in`() = runTest {
        whenever(dao.observeProfile()).thenReturn(flowOf(null))

        val result = repository.observeProfile().first()

        assertNull(result)
    }

    @Test
    fun `observeProfile emits the profile mapped from the entity`() = runTest {
        whenever(dao.observeProfile()).thenReturn(flowOf(baseEntity))

        val result = repository.observeProfile().first()

        assertEquals("Alice", result?.firstName)
        assertEquals("Dupont", result?.lastName)
        assertEquals("alice@mail.com", result?.email)
        assertEquals("EUR", result?.currency)
        assertTrue(result?.isLoggedIn == true)
    }

    @Test
    fun `observeProfile normalises the currency code to uppercase`() = runTest {
        val entityWithLowerCaseCurrency = baseEntity.copy(currency = "eur")
        whenever(dao.observeProfile()).thenReturn(flowOf(entityWithLowerCaseCurrency))

        val result = repository.observeProfile().first()

        // normalizeCurrencyCode should normalise to uppercase
        assertEquals("EUR", result?.currency)
    }

    // ── saveProfile ────────────────────────────────────────────────────────────

    @Test
    fun `saveProfile creates a new entity when no active profile exists`() = runTest {
        whenever(dao.getActiveProfile()).thenReturn(null)
        val captor = argumentCaptor<ProfileEntity>()

        repository.saveProfile(baseProfile)

        verify(dao).upsert(captor.capture())
        val saved = captor.firstValue
        assertEquals("Alice", saved.firstName)
        assertEquals("alice@mail.com", saved.email)
        assertEquals("EUR", saved.currency)
        assertEquals("", saved.password) // no password set via saveProfile for a new profile
    }

    @Test
    fun `saveProfile updates the existing profile while preserving the password`() = runTest {
        whenever(dao.getActiveProfile()).thenReturn(baseEntity)
        val captor = argumentCaptor<ProfileEntity>()
        val updatedProfile = baseProfile.copy(firstName = "Alicia")

        repository.saveProfile(updatedProfile)

        verify(dao).upsert(captor.capture())
        val saved = captor.firstValue
        assertEquals("Alicia", saved.firstName)
        assertEquals("hashed_pw", saved.password) // password preserved
        assertEquals("1", saved.id) // existing id preserved
    }

    @Test
    fun `saveProfile propagates isLoggedIn from the existing profile`() = runTest {
        whenever(dao.getActiveProfile()).thenReturn(baseEntity.copy(isLoggedIn = true))
        val captor = argumentCaptor<ProfileEntity>()
        val profileNotLoggedIn = baseProfile.copy(isLoggedIn = false)

        repository.saveProfile(profileNotLoggedIn)

        verify(dao).upsert(captor.capture())
        // isLoggedIn = false || existing.isLoggedIn = true => true
        assertTrue(captor.firstValue.isLoggedIn)
    }

    // ── clearProfile ──────────────────────────────────────────────────────────

    @Test
    fun `clearProfile calls logout on the dao`() = runTest {
        repository.clearProfile()
        verify(dao).logout()
    }

    // ── isLoggedIn ────────────────────────────────────────────────────────────

    @Test
    fun `isLoggedIn returns true when a profile is logged in`() = runTest {
        whenever(dao.loggedInCount()).thenReturn(1)

        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when no profile is logged in`() = runTest {
        whenever(dao.loggedInCount()).thenReturn(0)

        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false with a negative count`() = runTest {
        whenever(dao.loggedInCount()).thenReturn(-1)

        assertFalse(repository.isLoggedIn())
    }

    // ── observeCurrencies ─────────────────────────────────────────────────────

    @Test
    fun `observeCurrencies emits the currency list`() = runTest {
        val currencies = listOf(
            CurrencyEntity(code = "EUR", name = "Euro"),
            CurrencyEntity(code = "USD", name = "US Dollar")
        )
        whenever(currencyDao.observeCurrencies()).thenReturn(flowOf(currencies))

        val result = repository.observeCurrencies().first()

        assertEquals(2, result.size)
        assertTrue(result.contains("EUR" to "Euro"))
        assertTrue(result.contains("USD" to "US Dollar"))
    }

    @Test
    fun `observeCurrencies inserts the default currencies on start`() = runTest {
        whenever(currencyDao.observeCurrencies()).thenReturn(flowOf(emptyList()))
        val captor = argumentCaptor<List<CurrencyEntity>>()

        repository.observeCurrencies().first()

        verify(currencyDao).insertAll(captor.capture())
        // Default currency list must be non-empty and contain EUR, USD and GBP
        val inserted = captor.firstValue
        assertTrue(inserted.isNotEmpty())
        assertTrue(inserted.any { it.code == "EUR" })
        assertTrue(inserted.any { it.code == "USD" })
        assertTrue(inserted.any { it.code == "GBP" })
    }

    @Test
    fun `observeCurrencies inserts 31 default currencies`() = runTest {
        whenever(currencyDao.observeCurrencies()).thenReturn(flowOf(emptyList()))
        val captor = argumentCaptor<List<CurrencyEntity>>()

        repository.observeCurrencies().first()

        verify(currencyDao).insertAll(captor.capture())
        assertEquals(31, captor.firstValue.size)
    }

    @Test
    fun `observeCurrencies returns empty list when no currencies are stored`() = runTest {
        whenever(currencyDao.observeCurrencies()).thenReturn(flowOf(emptyList()))

        val result = repository.observeCurrencies().first()

        assertTrue(result.isEmpty())
    }

    // ── Mapping ProfileEntity <-> Profile ────────────────────────────────────

    @Test
    fun `toDomain mapping preserves all fields`() = runTest {
        whenever(dao.observeProfile()).thenReturn(flowOf(baseEntity))

        val result = repository.observeProfile().first()

        assertEquals(baseEntity.firstName, result?.firstName)
        assertEquals(baseEntity.lastName, result?.lastName)
        assertEquals(baseEntity.email, result?.email)
        assertEquals(baseEntity.isLoggedIn, result?.isLoggedIn)
    }

    @Test
    fun `toEntity mapping preserves email and names`() = runTest {
        whenever(dao.getActiveProfile()).thenReturn(null)
        val captor = argumentCaptor<ProfileEntity>()

        repository.saveProfile(
            Profile(
                firstName = "Jean",
                lastName = "Martin",
                email = "jean@mail.com",
                currency = "USD",
                isLoggedIn = true
            )
        )

        verify(dao).upsert(captor.capture())
        assertEquals("Jean", captor.firstValue.firstName)
        assertEquals("Martin", captor.firstValue.lastName)
        assertEquals("jean@mail.com", captor.firstValue.email)
        assertEquals("USD", captor.firstValue.currency)
    }
}

