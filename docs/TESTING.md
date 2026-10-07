# Testing and Verification

## Phase 2 status — Complete, October 6, 2026

### Build and local tests

- `./gradlew :app:test :app:assembleDebug :app:compileDebugAndroidTestKotlin` — passed.
- Unit tests: seven cases in the debug variant and seven in release (formatting and autosave behavior; 14 executions total).
- Instrumentation test sources compile, including migration, formatting round-trip, duplicate/delete, ordering-after-edit, and file reopen tests. Compilation does not mean they passed on a device.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (11 MB). Gradle config retains production ID `com.youniscript.app` and appends `.debug` only for the debug variant.
- Room schema 2 is exported under `app/schemas/com.youniscript.app.data.LibraryDatabase/2.json`. The explicit 1→2 migration adds formatting/style/tags/collection/book columns with defaults and preserves page text and timestamps.

### Physical Samsung SM-A356U, Android 16

- Phase 1: install and launch succeeded; the library and editor were manually opened.
- Before the October 6, 2026 rerun, `adb devices` reported the phone as `device`, APK metadata reported `com.youniscript.app.debug`, and Gradle configuration retained `com.youniscript.app` for production while adding `.debug` to debug builds.
- The expanded `./gradlew :app:connectedDebugAndroidTest` passed all six tests on the SM-A356U running Android 16.
- The completed Phase 2 instrumentation run used the then-configured debug target and did not target the production package. Current Gradle configuration assigns future instrumentation runs to the separate `instrumentation` target.
- The user personally verified manual autosave on the physical SM-A356U: a new entry remained saved and appeared in My Library after leaving and reopening the editor. Manual autosave is accepted as PASS based on that direct device verification.
- Other manual flows such as formatting, clipboard, undo/redo, and rotation remain unverified; they are outside the autosave pass.
- At the final read-only package check, `com.youniscript.app.debug` was installed and `com.youniscript.app` was not listed. The earlier connected-test incident had already removed the production package; this Phase 2 rerun did not install, uninstall, wipe, or modify it.

### Device data incident and mitigation

The connected-test runner removed the original `com.youniscript.app` package after its earlier test run. Before cleanup, an existing page was visible in the app; no database backup had been made, so that page may have been removed with the package. Its visible title and body were both `test`. The user confirmed this was test data and explicitly requested that it not be recreated. Leave the phone's previous page untouched.

Debug builds use `com.youniscript.app.debug`; the production application ID remains `com.youniscript.app`. Before each device test, check that `adb devices` reports the intended phone as `device`, verify the APK's `.debug` application ID, and confirm the test plan will not install, uninstall, wipe, or modify the production package or its data. The October 6 rerun followed these checks and targeted only debug.

## Follow-up manual checks (not required for Phase 2 autosave completion)

The Phase 3 real-content test below covers create, multi-paragraph writing, formatting, navigation, page-style changes, and force-stop/relaunch. Clipboard, undo/redo, rotation, and long-session background/foreground remain unverified in this manual pass. Use disposable debug-package data only and never touch the production package or its data.

## Safe commands

```sh
./gradlew :app:test
./gradlew :app:assembleDebug
./gradlew :app:compileInstrumentationAndroidTestKotlin
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.youniscript.app.debug/com.youniscript.app.MainActivity
```

Do not run connected tests during visual development. The isolated `instrumentation` build type and APK IDs are documented below, but its cleanup isolation remains unverified on-device, so the configuration is not yet considered safe for connected runs. Production `com.youniscript.app` must remain untouched. Never use device-wide reset or data-wipe commands.

## Phase 3 verification — October 6, 2026

### Physical Samsung SM-A356U / Android 16

