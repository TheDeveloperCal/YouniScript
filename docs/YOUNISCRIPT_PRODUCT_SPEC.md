# YouniScript Product Specification

YouniScript is a private, offline-first personal library for writing and manuscripts. Its tagline is “Write what is yours.” The core product idea is that any piece of writing can begin as a page and later belong to books, journals, collections, or other user-defined structures.

The app does not require an account, network access, advertising, analytics, or AI. It currently provides a locally persisted Library, a presentation-only page-style system, independent Light/Dark/System app appearance, bookmarks, recoverable Trash, books/manuscripts, local search and page sorting/type filters, YouniProof suggestions, dedicated journals, specialized personal-entry metadata, collections, annotations, stable page links, glossary terms, revisions, photo/drawing/audio attachments, JSON and encrypted backup/restore, TXT/Markdown exports, and SAF book PDF/EPUB publishing. Settings also offers an optional Keystore-backed PIN app lock with platform biometric/device authentication and configurable background timeout. Multiple reader modes, resume position, system TTS, monthly timeline, storage accounting, confirmed orphan cleanup, a local author profile, and user-managed writing templates are implemented; comprehensive accessibility/performance coverage and full export fidelity remain unfinished.

## Foundation vertical slice

- Open to “My Library,” with a calm empty state.
- Create a page from the primary plus action.
- Edit a title and body in a distraction-light editor.
- Save locally to Room while typing, on leaving the editor, and when the activity stops.
- Reopen a page from the library.

## Phase 2 writing experience

- Editable title and body with cursor/selection handling, native keyboard actions, and a keyboard-resizing layout.
- Compact selected-text actions for bold, italic, underline, and strikethrough; a format menu for headings, subheadings, lists, quotes, dividers, and paragraph alignment.
- Undo/redo, copy/paste/select-all, duplicate, rename, and confirmed delete actions.
- Debounced/interval autosave with focus, navigation, and activity-stop flushes and visible retry feedback on save errors.
- Page cards show title, preview, and modified time. Room migrations currently reach schema v8 and preserve writing while adding rich formatting, manuscript relationships, bookmarks, journals, trash, collections, annotations, links, glossary terms, revisions, attachments, and specialized-entry metadata.
- Standalone page lists can be sorted by recently edited, newest, oldest, or title and filtered by thought, quote, dream, letter, and journal-entry tags. Book/journal/collection search remains local; the more complete cross-record query/filter surface is still an area for expansion.
- Appearance offers Light, Dark, and System Default, persists the choice locally, and keeps app chrome independent from the selected page style. Settings also persists the default style for new independent pages; existing pages and book-level styles remain unchanged. Bookmarks are stored on each page, can be toggled from the editor, and are included in Library bookmark browsing and supported backups.
- The Library's “On This Day” card surfaces up to two pages created in earlier years on today's local month/day. The Timeline dialog groups writing by month, filters by content kind/bookmark, and opens its source page.
- The Library's Create hub offers New Page, Quick Thought, Journal Entry, Letter, Quote, Dream, New Book, plus photo, drawing, and audio entry points. Specialized thought/quote/dream/letter details use `PersonalEntry` records linked to stable page IDs. Journals use dedicated journal and journal-entry tables. These actions and the media flows are available in the UI; broad physical-device coverage remains incomplete.
- Settings supports creating, editing, and deleting up to 100 reusable writing templates. Templates are stored locally in app-private preferences and appear in Create; selecting one creates a new editable page with its saved title and starting text. Deleting a template leaves previously created pages unchanged. Template preferences are device-local and are not included in library backups.
- Settings includes a device-local author profile with author name, short biography, letter signature, and text-based seal/initials preview. The name pre-fills new book credits; the signature pre-fills new personal letters. During book creation, the writer may choose to add the biography and/or text seal to an editable About the Author front-matter component. The profile remains in app-private preferences and is not included in library backups; the seal is a visual mark, not cryptographic proof.
- Journals are dedicated Room records. Each journal entry has its own stable relation record to a stable page ID and an entry timestamp; page title/body/rich formatting and PageStyle use the existing editor. Journals support create, edit, archive/unarchive, delete with page preservation, chronological entry browsing, search by journal or entry text, and supported backup/restore.
- Page deletion moves the page into a recoverable Trash with a timestamp. Pages can be restored or permanently deleted individually, or the Trash can be emptied after confirmation. Trash state is included in supported library backups; deleting a journal preserves its entry pages.
- Collections use separate membership rows for pages, books, and journals, so collection membership does not copy content. Annotations are separate records; page links use stable page IDs and expose backlinks; glossary terms have unique normalized spellings. Revisions preserve prior title, body, formatting, and style snapshots. Photos, drawings, and audio are copied to app-private files and referenced by attachment records.
- Phase 2 was accepted complete on October 6, 2026. The six debug instrumentation tests passed on the physical SM-A356U, and the user directly verified that a new entry autosaved and appeared in My Library after leaving and reopening the editor.

## Phase 3 visual foundation

