package fr.univ.nantes.domain.login

/**
 * Repository interface for user authentication.
 *
 * Returns a list representing authentication result:
 * - [username, email] for successful authentication
 * - [] (empty list) when username doesn't exist
 * - [""] (single empty string) when password is incorrect
 */
interface LoginRepository {
    /**
     * Attempts to authenticate a user with the given credentials.
     *
     * @param username The username provided by the user.
     * @param password The password provided by the user.
     * @return A list representing the authentication result.
     */
    suspend fun authenticateUser(username: String, password: String): List<String>
}