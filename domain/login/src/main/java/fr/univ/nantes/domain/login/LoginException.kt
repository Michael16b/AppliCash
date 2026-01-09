package fr.univ.nantes.domain.login

/**
 * Base type for all domain-level errors that can occur during the login process.
 *
 * Subclasses of this sealed class represent specific failure cases that callers
 * can handle explicitly when performing authentication.
 */
sealed class LoginException : Throwable() {
    /**
     * Thrown when a user account is found for the given credentials, but the
     * supplied password does not match the stored password for that account.
     */
    data object WrongPasswordException : LoginException()

    /**
     * Thrown when no user account exists for the provided identifier
     * (for example, username or email) during a login attempt.
     */
    data object NotExistingException : LoginException()
}
