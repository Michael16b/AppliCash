package fr.univ.nantes.data.login

sealed class LoginException : Throwable() {
    data object WrongPasswordException : LoginException()

    data object NotExistingException : LoginException()
}
