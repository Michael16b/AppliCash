package fr.univ.nantes.feature.login

import androidx.lifecycle.ViewModel
import fr.univ.nantes.data.login.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow

class LoginViewModel(
    private val repository: LoginRepository,
) : ViewModel() {
    val defaultUsername: String = ""
    val username = MutableStateFlow(defaultUsername)
    val password = MutableStateFlow("")

    val setUsername: (String) -> Unit = { username.value = it }

    val setPassword: (String) -> Unit = { password.value = it }

    fun onLoginClick(navigate: () -> Unit) {
        val response =
            repository.authenticateUser(
                username.value,
                password.value,
            )
        when {
            response.isEmpty() -> {}
            response.count() == 1 && response.first() == "" -> {}
            else -> navigate()
        }
    }
}