- Host ADB found `RFCXB0QR9CF` connected as Samsung SM-A356U / Android 16. The debug app package was `com.youniscript.app.debug`. `com.youniscript.app` had no installed package path before or after this work; it was not installed, uninstalled, or modified. Final package listing contained only `com.youniscript.app.debug` for the production/debug ID prefix.
- Installed updated debug APKs only with `adb install -r`, which preserved the debug database. Final debug APK size was 11 MB. APK inspection found no bundled font files or large raster assets; page and cover visuals are Compose primitives.
- Created a disposable debug page titled `Phase3 test page` with three paragraphs. Formatted paragraph one as a heading, left the editor, reopened from My Library, applied all nine styles, force-stopped the app, relaunched, and reopened the page. The Room row retained ID `50921218-fb99-4369-a8b8-a6d0849218ac`; its exact body text remained unchanged, formatting JSON retained the heading range (`s=0`, `e=120`, `kind=1`), and `pageStyleId` persisted as `dark-journal`. After review, only this disposable page was deleted. The pre-existing blank `Untitled page` remained in Modern Paper.
- Reviewed all nine rendered page styles with the real text: Modern Paper, Classic Book, Notebook, Typewriter, Parchment, Ancient Manuscript, Medieval Manuscript, Literary, and Dark Journal. Text stayed legible, the heading remained distinct, and no page content was clipped. Dark Journal's light text remained readable on the dark page, with page-aware status-bar icons. The chooser's Dark Journal name/subtitle had low contrast; this was fixed by using shell ink tokens for picker labels while keeping the page miniature dark.
- Reviewed the keyboard on the real text page. The active line could sit under the keyboard accessory row; `imePadding()` was added to the editor root. Rebuilt, reinstalled with `-r`, and confirmed the end-of-document caret stays above the IME accessory while text can scroll into view.
- Checked font scale at the device default `1.0` and temporarily at `1.3`; both showed readable text without layout clipping with the keyboard hidden. Restored the device setting to `1.0` afterward.
- Reviewed the populated Library and a fresh empty Library. To avoid clearing debug app data, the empty state was viewed by installing/launching only the isolated `com.youniscript.app.instrumentation` app APK in its previously absent package sandbox; the instrumentation test APK and test runner were not installed or run. That empty-state app package was removed afterward. Debug data was preserved.
- Accessibility hierarchy inspection exposed labels for `Create a new page`, `Formatting options`, `Page style: …`, `More page actions`, and each named style option; picker rows and dialogs have selectable/clickable semantics and visible close text. Minimum-size top controls and page rows were visually checked. TalkBack was not enabled on the device (`enabled_accessibility_services` was `null`), so spoken traversal was not tested.
- Book/journal cover components were reviewed in source for distinct layouts, template support, and descriptive semantics; they are visual foundations and are not yet displayed as live Library entries.

### Build validation and screenshots

- `./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed after the final picker contrast, accessibility-label, and IME inset changes. No connected instrumentation task was run.
- `./gradlew :app:assembleInstrumentation` — passed to build the isolated app shell for the empty-state visual check only. No test APK was installed, and no instrumentation runner was invoked.
- Screenshots: populated Library `/tmp/youniscript-phase3-populated-library.png`; empty Library `/tmp/youniscript-phase3-final-empty.png`; real text in Modern Paper `/tmp/youniscript-phase3-real-content-modern.png`; Classic Book `/tmp/youniscript-phase3-style-classic.png`; Notebook `/tmp/youniscript-phase3-style-notebook.png`; Typewriter `/tmp/youniscript-phase3-style-typewriter.png`; Parchment `/tmp/youniscript-phase3-style-parchment.png`; Ancient Manuscript `/tmp/youniscript-phase3-style-ancient-real.png`; Medieval Manuscript `/tmp/youniscript-phase3-style-medieval.png`; Literary `/tmp/youniscript-phase3-style-literary.png`; Dark Journal `/tmp/youniscript-phase3-style-dark.png`; fixed style picker `/tmp/youniscript-phase3-picker-contrast-fixed.png`; font scale 1.3 `/tmp/youniscript-phase3-font-scale-130.png`; keyboard/IME insets `/tmp/youniscript-phase3-ime-insets.png`.

### Remaining coverage limits

- The user's previous 6/6 connected tests remain the accepted Phase 2 instrumentation result. No new connected tests were run in this visual pass.
- This pass did not manually re-test clipboard, undo/redo, rotation, or extended background/foreground persistence. Spoken TalkBack traversal, reduced-motion behavior, tablets, and long-running performance remain unverified.
- The reusable book and journal visuals are not wired to Library data yet; no Book/Journal creation behavior is exposed.

## Isolated instrumentation target

The `instrumentation` build type inherits debug settings but has its own package ID, `com.youniscript.app.instrumentation`. `testBuildType = "instrumentation"` routes generated test APKs to that target, and the test APK ID is `com.youniscript.app.instrumentation.test`. Local source compilation succeeds. For the empty-Library visual review, only the isolated app APK was installed and launched in its fresh sandbox; it was then uninstalled. No test APK or instrumentation runner was used. Cleanup isolation for connected instrumentation tests remains unverified, so do not run connected tests during visual iteration. The interactive `com.youniscript.app.debug` package remains installed with its original blank page in Modern Paper.

## Phase 4 and Android 16 edge-to-edge verification — October 6, 2026

### Physical Samsung SM-A356U / Android 16

- Device ID `RFCXB0QR9CF` was connected. Navigation mode was initially three-button (`secure navigation_mode=0`). Samsung also offered gesture navigation; it was temporarily selected for the checks and restored to three-button (`0`) afterward.
- The debug app was installed only through `adb install -r app/build/outputs/apk/debug/app-debug.apk`. `com.youniscript.app.debug` remained installed; `com.youniscript.app` had no installed package path. No connected instrumentation task was run, and no production package operation was made.
- In normal Library mode, the app background continues behind transparent system bars and the three-button controls remain visible. In gesture mode, the Library background also reaches the bottom gesture area. Existing card and FAB content remained above the navigation area.
- Writing mode and book-reading mode hid status and navigation bars. A bottom-edge swipe during reading brought the system status area and gesture navigation handle back transiently. Exiting immersive mode returned to normal system bars.
- In the editor, opening the Samsung keyboard resized the writing area and kept the formatting toolbar visible; the active caret remained in the visible writing area. With the keyboard open in gesture mode, Android's navigation handle remained available. Closing the keyboard returned the editor to its full writing area.
- The reading preview displayed its title page, generated contents, chapter opening, and the saved multi-paragraph page with its page style. The reader content scrolled while the system bars were hidden. Rotation was not checked.
- Real-content persistence: created book `Phase 4 Edge Test`, chapter `Chapter 1`, and page ID `21409a79-f6c1-44d6-d869-1afc99acd6f1`. Entered three paragraphs, assigned the `ancient-manuscript` page override (book default `literary`), left the editor, reopened the manuscript, force-stopped and relaunched the debug app, then reopened the page. The exact body text, page ID, and style override were still present in Room and visible after relaunch. The editor and reader rendered the same saved paragraphs. The test book remains in the debug app as local test data.
- Book-cover template, create flow, chapter workspace, overview, outline, and reading preview were reviewed on the physical device. The cover and workspace fit the phone layout; the chapter count wording was corrected to singular where appropriate. This was a smoke review, not complete coverage of every manuscript operation.
- The Samsung's navigation setting was restored to three-button mode. A duplicate of the disposable debug test book was briefly created during UI navigation, then removed along with its detached duplicate page; the original test book and its chapter/page content remain intact.

### Local validation and limits

- `./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed after the final Phase 4 UI wording change. Instrumentation sources compiled; no instrumentation tests were run.
- Debug APK remains approximately 11 MB. Existing covers are drawn with Compose and optional user cover images are URI-backed; no bundled font library or large raster texture was added.
- Screenshots: three-button Library after restoration `/tmp/youniscript-phase4-threebutton-restored.png`; Library in gesture mode `/tmp/youniscript-phase4-gesture-library-final.png`; editor in gesture mode `/tmp/youniscript-phase4-gesture-editor.png`; keyboard open in gesture mode `/tmp/youniscript-phase4-gesture-ime.png`; reading view `/tmp/youniscript-phase4-reading-final.png`; transient status/navigation reveal `/tmp/youniscript-phase4-gesture-nav-reveal.png`; saved page after relaunch `/tmp/youniscript-phase4-after-relaunch-page.png`.
- Not complete: TalkBack spoken traversal, automated backup/import instrumentation execution, large manuscripts, full security, journal/media/collections, revisions/time capsules, and PDF/EPUB exports have not been verified or implemented. The current product does not satisfy the master brief's full completion criteria. Do not mark Phase 4 complete or begin another phase based on this partial pass.