- Establish a warm paper, ink, and sage identity for the modern app shell.
- Use system serif typography for editorial emphasis without increasing APK size with unlicensed font files.
- Refine My Library's hierarchy, empty state, and page cards while preserving the calm, user-owned writing experience.
- Keep visual styling separate from page content so future page styles can vary independently.
- The editor offers nine presentation-only page styles (Modern Paper, Classic Book, Notebook, Typewriter, Parchment, Ancient Manuscript, Medieval Manuscript, Literary, Dark Journal). A style choice is stored in the existing page style ID field and does not modify page text or rich-text formatting.
- Applying a style is presentation-only and preserves the existing page identity, body text, and stored formatting. The chooser displays miniature page previews; physical-device testing confirmed the selected style survives leaving/reopening the page and force-stop/relaunch.
- The Library uses a page-style miniature in populated page rows. Reusable book-cover and journal-cover components establish visual foundations without exposing nonfunctional creation actions.

## Phase 4 books and manuscripts — in progress

- Books are first-class local Room records with title, subtitle, author, description, created/modified times, cover template/image URI, default page style, draft/finished/archived status, and explicit ordering metadata.
- A book owns explicitly ordered chapters and typed front-matter components. Pages retain their stable page IDs and are associated to a book/chapter; moving within a book updates the relationship and order instead of copying the page. Deleting a chapter or book detaches its pages to preserve writing.
- The Library offers book creation with live template preview, optional user cover image via the system photo picker, a default page style, and a book workspace. The workspace supports chapter creation/rename/duplication/deletion/reordering, page creation/reordering/movement, front-matter writing, book duplication/deletion, an outline, and an immersive reading preview with generated title page and table of contents.
- The book flow supports sections and their ordering/movement, adding an existing independent page while retaining its ID and contents, book status and cover/default-style editing, chapter/page movement, front matter, outline, and immersive reading. Reader position and per-book reader mode persist locally. The wide two-page layout, large-manuscript behavior, all modes and reading comfort/accessibility still need broader device validation. See `TESTING.md` for the current record.

## Library search and backup

- Local search matches standalone page title/body/tags and book/chapter metadata and page content. It operates on locally observed Room records and has no network dependency.
- Settings can export a versioned JSON snapshot of the supported Room records and import it transactionally only when IDs do not collide. JSON is plaintext and contains attachment/cover references but does not package their file bytes. TXT and Markdown exports produce a human-readable hierarchy with stable page IDs, timestamps, page styles, revisions, annotations, glossary, and text metadata; they do not embed media bytes. The password-encrypted `.ysbackup` archive packages supported cover and page-media bytes. App-lock preferences and Keystore keys are intentionally device-local and are not portable in a backup.

## App identity

- The Android launcher and in-app branding use the approved supplied Y/open-book emblem. The adaptive foreground and legacy icon derive from that artwork. The adaptive monochrome layer is omitted to preserve the approved colors in launcher displays. The native Android splash centers the same emblem with the “YouniScript” wordmark and “Write what is yours.” on warm ivory, then transitions directly into the Library.
- YouniProof offers local deterministic suggestions for a small set of common misspellings, subject/verb patterns, punctuation, capitalization, repeated words, and long sentences. Users choose Replace or Ignore; Replace can be undone. A local personal dictionary and exact-word replacements are configurable. It is not a general spellchecker or comprehensive grammar engine.

## Product constraints

- Keep the app useful offline and do not request `INTERNET` permission.
- Keep creation neutral: the user defines what a page means.
- Treat user writing as valuable and preserve it through stable IDs, Room migrations, transactional manuscript operations, and explicit backup for supported records. The `.ysbackup` archive is password-encrypted. The optional app lock uses a salted HMAC verifier protected by Android Keystore and `FLAG_SECURE`, but does not encrypt the local database or media files.
- Prefer a focused phone experience and progressive disclosure of advanced tools.

## Remaining product work

The consolidated master brief remains the roadmap. Implemented: book reader presentation modes (continuous, paginated, two-page spread on wide layouts, focus, presentation, sepia, dark, and centered/typewriter), per-book saved mode and position, chapter navigation, Android system TTS for page/chapter/book with speed and queue controls, local Reading defaults, SAF PDF/EPUB publishing, monthly timeline browsing with writing-type filters, and a Storage & Backups usage summary with largest-attachment sizes and confirmed cleanup of only unreferenced app-private page-media files. Reader and export surfaces use optional local book cover artwork. PDF uses Android's native PDF renderer and includes title/cover treatment, front matter, contents, chapter headings, pages, and page numbers; EPUB creates a semantic EPUB 3 package with nav, metadata, ordered front matter/chapters, and optional local cover image. The local author profile now supplies defaults and optional book front matter, and user-managed templates can seed editable pages. Remaining work includes time capsules, richer export formatting and complete EPUB/PDF styling/media, comprehensive accessibility traversal, large-manuscript/performance checks, and end-to-end physical-device verification of these new reading/publishing/storage features. App-lock activation/biometric behavior, author-profile/template downstream flows, backup media round-trip, and several collection, journal, specialized-entry and media flows still need broader device verification. Phase 4 remains in progress.
