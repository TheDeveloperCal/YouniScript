# Design System

## App identity

- The official emblem is the approved supplied artwork `youniscript_icon.png`: a deep forest rounded square with an ivory open book, integrated Y, and muted sage page layers. It is packaged directly as raster artwork; do not redraw or reinterpret it. The launcher, Library empty state, About screen, and splash all use this same source emblem.
- Android adaptive icons use the approved artwork as the foreground over a matching forest background. The monochrome adaptive layer is intentionally omitted so launchers do not replace the approved ivory and sage colors with a themed silhouette. The legacy launcher resource references the approved raster directly. The AndroidX system splash uses a lightweight transparent lockup containing the approved emblem, serif YouniScript wordmark, and smaller “Write what is yours.” tagline on warm ivory.
- Brand source artwork is rasterized only to Android-appropriate resource sizes. No landscape art, third-party font, animation, or branding dependency was added beyond the existing SplashScreen support.

## Visual identity

YouniScript is a private personal library with a contemporary app shell and an expressive writing surface. Use warm paper, ink, muted sage, and restrained natural brown. Keep the shell quiet and modern; specialized manuscript details belong to optional page styles. Avoid productivity metrics, feeds, heavy shadows, loud gradients, and generic purple styling.

## Tokens and typography

`YouniColors` in `ui/theme/YouniDesign.kt` centralizes library background, paper and elevated paper surfaces, primary/secondary/muted ink, sage accents, natural brown, border, divider, error, success, and selection colors. `YouniSpacing` provides the shared compact-to-wide spacing steps. Use these tokens instead of introducing screen-specific color literals.

The shared palette has separate warm-paper Light and charcoal Dark values. Settings persists Light, Dark, or System Default independently of page style. System bars follow the app appearance on normal surfaces and continue to follow the writing page's own light/dark treatment in immersive editing. Settings also chooses and persists the default page style for future independent pages. The current implementation has been build-validated; device contrast and all surface combinations still need physical review.

`YouniScriptTheme` exports `YouniTypography`: the app shell uses platform sans-serif Material typography, while display and editorial title roles use the system serif. Page styles choose readable system serif or monospace variants and set their own body/title/heading sizes and line height. No third-party font files or large visual assets are bundled.

## Shared components and interaction

- `BrandMark` displays the approved Y/open-book emblem.
- Library page rows use a small page-style miniature, serif title, short body preview, and modified-time metadata. The empty state has a concise literary message and one clear writing action.
- The Library search field is a quiet paper-surface control. It searches local page text/tags and book/chapter metadata/content; empty results get a plain status message without replacing empty-library onboarding.
- `BookCover` and `JournalCover` in `ui/LibraryVisuals.kt` provide distinct reusable covers. Book templates support Minimal, Classic, Parchment, Ancient, Literary, Dark, Modern, and Manuscript treatments. Books now appear in the Library and use local Room-backed book, chapter, front-matter, and page relationships.
- Keep the editor's writing area dominant. Formatting and page actions stay compact and contextual. Menus and dialogs use paper/ink colors appropriate to the selected page style.
- Page bookmarks are managed from the editor's contextual actions and browsed from the Library. Bookmark state is metadata and does not alter manuscript text or formatting.
- “On This Day” appears as a compact Library card only when matching earlier writing exists. It stays quiet and is based on the page's original creation date.
- Do not signal selection or status by color alone. Keep touch targets, labels, and system contrast legible.

## Page styles

`PageStyles` is the registry for nine stable IDs: `modern-paper` (default for unset/unknown IDs), `classic-book`, `notebook`, `typewriter`, `parchment`, `ancient-manuscript`, `medieval-manuscript`, `literary`, and `dark-journal`. `PageStyle` holds the page/background treatment, typography and sizing, margins, line spacing, border, rules, ornament, initial, running header, and page-number choices. Shared Compose rendering and `PageStyleMiniature` apply these configurations; styles do not replace page content with images.

`PageStylePickerDialog` shows sample page previews with representative text and marks the selected style. The chosen stable ID is stored in the existing `Page.pageStyleId` field. Applying a style preserves the page ID, text, formatting, metadata, and modified timestamp. The choice is designed to survive leaving and reopening the page through the existing persistence path.

## Responsive and visual principles

The current primary layout targets phones and uses Compose's available width with generous editor margins. Large-screen-specific navigation, grid/list switching, adjustable font settings, and richer library organization remain future work. Keep previews light and procedural, avoid loading full-size cover imagery into scrolling lists, and prefer restrained transitions.

## Window edges and immersive surfaces

`MainActivity` uses AndroidX `enableEdgeToEdge` with transparent system bars. Library and workspace backgrounds continue behind the status and navigation areas; normal-mode controls stay within the Compose layout's safe content area. In writing and book-reading modes, the activity hides both bars using `WindowInsetsControllerCompat` with transient bars revealed by swipe. Navigation remains available through Android's system gestures/buttons. The editor applies IME insets so the writing area and formatting controls remain usable while the keyboard is open. Do not add fixed status/navigation padding; use window insets.

