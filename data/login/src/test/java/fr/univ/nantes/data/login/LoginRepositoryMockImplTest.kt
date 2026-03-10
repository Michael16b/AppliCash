package fr.univ.nantes.data.login

import fr.univ.nantes.domain.login.LoginException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires de LoginRepositoryMockImpl (boîte blanche, sans mock).
 * Vérifie toutes les branches : succès, NotExistingException, WrongPasswordException,
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
    fun `authenticate avec credentials valides retourne l utilisateur`() = runTest {
        val user = repository.authenticate("admin@admin", "admin")

        assertNotNull(user)
        assertEquals("admin", user.username)
        assertEquals("admin@admin", user.email)
    }

    @Test
    fun `authenticate avec email inconnu leve NotExistingException`() = runTest {
        try {
            repository.authenticate("unknown@mail.com", "admin")
            fail("NotExistingException attendue")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `authenticate avec mauvais mot de passe leve WrongPasswordException`() = runTest {
        try {
            repository.authenticate("admin@admin", "wrongpassword")
            fail("WrongPasswordException attendue")
        } catch (e: LoginException.WrongPasswordException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `authenticate avec email et mot de passe vides leve NotExistingException`() = runTest {
        try {
            repository.authenticate("", "")
            fail("NotExistingException attendue")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    fun `createUser avec nouvel email retourne l utilisateur`() = runTest {
        val user = repository.createUser("Jean", "Dupont", "jean@mail.com", "pass123", "EUR")

        assertNotNull(user)
        assertEquals("Jean", user.username)
        assertEquals("jean@mail.com", user.email)
    }

    @Test
    fun `createUser avec email admin existant leve AlreadyExistsException`() = runTest {
        try {
            repository.createUser("Admin", "User", "admin@admin", "pass", "EUR")
            fail("AlreadyExistsException attendue")
        } catch (e: LoginException.AlreadyExistsException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `createUser avec firstName vide utilise l email comme username`() = runTest {
        val user = repository.createUser("", "Dupont", "test@mail.com", "pass", "EUR")

        assertEquals("test@mail.com", user.username)
    }

    @Test
    fun `createUser avec firstName non vide utilise le firstName comme username`() = runTest {
        val user = repository.createUser("Marie", "Martin", "marie@mail.com", "pass", "USD")

        assertEquals("Marie", user.username)
    }
}