## Consolidated build continuation — October 6, 2026

### Local search and backup implementation

- Added local Library search across independent page titles/body/tags, book title/subtitle/author/description, chapter titles, and manuscript page titles/body/tags. Search operates on Room's already loaded local records and does not require network access or a schema change.
- Added versioned JSON export/import for current page and manuscript records (pages, books, chapters, sections, and book components). Export uses a consistent Room transaction snapshot. Import validates record IDs, relationships, and order values before atomically adding data; any ID collision rejects the full import, and existing records are never overwritten.
- The JSON is plaintext and does not embed external cover image files. No media attachment, collection, drawing, audio, revision, or security entities currently exist to export.
- `./gradlew --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. Local unit-test reports contain 48 test executions (16 each in debug, instrumentation, and release) with no failures/errors across the configured local variants. Instrumentation source compilation passed; no connected tests were run.
- `./gradlew --max-workers=1 :app:assembleInstrumentation` — passed for an isolated manual backup restore check. The instrumentation app package was installed only in its separate package sandbox; no test APK/runner was installed/launched. It was removed after verification.

### Physical Samsung SM-A356U / Android 16

- Updated the final `com.youniscript.app.debug` build using `adb install -r`. ADB verified the device as `RFCXB0QR9CF`; `com.youniscript.app` had no installed path and was not touched. The separate instrumentation app was removed after the import check. The device's auto-rotation was restored to on (`accelerometer_rotation=1`) and navigation mode remained three-button (`navigation_mode=0`).
- Verified search in the populated Library: the `Phase 4 Edge Test` title returned its book; a query matching manuscript page text returned the owning book.
- Exported `YouniScript-library-backup.json` through Android's document picker into Downloads. The resulting 4,367-byte JSON file contained stable page IDs, text, formatting/style fields, and the book/chapter/section relationships.
- Imported that file into the fresh `com.youniscript.app.instrumentation` sandbox through Android's document picker, then checked its Room database after force-stop. It held 5 pages, 1 book, and 2 sections. Page `26d5ad20-fa24-4a2e-a619-29966e5d0afd` retained the body “This page will keep its stable identity and formatting inside the manuscript.” and `ancient-manuscript` style; `Phase 4 Edge Test` retained its `manuscript` cover template and `literary` default style. The isolated package was uninstalled after this check.
- Submitted the same backup to the populated debug database to exercise duplicate-ID safety. After force-stop, the debug database still had 5 pages, 1 book, and 2 sections; the sampled page ID/body/style were unchanged.
- Opened Settings and verified the Storage & Backups controls are available. JSON remains unencrypted and excludes cover images; the newer password-encrypted `.ysbackup` flow is documented below.
- Physical search and backup transfer were manually checked. The dedicated in-memory Room/codec assertion remains source-compiled only; it was not run through a test runner.

### Current APK and limits

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; 11,006,800 bytes (about 10.5 MiB) after encrypted archive and durable cover-file support. UI remains built from Compose primitives; no new media/font libraries were added.
- Full device review requested by the master brief remains incomplete. In particular: journal/thought/quote/dream/letter/collection flows, drawing/audio/photo attachment, offline verification for every workflow, app lock, revisions/time capsules, PDF/EPUB export, large-manuscript performance, and spoken TalkBack traversal remain unfinished or unverified. JSON backup omits cover image files. Encrypted archive restore with cover assets was not completed in this run.
- Phase 4 remains in progress. No later phase has been started or declared complete.


### Encrypted archive and cover persistence — October 6, 2026

- Implemented password-encrypted `.ysbackup` export/restore for the currently supported page and manuscript records. Archive encryption uses AES-256-GCM with PBKDF2-HMAC-SHA256 key derivation, a random salt and nonce, and authenticated header data. Archive validation bounds input sizes and checks the manifest and referenced cover assets before restore. No passphrase is saved or logged. This does not encrypt the Room database.
- Book cover selections are copied to app-private files so picker-granted temporary URIs do not expire. Archive export includes the referenced cover file bytes. JSON exports remain plaintext and still omit those assets.
- Local validation after these changes: `./gradlew --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed; Gradle reports 48 local test executions (16 each in debug, instrumentation, and release) with no failures; tasks were up-to-date in this final rerun. `:app:assembleInstrumentation` — passed. No connected instrumentation tests were run. Debug APK is 11,006,800 bytes.
- Physical-device check: exported a 1,987-byte opaque `.ysbackup` from `com.youniscript.app.debug` on `RFCXB0QR9CF`, after selecting a disposable synthetic PNG cover that had been copied into private app storage. The document picker saved the archive successfully. We installed the archive into the separate `com.youniscript.app.instrumentation` sandbox and opened its restore dialog, but restore did not complete or provide an accessible success/error result; therefore the complete encrypted restore and cover-byte round trip is **not verified**. The isolated package and temporary QA archive/image were removed. Production package `com.youniscript.app` remained absent and untouched; debug remains installed.
- The encrypted codec's local tests cover successful passphrase round-trip, wrong-passphrase and tamper rejection, plaintext opacity, and minimum passphrase length. Instrumentation sources compile, but the Room archive/cover restore test was not executed.

