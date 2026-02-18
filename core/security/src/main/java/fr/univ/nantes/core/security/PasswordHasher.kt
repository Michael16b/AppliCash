package fr.univ.nantes.core.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure password hashing utility using PBKDF2WithHmacSHA256.
 * This provides protection against brute-force attacks through:
 * - Salt: Random per-password salt to prevent rainbow table attacks
 * - Iterations: High iteration count to slow down brute-force attempts
 * - Strong algorithm: PBKDF2 with HMAC-SHA256
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 with a random salt.
     * 
     * @param password The plain text password to hash
     * @return A string containing the salt and hash in the format: salt:hash (both hex-encoded)
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        
        val hash = pbkdf2(password, salt)
        
        return "${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    /**
     * Verifies a password against a stored hash.
     * 
     * @param password The plain text password to verify
     * @param storedHash The stored hash in the format: salt:hash
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        
        val salt = hexToBytes(parts[0])
        val expectedHash = hexToBytes(parts[1])
        val actualHash = pbkdf2(password, salt)
        
        return actualHash.contentEquals(expectedHash)
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
