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

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    fun onLoginClick(navigate: (String) -> Unit) {
        try {
            val user = loginUseCase.authenticateUser(
                username.value,
                password.value,
            )
            navigate(user.username)
        } catch (e: LoginException) {
            Log.d("LoginViewModel", "Authentication failed")
        }
    }
}