### Initial launcher identity pass (superseded) — October 6, 2026

- Added a vector Android adaptive icon with forest-green background, ivory reader/book foreground, and monochrome layer, plus a matching vector mipmap fallback. The native AndroidX SplashScreen theme uses warm ivory, a forest reader/book mark, and the “YouniScript / Write what is yours.” wordmark with a subtle mountain/lake footer on API 31+; it does not hold the splash or add a custom loading activity.
- `./gradlew --max-workers=1 :app:test :app:assembleDebug` — passed. Debug application ID remains `com.youniscript.app.debug`. Final debug APK: 13,790,803 bytes (13.1 MiB); the 960×320 px footer PNG is 15,181 bytes and icon assets are vectors. No font or media library was added for branding.
- Installed with `adb install -r` on Samsung SM-A356U / Android 16 (`RFCXB0QR9CF`). The updated reader/book icon appeared in the Samsung app drawer; the device's themed-icon setting recolors launcher artwork. A cold-start capture showed the ivory system splash with wordmark and landscape footer. After launch, the existing populated Library appeared with its content and layout. The Library retained the edge-to-edge paper background and three-button navigation treatment. Production `com.youniscript.app` was not installed or modified; only `com.youniscript.app.debug` appeared in the package-prefix check. The splash naturally exited into the Library; no explicit artificial delay was added.
- Final screenshots: `/tmp/youniscript-appdrawer-final.png`, `/tmp/youniscript-splash-final.png`, and `/tmp/youniscript-library-final-brand.png`. Verification was on the light launcher/app theme; alternate launcher-mask and dark-theme variations were not separately tested.


### Branding concept replacement — October 6, 2026

