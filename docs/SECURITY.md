# Security and Privacy

## Current behavior

- The manifest does not request `INTERNET`, microphone, or broad storage permissions.
- User pages are stored locally in the app's Room database.
- App appearance is stored in local preferences. Page bookmarks are ordinary local page metadata and are included in supported backup snapshots.
- Android automatic app backup is disabled. Settings offers plaintext JSON export/import and a password-encrypted `.ysbackup` archive for supported page/manuscript data and book cover images.
- The app does not add analytics, advertising, or network APIs.
- YouniProof analyzes the current editor text in-process. It makes no network requests and does not persist suggestion output; the personal dictionary and replacement rules are stored in app-private preferences.
- Create-sheet prompts use the same local page persistence as ordinary writing. They introduce no account, network, or external storage path.
- Journal metadata and entry-to-page relationships are stored in the app's Room database. Deleting a journal removes its relation rows while keeping the page records, and supported backup snapshots include both journal and entry records.
- Page deletion is soft by default. The page body and manuscript relationship remain in Room until explicit permanent deletion or confirmed Empty Trash; backup snapshots preserve the Trash flags.
- System text-to-speech runs through Android's TTS service only when a user starts Read Aloud. The manifest does not grant network access; installed voice/network behavior is provided by Android, not a YouniScript cloud service.
- Storage cleanup only considers app-private files below `files/page-media` whose canonical paths are not referenced by a `MediaAttachment`. Cleanup requires a confirmation with candidate count and size; external URIs are excluded.

## App lock

Settings can enable a 4–12 digit app PIN, change it after verifying the current PIN, turn the lock off after PIN verification, and choose immediate, 1-minute, 5-minute, or 15-minute background locking. The PIN itself is not saved: the verifier is an HMAC-SHA256 over a random salt and the PIN, keyed by a non-exportable app-specific Android Keystore key. Repeated failed PINs trigger increasing cooldowns, capped at five minutes. On Android 11 and later the lock screen can use the system biometric or device-credential prompt; older supported Android versions use the app PIN.

When the lock is configured, Android's `FLAG_SECURE` protects app content in screenshots and Recents, and the app asks for authentication after the chosen background interval. Rotation preserves the current lock state. The app lock is an access gate, not encryption: Room, media files, and plaintext JSON exports remain unencrypted at rest. The passphrase-protected `.ysbackup` uses AES-256-GCM with PBKDF2-HMAC-SHA256 and includes supported library records and media. The passphrase is not stored.

## Remaining security limits

Lock activation, PIN change/disable, auto-lock timing, biometric behavior, and Recents redaction have not yet received a physical-device behavioral test. Read Aloud, PDF/EPUB document output, and confirmed orphan cleanup also need broader verification. Forgetting the app PIN can prevent access to the library unless system authentication can unlock it; there is no account-based recovery. Rooted/compromised devices, OS-level extraction, and unencrypted database/media access are outside the protection offered by this PIN gate. Do not describe the on-device database as encrypted or the app as protected against device compromise.

Never log page text, secrets, or media. Add permission requests only at the point the user invokes a feature that needs them. Portable exports contain private writing; import validates before modifying the library and refuses ID collisions rather than overwriting existing content.
