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
 * Tests unitaires de CurrencyRepository.
 * Stratégie cache-first TTL 1h :
 *   - cache valide → pas d'appel réseau
 *   - cache expiré → appel réseau + mise en cache
 *   - erreur réseau + cache stale → fallback sur le cache
 *   - erreur réseau + pas de cache → null
 */
class CurrencyRepositoryTest {

    private lateinit var api: FrankfurterApi
    private lateinit var dao: ExchangeRateDao
    private lateinit var repository: CurrencyRepository

    // TTL = 1h = 3_600_000 ms
    private val TTL_MS = 60 * 60 * 1000L
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        api = mock()
        dao = mock()
        repository = CurrencyRepository(api, dao)
    }

    // ── Même devise ────────────────────────────────────────────────────────────

    @Test
    fun `getRate retourne 1 0 quand les deux devises sont identiques`() = runTest {
        val result = repository.getRate("EUR", "EUR")
        assertEquals(1.0, result)
        verify(api, never()).getLatestRates(any())
        verify(dao, never()).getRate(any(), any())
    }

    @Test
    fun `getRate est insensible a la casse pour la meme devise`() = runTest {
        // "eur" et "EUR" sont normalisés → from == to → retourne 1.0 sans appel DAO ni API
        val result = repository.getRate("eur", "eur")
        assertEquals(1.0, result)
        verify(api, never()).getLatestRates(any())
    }

    // ── Cache valide (< 1h) ───────────────────────────────────────────────────

    @Test
    fun `getRate retourne le taux du cache quand le cache est valide`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (TTL_MS / 2)) // cache de 30 min
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.08)

        val result = repository.getRate("EUR", "USD")

        assertEquals(1.08, result!!, 0.001)
        verify(api, never()).getLatestRates(any())
    }

    @Test
    fun `getRate n appelle pas l API quand le cache est valide`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L) // cache très frais
        whenever(dao.getRate("EUR", "GBP")).thenReturn(0.86)

        repository.getRate("EUR", "GBP")

        verify(api, never()).getLatestRates(any())
    }

    // ── Cache expiré (> 1h) → appel réseau ───────────────────────────────────

    @Test
    fun `getRate appelle l API quand le cache est expire`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (TTL_MS * 2)) // cache de 2h → expiré
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
    fun `getRate persiste les taux recus depuis l API`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null) // pas de cache
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
    fun `getRate supprime les entrees plus vieilles que 24h lors d un fetch`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        val response = FrankfurterResponse("EUR", "2026-03-10", mapOf("USD" to 1.08))
        whenever(api.getLatestRates("EUR")).thenReturn(response)

        repository.getRate("EUR", "USD")

        verify(dao).deleteOlderThan(any())
    }

    @Test
    fun `getRate retourne null quand la devise cible n est pas dans la reponse API`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        val response = FrankfurterResponse("EUR", "2026-03-10", mapOf("USD" to 1.08))
        whenever(api.getLatestRates("EUR")).thenReturn(response)

        val result = repository.getRate("EUR", "JPY") // JPY absent de la réponse

        assertNull(result)
    }

    // ── Erreur réseau + fallback cache stale ──────────────────────────────────

    @Test
    fun `getRate retourne le cache stale quand le reseau echoue`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - (TTL_MS * 3)) // expiré
        // RuntimeException car suspend fun ne déclare pas IOException (checked)
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.07) // stale cache

        val result = repository.getRate("EUR", "USD")

        assertEquals(1.07, result!!, 0.001)
    }

    @Test
    fun `getRate retourne null quand reseau echoue et pas de cache stale`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(null)

        val result = repository.getRate("EUR", "USD")

        assertNull(result)
    }

    // ── convert ───────────────────────────────────────────────────────────────

    @Test
    fun `convert retourne le montant converti avec le bon taux`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "USD")).thenReturn(1.08)

        val result = repository.convert(100.0, "EUR", "USD")

        assertEquals(108.0, result!!, 0.001)
    }

    @Test
    fun `convert retourne null quand le taux est indisponible`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)
        whenever(api.getLatestRates("EUR")).thenThrow(RuntimeException("No network"))
        whenever(dao.getRate("EUR", "USD")).thenReturn(null)

        val result = repository.convert(50.0, "EUR", "USD")

        assertNull(result)
    }

    @Test
    fun `convert retourne le montant identique pour meme devise`() = runTest {
        val result = repository.convert(200.0, "USD", "USD")
        assertEquals(200.0, result!!, 0.001)
    }

    // ── getCacheAgeMinutes ────────────────────────────────────────────────────

    @Test
    fun `getCacheAgeMinutes retourne null si pas de cache`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(null)

        val result = repository.getCacheAgeMinutes("EUR")

        assertNull(result)
    }

    @Test
    fun `getCacheAgeMinutes retourne l age approximatif en minutes`() = runTest {
        val thirtyMinutesAgo = now - (30 * 60 * 1000L)
        whenever(dao.getLastFetchTime("EUR")).thenReturn(thirtyMinutesAgo)

        val result = repository.getCacheAgeMinutes("EUR")

        assertNotNull(result)
        assertTrue(result!! in 29L..31L) // tolérance ±1 min
    }

    // ── getAvailableCurrencies ────────────────────────────────────────────────

    @Test
    fun `getAvailableCurrencies retourne les devises disponibles du cache`() = runTest {
        // Cache valide pour éviter l'appel API
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "EUR")).thenReturn(1.0)
        whenever(dao.getAvailableCurrencies("EUR")).thenReturn(listOf("GBP", "JPY", "USD"))

        val result = repository.getAvailableCurrencies("EUR")

        assertEquals(listOf("GBP", "JPY", "USD"), result)
    }

    @Test
    fun `getAvailableCurrencies normalise la devise en majuscules`() = runTest {
        whenever(dao.getLastFetchTime("EUR")).thenReturn(now - 1000L)
        whenever(dao.getRate("EUR", "EUR")).thenReturn(1.0)
        whenever(dao.getAvailableCurrencies("EUR")).thenReturn(listOf("USD"))

        repository.getAvailableCurrencies("eur")

        verify(dao).getAvailableCurrencies("EUR")
    }
}





