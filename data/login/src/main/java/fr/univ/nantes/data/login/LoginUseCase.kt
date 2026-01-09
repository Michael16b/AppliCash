package fr.univ.nantes.data.login

class LoginUseCase(
    private val loginRepository: LoginRepository,
) {
    @Throws(
        LoginException.WrongPasswordException::class,
        LoginException.NotExistingException::class,
    )
    fun authenticateUser(
        username: String,
        password: String,
    ): User {
        val response = loginRepository.authenticateUser(username, password)
        return when {
            response.isEmpty() -> throw LoginException.NotExistingException
            response.count() == 1 && response.first() == "" -> throw LoginException.WrongPasswordException
            else ->
                User(
                    username = response[0],
                    email = response[1],
                )
        }
    }
}
