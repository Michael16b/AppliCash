package fr.univ.nantes.data.currency

import android.util.Log
import fr.univ.nantes.data.currency.api.FrankfurterApi
import fr.univ.nantes.data.currency.db.ExchangeRateDao
import fr.univ.nantes.data.currency.db.ExchangeRateEntity
import kotlinx.coroutines.CancellationException

/**
 * Repository responsible for fetching and caching exchange rates.
 *
 * Uses a cache-first strategy: if a rate is cached and less than [CACHE_TTL_MS] old,
 * it is returned immediately without a network call. If the network is unavailable,
 * the last known cached rate is returned as a fallback to ensure offline display.
 */
open class CurrencyRepository(
    private val api: FrankfurterApi,
    private val dao: ExchangeRateDao
) : ICurrencyRepository {
    companion object {
        /** Cache time-to-live: 1 hour in milliseconds. */
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
        private const val TAG = "CurrencyRepository"
    }

    /**
     * Returns the exchange rate from [from] to [to].
     *
     * Returns 1.0 if [from] and [to] are the same currency.
     * Returns null only if no rate is available (neither from cache nor from network).
     */
    override suspend fun getRate(from: String, to: String): Double? {
        if (from == to) return 1.0

        val normalizedFrom = from.uppercase()
        val normalizedTo = to.uppercase()

        // Check the cache
        val lastFetch = dao.getLastFetchTime(normalizedFrom)
        val now = System.currentTimeMillis()
        val isCacheValid = lastFetch != null && (now - lastFetch) < CACHE_TTL_MS

        Log.d(TAG, "getRate($normalizedFrom -> $normalizedTo) | cacheValid=$isCacheValid lastFetch=$lastFetch")

        if (isCacheValid) {
            val cachedRate = dao.getRate(normalizedFrom, normalizedTo)
            if (cachedRate != null) {
                Log.d(TAG, "Cache hit: $normalizedFrom -> $normalizedTo = $cachedRate")
                return cachedRate
            }
        }

        // Network call
        return try {
            Log.d(TAG, "Fetching rates from network for base=$normalizedFrom")
            val response = api.getLatestRates(normalizedFrom)
            Log.d(TAG, "Network success: ${response.rates.size} rates received, date=${response.date}")
            val timestamp = now
            // Evict entries older than 24 hours
            dao.deleteOlderThan(now - CACHE_TTL_MS * 24)
            // Persist all returned rates
            val entities = response.rates.map { (target, rate) ->
                ExchangeRateEntity(
                    baseCurrency = normalizedFrom,
                    targetCurrency = target,
                    rate = rate,
                    fetchedAt = timestamp
                )
            }
            dao.insertAll(entities)
            val result = response.rates[normalizedTo]
            Log.d(TAG, "Returning rate $normalizedFrom -> $normalizedTo = $result")
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Network unavailable: fall back to the stale cached rate if any
            val stale = dao.getRate(normalizedFrom, normalizedTo)
            Log.w(TAG, "Network error, using stale cache: $normalizedFrom -> $normalizedTo = $stale", e)
            stale
        }
    }

    /**
     * Converts [amount] from currency [from] to currency [to].
     *
     * Returns null if the exchange rate is unavailable.
     */
    override suspend fun convert(amount: Double, from: String, to: String): Double? {
        val rate = getRate(from, to) ?: return null
        return amount * rate
    }
}



