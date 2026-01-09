package fr.univ.nantes.domain.login

interface LoginRepository {
    // renvoie ["nom", "email"] en cas de succès
    // renvoie [] en cas de username inexistant
    // renvoie [""] en cas de mauvais password
    fun authenticateUser(username: String, password: String): List<String>
}