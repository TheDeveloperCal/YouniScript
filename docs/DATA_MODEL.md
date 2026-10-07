# Data Model

## Current schema (Room version 8)

### Page

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | stable UUID string | Page identity |
| `title`, `body` | text | User-authored content |
| `createdAt`, `updatedAt` | epoch milliseconds | Creation and last-edit times |
| `formatting` | JSON text | Versioned inline marks and paragraph ranges |
| `pageStyleId` | nullable text | Optional presentation override |
| `tags` | JSON text | Optional tag values |
| `collectionId` | nullable text | Legacy nullable association; many-to-many membership uses `collection_items` |
| `bookId`, `chapterId`, `sectionId` | nullable text | Manuscript membership |
| `bookOrder` | nullable integer | Explicit order within a chapter |
| `isBookmarked` | boolean | Local bookmark flag; false for pages already in the database |
| `isTrashed`, `trashedAt` | boolean, nullable timestamp | Recoverable soft-delete state |

Pages remain the universal editable writing object. Adding a standalone page to a book changes its relationship in a transaction and retains its identity, text, formatting, style, and tags.

### Manuscripts

- `Book` stores title, subtitle, author, description, created/modified timestamps, cover template, optional cover-image URI, default page style, status, and library order.
- `Chapter` has a book foreign key and explicit per-book order.
- `Section` has book/chapter foreign keys and explicit per-chapter order.
- `BookComponent` stores typed title-page, dedication, preface, introduction, and related front-matter content.

Deleting a chapter or section preserves its pages. Deleting a book detaches its pages and converts its written front-matter components into standalone pages with the same component IDs and text.

## Journals and personal-library records

- `Journal` and `JournalEntry` are dedicated container and stable page-link records; entry text and formatting remain on the page.
- `CollectionRecord` and `CollectionItem` provide non-duplicating membership for pages, books, and journals.
- `AnnotationRecord` stores marginalia separately from the content it references.
- `PageLink` references stable source/target page IDs and supports backlinks.
- `GlossaryTerm` stores unique terms, definitions, and an optional source page ID.
- `PageRevision` retains prior page title, body, formatting, style, and timestamp.
- `MediaAttachment` stores page association, type, private-file URI, display name, and creation time. Photo, drawing, and audio bytes live under app-private storage.
- `PersonalEntry` stores structured metadata for thought, quote, dream, and letter entries while the corresponding page remains the editable writing surface.

## Migrations

Explicit Room migrations are registered for versions 1→2 through 7→8. The 3→4 migration adds page section membership and the `sections` table. The 4→5 migration adds a false-default bookmark flag. The 5→6 migration adds `journals` and `journal_entries`; journal entries reference stable page IDs, and deleting a journal removes only its entry relationships. The 6→7 migration adds default-false `isTrashed` and nullable `trashedAt` fields plus an index; existing pages remain active after migration. The 7→8 migration creates the collections, collection membership, annotations, page links, glossary, revisions, media attachments, and personal-entry tables. Existing pages remain intact. No destructive migration fallback is configured. Exported Room schema snapshots are kept under `app/schemas/com.youniscript.app.data.LibraryDatabase/`.

## Backup format

Version 1 JSON snapshots serialize all current Room record types, including journals, collections, annotations, links, glossary terms, revisions, attachments, and specialized-entry metadata. JSON remains plaintext and does not embed cover or media file bytes; its app-private file references are device-local. The password-encrypted `.ysbackup` archive packages supported cover and page-media bytes and applies AES-256-GCM with PBKDF2-HMAC-SHA256 key derivation. Export snapshots Room data consistently. Restore validates IDs, relationships, assets, and order constraints before transactionally adding records; conflicts reject the import without overwriting existing records. App-lock preferences and Android Keystore keys are device-local and are not included in backups.

Future schema changes must preserve page IDs, titles, bodies, formatting, styles, metadata, and manuscript relationships. Add an explicit migration and migration coverage for each schema version; never use destructive recreation in production.

YouniProof preferences (enabled checks, personal dictionary, and custom replacements), appearance preferences, app-lock settings, and PIN verifier metadata live in app-private SharedPreferences rather than the Room library schema. The PIN itself is not stored; its verifier is keyed with Android Keystore. Suggestions are transient and are not written into page content or backup files.

Create-sheet quick thoughts, quotes, dreams, and letters use `PersonalEntry` metadata linked to stable page IDs. Journal entries use their own `JournalEntry` relationship table. The page remains the canonical user-authored title/body/formatting record in both cases.

The Room schema remains version 8. Reader mode, per-book resume leaf, default reader mode, and TTS rate are presentation preferences in app-private SharedPreferences; they do not duplicate page/book content and are not part of library backups. Timeline month groups are derived from `createdAt` in the device's local time zone. Storage totals derive from current database/WAL files and attachment records; app-private orphan file cleanup does not introduce schema state.

The optional author name, biography, letter signature, and plain-text seal are stored in app-private SharedPreferences, not Room or library backups. New letter metadata receives the configured signature as a starting value. New books use the configured author name; biography and seal are copied into an `about-author` BookComponent only when selected at book creation. The seal is display text and carries no cryptographic identity guarantee.

User writing templates are bounded JSON records in a separate app-private SharedPreferences file (up to 100 templates, each with a title and starting text). They are local preferences and are not part of the Room or backup schemas; using one creates a regular page with a stable page ID, after which the page no longer depends on its source template.