- Replaced the reader-character and landscape concept with a single-ink Y/open-book symbol. `ic_youniscript_mark.xml` is the reusable forest version; the adaptive foreground, monochrome layer, and legacy vector fallback use matching geometry. The empty Library's previous text-only Y badge now uses the same book mark. No application behavior or package IDs changed.
- Replaced the separate splash mark/footer composition with one transparent splash lockup containing the Y/book symbol, serif “YouniScript” wordmark, and smaller “Write what is yours.” tagline. It fits Android's system splash safe area; the background remains warm ivory, and no fake loading screen or additional delay is used. Removed the landscape asset.
- `./gradlew --max-workers=1 :app:test :app:assembleDebug` — passed after the vector mark and Library use were updated; the final splash safe-area image change was followed by `:app:assembleDebug`, which passed.
- Installed with `adb install -r` on Samsung SM-A356U / Android 16 (`RFCXB0QR9CF`). The app drawer showed the Y/book symbol inside Samsung's themed-icon mask. A cold-start screenshot showed the complete uncropped splash lockup; after two seconds the existing populated Library was visible with its saved test content. No flash or edge-to-edge regression was observed in the captured launch. Production package `com.youniscript.app` was not installed or modified; only `com.youniscript.app.debug` was listed.
- Final APK size: recorded below after packaging. Physical launcher review used the device's current dark blue wallpaper and themed icons. A separate light-wallpaper comparison and alternate launcher-mask review were not performed; themed monochrome coloring is controlled by the launcher. Final APK size: 13,790,799 bytes (13.1 MiB).
- Screenshots: `/tmp/youniscript-brand-minimal-icon.png`, `/tmp/youniscript-brand-lockup-fit.png`, `/tmp/youniscript-brand-icon-final.png`, and `/tmp/youniscript-brand-library-final.png`.

### Approved official emblem — October 7, 2026

- Replaced the earlier vector interpretation with the supplied approved artwork at `app/src/main/res/drawable-nodpi/ic_youniscript_approved_legacy.png`. The adaptive foreground, legacy icon, monochrome alpha silhouette, empty Library mark, About screen, and native splash lockup all derive from that source image. Adaptive resources point at the approved foreground and monochrome assets; `AndroidManifest.xml` continues to point at the `ic_youniscript` mipmap resource, and both splash theme resource sets point at `youniscript_splash_lockup`.
- The splash lockup uses the approved emblem with the YouniScript wordmark and “Write what is yours.” on warm ivory; it contains no landscape or character artwork. The previous vector logo resources were removed.
- `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,970,041 bytes. APK inspection confirmed the manifest references `@mipmap/ic_youniscript`; its adaptive icon references the approved foreground and monochrome assets; both are packaged with the 512×512 legacy emblem and splash lockup. The packaged legacy emblem is an exact match to the approved source resampled to 512×512. The previous vector logo resources are absent from the APK.
- Installed by `adb install -r` to the unlocked Samsung SM-A356U (`RFCXB0QR9CF`) as `com.youniscript.app.debug`, preserving its data. The first launch exposed an incomplete Room v7→v8 migration: the `personal_entries.pageId` foreign key was missing. Added the missing cascade foreign key to the migration, rebuilt, and updated the debug package without clearing data. The app then opened successfully to the existing populated Library. Physical screenshots: `/tmp/youniscript-approved-appdrawer.png`, `/tmp/youniscript-approved-splash-at-launch.png`, `/tmp/youniscript-approved-library.png`, and `/tmp/youniscript-approved-about.png`.
- The Samsung launcher currently applies its themed-icon mode, so its app-drawer rendering is a black-and-sage monochrome Y/book silhouette derived from the approved art; the in-app About mark and splash show the approved full-color emblem. Production package `com.youniscript.app` was absent and untouched. No connected instrumentation tests were run.

### Final approved-icon packaging pass — October 7, 2026

- The adaptive icon's optional monochrome layer was removed after device review showed Samsung replacing the approved ivory/sage palette with a black themed silhouette. Adaptive and legacy icons now render the supplied full-color artwork. The unused monochrome PNG was removed.
- Built with `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. This ran local unit tests and compiled instrumentation sources; no connected instrumentation tests were run.
- Inspected the final APK with `aapt` and `unzip`: the manifest's `icon` and `roundIcon` both resolve to `@mipmap/ic_youniscript`; its adaptive XML references the approved foreground over `@color/youni_forest` and has no monochrome entry. The APK contains the approved foreground, 512×512 legacy emblem, and splash lockup. The legacy emblem is pixel-identical to the supplied 1254×1254 asset resized to 512×512 with Lanczos. The Android 12+ splash theme references `@drawable/youniscript_splash_lockup` on warm ivory.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 12,607,692 bytes. Installed via `adb install -r` as `com.youniscript.app.debug`, preserving existing debug data. The Samsung SM-A356U / Android 16 app drawer showed the approved full-color open-book/Y icon; launch showed the parchment splash lockup and opened the existing populated Library; the About section displayed the same emblem. Captures: `/tmp/youniscript-final-icon.png`, `/tmp/youniscript-final-splash.png`, `/tmp/youniscript-final-library.png`, `/tmp/youniscript-final-about.png`.
- `pm path com.youniscript.app.debug` returned the installed debug APK. `pm path com.youniscript.app` returned no package path; production `com.youniscript.app` was not installed or modified.

