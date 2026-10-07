package com.youniscript.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import kotlin.math.min

/** PIN verifier backed by a non-exportable, app-specific Android Keystore HMAC key. */
object AppLockCrypto {
    const val IMMEDIATE = 0L
    const val ONE_MINUTE = 60_000L
    const val FIVE_MINUTES = 5 * ONE_MINUTE
    const val FIFTEEN_MINUTES = 15 * ONE_MINUTE

    private const val PREFS = "app_lock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SALT = "salt"
    private const val KEY_VERIFIER = "verifier"
    private const val KEY_TIMEOUT = "timeout_ms"
    private const val KEY_BACKGROUND_AT = "background_at"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKED_UNTIL = "locked_until"
    private const val KEY_ALIAS_PREFIX = "youniscript.app-lock.hmac."
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val HMAC = "HmacSHA256"

    fun hasPin(context: Context): Boolean = preferences(context).contains(KEY_VERIFIER)

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, false) && hasPin(context)

    fun autoLockTimeout(context: Context): Long =
        preferences(context).getLong(KEY_TIMEOUT, IMMEDIATE).coerceIn(IMMEDIATE, FIFTEEN_MINUTES)

    fun backgroundedAt(context: Context): Long = preferences(context).getLong(KEY_BACKGROUND_AT, 0L)

    fun shouldLockAfterBackground(backgroundedAt: Long, timeout: Long, now: Long): Boolean =
        backgroundedAt <= 0L || now < backgroundedAt || now - backgroundedAt >= timeout

    fun setBackgroundedAt(context: Context, timestamp: Long) {
        preferences(context).edit().putLong(KEY_BACKGROUND_AT, timestamp).apply()
    }

    fun updateTimeout(context: Context, timeout: Long) {
        require(timeout in listOf(IMMEDIATE, ONE_MINUTE, FIVE_MINUTES, FIFTEEN_MINUTES))
        preferences(context).edit().putLong(KEY_TIMEOUT, timeout).apply()
    }

    fun savePin(context: Context, pin: CharArray) {
        require(validPin(pin)) { "Use a PIN with 4 to 12 digits." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val verifier = calculateVerifier(context, pin, salt, createKey = true)
        val saved = preferences(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_VERIFIER, Base64.encodeToString(verifier, Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_BACKGROUND_AT, 0L)
            .commit()
        salt.fill(0)
        verifier.fill(0)
        check(saved) { "The PIN couldn't be saved securely." }
    }

    fun verifyPin(context: Context, pin: CharArray): Boolean {
        if (!validPin(pin)) return false
        val preferences = preferences(context)
        val now = System.currentTimeMillis()
        if (now < preferences.getLong(KEY_LOCKED_UNTIL, 0L)) return false
        val salt = preferences.getString(KEY_SALT, null)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() } ?: return false
        val expected = preferences.getString(KEY_VERIFIER, null)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() } ?: return false
        val matches = try {
            val actual = calculateVerifier(context, pin, salt, createKey = false)
            try { java.security.MessageDigest.isEqual(expected, actual) }
            finally { actual.fill(0); expected.fill(0) }
        } catch (_: Exception) {
            false
        } finally {
            salt.fill(0)
        }
        if (matches) {
            preferences.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
        } else {
            val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val cooldown = failedAttemptCooldown(attempts)
            preferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
                .putLong(KEY_LOCKED_UNTIL, if (cooldown == 0L) 0L else now + cooldown).apply()
        }
        return matches
    }

    fun clearPin(context: Context): Boolean {
        val cleared = preferences(context).edit()
            .remove(KEY_SALT)
            .remove(KEY_VERIFIER)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .putBoolean(KEY_ENABLED, false)
            .putLong(KEY_BACKGROUND_AT, 0L)
            .commit()
        if (!cleared) return false
        val alias = keyAlias(context)
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        }
        return true
    }

    fun validPin(pin: CharArray): Boolean = pin.size in 4..12 && pin.all { it in '0'..'9' }

    fun failedAttemptCooldown(attemptCount: Int): Long {
        if (attemptCount < 5) return 0L
        val doublingSteps = (attemptCount - 5).coerceAtMost(4)
        return min(30_000L shl doublingSteps, 5 * 60_000L)
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun calculateVerifier(context: Context, pin: CharArray, salt: ByteArray, createKey: Boolean): ByteArray {
        val key = getKey(context, createKey)
        val pinBytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(pin)).let { buffer ->
            ByteArray(buffer.remaining()).also(buffer::get)
        }
        val input = ByteArray(salt.size + pinBytes.size)
        salt.copyInto(input)
        pinBytes.copyInto(input, destinationOffset = salt.size)
        return try {
            Mac.getInstance(HMAC).run { init(key); doFinal(input) }
        } finally {
            pinBytes.fill(0)
            input.fill(0)
        }
    }

    private fun getKey(context: Context, create: Boolean): SecretKey {
        val alias = keyAlias(context)
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create) { "The app-lock key is unavailable." }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            ).setDigests(KeyProperties.DIGEST_SHA256).build(),
        )
        return generator.generateKey()
    }

    private fun keyAlias(context: Context) = KEY_ALIAS_PREFIX + context.packageName
}
