package fr.univ.nantes.data.profil

import fr.univ.nantes.core.security.PasswordHasher
import fr.univ.nantes.domain.profil.Profile
import fr.univ.nantes.domain.profil.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val currencyDao: CurrencyDao
) : ProfileRepository {

    private val defaultCurrencies = listOf(
        CurrencyEntity(code = "EUR", name = "Euro"),
        CurrencyEntity(code = "USD", name = "Dollar"),
        CurrencyEntity(code = "GBP", name = "Livre"),
        CurrencyEntity(code = "JPY", name = "Yen")
    )

    override fun observeProfile(): Flow<Profile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    override fun observeCurrencies(): Flow<List<String>> =
        currencyDao.observeCurrencies()
            .onStart {
                if (currencyDao.countCurrencies() == 0) {
                    currencyDao.insertAll(defaultCurrencies)
                }
            }
            .map { list -> list.map { "${it.code} - ${it.name}" } }

    override suspend fun saveProfile(profile: Profile) {
        val existing = dao.findByEmail(profile.email)
        val hashed = existing?.password ?: ""
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
}
