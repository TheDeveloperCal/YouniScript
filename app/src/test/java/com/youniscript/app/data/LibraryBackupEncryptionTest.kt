package com.youniscript.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LibraryBackupEncryptionTest {
    @Test
    fun encryptsAndAuthenticatesBackupBytes() {
        val plain = "private writing in a library backup".toByteArray()
        val encrypted = LibraryBackupEncryption.encrypt(plain, "a-long-private-passphrase".toCharArray())

        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains("private writing"))
        assertArrayEquals(plain, LibraryBackupEncryption.decrypt(encrypted, "a-long-private-passphrase".toCharArray()))
    }

    @Test
    fun rejectsWrongPasswordAndModifiedCiphertext() {
        val encrypted = LibraryBackupEncryption.encrypt(byteArrayOf(1, 2, 3), "a-long-private-passphrase".toCharArray())
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupEncryption.decrypt(encrypted, "a-different-passphrase".toCharArray())
        }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupEncryption.decrypt(encrypted, "a-long-private-passphrase".toCharArray())
        }
    }

    @Test
    fun requiresAReasonablePassphraseLength() {
        assertThrows(IllegalArgumentException::class.java) {
            LibraryBackupEncryption.encrypt(byteArrayOf(1), "short".toCharArray())
        }
    }

}