### Keystore-backed app lock — October 7, 2026

- Added an optional 4–12 digit PIN lock. PIN verifiers use HMAC-SHA256 with a random salt and an app-specific non-exportable Android Keystore key; the entered PIN is not written to preferences. Five or more consecutive failures trigger an increasing wait, capped at five minutes.
- Settings provides PIN setup/change, verified lock disable, immediate lock, and background lock intervals of immediately, 1, 5, or 15 minutes. Android 11+ uses the platform biometric/device credential prompt when requested. `FLAG_SECURE` is set while the lock is configured to suppress screenshots and Recents previews. Rotation skips background locking and retains the lock state.
- `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` passed after implementation. Local unit tests cover ASCII PIN policy and cooldown progression. Instrumentation sources compiled; no connected instrumentation tests ran.
- Installed the new debug APK with `adb install -r` on the unlocked Samsung SM-A356U / Android 16. The app opened to the existing populated Library, and the Security settings/PIN setup dialog rendered. The dialog was dismissed without entering a PIN so the user's debug library remained unlocked and unchanged. App-lock activation, wrong-PIN timing, lock/relaunch, biometric prompt, and Recents redaction were not physically tested.
- The Room database and local media remain unencrypted. App lock is an access gate, not at-rest encryption. Production `com.youniscript.app` was not installed or modified; only `com.youniscript.app.debug` was updated.
- This does not complete the master product brief. PDF/EPUB/TXT/Markdown rendering fidelity, author-profile downstream flows, full reading modes/read aloud, storage manager, broad accessibility/performance validation, and the remaining end-to-end physical checks are still incomplete or unverified.

### Plain-text and Markdown library export — October 7, 2026

- Added Settings export actions for `.txt` and `.md`, using Android's system document-creation flow. Both formats export a consistent Room snapshot with book/chapter/section hierarchy, journal entries, standalone and trashed pages, stable page IDs, dates, styles, tags, collections, glossary terms, annotations, page links, revision text, media index, and specialized-entry metadata. Markdown also renders stored headings, quotes, bold, italic, underline, and strike marks where supported by the page formatting data.
- Media bytes are not placed in plaintext TXT/Markdown. The export includes an attachment index and tells the user to use encrypted `.ysbackup` when they need media files. The Storage & Backups copy now distinguishes these formats from the encrypted archive.
- `LibraryTextExportTest` checks readable manuscript structure, page text, and stable IDs in both formats. `:app:test`, `:app:assembleDebug`, and instrumentation source compilation passed after adding both exports. No connected instrumentation tests were run.
- The new debug build was installed with `adb install -r` on the unlocked Samsung SM-A356U / Android 16, preserving existing debug data. The app reopened into the existing Library. Production `com.youniscript.app` had no installed package path and was not touched. No TXT/Markdown file was created from the populated physical library because that would copy its private writing to a shared destination; the actual export flow remains device-unverified.
- TXT/Markdown output has local structural coverage only; the text and Markdown import path is implemented as new-page import but has not been manually exercised in this pass. PDF/EPUB exports, additional document import formats, app-lock behavior, and broader physical-device workflows remain incomplete or unverified.

### Combined debug build — October 7, 2026

### Reader modes and book publishing — October 7, 2026

- Added continuous and paginated book views, a two-page spread for layouts at least 720 dp wide, focus/presentation/sepia/dark/centered reader modes, chapter navigation, local per-book position and mode persistence, and system TTS actions scoped to a page/chapter/book with playback rate controls.
- Added SAF manuscript PDF and EPUB exports. EPUB archive structure is covered by a local unit test that opens the generated ZIP and checks `mimetype`, container, package spine, nav links, and escaped chapter content. Reader mode state is private SharedPreferences and does not affect page records.
- `./gradlew --no-daemon --offline :app:test :app:compileDebugKotlin` passed after these changes; the EPUB structural test passed. No connected instrumentation tests were run. Reader modes, TTS voice behavior, generated PDF/EPUB rendering, and accessibility on those screens have not yet been visually verified on hardware.
- PDF/EPUB currently export readable textual content and optional book cover; page-level rich formatting, full PageStyle fidelity, signatures, and attached media are not included.

### Reader, Reading settings, and storage manager device review — October 7, 2026

