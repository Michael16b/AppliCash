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
        CurrencyEntity(code = "AUD", name = "Australian Dollar"),
        CurrencyEntity(code = "BGN", name = "Bulgarian Lev"),
        CurrencyEntity(code = "BRL", name = "Brazilian Real"),
        CurrencyEntity(code = "CAD", name = "Canadian Dollar"),
        CurrencyEntity(code = "CHF", name = "Swiss Franc"),
        CurrencyEntity(code = "CNY", name = "Chinese Renminbi Yuan"),
        CurrencyEntity(code = "CZK", name = "Czech Koruna"),
        CurrencyEntity(code = "DKK", name = "Danish Krone"),
        CurrencyEntity(code = "EUR", name = "Euro"),
        CurrencyEntity(code = "GBP", name = "British Pound"),
        CurrencyEntity(code = "HKD", name = "Hong Kong Dollar"),
        CurrencyEntity(code = "HUF", name = "Hungarian Forint"),
        CurrencyEntity(code = "IDR", name = "Indonesian Rupiah"),
        CurrencyEntity(code = "ILS", name = "Israeli New Sheqel"),
        CurrencyEntity(code = "INR", name = "Indian Rupee"),
        CurrencyEntity(code = "ISK", name = "Icelandic Króna"),
        CurrencyEntity(code = "JPY", name = "Japanese Yen"),
        CurrencyEntity(code = "KRW", name = "South Korean Won"),
        CurrencyEntity(code = "MXN", name = "Mexican Peso"),
        CurrencyEntity(code = "MYR", name = "Malaysian Ringgit"),
        CurrencyEntity(code = "NOK", name = "Norwegian Krone"),
        CurrencyEntity(code = "NZD", name = "New Zealand Dollar"),
        CurrencyEntity(code = "PHP", name = "Philippine Peso"),
        CurrencyEntity(code = "PLN", name = "Polish Złoty"),
        CurrencyEntity(code = "RON", name = "Romanian Leu"),
        CurrencyEntity(code = "SEK", name = "Swedish Krona"),
        CurrencyEntity(code = "SGD", name = "Singapore Dollar"),
        CurrencyEntity(code = "THB", name = "Thai Baht"),
        CurrencyEntity(code = "TRY", name = "Turkish Lira"),
        CurrencyEntity(code = "USD", name = "US Dollar"),
        CurrencyEntity(code = "ZAR", name = "South African Rand"),
    )

    override fun observeProfile(): Flow<Profile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    override fun observeCurrencies(): Flow<List<Pair<String, String>>> =
        currencyDao.observeCurrencies()
            .onStart {
                if (currencyDao.countCurrencies() == 0) {
                    currencyDao.insertAll(defaultCurrencies)
                }
            }
            .map { list -> list.map { it.code to it.name } }

    override suspend fun saveProfile(profile: Profile) {
        val existing = dao.getActiveProfile()
        val hashed = existing?.password ?: ""
        val entity = profile.toEntity(
            id = existing?.id ?: 0,
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
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            currency = currency,
            password = password,
            isLoggedIn = isLoggedIn
        )
}
