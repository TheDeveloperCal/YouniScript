# Build and Run

## Requirements

- JDK 17 or newer (JDK 21 is installed in the current environment).
- Android SDK with platform 35.
- Gradle 8.10.2 (pinned by the included wrapper) for Android Gradle Plugin 8.8.2.
- Network access to Google Maven, Maven Central, and the Gradle Plugin Portal for dependencies on the first build.

The app targets and compiles against API 35 and supports API 26 onward. It uses AndroidX `enableEdgeToEdge` and `WindowInsetsControllerCompat`; normal screens draw behind transparent system bars, while writing and book reading use transient immersive bars. Android 16 edge-to-edge was reviewed on an SM-A356U. It uses Kotlin 2.1.20, Compose BOM 2025.05.00, and Room 2.7.1 (database schema v8). The manifest contains no network permission.

## Build

```sh
./gradlew :app:assembleDebug
./gradlew --no-daemon --offline :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin
```

The wrapper pins Gradle 8.10.2. Its distribution and dependencies are downloaded on the first build. Ensure `GRADLE_USER_HOME` is writable.

## Run on device

```sh
adb devices -l
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.youniscript.app.debug/.MainActivity
```

The visual-development debug build uses `com.youniscript.app.debug`. `adb install -r` updates that package while retaining its app data. Do not run connected instrumentation tests during interactive visual development: the previous connected-test workflow removed the debug package and its data. Production `com.youniscript.app` must remain untouched. A manually launched `instrumentation` app uses `com.youniscript.app.instrumentation`; its database is a separate sandbox and may be used for isolated manual checks. This does not verify connected runner cleanup. Reader mode and resume preferences live in the debug package's private preferences.

## Isolated instrumentation build

The connected test target is configured as `instrumentation`, not `debug`. Its app and test APKs are isolated as `com.youniscript.app.instrumentation` and `com.youniscript.app.instrumentation.test`.

```sh
./gradlew :app:assembleInstrumentation :app:assembleInstrumentationAndroidTest
./gradlew :app:connectedInstrumentationAndroidTest
```

The APKs and generated manifest were checked locally to confirm these IDs. The connected task has not been run with this new variant; do not use it during visual iteration. Keep visual install/launch operations on `app-debug.apk` and `com.youniscript.app.debug`. Local validation commands that do not connect to a device:

```sh
./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin
```

Settings offers system document-picker flows for plaintext JSON export/import and password-encrypted `.ysbackup` export/restore. The encrypted archive packages supported page/manuscript records and book cover images. Import adds records only when IDs do not conflict; it does not replace the current Library. On-device verification of the encrypted cover-image round trip is pending.

Appearance and default page-style preferences are stored locally. The latter applies to new independent pages; books retain their own style inheritance.

YouniProof's enabled checks, personal dictionary, and exact-word replacement pairs are stored in the app's private preferences. Its suggestions are generated locally by a small deterministic rule set; no language model, network request, or extra dependency is used. “Replace” applies only the selected suggestion, and the editor's Undo restores that edit.

The Library plus button opens a Create sheet for blank pages, quick thoughts, journal-entry prompts, letters, quotes, dreams, and books. Writing prompts create regular pages with type tags so all content continues to use the existing editor and local persistence.

Journals are persisted in Room schema version 8. Create one from the Library Create sheet, open it to browse dated entries, and use the existing editor for entry text and styles. Entry links and journal metadata are included in the JSON and encrypted backup snapshot formats.

Room schema version 7 added page Trash fields and version 8 added personal-library relationship tables. The Library Trash view allows restore, confirmed permanent deletion, and confirmed emptying. Moving a manuscript page to Trash preserves its book/chapter IDs so restoring it returns it to its original outline position.

Book reading offers scroll, paginated, focus, presentation, paper/sepia, dark, centered-line, and adaptive wide-screen spread modes. It remembers each book's position and reader mode, offers Android TTS, and uses the document picker for PDF/EPUB export. Settings also shows local database/media size estimates and confirmed app-private orphan media cleanup.
