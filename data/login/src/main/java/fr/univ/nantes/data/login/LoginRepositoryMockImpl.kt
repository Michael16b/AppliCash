package fr.univ.nantes.data.login

import fr.univ.nantes.domain.login.LoginRepository

class LoginRepositoryMockImpl : LoginRepository {

    override suspend fun authenticateUser(username: String, password: String): List<String> {
        return when {
            username != "admin" -> listOf()
            password != "admin" -> listOf("")
            else -> listOf(
                "MIAGE qui a bien avancé le tp",
                "miage@univ-nantes.fr"
            )
        }
    }
}