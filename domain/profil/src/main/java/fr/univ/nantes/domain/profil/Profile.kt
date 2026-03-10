package fr.univ.nantes.domain.profil

data class Profile(
    val firstName: String,
    val lastName: String,
    val email: String,
    val currency: String,
    val isLoggedIn: Boolean = false
)
