package fr.univ.nantes.domain.login

class LoginUseCase(
    private val loginRepository: LoginRepository,
) {
    @Throws(
        LoginException.WrongPasswordException::class,
        LoginException.NotExistingException::class,
    )
    suspend fun authenticateUser(
        email: String,
        password: String,
    ): User = loginRepository.authenticate(email, password)

    @Throws(LoginException.AlreadyExistsException::class)
    suspend fun registerUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        currency: String
    ): User = loginRepository.createUser(firstName, lastName, email, password, currency)
}
