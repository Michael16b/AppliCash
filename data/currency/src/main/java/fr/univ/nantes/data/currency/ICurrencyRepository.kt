package fr.univ.nantes.data.currency

/**
 * Interface for currency conversion operations.
 * Allows for easy mocking in tests.
 */
interface ICurrencyRepository {
    /**
     * Returns the exchange rate from [from] to [to].
     * Returns 1.0 if [from] and [to] are the same currency.
     * Returns null if no rate is available.
     */
    suspend fun getRate(from: String, to: String): Double?

    /**
     * Converts [amount] from currency [from] to currency [to].
     * Returns null if the exchange rate is unavailable.
     */
    suspend fun convert(amount: Double, from: String, to: String): Double?

    /**
     * Returns the age of the cached rates for [base] currency in minutes,
     * or null if no cache exists.
     */
    suspend fun getCacheAgeMinutes(base: String): Long?
}