- Installed the current debug APK with `adb install -r` on unlocked Samsung SM-A356U / Android 16. Opened the existing local book without changing its pages. The continuous reader rendered its title, contents and page cards in immersive mode. The mode menu displayed all eight reader presentation choices; paginated mode opened a single leaf and retained the visible content. Settings displayed the Reading default selector and speech-rate slider. Storage & Backups displayed database, image, audio, and drawing sizes. The observed values were 464 KB database, 564 KB images, 39 KB audio, 26 KB drawings. The reader's page/chapter/book TTS scope menu opened on device. Two-page spread requires a wide layout and was not verified on this phone.
- The populated debug Library remained present after update/install. Production package lookup returned no path; no production package was installed, cleared, or modified. No connected instrumentation tests were run.
- Final local command after reader/timeline/storage changes: `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. Instrumentation source compilation passed; no connected tests.
- Current debug APK is 12,088,583 bytes. The generated PDF itself and the exported EPUB were not opened in external readers; EPUB structural tests passed, but final rendering/reader compatibility remains unverified. TTS speech output, voice availability beyond the scope menu, accessibility/TalkBack, app-lock activation, and orphan cleanup were not exercised on device.
- The latest 12,088,583-byte APK was installed with `adb install -r` to `com.youniscript.app.debug` after this device review, preserving app data. Production package lookup remained empty. The latest compile/test/build command passed after adding adaptive cover rendering and large attachment listing; the cover rendering change was not separately re-opened on device.
- Standalone-page sort controls (recent edit, creation newest/oldest, alphabetical) and type filters are implemented but were only compile-validated in the latest pass. Timeline browsing is month-grouped and offers content-kind/bookmark filters; the Timeline dialog itself was not physically opened.
- Test result XML reports 30 unit tests in each of debug, release, and instrumentation source-set variants, all with zero failures/errors/skips. Instrumentation Android-test source compilation succeeded; no connected runner was invoked.

- Final validation command for the current source: `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. The local suite includes PIN-format/cooldown and TXT/Markdown-structure cases. Instrumentation sources compiled; no connected test runner was used.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 12,019,098 bytes. The current APK was installed with `adb install -r` as `com.youniscript.app.debug`; its launch returned to the pre-existing populated Library and the Settings Storage & Backups section displayed TXT and Markdown export actions. I did not save an export of the user's private library to shared storage.
- Production `com.youniscript.app` returned no installed package path during the check and was not modified. App-lock authentication and export-file round trips remain unverified on the device.

### Appearance and page bookmarks — October 6, 2026

- Added persisted Light, Dark, and System Default appearance selection. Light and Dark use separate app-shell palettes. Page styles stay independent, and immersive system-bar icon appearance still follows the current page's light/dark styling.
- Added a page bookmark field through Room migration 4→5. The default is false for existing pages. The editor can bookmark or unbookmark a page; the Library can browse bookmarked pages; supported JSON and encrypted backups retain bookmark state, with older JSON files defaulting to unbookmarked.
- Added a quiet “On This Day” Library card that surfaces up to two earlier-year entries by local month/day and opens the source page. This was code/build validated but not visually reviewed on the locked device.
- Added a persisted default page-style setting for newly created independent pages. Book pages still inherit their manuscript style. Existing pages are not changed when this preference changes.
- Local validation after the final settings/archive changes: `GRADLE_USER_HOME=/tmp/ys-gradle gradle --offline --no-daemon --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed. The local unit-test XML reports 45 test cases, zero failures/errors/skips. Instrumentation sources compiled, including bookmark persistence, migration defaults, and backup round-trip cases; no connected instrumentation test was run. The new Room schema snapshot is `app/schemas/com.youniscript.app.data.LibraryDatabase/5.json`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,448,356 bytes (about 10.9 MiB). Kotlin compilation and dexing passed after cleaning generated outputs. No new dependency was added.
- ADB identified `RFCXB0QR9CF` as `SM_A356U`; the built APK reported `com.youniscript.app.debug`. The production package path query returned no installed package, and `adb install -r` updated only the debug package. The device remained at its secure lock screen during capture, so I could not visually inspect Settings, theme combinations, or bookmark behavior in the app. No credentials were entered and no lock bypass was attempted. No connected tests were run; production `com.youniscript.app` was not touched.
- This update does not complete the master product brief. Journals, collections, YouniProof, annotations, cross-links, media, revisions, app lock/biometrics, timeline, publishing, full backup coverage, broad accessibility, and the requested end-to-end device test remain unfinished or unverified.

### YouniProof local writing suggestions — October 7, 2026

- Added a deterministic, in-process rule engine with no network, model, or added library. It proposes a small list of common typo fixes, three subject/verb patterns, spacing/repeated punctuation, lowercase first-person “i”, repeated words, and long-sentence clarity prompts. It does not modify source text during analysis. The rule list is deliberately limited and should not be presented as a general spellchecker.
- Added editor actions to Replace, Ignore, and Undo a proposed replacement. Added persisted switches, local personal dictionary, and exact-word custom replacements in Settings. The engine protects spans inside paired straight double quotes from word-level suggestions.
- Added six unit cases for the suggestion engine. `./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` passed on October 7, 2026. There are 21 unit cases in each configured variant (debug, release, and instrumentation source set), with zero failures. Instrumentation sources compiled; no connected runner was used.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,472,374 bytes. No dependency was added. This is 24,018 bytes larger than the previous measured APK.
- The Samsung remained unavailable for visual review during this change; no physical YouniProof, accessibility, rotation, offline, or behavior verification is claimed. Production `com.youniscript.app` was not installed or modified, and no destructive connected tests were run.
- Remaining master-build functionality is still substantial; see the product specification and the master brief. This update does not mark the master build complete.

### Library Create hub — October 7, 2026

- Replaced the Library plus action's direct blank-page creation with a grouped Create bottom sheet. It offers New Page, Quick Thought, New Journal Entry, New Letter, New Quote, New Dream, and New Book. Existing book creation is reused; writing entries open directly in the existing autosaving editor.
- Template pages begin with an appropriate title and a local type tag; their body starts empty to avoid inserting fake/example content. They remain normal pages for editing, search, styling, and backup. This is a fast capture/template entry point, not a dedicated journal system or specialized data model.
- Re-ran `./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` on October 7, 2026 — passed. The test reports contain 21 cases for each debug, release, and instrumentation source-set variant, with zero failures. Instrumentation sources compiled; no connected tests ran.
- The updated debug APK is `app/build/outputs/apk/debug/app-debug.apk`, 11,479,003 bytes. This is 6,629 bytes above the prior YouniProof build. No physical-device review was performed for this menu. Production `com.youniscript.app` remains untouched.

### Approved branding and author identity — October 7, 2026

- Replaced the launcher/adaptive/legacy icon and Android 12+ splash resources using the supplied `youniscript_icon.png` artwork. The manifest's `android:icon` and `android:roundIcon` resolve to `@mipmap/ic_youniscript`; the adaptive icon XML references the approved foreground and forest background; both starting themes reference the splash lockup. APK inspection found density-specific fallback images and the adaptive/splash resource entries. The packaged xxxhdpi icon matched the supplied artwork after the documented resize pipeline.
- Physical Samsung SM-A356U review: the YouniScript open-book/Y icon was visible in the app drawer; the startup frame displayed the emblem, wordmark, and “Write what is yours.” on parchment. The Library launched. About-screen rendering, launcher wallpaper variants, and a frame-by-frame flash review were not performed.
- Added a device-local author profile in Settings. Physically opened Settings on the Samsung and confirmed the Author Profile section and inputs render. The profile uses app-private preferences and is not part of the Room backup. Preference persistence, new-letter defaults, optional book About the Author insertion, and seal rendering were not manually exercised on device.
- Ran `./gradlew --no-daemon --offline --max-workers=1 :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` — passed, including all local unit-test variants and instrumentation source compilation. No connected instrumentation tests ran.
- The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`, 13,329,993 bytes. Installed with `adb install -r` as `com.youniscript.app.debug`; the package list showed only the debug application. Production `com.youniscript.app` was not installed, changed, or uninstalled.

