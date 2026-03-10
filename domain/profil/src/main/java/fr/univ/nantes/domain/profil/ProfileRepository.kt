package fr.univ.nantes.domain.profil

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    fun observeCurrencies(): Flow<List<Pair<String, String>>>
    suspend fun saveProfile(profile: Profile)
    suspend fun clearProfile()
    suspend fun isLoggedIn(): Boolean = false
}
