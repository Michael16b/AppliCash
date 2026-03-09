package fr.univ.nantes.feature.profil

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CURRENCY = "EUR - Euro"

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val currency: String = DEFAULT_CURRENCY,
    val currencies: List<String> = emptyList(),
    val isExistingProfile: Boolean = false,
    val isLoading: Boolean = true,
    val errors: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false,
    val shouldRedirectLogin: Boolean = false
)

class ProfilViewModel(
    private val profileUseCase: ProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeCurrencies()
    }

    private fun observeCurrencies() {
        viewModelScope.launch {
            try {
                profileUseCase.observeCurrencies().collect { currencies ->
                    _uiState.update { state ->
                        val selected = if (state.currency == DEFAULT_CURRENCY && currencies.isNotEmpty()) {
                            currencies.first()
                        } else state.currency
                        state.copy(currencies = currencies, currency = selected)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // En cas d'erreur, utiliser la devise par défaut
                _uiState.update { it.copy(currencies = emptyList(), currency = DEFAULT_CURRENCY) }
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            try {
                profileUseCase.observeProfile().collect { profile ->
                    if (profile == null) {
                        _uiState.update { it.copy(isExistingProfile = false, isLoading = false, shouldRedirectLogin = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                firstName = profile.firstName,
                                lastName = profile.lastName,
                                email = profile.email,
                                currency = profile.currency,
                                isExistingProfile = true,
                                isLoading = false,
                                errors = emptyMap(),
                                shouldRedirectLogin = false
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isExistingProfile = false, isLoading = false, shouldRedirectLogin = true) }
            }
        }
    }

    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currency = value) }

    fun saveProfile() {
        val current = _uiState.value
        val errors = validate(current)

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            try {
                profileUseCase.save(
                    Profile(
                        firstName = current.firstName.trim(),
                        lastName = current.lastName.trim(),
                        email = current.email.trim(),
                        currency = current.currency,
                        isLoggedIn = true
                    )
                )
                _uiState.update { it.copy(isExistingProfile = true, errors = emptyMap(), saveSuccess = true) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun redirectToLogin(onLogout: () -> Unit) {
        performLogout(resetUiState = false, onLogout = onLogout)
    }

    fun logout(onLogout: () -> Unit) {
        performLogout(resetUiState = true, onLogout = onLogout)
    }

    private fun performLogout(resetUiState: Boolean, onLogout: () -> Unit) {
        viewModelScope.launch {
            profileUseCase.clear()
            _uiState.update {
                if (resetUiState) {
                    ProfileUiState(isExistingProfile = false, isLoading = false)
                } else {
                    it.copy(shouldRedirectLogin = false)
                }
            }
            onLogout()
        }
    }

    private fun validate(state: ProfileUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (state.firstName.isBlank()) errors["firstName"] = "Le prénom est obligatoire"
        if (state.lastName.isBlank()) errors["lastName"] = "Le nom est obligatoire"
        if (state.email.isBlank()) {
            errors["email"] = "L'email est obligatoire"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            errors["email"] = "Format d'email invalide"
        }
        return errors
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
