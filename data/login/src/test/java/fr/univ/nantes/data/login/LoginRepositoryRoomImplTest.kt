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
 * Tests unitaires de LoginRepositoryRoomImpl.
 * Utilise un mock de ProfileDao et le vrai PasswordHasher (PBKDF2)
 * car c'est une crypto standard sans dépendance Android.
 */
class LoginRepositoryRoomImplTest {

    private lateinit var profileDao: ProfileDao
    private lateinit var repository: LoginRepositoryRoomImpl

    // Entité de base réutilisable
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
    fun `authenticate avec credentials valides retourne l utilisateur`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        val user = repository.authenticate(validEmail, validPassword)

        assertNotNull(user)
        assertEquals("Alice", user.username)
        assertEquals(validEmail, user.email)
    }

    @Test
    fun `authenticate appelle logout puis upsert avec isLoggedIn=true`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)
        val captor = argumentCaptor<ProfileEntity>()

        repository.authenticate(validEmail, validPassword)

        verify(profileDao).logout()
        verify(profileDao).upsert(captor.capture())
        assertEquals(true, captor.firstValue.isLoggedIn)
    }

    @Test
    fun `authenticate avec email inconnu leve NotExistingException`() = runTest {
        whenever(profileDao.findByEmail("unknown@mail.com")).thenReturn(null)

        try {
            repository.authenticate("unknown@mail.com", validPassword)
            fail("NotExistingException attendue")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
    }

    @Test
    fun `authenticate avec mauvais mot de passe leve WrongPasswordException`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        try {
            repository.authenticate(validEmail, "mauvaisMotDePasse")
            fail("WrongPasswordException attendue")
        } catch (e: LoginException.WrongPasswordException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
    }

    @Test
    fun `authenticate username est le firstName si non vide`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        val user = repository.authenticate(validEmail, validPassword)

        assertEquals("Alice", user.username)
    }

    @Test
    fun `authenticate username est l email si firstName est vide`() = runTest {
        val profileWithBlankName = baseProfile.copy(firstName = "")
        whenever(profileDao.findByEmail(validEmail)).thenReturn(profileWithBlankName)

        val user = repository.authenticate(validEmail, validPassword)

        assertEquals(validEmail, user.username)
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    fun `createUser avec nouvel email cree l utilisateur`() = runTest {
        whenever(profileDao.findByEmail("new@mail.com")).thenReturn(null)

        val user = repository.createUser("Bob", "Martin", "new@mail.com", "pass123", "USD")

        assertNotNull(user)
        assertEquals("Bob", user.username)
        assertEquals("new@mail.com", user.email)
    }

    @Test
    fun `createUser appelle logout puis upsert avec isLoggedIn=true`() = runTest {
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
    fun `createUser hache le mot de passe avant de l enregistrer`() = runTest {
        whenever(profileDao.findByEmail("new@mail.com")).thenReturn(null)
        val captor = argumentCaptor<ProfileEntity>()

        repository.createUser("Bob", "Martin", "new@mail.com", "pass123", "USD")

        verify(profileDao).upsert(captor.capture())
        val savedPassword = captor.firstValue.password
        // Le mot de passe doit être haché (non stocké en clair)
        assert(savedPassword != "pass123") { "Le mot de passe ne doit pas être stocké en clair" }
        assert(PasswordHasher.verifyPassword("pass123", savedPassword)) { "Le hash doit être vérifiable" }
    }

    @Test
    fun `createUser avec email existant leve AlreadyExistsException`() = runTest {
        whenever(profileDao.findByEmail(validEmail)).thenReturn(baseProfile)

        try {
            repository.createUser("Alice", "Dupont", validEmail, "pass", "EUR")
            fail("AlreadyExistsException attendue")
        } catch (e: LoginException.AlreadyExistsException) {
            assertNotNull(e)
        }
        verify(profileDao, never()).logout()
        verify(profileDao, never()).upsert(any())
    }

    @Test
    fun `createUser username est le firstName si non vide`() = runTest {
        whenever(profileDao.findByEmail("bob@mail.com")).thenReturn(null)

        val user = repository.createUser("Bob", "Martin", "bob@mail.com", "pass", "EUR")

        assertEquals("Bob", user.username)
    }

    @Test
    fun `createUser username est l email si firstName est vide`() = runTest {
        whenever(profileDao.findByEmail("bob@mail.com")).thenReturn(null)

        val user = repository.createUser("", "Martin", "bob@mail.com", "pass", "EUR")

        assertEquals("bob@mail.com", user.username)
    }
}

