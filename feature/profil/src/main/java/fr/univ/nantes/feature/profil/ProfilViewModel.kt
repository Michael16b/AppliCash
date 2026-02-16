package fr.univ.nantes.feature.profil

import androidx.lifecycle.ViewModel
import android.util.Patterns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// On peut garder une seule data class pour simplifier
data class ProfilState(
    val name: String = "",
    val firstname: String = "",
    val email: String = "",
)

class ProfilViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfilState())
    val state: StateFlow<ProfilState> = _state.asStateFlow()

    fun validateProfil(name: String, firstname: String, email: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (name.isBlank()) errors["name"] = "Le nom est obligatoire"
        if (firstname.isBlank()) errors["firstname"] = "Le prénom est obligatoire"
        if (email.isBlank()) {
            errors["email"] = "L'e‑mail est obligatoire"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Format d'e‑mail invalide"
        }
        return errors
    }

    fun saveProfil(name: String, firstname: String, email: String): Boolean {
        val validationErrors = validateProfil(name, firstname, email)

        if (validationErrors.isEmpty()) {
            // On met à jour le StateFlow avec les nouvelles valeurs
            _state.update { currentState ->
                currentState.copy(
                    name = name,
                    firstname = firstname,
                    email = email
                )
            }
            return true
        }
        return false
    }
}
