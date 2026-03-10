package fr.univ.nantes.domain.profil

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [normalizeCurrencyCode] extension function.
 *
 * BR9: currency codes must be normalized to 3-letter ISO 4217 uppercase codes
 *      before being used in conversion calls.
 *
 * CA5: edge cases — blank strings, legacy formats, mixed case, extra whitespace.
 */
class CurrencyExtensionsTest {

    // ── Standard ISO codes ────────────────────────────────────────────────────

    @Test
    fun `normalizeCurrencyCode returns uppercase 3-letter code unchanged`() {
        assertEquals("EUR", "EUR".normalizeCurrencyCode())
        assertEquals("USD", "USD".normalizeCurrencyCode())
        assertEquals("GBP", "GBP".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode uppercases a lowercase code`() {
        assertEquals("EUR", "eur".normalizeCurrencyCode())
        assertEquals("USD", "usd".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode uppercases a mixed-case code`() {
        assertEquals("EUR", "Eur".normalizeCurrencyCode())
        assertEquals("GBP", "gBp".normalizeCurrencyCode())
    }

    // ── Legacy formats with full name ─────────────────────────────────────────

    @Test
    fun `normalizeCurrencyCode extracts code from legacy EUR - Euro format`() {
        assertEquals("EUR", "EUR - Euro".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode extracts code from legacy USD - Dollar format`() {
        assertEquals("USD", "USD - Dollar".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode extracts code from legacy GBP - Pound format`() {
        assertEquals("GBP", "GBP - Pound".normalizeCurrencyCode())
    }

    // ── Whitespace handling ────────────────────────────────────────────────────

    @Test
    fun `normalizeCurrencyCode trims leading and trailing whitespace`() {
        assertEquals("EUR", "  EUR  ".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode handles code with internal spaces`() {
        assertEquals("EUR", "EUR Euro".normalizeCurrencyCode())
    }

    // ── Edge cases (CA5) ──────────────────────────────────────────────────────

    @Test
    fun `normalizeCurrencyCode truncates to 3 characters when code is too long`() {
        // A value longer than 3 chars should still give the first 3 uppercased
        assertEquals("EUR", "EURX".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode handles single character input`() {
        assertEquals("E", "e".normalizeCurrencyCode())
    }

    @Test
    fun `normalizeCurrencyCode handles two character input`() {
        assertEquals("EU", "eu".normalizeCurrencyCode())
    }
}
