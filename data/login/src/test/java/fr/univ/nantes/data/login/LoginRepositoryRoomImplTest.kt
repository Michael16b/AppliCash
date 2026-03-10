package fr.univ.nantes.data.login

import fr.univ.nantes.core.security.PasswordHasher
import fr.univ.nantes.data.profil.ProfileDao
import fr.univ.nantes.data.profil.ProfileEntity
import fr.univ.nantes.domain.login.LoginException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for LoginRepositoryRoomImpl.
 * Uses a mocked ProfileDao and the real PasswordHasher (PBKDF2),
 * since it is a standard crypto utility with no Android dependency.
 */
class LoginRepositoryRoomImplTest {

    private lateinit var profileDao: ProfileDao
    private lateinit var repository: LoginRepositoryRoomImpl

    // Base entity for reuse
    private val validEmail = "alice@mail.com"
    private val validPassword = "SecurePass1!"
    private lateinit var hashedPassword: String
    private lateinit var baseProfile: ProfileEntity

    @Before
    fun setUp() {
        profileDao = mock()
        repository = LoginRepositoryRoomImpl(profileDao)
        hashedPassword = PasswordHasher.hashPassword(validPassword)
        baseProfile = ProfileEntity(
            id = 1,
            firstName = "Alice",
            lastName = "Dupont",
            email = validEmail,
            currency = "EUR",
            password = hashedPassword,
            isLoggedIn = false
        )
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    fun `authenticate with valid credentials returns the user`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        val user = repository.authenticate(validEmail, validPassword)

        assertNotNull(user)
        assertEquals("Alice", user.username)
        assertEquals(validEmail, user.email)
    }

    @Test
    fun `authenticate calls logout then upsert with isLoggedIn set to true`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)
        val captor = argumentCaptor<ProfileEntity>()

        repository.authenticate(validEmail, validPassword)

        verify(profileDao).logout()
        verify(profileDao).upsert(captor.capture())
        assertEquals(true, captor.firstValue.isLoggedIn)
    }

    @Test
    fun `authenticate with unknown email throws NotExistingException`() = runTest {
        whenever(profileDao.findByEmail("unknown@mail.com")).thenReturn(null)

        try {
            repository.authenticate("unknown@mail.com", validPassword)
            fail("NotExistingException expected")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
    }

    @Test
    fun `authenticate with wrong password throws WrongPasswordException`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        try {
            repository.authenticate(validEmail, "wrongPassword")
            fail("WrongPasswordException expected")
        } catch (e: LoginException.WrongPasswordException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
    }

    @Test
    fun `authenticate username is firstName when firstName is not blank`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        val user = repository.authenticate(validEmail, validPassword)

        assertEquals("Alice", user.username)
    }

    @Test
    fun `authenticate username is email when firstName is blank`() = runTest {
        val profileWithBlankName = baseProfile.copy(firstName = "")
        whenever(profileDao.findByEmail(validEmail)).thenReturn(profileWithBlankName)

        val user = repository.authenticate(validEmail, validPassword)

        assertEquals(validEmail, user.username)
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    fun `createUser with a new email creates the user`() = runTest {
        whenever(profileDao.findByEmail("new@mail.com")).thenReturn(null)

        val user = repository.createUser("Bob", "Martin", "new@mail.com", "pass123", "USD")

        assertNotNull(user)
        assertEquals("Bob", user.username)
        assertEquals("new@mail.com", user.email)
    }

    @Test
    fun `createUser calls logout then upsert with isLoggedIn set to true`() = runTest {
        whenever(profileDao.findByEmail("new@mail.com")).thenReturn(null)
        val captor = argumentCaptor<ProfileEntity>()

        repository.createUser("Bob", "Martin", "new@mail.com", "pass123", "USD")

        verify(profileDao).logout()
        verify(profileDao).upsert(captor.capture())
        assertEquals(true, captor.firstValue.isLoggedIn)
        assertEquals("Bob", captor.firstValue.firstName)
        assertEquals("new@mail.com", captor.firstValue.email)
        assertEquals("USD", captor.firstValue.currency)
    }

    @Test
    fun `createUser hashes the password before saving`() = runTest {
        whenever(profileDao.findByEmail("new@mail.com")).thenReturn(null)
        val captor = argumentCaptor<ProfileEntity>()

        repository.createUser("Bob", "Martin", "new@mail.com", "pass123", "USD")

        verify(profileDao).upsert(captor.capture())
        val savedPassword = captor.firstValue.password
        // Password must not be stored in plain text
        assert(savedPassword != "pass123") { "Password must not be stored in plain text" }
        assert(PasswordHasher.verifyPassword("pass123", savedPassword)) { "Hash must be verifiable" }
    }

    @Test
    fun `createUser with existing email throws AlreadyExistsException`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        try {
            repository.createUser("Alice", "Dupont", validEmail, "pass", "EUR")
            fail("AlreadyExistsException expected")
        } catch (e: LoginException.AlreadyExistsException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
        verify(profileDao, never()).upsert(any())
    }

    @Test
    fun `createUser username is firstName when firstName is not blank`() = runTest {
        whenever(profileDao.findByEmail("bob@mail.com")).thenReturn(null)

        val user = repository.createUser("Bob", "Martin", "bob@mail.com", "pass", "EUR")

        assertEquals("Bob", user.username)
    }

    @Test
    fun `createUser username is email when firstName is blank`() = runTest {
        whenever(profileDao.findByEmail("bob@mail.com")).thenReturn(null)

        val user = repository.createUser("", "Martin", "bob@mail.com", "pass", "EUR")

        assertEquals("bob@mail.com", user.username)
    }
}

