package fr.univ.nantes.domain.login

sealed class LoginException : Throwable() {
    data object WrongPasswordException : LoginException()

    data object NotExistingException : LoginException()
}
