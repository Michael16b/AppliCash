package fr.univ.nantes.domain.login

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [LoginUseCase].
 *
 * CA6: use cases must propagate exceptions correctly (Success/Error paths).
 *
 * Note: LoginException subtypes are Kotlin data objects that extend Throwable (not Exception).
 * Mockito treats them as checked exceptions on suspend functions, so we use
 * doAnswer { throw ... } instead of thenThrow for exception stubs.
 */
class LoginUseCaseTest {

    private lateinit var repository: LoginRepository
    private lateinit var useCase: LoginUseCase

    private val validUser = User(username = "Alice", email = "alice@mail.com")

    @Before
    fun setUp() {
        repository = mock()
        useCase = LoginUseCase(repository)
    }

    // ── authenticateUser ──────────────────────────────────────────────────────

    @Test
    fun `authenticateUser delegates to repository and returns user on success`() = runTest {
        whenever(repository.authenticate("alice@mail.com", "pass123")).thenReturn(validUser)

        val result = useCase.authenticateUser("alice@mail.com", "pass123")

        assertEquals(validUser, result)
        verify(repository).authenticate("alice@mail.com", "pass123")
    }

    @Test
    fun `CA6 - authenticateUser propagates NotExistingException from repository`() = runTest {
        doAnswer { throw LoginException.NotExistingException }
            .whenever(repository).authenticate("unknown@mail.com", "pass")

        try {
            useCase.authenticateUser("unknown@mail.com", "pass")
            fail("NotExistingException expected")
        } catch (e: LoginException.NotExistingException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `CA6 - authenticateUser propagates WrongPasswordException from repository`() = runTest {
        doAnswer { throw LoginException.WrongPasswordException }
            .whenever(repository).authenticate("alice@mail.com", "wrong")

        try {
            useCase.authenticateUser("alice@mail.com", "wrong")
            fail("WrongPasswordException expected")
        } catch (e: LoginException.WrongPasswordException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `authenticateUser returns the exact user object from repository`() = runTest {
        val expectedUser = User(username = "Bob", email = "bob@mail.com")
        whenever(repository.authenticate("bob@mail.com", "secret")).thenReturn(expectedUser)

        val result = useCase.authenticateUser("bob@mail.com", "secret")

        assertEquals("Bob", result.username)
        assertEquals("bob@mail.com", result.email)
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    fun `registerUser delegates to repository and returns user on success`() = runTest {
        whenever(
            repository.createUser("Alice", "Dupont", "alice@mail.com", "pass", "EUR")
        ).thenReturn(validUser)

        val result = useCase.registerUser("Alice", "Dupont", "alice@mail.com", "pass", "EUR")

        assertEquals(validUser, result)
        verify(repository).createUser("Alice", "Dupont", "alice@mail.com", "pass", "EUR")
    }

    @Test
    fun `CA6 - registerUser propagates AlreadyExistsException from repository`() = runTest {
        doAnswer { throw LoginException.AlreadyExistsException }
            .whenever(repository).createUser("Alice", "Dupont", "alice@mail.com", "pass", "EUR")

        try {
            useCase.registerUser("Alice", "Dupont", "alice@mail.com", "pass", "EUR")
            fail("AlreadyExistsException expected")
        } catch (e: LoginException.AlreadyExistsException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `registerUser passes all parameters correctly to repository`() = runTest {
        whenever(
            repository.createUser("Jean", "Martin", "jean@mail.com", "myPass", "USD")
        ).thenReturn(User(username = "Jean", email = "jean@mail.com"))

        useCase.registerUser("Jean", "Martin", "jean@mail.com", "myPass", "USD")

        verify(repository).createUser("Jean", "Martin", "jean@mail.com", "myPass", "USD")
    }

    @Test
    fun `registerUser returns correct username from repository`() = runTest {
        val expected = User(username = "Marie", email = "marie@mail.com")
        whenever(
            repository.createUser("Marie", "Curie", "marie@mail.com", "pass", "EUR")
        ).thenReturn(expected)

        val result = useCase.registerUser("Marie", "Curie", "marie@mail.com", "pass", "EUR")

        assertEquals("Marie", result.username)
    }
}
