package fr.univ.nantes.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ.nantes.domain.login.LoginException
import fr.univ.nantes.domain.login.LoginUseCase
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CURRENCY = "EUR"

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val currency: String = DEFAULT_CURRENCY,
    val currencies: List<Pair<String, String>> = emptyList(),
    val isRegister: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val profileUseCase: ProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        observeCurrencies()
    }

    private fun observeCurrencies() {
        viewModelScope.launch {
            try {
                profileUseCase.observeCurrencies().collect { list ->
                    _uiState.update { state ->
                        val selected = if (state.currency == DEFAULT_CURRENCY && list.isNotEmpty()) {
                            list.firstOrNull { it.first == DEFAULT_CURRENCY }?.first
                                ?: list.first().first
                        } else {
                            state.currency
                        }
                        state.copy(currencies = list, currency = selected)
                    }
                }
            } catch (_: Exception) {
                // Keep the current state on error, BD will retry on next collect
            }
        }
    }

    fun toggleMode() = _uiState.update { it.copy(isRegister = !it.isRegister, errorMessage = null) }
    fun setEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun setFirstName(value: String) = _uiState.update { it.copy(firstName = value, errorMessage = null) }
    fun setLastName(value: String) = _uiState.update { it.copy(lastName = value, errorMessage = null) }
    fun setCurrency(value: String) = _uiState.update { it.copy(currency = value, errorMessage = null) }

    fun submit(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (!validate(state)) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val user = if (state.isRegister) {
                    loginUseCase.registerUser(
                        firstName = state.firstName.trim(),
                        lastName = state.lastName.trim(),
                        email = state.email.trim(),
                        password = state.password,
                        currency = state.currency
                    )
                } else {
                    loginUseCase.authenticateUser(state.email.trim(), state.password)
                }
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(user.username)
            } catch (e: LoginException) {
                val message = when (e) {
                    is LoginException.NotExistingException -> "Utilisateur inexistant"
                    is LoginException.WrongPasswordException -> "Mot de passe incorrect"
                    is LoginException.AlreadyExistsException -> "Un compte existe déjà avec cet email"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }

    private fun validate(state: LoginUiState): Boolean {
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(errorMessage = "Email invalide") }
            return false
        }
        if (state.password.length < 4) {
            _uiState.update { it.copy(errorMessage = "Mot de passe trop court") }
            return false
        }
        if (state.isRegister) {
            if (state.firstName.isBlank() || state.lastName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Nom et prénom requis") }
                return false
            }
        }
        return true
    }
}