### Dedicated journals — October 7, 2026

- Added Room schema version 6 with `journals` and `journal_entries`. Entry records link a journal to an existing stable page ID and timestamp; rich title/body/formatting/style persist in the page record. Deleting a journal removes its entries/links but preserves the writing pages.
- Added Library journal cards and a journal workspace with chronological entries, create-entry, edit title/description, archive/unarchive, and confirmed delete flows. The Create sheet's Journal Entry action lets the user choose a journal; New Journal creates a journal container. Search matches journal metadata and entry title/body locally.
- Included journal and journal-entry records in existing JSON and encrypted backup snapshots, with validation for unique IDs and valid journal/page references. Existing version 1 JSON archives without journal arrays remain readable.
- Added local snapshot validation cases and an instrumentation persistence/data-preservation case. `:app:test`, `:app:assembleDebug`, and instrumentation source compilation passed on October 7, 2026. No connected instrumentation test was run. Physical UI, rotation, TalkBack, and device migration behavior were not reviewed.

### Recoverable Trash — October 7, 2026

- Added Room schema version 7 with default-false `isTrashed` and nullable `trashedAt`, migrated from version 6 without changing existing page content. Book/journal page queries omit trashed pages; the page's stable identity and manuscript relationship remain available for restoration.
- Editor delete now saves pending edits before moving the page to Trash. The Library has a searchable Trash view, per-page restore, confirmed permanent delete, and confirmed Empty Trash. Journal deletion removes only journal-entry links and preserves pages.
- Trash fields are serialized in supported backups; older JSON archives default missing Trash fields to active pages. No destructive restore or connected tests were run.
- Final local validation: `./gradlew :app:test :app:assembleDebug :app:compileInstrumentationAndroidTestKotlin` passed. Each debug, release, and instrumentation unit-test variant reports 23 tests, zero failures/errors/skips. Instrumentation sources including journal and migration cases compiled; they were not run on a device.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,535,471 bytes. The device was not visually reviewed, and production `com.youniscript.app` was not installed, reset, or modified.
- This is not the complete master product: multiple systems from the final brief remain unimplemented or unverified.
