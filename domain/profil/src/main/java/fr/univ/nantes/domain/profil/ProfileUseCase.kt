package fr.univ.nantes.domain.profil

class ProfileUseCase(private val repository: ProfileRepository) {
    fun observeProfile() = repository.observeProfile()
    suspend fun save(profile: Profile) = repository.saveProfile(profile)
    suspend fun clear() = repository.clearProfile()
}

