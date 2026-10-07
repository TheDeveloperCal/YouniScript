package com.youniscript.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockCryptoTest {
    @Test
    fun acceptsOnlyFourToTwelveDigits() {
        assertTrue(AppLockCrypto.validPin("1234".toCharArray()))
        assertTrue(AppLockCrypto.validPin("012345678901".toCharArray()))
        assertFalse(AppLockCrypto.validPin("123".toCharArray()))
        assertFalse(AppLockCrypto.validPin("1234567890123".toCharArray()))
        assertFalse(AppLockCrypto.validPin("12a4".toCharArray()))
        assertFalse(AppLockCrypto.validPin("１２３４".toCharArray()))
    }

    @Test
    fun increasesCooldownAfterRepeatedFailedAttempts() {
        assertTrue(AppLockCrypto.failedAttemptCooldown(4) == 0L)
        assertTrue(AppLockCrypto.failedAttemptCooldown(5) == 30_000L)
        assertTrue(AppLockCrypto.failedAttemptCooldown(6) == 60_000L)
        assertTrue(AppLockCrypto.failedAttemptCooldown(20) == 5 * 60_000L)
    }

    @Test
    fun respectsBackgroundTimeoutAcrossColdStarts() {
        assertTrue(AppLockCrypto.shouldLockAfterBackground(0L, AppLockCrypto.FIFTEEN_MINUTES, 1_000L))
        assertFalse(AppLockCrypto.shouldLockAfterBackground(1_000L, AppLockCrypto.FIFTEEN_MINUTES, 2_000L))
        assertTrue(AppLockCrypto.shouldLockAfterBackground(1_000L, AppLockCrypto.FIFTEEN_MINUTES, 901_000L))
        assertTrue(AppLockCrypto.shouldLockAfterBackground(1_000L, AppLockCrypto.IMMEDIATE, 1_000L))
        assertTrue(AppLockCrypto.shouldLockAfterBackground(2_000L, AppLockCrypto.FIFTEEN_MINUTES, 1_000L))
    }
}
