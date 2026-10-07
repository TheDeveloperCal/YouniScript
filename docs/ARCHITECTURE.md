# Architecture

## Current foundation and writing editor

- Android application module using Kotlin, Jetpack Compose, Material 3, and Room.
- `MainActivity` owns library/editor navigation, page actions, and a serialized autosave queue. Typing saves after a short quiet interval, with a one-second maximum write interval; blur, back navigation, and activity stop flush pending edits.
- Room is the source of truth. The library observes pages using a `Flow`; stable UUID strings identify pages.
- The editor keeps plain text plus compact formatting ranges for inline marks and paragraph appearance. `FormattingDocument` is the conversion boundary between stored content and Compose `AnnotatedString` state.
- Room is at schema version 8. Explicit migrations 1→2 through 7→8 add formatting, manuscript, bookmark, journal, Trash, and personal-library relationships without destructive recreation. Schema snapshots are exported under `app/schemas`.
- Visual debug builds use `com.youniscript.app.debug`; the isolated app build type uses `com.youniscript.app.instrumentation`. Do not run connected tests during visual development because a previous connected workflow removed debug data. Production is `com.youniscript.app` and remains protected.
- Local Library search filters observed Room pages/books/chapters. JSON and password-encrypted `.ysbackup` exports use a Room transaction snapshot and validate relationships before additive transactional restore. The encrypted archive embeds supported book cover assets; JSON remains plaintext and excludes external covers.
- No network permission or remote service is part of the app.
- Appearance is a local preference (Light, Dark, or System Default) applied through a Compose palette and Material color scheme. Page-style colors remain independent. Page bookmark state is a Room boolean and is serialized in supported library backups.
- The default page style is a separate local writing preference applied only when creating independent pages. Pages inside books continue to use their book/chapter style inheritance.
- “On This Day” derives matching month/day entries from existing `createdAt` values using the device's local time zone; it adds no schema or background notification work.
- YouniProof is a pure local analysis object that returns typed ranges and proposed replacements; it never edits the page. The editor applies a user-selected proposal through the existing formatting document and undo stack. Its switches, dictionary, and replacements are local app preferences.
- The Library Create sheet routes page, thought, letter, quote, and dream prompts through the shared page creation, Room save, autosave, and editor flow. Those prompt types are represented by the existing page tag field; journal entries use dedicated relation records described below.
- Book reading mode and resume leaf position are stored in private local SharedPreferences keyed by stable book ID. Reader navigation uses the same ordered chapter/page/section snapshot as the manuscript screen. Android `TextToSpeech` is created only while the reader is composed and stopped/shut down on disposal; no network service is introduced.
- `BookPublishing` snapshots the visible book model and emits PDF through Android `PdfDocument`, or EPUB 3 through the JDK ZIP stream. Both are saved via the Storage Access Framework. These publishing formats are generated locally and do not alter the Room source records.
- Storage usage is derived from the local Room attachment list and app-private file lengths. Orphan cleanup compares canonical files under `files/page-media` to database references and only deletes the confirmed unreferenced candidates; external URIs and files outside that directory are ignored.
- The local author profile is stored in app-private SharedPreferences. It supplies defaults for newly created books and letters; optional biography/seal content is copied into the book's `about-author` component only when the writer selects those options during creation.
- `WritingTemplateCodec` stores up to 100 user-defined title/body pairs as validated JSON in app-private SharedPreferences. Choosing a template creates a new page through the standard Room/editor flow; editing or removing the template does not mutate existing pages. Templates are device-local and excluded from library backups.
- Journal and JournalEntry are first-class Room entities. JournalEntry relates one journal to one stable Page ID and timestamp; page body, formatting, and style remain canonical in Page. Deleting a journal removes its relation rows while preserving writing pages.
- Page Trash is represented by `isTrashed` and `trashedAt` columns, not by deleting content. Book and journal reader queries omit trashed pages; restore reactivates the existing row and permanent deletion cascades only its dependent journal-entry relation.

## Intended direction

As features grow, separate presentation, domain rules, repositories/persistence, security, media, export/import, and page rendering. Keep page content independent from its presentation style, and represent book/journal/collection membership with stable relationships rather than copying page text.

## Current limitations

The app does not yet have a domain/repository layer, dependency injection, encrypted database storage, or background work. Rich text currently covers inline emphasis, headings, subheadings, quotes, alignment, literal bullet/number prefixes, and a text divider. List continuation behavior and advanced manuscript layout remain future work. Autosave is immediate-on-edit after a short debounce and flushed on lifecycle boundaries; abrupt process death inside an active database write remains subject to Android/SQLite transaction completion.
