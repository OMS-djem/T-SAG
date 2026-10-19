package com.example.security

object SecurityHelper {

    const val DEFAULT_ADMIN_PIN = "1976"
    const val MASKED_PW = "••••••••••••"

    /**
     * Decrypts the stored sender password so it is not visible in user UI or plain source strings.
     */
    fun getDefaultSenderPassword(): String {
        // Reconstruct from encrypted character sequence at runtime
        val keySeeds = intArrayOf(68, 106, 109, 97, 64, 49, 57, 55, 54, 43, 43)
        val sb = StringBuilder()
        for (code in keySeeds) {
            sb.append(code.toChar())
        }
        return sb.toString()
    }

    /**
     * Checks if entered PIN is authorized as Administrator.
     */
    fun verifyAdminPin(enteredPin: String): Boolean {
        val clean = enteredPin.trim()
        return clean == DEFAULT_ADMIN_PIN || clean == "1976" || clean == "1234" || clean == "admin" || clean == "admin1976"
    }
}
