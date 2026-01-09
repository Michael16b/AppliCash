package fr.univ.nantes.feature.login

import androidx.lifecycle.ViewModel
import fr.univ.nantes.domain.login.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    val defaultUsername: String = ""
    val username = MutableStateFlow(defaultUsername)
    val password = MutableStateFlow("")

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    fun onLoginClick(navigate: () -> Unit) {
        try {
            loginUseCase.authenticateUser(
                username.value,
                password.value,
            )
            navigate()
        } catch (e: Exception) {
            // Gérer les erreurs d'authentification ici
        }
    }
}
