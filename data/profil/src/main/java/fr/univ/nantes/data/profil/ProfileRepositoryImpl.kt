package fr.univ.nantes.data.profil

import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: ProfileDao
) : ProfileRepository {

    override fun observeProfile(): Flow<Profile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    override suspend fun saveProfile(profile: Profile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun clearProfile() {
        dao.clear()
    }

    private fun ProfileEntity.toDomain(): Profile =
        Profile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency
        )

    private fun Profile.toEntity(): ProfileEntity =
        ProfileEntity(
            id = 0,
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency
        )
}

