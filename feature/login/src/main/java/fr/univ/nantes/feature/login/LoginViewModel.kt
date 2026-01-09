package fr.univ.nantes.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    val defaultUsername: String = ""
    val username = MutableStateFlow(defaultUsername)
    val password = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    val clearError: () -> Unit = { errorMessage.value = null }

    fun onLoginClick(navigate: () -> Unit) {
        try {
            errorMessage.value = null
            loginUseCase.authenticateUser(
                username.value,
                password.value,
            )
            navigate(user.username)
        } catch (e: LoginException) {
            Log.d("LoginViewModel", "Authentication failed: $e")
            errorMessage.value = when (e) {
                is LoginException.NotExistingException -> "Username does not exist"
                is LoginException.WrongPasswordException -> "Incorrect password"
            }
        }
    }
}
