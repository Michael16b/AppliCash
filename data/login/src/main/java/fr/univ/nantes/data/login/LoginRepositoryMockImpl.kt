package fr.univ.nantes.data.login

import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginRepository
import fr.univ.nantes.domain.login.User

class LoginRepositoryMockImpl : LoginRepository {
    override suspend fun authenticate(email: String, password: String): User {
        if (email != "admin@admin") throw LoginException.NotExistingException
        if (password != "admin") throw LoginException.WrongPasswordException
        return User(username = "admin", email = email)
    }

    override suspend fun createUser(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        currency: String
    ): User {
        // simplistic mock that always fails if email already equals admin
        if (email == "admin@admin") throw LoginException.AlreadyExistsException
        return User(username = firstName.ifBlank { email }, email = email)
    }
}
