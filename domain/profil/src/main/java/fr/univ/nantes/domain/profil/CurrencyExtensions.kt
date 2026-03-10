package fr.univ.nantes.domain.profil

/**
 * Normalizes a raw currency string to a 3-letter ISO 4217 code.
 *
 * Handles legacy values stored before the ISO migration (e.g. "EUR - Euro", "USD - Dollar")
 * by extracting the first word and uppercasing it.
 *
 * Examples:
 * - "GBP" -> "GBP"
 * - "EUR - Euro" -> "EUR"
 * - "usd" -> "USD"
 */
fun String.normalizeCurrencyCode(): String = this.trim().split(" ", "-").first().uppercase().take(3)
