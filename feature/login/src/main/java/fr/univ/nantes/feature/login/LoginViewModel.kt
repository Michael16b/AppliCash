package fr.univ.nantes.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import fr.univ.nantes.domain.login.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    val defaultUsername: String = ""
    val username = MutableStateFlow(defaultUsername)
    val password = MutableStateFlow("")

    private val _authenticatedUser = MutableStateFlow<User?>(null)
    val authenticatedUser: StateFlow<User?> = _authenticatedUser.asStateFlow()

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    fun onLoginClick(navigate: () -> Unit) {
        try {
            val user = loginUseCase.authenticateUser(
                username.value,
                password.value,
            )
            _authenticatedUser.value = user
            navigate()
        } catch (e: LoginException) {
            Log.d("LoginViewModel", "Authentication failed")
        }
    }
}
