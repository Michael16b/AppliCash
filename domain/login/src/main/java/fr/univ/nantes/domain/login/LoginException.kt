package fr.univ.nantes.domain.login

sealed class LoginException : Exception() {
    data object WrongPasswordException : LoginException()

    data object NotExistingException : LoginException()
}