On the Samsung SM-A356U running Android 16, Library, editor, book reader, keyboard, and transient bar reveal were reviewed in both three-button and gesture navigation. Gesture mode was temporary; the device was restored to three-button navigation after the check. A later Phase 4 continuation checked Library, outline, editor, style picker, and book details in landscape and portrait; route and selection state remained. The keyboard closed during editor rotation while the editor and toolbar stayed available.

YouniProof lives in the writing toolbar and presents suggestions in a scrollable dialog using the manuscript's quiet paper palette. Suggestions are visibly labeled by kind; Replace and Ignore are explicit actions, and Undo is offered after a replacement. The editor preview and dialog have not yet had a physical accessibility or visual review for this feature.

The Library's plus action opens a grouped Create bottom sheet. Writing prompts remain one tap from the editor and use the existing local page surface; the menu uses text actions and restrained grouping instead of a grid of decorative tiles. The new menu has passed local compilation but has not yet been visually reviewed on hardware.

Journal screens use the same ivory library surface, sage date labels, serif journal titles, and quiet bordered entry cards. Empty journals invite a first entry; entries are ordered by their saved timestamp. This UI has only been locally compiled, not physically reviewed.

Trash is a reversible Library view with visible restore and permanent-delete actions. Permanent deletion and Empty Trash use explicit confirmation dialogs; normal Library, book, and journal views hide trashed pages.

## Book reading modes

The reader keeps navigation controls in a compact top row and reading controls in a horizontally scrollable secondary row. Its mode menu separates continuous scroll, paginated leaves, wide-layout two-page spreads, focus reading, presentation reading, sepia paper, dark reading, and centered-line/typewriter presentation. Per-book mode and leaf position are saved locally; the library-wide default reading mode is separate. Read Aloud uses Android's installed system TTS engine and offers page, chapter, or book scope with pause/resume/stop and a locally remembered speech rate. TTS availability depends on a compatible device language engine. On the physical SM-A356U, the continuous reader, mode list, paginated leaf, and TTS scope menu were opened; the Reading settings and storage accounting were also visually reviewed. TalkBack, two-page layout, speech output, and export rendering remain unverified.

Book PDF and EPUB actions are in the manuscript workspace's Export menu and use Android's document destination picker. PDF output uses a restrained forest cover and paper pages; EPUB output uses semantic XHTML, a navigation document, metadata, and optional embedded local cover art. User-selected local book covers also render on the title leaf in the reader with a bounded downsample. Current exports flatten rich formatting to readable text; they do not reproduce every PageStyle ornament or include attachment media beyond the optional book cover.

The Library's Timeline action opens a chronological month-grouped view with local writing/bookmark filters. Storage & Backups reports the database, image, audio, and drawing footprint; unreferenced files are counted separately and can only be removed after a confirmation names the count and size. Cleanup is constrained to files under the app-private page-media directory.

## Device review status

On Samsung SM-A356U / Android 16, all nine page styles were applied to a real three-paragraph entry with a formatted heading. The paper styles kept legible text and page margins; Notebook rules remain subtle; Typewriter uses monospaced text; Ancient and Medieval Manuscript ornaments stay restrained; Dark Journal maintains readable page and system-bar contrast. Style-picker labels now use shell ink tokens independently of each page's ink, so Dark Journal's light page text remains legible on the light chooser surface.

IME insets keep the active writing line above the keyboard. The editor was checked at the device's normal font scale (1.0) and a temporary larger scale (1.3), then the system setting was restored to 1.0. The populated Library and isolated empty-state Library were reviewed. Book reading uses an intentionally quiet, full-screen layout with a generated title page and contents, chapter openings, and style-aware page content. Settings exposes JSON export/import and encrypted archive export/restore actions. JSON remains plaintext and excludes cover-image files; the encrypted archive includes supported book cover assets. Search and backup actions were reviewed in the populated Library/settings on device. See `TESTING.md` for current book-flow, accessibility, and remaining coverage limits.

## Author identity

Settings provides optional author name, biography, signature, and plain-text seal fields. The seal preview uses a restrained circular monogram treatment in the existing ink/sage palette; it is deliberately labeled as display text rather than a verified signature. Book creation can add the writer's biography and seal to its About the Author front matter, while the author name is used in the book credit and the configured signature pre-fills new letters.

## User writing templates

The Create sheet distinguishes built-in starting prompts from the writer's own reusable templates. Personal templates are named, optional body text is shown as a subdued preview, and choosing one opens a normal editable page. Template management lives in Settings, with explicit edit and delete actions; deleting a template never removes pages already created from it.
