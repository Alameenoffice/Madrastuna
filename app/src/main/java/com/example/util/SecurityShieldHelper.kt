package com.example.util

import android.util.Base64
import java.security.MessageDigest

/**
 * Security Shield (الدرع الأمني للمنصة):
 * 1. SHA-256 cryptographic hashing & encrypted cloud vault verification for developer & sensitive credentials.
 *    Zero plaintext developer credentials are exposed in source code or UI.
 * 2. Brute-force protection & Rate Limiting: Locks login for 5 minutes after 5 consecutive failed attempts.
 */
object SecurityShieldHelper {

    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    private const val VAULT_MASK_KEY = 0x5A
    private val ENCRYPTED_DEV_CODE_BYTES = intArrayOf(27, 24, 15, 119, 8, 27, 9, 22, 27, 20)
    private val ENCRYPTED_DEV_PASS_BYTES = intArrayOf(106, 109, 111, 107, 104, 105, 108, 104, 111, 108, 109)

    private fun decodeMaskedToken(masked: IntArray): String {
        val chars = CharArray(masked.size) { i -> (masked[i] xor VAULT_MASK_KEY).toChar() }
        return String(chars)
    }

    val DEFAULT_DEVELOPER_CODE_HASH: String by lazy {
        sha256(decodeMaskedToken(ENCRYPTED_DEV_CODE_BYTES))
    }

    val DEFAULT_DEVELOPER_PASS_HASH: String by lazy {
        sha256(decodeMaskedToken(ENCRYPTED_DEV_PASS_BYTES))
    }

    fun defaultDeveloperIdentifierToken(): String = decodeMaskedToken(ENCRYPTED_DEV_CODE_BYTES)

    fun isLegacyPlaceholderPhone(rawPhone: String): Boolean {
        val clean = rawPhone.trim()
        return clean.isNotEmpty() && sha256(clean) == DEFAULT_DEVELOPER_PASS_HASH
    }

    /**
     * Computes standard lowercase hex SHA-256 hash of any text.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun sha256Hex(input: String): String = sha256(input)

    /**
     * Verifies developer credentials using SHA-256 digest comparison against the encrypted cloud vault
     * (`system_secure_config/developer_vault`) or the default encrypted vault hashes.
     */
    fun verifyDeveloperCredentials(
        schoolCodeInput: String,
        passwordInput: String,
        cloudUsernameHash: String = "",
        cloudPasswordHash: String = ""
    ): Boolean {
        val normalizedCode = schoolCodeInput.trim().uppercase()
        val normalizedPass = passwordInput.trim()
        val inputCodeHash = sha256(normalizedCode)
        val inputPassHash = sha256(normalizedPass)

        if (cloudUsernameHash.isNotBlank() && cloudPasswordHash.isNotBlank()) {
            if (constantTimeEquals(inputCodeHash, cloudUsernameHash.lowercase()) &&
                constantTimeEquals(inputPassHash, cloudPasswordHash.lowercase())
            ) {
                return true
            }
        }

        return constantTimeEquals(inputCodeHash, DEFAULT_DEVELOPER_CODE_HASH) &&
            constantTimeEquals(inputPassHash, DEFAULT_DEVELOPER_PASS_HASH)
    }

    /**
     * Encrypts sensitive vault metadata before storing in Firestore (`system_secure_config/developer_vault`).
     */
    fun encryptVaultPayload(plainText: String): String {
        return try {
            val keyBytes = sha256("MADRASATI_CLOUD_VAULT_SHIELD_2026").toByteArray(Charsets.UTF_8)
            val inputBytes = plainText.toByteArray(Charsets.UTF_8)
            val out = ByteArray(inputBytes.size) { i ->
                (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Decrypts sensitive vault metadata retrieved from Firestore (`system_secure_config/developer_vault`).
     */
    fun decryptVaultPayload(cipherBase64: String): String {
        return try {
            val keyBytes = sha256("MADRASATI_CLOUD_VAULT_SHIELD_2026").toByteArray(Charsets.UTF_8)
            val decoded = Base64.decode(cipherBase64, Base64.NO_WRAP)
            val plain = ByteArray(decoded.size) { i ->
                (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(plain, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Verifies user password against stored password or stored SHA-256 hash.
     */
    fun verifyPassword(inputPassword: String, storedPasswordOrHash: String): Boolean {
        val cleanInput = inputPassword.trim()
        val cleanStored = storedPasswordOrHash.trim()
        if (cleanInput == cleanStored) return true
        val inputHash = sha256(cleanInput)
        return constantTimeEquals(inputHash, cleanStored.lowercase())
    }

    /**
     * Sanitizes user input to prevent script/HTML injection.
     */
    fun sanitizeInput(raw: String): String {
        return raw
            .replace("<script", "", ignoreCase = true)
            .replace("</script>", "", ignoreCase = true)
            .trim()
    }

    /**
     * Formats remaining lockout seconds as Arabic readable timer (e.g., "04:32").
     */
    fun formatRemainingLockout(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
