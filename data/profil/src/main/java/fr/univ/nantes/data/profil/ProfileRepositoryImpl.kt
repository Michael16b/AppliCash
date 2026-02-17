package fr.univ.nantes.data.profil

import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val currencyDao: CurrencyDao
) : ProfileRepository {

    override fun observeProfile(): Flow<Profile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    override fun observeCurrencies(): Flow<List<String>> =
        currencyDao.observeCurrencies().map { list -> list.map { "${it.code} - ${it.name}" } }

    override suspend fun saveProfile(profile: Profile) {
        val existing = dao.findByEmail(profile.email)
        val hashed = when {
            profile.passwordHash.isNotBlank() -> hashPassword(profile.passwordHash)
            existing?.password?.isNotBlank() == true -> existing.password
            else -> ""
        }
        val entity = profile.toEntity(
            password = hashed,
            isLoggedIn = profile.isLoggedIn || existing?.isLoggedIn == true
        )
        dao.upsert(entity)
    }

    override suspend fun clearProfile() {
        dao.logout()
    }

    private fun ProfileEntity.toDomain(): Profile =
        Profile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            passwordHash = password,
            isLoggedIn = isLoggedIn
        )

    private fun Profile.toEntity(password: String, isLoggedIn: Boolean): ProfileEntity =
        ProfileEntity(
            id = 0,
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            password = password,
            isLoggedIn = isLoggedIn
        )

    private fun hashPassword(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
