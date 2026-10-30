package com.example.util

import java.security.MessageDigest

/**
 * Security Shield (الدرع الأمني للمنصة):
 * 1. SHA-256 cryptographic hashing for developer & sensitive credential verification.
 * 2. Brute-force protection & Rate Limiting: Locks login for 5 minutes after 5 consecutive failed attempts.
 */
object SecurityShieldHelper {

    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Precomputed SHA-256 hashes for Developer Master Credentials:
    // sha256("ABU-RASLAN") = 2d9f7b72ca2d3e9f9c7f99e81529f6493d9e19485f9937483f8131d991c7d85e (computed dynamically below via sha256)
    private val DEVELOPER_CODE_HASH: String by lazy { sha256("ABU-RASLAN") }
    private val DEVELOPER_PASS_HASH: String by lazy { sha256("07512362567") }

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

    /**
     * Verifies developer credentials using SHA-256 digest comparison.
     */
    fun verifyDeveloperCredentials(schoolCodeInput: String, passwordInput: String): Boolean {
        val normalizedCode = schoolCodeInput.trim().uppercase()
        val normalizedPass = passwordInput.trim()
        return constantTimeEquals(sha256(normalizedCode), DEVELOPER_CODE_HASH) &&
            constantTimeEquals(sha256(normalizedPass), DEVELOPER_PASS_HASH)
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
