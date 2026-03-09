package fr.univ.nantes.data.profil

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
        val existing = dao.getActiveProfile()
        val hashed = existing?.password ?: ""
        val entity = profile.toEntity(
            id=existing?.id ?: 0,
            password = hashed,
            isLoggedIn = profile.isLoggedIn || existing?.isLoggedIn == true
        )
        dao.upsert(entity)
    }

    override suspend fun clearProfile() {
        dao.logout()
    }

    override suspend fun isLoggedIn(): Boolean = dao.loggedInCount() > 0

    private fun ProfileEntity.toDomain(): Profile =
        Profile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            isLoggedIn = isLoggedIn
        )

    private fun Profile.toEntity(id: Int, password: String, isLoggedIn: Boolean): ProfileEntity =
        ProfileEntity(
            id=id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            password = password,
            isLoggedIn = isLoggedIn
        )
}
