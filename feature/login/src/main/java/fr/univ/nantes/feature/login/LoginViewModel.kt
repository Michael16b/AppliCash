// kotlin
package fr.univ.nantes.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    val defaultUsername: String = ""
    val username = MutableStateFlow(defaultUsername)
    val password = MutableStateFlow("")

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    val clearError: () -> Unit = { _errorMessage.value = null }

    fun onLoginClick(navigate: (String) -> Unit) {
        try {
            _errorMessage.value = null
            loginUseCase.authenticateUser(
                username.value,
                password.value,
            )
            navigate(username.value)
        } catch (e: LoginException) {
            Log.d("LoginViewModel", "Authentication failed: ${e.message}")
            _errorMessage.value = when (e) {
                is LoginException.NotExistingException -> "Username does not exist"
                is LoginException.WrongPasswordException -> "Incorrect password"
            }
        }
    }
}
