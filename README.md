# YouniScript

**Write what is yours.**

YouniScript is an offline-first personal library for writing, books, journals, memories, and ideas. Your library is stored locally; the app requires no account or internet connection. You choose when to export or back up your work.

## Features

- Rich-text pages, automatic saving, page styles, revisions, annotations, bookmarks, and cross-links
- Books and manuscripts with immersive reading, multiple reading modes, and Android Read Aloud
- Journals, collections, glossary, local media, templates, and archive tools
- Local YouniProof suggestions, a personal dictionary, and custom replacements
- Optional app lock, encrypted backups, and import/export
- Light, dark, and system themes

## Technology

Kotlin, Jetpack Compose, Material 3, Room, and Android platform APIs.

## Build

Use Android Studio with JDK 17 and Android SDK 35, or run:

```sh
./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## License and branding

YouniScript source code is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE). Android, Kotlin, Jetpack Compose, Room, and other third-party components retain their own licenses.

The YouniScript name, logo, icon, splash artwork, and other branding are not licensed under the GPL and remain the property of DeveloperCal.

## Support

If you find YouniScript useful, you can support its continued development through [Liberapay](https://liberapay.com/DeveloperCal/).
