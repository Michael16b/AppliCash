package fr.univ.nantes.data.currency

import fr.univ.nantes.data.currency.api.FrankfurterApi
import fr.univ.nantes.data.currency.api.FrankfurterResponse
import fr.univ.nantes.data.currency.db.ExchangeRateDao
import fr.univ.nantes.data.currency.db.ExchangeRateEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for CurrencyRepository.
 * Cache-first strategy with a 1-hour TTL:
 *   - valid cache  → no network call
 *   - expired cache → network call + cache update
 *   - network error + stale cache → fallback to stale cache
 *   - network error + no cache → null
 */
class CurrencyRepositoryTest {

    private lateinit var api: FrankfurterApi
    private lateinit var dao: ExchangeRateDao
    private lateinit var repository: CurrencyRepository

    // TTL = 1 h = 3_600_000 ms
    private val ttlMs = 60 * 60 * 1000L
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        api = mock()
        dao = mock()
        repository = CurrencyRepository(api, dao)
    }

    // ── Same currency ──────────────────────────────────────────────────────────

    @Test
    fun `getRate returns 1 0 when both currencies are identical`() = runTest {
        val result = repository.getRate("EUR", "EUR")
        assertEquals(1.0, result)
        verify(api, never()).getLatestRates(any())
        verify(dao, never()).getRate(any(), any())
    }

    @Test
    fun `getRate is case-insensitive for the same currency`() = runTest {
        // Both normalised to the same string → from == to → returns 1.0 without any DAO/API call
        val result = repository.getRate("eur", "eur")
        assertEquals(1.0, result)
        verify(api, never()).getLatestRates(any())
    }

    // ── Valid cache (< 1 h) ───────────────────────────────────────────────────

    @Test
    fun `getRate returns the cached rate when cache is valid`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (ttlMs / 2)) // 30-min-old cache
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.08)

        val result = repository.getRate("EUR", "USD")

        assertEquals(1.08, result!!, 0.001)
        verify(api, never()).getLatestRates(any())
    }

    @Test
    fun `getRate does not call the API when cache is valid`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L) // very fresh cache
        whenever(dao.getRate("EUR", "GBP")).thenReturn(0.86)

        repository.getRate("EUR", "GBP")

        verify(api, never()).getLatestRates(any())
    }

    // ── Expired cache (> 1 h) → network call ─────────────────────────────────

    @Test
    fun `getRate calls the API when cache is expired`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (ttlMs * 2)) // 2-h-old cache → expired
        val response = FrankfurterResponse(
            base = "EUR",
            date = "2026-03-10",
            rates = mapOf("USD" to 1.08, "GBP" to 0.86)
        )
        whenever(api.getLatestRates("EUR")).thenReturn(response)

        val result = repository.getRate("EUR", "USD")

        assertEquals(1.08, result!!, 0.001)
        verify(api).getLatestRates("EUR")
    }

    @Test
    fun `getRate persists the rates received from the API`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null) // no cache
        val response = FrankfurterResponse(
            base = "EUR",
            date = "2026-03-10",
            rates = mapOf("USD" to 1.08, "GBP" to 0.86, "JPY" to 162.5)
        )
        whenever(api.getLatestRates("EUR")).thenReturn(response)
        val captor = argumentCaptor<List<ExchangeRateEntity>>()

        repository.getRate("EUR", "USD")

        verify(dao).insertAll(captor.capture())
        assertEquals(3, captor.firstValue.size)
        assertTrue(captor.firstValue.all { it.baseCurrency == "EUR" })
    }

    @Test
    fun `getRate deletes entries older than 24 h when fetching`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        val response = FrankfurterResponse("EUR", "2026-03-10", mapOf("USD" to 1.08))
        whenever(api.getLatestRates("EUR")).thenReturn(response)

        repository.getRate("EUR", "USD")

        verify(dao).deleteOlderThan(any())
    }

    @Test
    fun `getRate returns null when target currency is not in the API response`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        val response = FrankfurterResponse("EUR", "2026-03-10", mapOf("USD" to 1.08))
        whenever(api.getLatestRates("EUR")).thenReturn(response)

        val result = repository.getRate("EUR", "JPY") // JPY absent from response

        assertNull(result)
    }

    // ── Network error + stale cache fallback ──────────────────────────────────

    @Test
    fun `getRate returns stale cache when network fails`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (ttlMs * 3)) // expired
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.07) // stale cache

        val result = repository.getRate("EUR", "USD")

        assertEquals(1.07, result!!, 0.001)
    }

    @Test
    fun `getRate returns null when network fails and no stale cache exists`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(null)

        val result = repository.getRate("EUR", "USD")

        assertNull(result)
    }

    // ── convert ───────────────────────────────────────────────────────────────

    @Test
    fun `convert returns the converted amount using the correct rate`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.08)

        val result = repository.convert(100.0, "EUR", "USD")

        assertEquals(108.0, result!!, 0.001)
    }

    @Test
    fun `convert returns null when the rate is unavailable`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(null)

        val result = repository.convert(50.0, "EUR", "USD")

        assertNull(result)
    }

    @Test
    fun `convert returns the same amount for identical currencies`() = runTest {
        val result = repository.convert(200.0, "USD", "USD")
        assertEquals(200.0, result!!, 0.001)
    }

    // ── getCacheAgeMinutes ────────────────────────────────────────────────────

    @Test
    fun `getCacheAgeMinutes returns null when no cache exists`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)

        val result = repository.getCacheAgeMinutes("EUR")

        assertNull(result)
    }

    @Test
    fun `getCacheAgeMinutes returns the approximate age in minutes`() = runTest {
        val thirtyMinutesAgo = now - (30 * 60 * 1000L)
        whenever(dao.getLastFetchTime("EUR")).thenReturn(thirtyMinutesAgo)

        val result = repository.getCacheAgeMinutes("EUR")

        assertNotNull(result)
        assertTrue(result!! in 29L..31L) // ±1 min tolerance
    }

    // ── getAvailableCurrencies ────────────────────────────────────────────────

    @Test
    fun `getAvailableCurrencies returns the cached currency list`() = runTest {
        // Use a valid cache to avoid triggering an unmocked API call
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "EUR")).thenReturn(1.0)
        whenever(dao.getAvailableCurrencies("EUR")).thenReturn(listOf("GBP", "JPY", "USD"))

        val result = repository.getAvailableCurrencies("EUR")

        assertEquals(listOf("GBP", "JPY", "USD"), result)
    }

    @Test
    fun `getAvailableCurrencies normalises the currency code to uppercase`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "EUR")).thenReturn(1.0)
        whenever(dao.getAvailableCurrencies("EUR")).thenReturn(listOf("USD"))

        repository.getAvailableCurrencies("eur")

        verify(dao).getAvailableCurrencies("EUR")
    }
}
