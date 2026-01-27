package fr.univ.nantes.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
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
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val user = loginUseCase.authenticateUser(
                    username.value,
                    password.value
                )
                navigate(user.username)
            } catch (e: LoginException) {
                Log.d("LoginViewModel", "Authentication failed: ${e::class.simpleName}")
                _errorMessage.value = when (e) {
                    is LoginException.NotExistingException -> "Username does not exist"
                    is LoginException.WrongPasswordException -> "Incorrect password"
                }
            }
        }
    }
}
