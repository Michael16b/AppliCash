package fr.univ.nantes.data.login

import fr.univ.nantes.domain.login.LoginException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginRepositoryMockImpl (white-box, no mocks required).
 * Covers all branches: success, NotExistingException, WrongPasswordException,
 * AlreadyExistsException.
 */
class LoginRepositoryMockImplTest {

    private lateinit var repository: LoginRepositoryMockImpl

    @Before
    fun setUp() {
        repository = LoginRepositoryMockImpl()
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    fun `authenticate with valid credentials returns the user`() = runTest {
        val user = repository.authenticate("admin@admin", "admin")

        assertNotNull(user)
        assertEquals("admin", user.username)
        assertEquals("admin@admin", user.email)
    }

    @Test
    fun `authenticate with unknown email throws NotExistingException`() = runTest {
        try {
            repository.authenticate("unknown@mail.com", "admin")
            fail("NotExistingException expected")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `authenticate with wrong password throws WrongPasswordException`() = runTest {
        try {
            repository.authenticate("admin@admin", "wrongpassword")
            fail("WrongPasswordException expected")
        } catch (e: LoginException.WrongPasswordException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `authenticate with empty email and password throws NotExistingException`() = runTest {
        try {
            repository.authenticate("", "")
            fail("NotExistingException expected")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    fun `createUser with a new email returns the user`() = runTest {
        val user = repository.createUser("Jean", "Dupont", "jean@mail.com", "pass123", "EUR")

        assertNotNull(user)
        assertEquals("Jean", user.username)
        assertEquals("jean@mail.com", user.email)
    }

    @Test
    fun `createUser with the existing admin email throws AlreadyExistsException`() = runTest {
        try {
            repository.createUser("Admin", "User", "admin@admin", "pass", "EUR")
            fail("AlreadyExistsException expected")
        } catch (e: LoginException.AlreadyExistsException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `createUser with blank firstName uses email as username`() = runTest {
        val user = repository.createUser("", "Dupont", "test@mail.com", "pass", "EUR")

        assertEquals("test@mail.com", user.username)
    }

    @Test
    fun `createUser with non-blank firstName uses firstName as username`() = runTest {
        val user = repository.createUser("Marie", "Martin", "marie@mail.com", "pass", "USD")

        assertEquals("Marie", user.username)
    }
}
