package fr.univ.nantes.domain.login

/**
 * Repository interface for user authentication and management.
 */
interface LoginRepository {
    /**
     * Attempts to authenticate a user with the given email and password.
     *
     * @param email The email provided by the user.
     * @param password The password provided by the user.
     * @return A [User] object representing the authenticated user.
     */
    suspend fun authenticate(email: String, password: String): User

    /**
     * Creates a new user with the given details.
     *
     * @param firstName The first name of the user.
     * @param lastName The last name of the user.
     * @param email The email of the user.
     * @param password The password for the user account.
     * @param currency The currency preference of the user.
     * @return A [User] object representing the newly created user.
     */
    suspend fun createUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        currency: String
    ): User
}
