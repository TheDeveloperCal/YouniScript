package com.youniscript.app.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/** Authenticated offline archive: AES-256-GCM, PBKDF2-HMAC-SHA256, random per-file salt and nonce. */
object LibraryBackupEncryption {
    private val magic = byteArrayOf(0x59, 0x53, 0x42, 0x4b, 0x01, 0x00, 0x00, 0x01) // YSBK, format 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val ITERATIONS = 310_000
    private const val MAX_ARCHIVE_BYTES = 100 * 1024 * 1024

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 12) { "Use a backup passphrase with at least 12 characters" }
        require(plain.size <= MAX_ARCHIVE_BYTES) { "The library backup is too large" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val header = magic + salt + nonce
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    fun decrypt(archive: ByteArray, password: CharArray): ByteArray {
        require(archive.size in (magic.size + SALT_BYTES + NONCE_BYTES + 16)..MAX_ARCHIVE_BYTES) {
            "The encrypted backup size is invalid"
        }
        require(archive.copyOfRange(0, magic.size).contentEquals(magic)) { "This is not a supported YouniScript encrypted backup" }
        val saltStart = magic.size
        val nonceStart = saltStart + SALT_BYTES
        val header = archive.copyOfRange(0, nonceStart + NONCE_BYTES)
        val salt = archive.copyOfRange(saltStart, nonceStart)
        val nonce = archive.copyOfRange(nonceStart, header.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        return try {
            cipher.doFinal(archive, header.size, archive.size - header.size)
        } catch (badTag: AEADBadTagException) {
            throw IllegalArgumentException("The passphrase is incorrect or the backup is damaged", badTag)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}

data class DecodedLibraryBackup(
    val snapshot: LibraryBackupSnapshot,
    val coverImages: Map<String, ByteArray>,
    val attachmentFiles: Map<String, ByteArray> = emptyMap(),
)

/** JSON manifest plus original cover bytes in a ZIP container, encrypted as one authenticated unit. */
object EncryptedLibraryBackupCodec {
    private const val MANIFEST = "library.json"
    private const val MAX_PLAIN_BYTES = 100 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 20 * 1024 * 1024
    private const val MAX_COVER_BYTES = 30 * 1024 * 1024
    private const val MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024

    fun encode(
        snapshot: LibraryBackupSnapshot,
        coverImages: Map<String, ByteArray>,
        password: CharArray,
        attachmentFiles: Map<String, ByteArray> = emptyMap(),
    ): ByteArray {
        snapshot.validate()
        val imageBookIds = snapshot.books.filter { it.coverImageUri != null }.map { it.id }.toSet()
        require(coverImages.keys == imageBookIds) { "Each referenced cover image must be included in the backup" }
        require(coverImages.values.all { it.isNotEmpty() && it.size <= MAX_COVER_BYTES }) { "A cover image is empty or too large" }
        val attachmentIds = snapshot.attachments.map { it.id }.toSet()
        require(attachmentFiles.keys == attachmentIds) { "Each page attachment must be included in the backup" }
        require(attachmentFiles.values.all { it.isNotEmpty() && it.size <= MAX_ATTACHMENT_BYTES }) { "A page attachment is empty or too large" }
        val portableSnapshot = snapshot.copy(books = snapshot.books.map { book ->
            if (book.id in coverImages) book.copy(coverImageUri = assetMarker(book.id)) else book
        }, attachments = snapshot.attachments.map { it.copy(localUri = attachmentMarker(it.id)) })
        val zipped = ByteArrayOutputStream().also { buffer ->
            ZipOutputStream(buffer).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST))
                zip.write(LibraryBackupCodec.encode(portableSnapshot).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                coverImages.forEach { (bookId, bytes) ->
                    zip.putNextEntry(ZipEntry(assetEntry(bookId)))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                attachmentFiles.forEach { (attachmentId, bytes) ->
                    zip.putNextEntry(ZipEntry(attachmentEntry(attachmentId)))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        return LibraryBackupEncryption.encrypt(zipped, password)
    }

    fun decode(encrypted: ByteArray, password: CharArray): DecodedLibraryBackup {
        val plain = LibraryBackupEncryption.decrypt(encrypted, password)
        require(plain.size <= MAX_PLAIN_BYTES) { "The uncompressed backup is too large" }
        var manifest: String? = null
        val assets = linkedMapOf<String, ByteArray>()
        val attachments = linkedMapOf<String, ByteArray>()
        var totalBytes = 0
        ZipInputStream(ByteArrayInputStream(plain)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory) { "Unexpected directory in backup" }
                val entryLimit = when {
                    entry.name == MANIFEST -> MAX_MANIFEST_BYTES
                    entry.name.startsWith("assets/media/") -> MAX_ATTACHMENT_BYTES
                    else -> MAX_COVER_BYTES
                }
                val bytes = zip.readBounded(entryLimit)
                totalBytes += bytes.size
                require(totalBytes <= MAX_PLAIN_BYTES) { "The uncompressed backup is too large" }
                when {
                    entry.name == MANIFEST -> {
                        require(manifest == null) { "The backup contains duplicate manifests" }
                        manifest = bytes.toString(Charsets.UTF_8)
                    }
                    entry.name.startsWith("assets/media/") -> {
                        val attachmentId = decodeMediaEntry(entry.name)
                        require(bytes.isNotEmpty()) { "A page attachment in this backup is empty" }
                        require(attachments.put(attachmentId, bytes) == null) { "The backup contains a duplicate attachment" }
                    }
                    entry.name.startsWith("assets/") -> {
                        val bookId = decodeAssetEntry(entry.name)
                        require(bytes.isNotEmpty()) { "A cover image in this backup is empty" }
                        require(assets.put(bookId, bytes) == null) { "The backup contains a duplicate asset" }
                    }
                    else -> throw IllegalArgumentException("The backup contains an unsupported file")
                }
                zip.closeEntry()
            }
        }
        val snapshot = LibraryBackupCodec.decode(manifest ?: error("The backup manifest is missing"))
        val bookById = snapshot.books.associateBy { it.id }
        val attachmentById = snapshot.attachments.associateBy { it.id }
        require(assets.keys.all { it in bookById }) { "A cover image has no matching book" }
        require(attachments.keys.all { it in attachmentById }) { "A page attachment has no matching record" }
        require(snapshot.books.all { book ->
            if (book.id in assets) book.coverImageUri == assetMarker(book.id)
            else book.coverImageUri == null
        }) { "The backup is missing a referenced cover image" }
        require(snapshot.attachments.all { attachment ->
            attachment.id in attachments && attachment.localUri == attachmentMarker(attachment.id)
        }) { "The backup is missing a referenced page attachment" }
        return DecodedLibraryBackup(snapshot, assets, attachments)
    }

    private fun assetMarker(bookId: String) = "youniscript-backup-asset:$bookId"
    private fun attachmentMarker(id: String) = "youniscript-backup-media:$id"
    private fun assetEntry(bookId: String) = "assets/" + Base64.getUrlEncoder().withoutPadding().encodeToString(bookId.toByteArray(Charsets.UTF_8))
    private fun attachmentEntry(id: String) = "assets/media/" + Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(Charsets.UTF_8))

    private fun decodeAssetEntry(entry: String): String {
        val encoded = entry.removePrefix("assets/")
        require(entry.startsWith("assets/") && encoded.matches(Regex("[A-Za-z0-9_-]{1,512}"))) { "Invalid asset name" }
        return String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }

    private fun decodeMediaEntry(entry: String): String {
        val encoded = entry.removePrefix("assets/media/")
        require(entry.startsWith("assets/media/") && encoded.matches(Regex("[A-Za-z0-9_-]{1,512}"))) { "Invalid attachment asset name" }
        return String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }

    private fun ZipInputStream.readBounded(limit: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val read = read(chunk)
            if (read < 0) break
            require(buffer.size() + read <= limit) { "A backup file entry is too large" }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }
}
