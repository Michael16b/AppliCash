package fr.univ.nantes.domain.login

/**
 * Représente le résultat de l'authentification d'un utilisateur.
 *
 * Correspondance avec l'ancienne API :
 * - Success(name, email) remplaçait ["nom", "email"]
 * - UserNotFound remplaçait []
 * - InvalidPassword remplaçait [""]
 */
sealed class LoginResult {
    data class Success(val name: String, val email: String) : LoginResult()
    object UserNotFound : LoginResult()
    object InvalidPassword : LoginResult()
}

interface LoginRepository {
    // renvoie Success(nom, email) en cas de succès
    // renvoie UserNotFound en cas de username inexistant
    // renvoie InvalidPassword en cas de mauvais password
    fun authenticateUser(username: String, password: String): LoginResult
}