package com.securityguard.app.core.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val prefs = context.getSharedPreferences("secure_pin", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun hasPin(): Boolean = prefs.contains("hash") && prefs.contains("salt")

    fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all(Char::isDigit))
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = derive(pin, salt)
        prefs.edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString("salt", null) ?: return false
        val expected = prefs.getString("hash", null) ?: return false
        val actual = derive(pin, Base64.decode(salt, Base64.NO_WRAP))
        return MessageDigest.isEqual(actual, Base64.decode(expected, Base64.NO_WRAP))
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
