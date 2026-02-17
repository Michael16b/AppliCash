package fr.univ.nantes.domain.profil

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
    suspend fun clearProfile()
}

