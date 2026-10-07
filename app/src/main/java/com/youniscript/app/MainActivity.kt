package com.youniscript.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.CancellationSignal
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.graphics.Color as AndroidColor
import android.widget.Toast
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.youniscript.app.data.LibraryDatabase
import com.youniscript.app.data.LibraryBackupCodec
import com.youniscript.app.data.LibraryTextExport
import com.youniscript.app.data.EncryptedLibraryBackupCodec
import com.youniscript.app.data.createBackupSnapshot
import com.youniscript.app.data.restoreBackup
import com.youniscript.app.data.Book
import com.youniscript.app.data.BookComponent
import com.youniscript.app.data.Chapter
import com.youniscript.app.data.Page
import com.youniscript.app.data.Journal
import com.youniscript.app.data.JournalEntry
import com.youniscript.app.data.JournalEntryWithPage
import com.youniscript.app.data.Section
import com.youniscript.app.data.CollectionRecord
import com.youniscript.app.data.CollectionItem
import com.youniscript.app.data.AnnotationRecord
import com.youniscript.app.data.PageLink
import com.youniscript.app.data.GlossaryTerm
import com.youniscript.app.data.PageRevision
import com.youniscript.app.data.MediaAttachment
import com.youniscript.app.data.PersonalEntry
import com.youniscript.app.data.AuthorProfile
import com.youniscript.app.data.WritingTemplate
import com.youniscript.app.data.WritingTemplateCodec
import com.youniscript.app.data.BookExportSnapshot
import com.youniscript.app.data.writePdf
import com.youniscript.app.data.writeEpub
import com.youniscript.app.security.AppLockCrypto
import com.youniscript.app.editor.ALIGN_CENTER
import com.youniscript.app.editor.ALIGN_END
import com.youniscript.app.editor.ALIGN_JUSTIFY
import com.youniscript.app.editor.ALIGN_START
import com.youniscript.app.editor.BLOCK_HEADING
import com.youniscript.app.editor.BLOCK_NORMAL
import com.youniscript.app.editor.BLOCK_QUOTE
import com.youniscript.app.editor.BLOCK_SUBHEADING
import com.youniscript.app.editor.FormattingDocument
import com.youniscript.app.editor.PageAutosaver
import com.youniscript.app.editor.SaveState
import com.youniscript.app.editor.ProofOptions
import com.youniscript.app.editor.ProofSuggestion
import com.youniscript.app.editor.YouniProof
import com.youniscript.app.ui.PageStyles
import com.youniscript.app.ui.PageStyleMiniature
import com.youniscript.app.ui.PageStylePickerDialog
import com.youniscript.app.ui.BookLibraryCard
import com.youniscript.app.ui.BookOverviewScreen
import com.youniscript.app.ui.BookOutlineScreen
import com.youniscript.app.ui.BookReadingScreen
import com.youniscript.app.ui.SettingsScreen
import com.youniscript.app.ui.AppLockScreen
import com.youniscript.app.ui.CreateBookDialog
import com.youniscript.app.ui.EditBookDetailsDialog
import com.youniscript.app.ui.FrontMatterEditorDialog
import com.youniscript.app.ui.JournalScreen
import com.youniscript.app.ui.JournalsSection
import com.youniscript.app.ui.JournalEditorDialog
import com.youniscript.app.ui.PersonalEntryDetailsDialog
import com.youniscript.app.ui.AnnotationManagerDialog
import com.youniscript.app.ui.PageLinksDialog
import com.youniscript.app.ui.RevisionHistoryDialog
import com.youniscript.app.ui.CollectionContent
import com.youniscript.app.ui.CollectionScreen
import com.youniscript.app.ui.CollectionEditorDialog
import com.youniscript.app.ui.GlossaryScreen
import com.youniscript.app.ui.MediaAttachmentGallery
import com.youniscript.app.ui.DrawingEditorDialog
import com.youniscript.app.ui.AudioRecordingDialog
import com.youniscript.app.ui.filterLibraryBooks
import com.youniscript.app.ui.filterLibraryPages
import com.youniscript.app.ui.theme.YouniColors
import kotlinx.coroutines.CancellationException
import com.youniscript.app.editor.MARK_BOLD
import com.youniscript.app.editor.MARK_ITALIC
import com.youniscript.app.editor.MARK_STRIKE
import com.youniscript.app.editor.MARK_UNDERLINE
import com.youniscript.app.ui.theme.YouniScriptTheme
import com.youniscript.app.ui.theme.LocalYouniInterfaceScale
import com.youniscript.app.ui.theme.YouniContentTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Ink @Composable get() = YouniColors.ink
private val MutedInk @Composable get() = YouniColors.mutedInk
private val Paper @Composable get() = YouniColors.library
private val CardPaper @Composable get() = YouniColors.paper
private val Accent @Composable get() = YouniColors.sage
private val ErrorInk @Composable get() = YouniColors.error

class MainActivity : ComponentActivity() {
    private var appLocked by mutableStateOf(false)
    private var appLockConfigured by mutableStateOf(false)
    private val dao by lazy { LibraryDatabase.get(this).pageDao() }
    private val bookDao by lazy { LibraryDatabase.get(this).bookDao() }
    private val journalDao by lazy { LibraryDatabase.get(this).journalDao() }
    private val personalDao by lazy { LibraryDatabase.get(this).personalLibraryDao() }
    private val writeMutex = Mutex()
    private var saveState by mutableStateOf(SaveState.Saved)
    private var operationError by mutableStateOf<String?>(null)
    private var defaultPageStyleId by mutableStateOf("modern-paper")
    private var authorProfile by mutableStateOf(AuthorProfile())
    private var writingTemplates by mutableStateOf<List<WritingTemplate>>(emptyList())
    private var proofOptions by mutableStateOf(ProofOptions())
    private val autosaver by lazy {
        PageAutosaver(
            scope = lifecycleScope,
            writePage = { page -> writeMutex.withLock {
                LibraryDatabase.get(this).withTransaction {
                    val previous = dao.findById(page.id)
                    if (previous != null && (previous.title != page.title || previous.body != page.body || previous.formatting != page.formatting || previous.pageStyleId != page.pageStyleId) &&
                        (previous.title.isNotBlank() || previous.body.isNotBlank())) {
                        personalDao.saveRevisionIfChanged(previous, System.currentTimeMillis())
                    }
                    dao.save(page)
                }
            } },
            onStateChanged = { state -> saveState = state },
        )
    }

    private fun scheduleSave(page: Page) = autosaver.schedule(page)

    private fun flushSave(page: Page? = null) {
        if (page == null) autosaver.flush() else autosaver.flush(page)
    }

    private fun saveAndClose(page: Page, onSaved: () -> Unit) {
        lifecycleScope.launch { if (autosaver.saveNow(page)) onSaved() }
    }

    private suspend fun persistPageAttachment(pageId: String, mediaType: String, displayName: String, bytes: ByteArray): MediaAttachment =
        withContext(Dispatchers.IO) {
            require(bytes.isNotEmpty() && bytes.size <= 25 * 1024 * 1024) { "The attachment is empty or exceeds 25 MB." }
            val mediaRoot = File(filesDir, "page-media").canonicalFile
            val pageFolder = File(mediaRoot, pageId).canonicalFile
            require(pageFolder.path.startsWith(mediaRoot.path + File.separator)) { "Invalid page attachment location." }
            check(pageFolder.exists() || pageFolder.mkdirs()) { "Could not create the attachment folder." }
            val extension = when {
                mediaType == "drawing" -> "png"
                mediaType == "audio" -> "m4a"
                displayName.endsWith(".png", ignoreCase = true) -> "png"
                displayName.endsWith(".webp", ignoreCase = true) -> "webp"
                displayName.endsWith(".gif", ignoreCase = true) -> "gif"
                else -> "jpg"
            }
            val file = File(pageFolder, "${UUID.randomUUID()}.$extension")
            val temporary = File(pageFolder, "${file.name}.tmp")
            try {
                temporary.writeBytes(bytes)
                check(temporary.renameTo(file)) { "Could not finish saving the attachment." }
            } finally {
                temporary.delete()
            }
        MediaAttachment(UUID.randomUUID().toString(), pageId, mediaType, Uri.fromFile(file).toString(), displayName, System.currentTimeMillis())
    }

    private suspend fun writeReadableLibraryExport(uri: Uri, markdown: Boolean) {
        val snapshot = LibraryDatabase.get(this).createBackupSnapshot()
        val text = withContext(Dispatchers.Default) {
            if (markdown) LibraryTextExport.markdown(snapshot) else LibraryTextExport.plainText(snapshot)
        }
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
                ?: error("Could not open the selected destination")
        }
    }

    private fun safeAttachmentFile(location: String): File? = runCatching {
        val uri = Uri.parse(location)
        require(uri.scheme == "file")
        val root = File(filesDir, "page-media").canonicalFile
        val candidate = File(uri.path ?: error("Missing attachment path")).canonicalFile
        require(candidate.path.startsWith(root.path + File.separator))
        candidate
    }.getOrNull()

    private fun createPage(kind: String = "page", onCreated: (Page) -> Unit) {
        operationError = null
        val now = System.currentTimeMillis()
        val title = when (kind) {
            "journal" -> "Journal · ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))}"
            "thought" -> "Quick Thought"
            "letter" -> "Letter"
            "quote" -> "Quote"
            "dream" -> "Dream"
            "research" -> "Research Note"
            "chapter" -> "Chapter Draft"
            "family-history" -> "Family History"
            "philosophy" -> "Philosophy"
            "manuscript" -> "Manuscript Page"
            else -> ""
        }
        val tags = if (kind == "page" || kind.startsWith("template-")) "[]" else org.json.JSONArray().put(kind).toString()
        val page = Page(UUID.randomUUID().toString(), title, "", now, now, pageStyleId = defaultPageStyleId, tags = tags)
        lifecycleScope.launch {
            val result = runCatching {
                writeMutex.withLock {
                    LibraryDatabase.get(this@MainActivity).withTransaction {
                        dao.save(page)
                        if (kind in setOf("thought", "letter", "quote", "dream")) {
                            personalDao.savePersonalEntry(PersonalEntry(
                                id = UUID.randomUUID().toString(),
                                pageId = page.id,
                                kind = kind,
                                quoteDate = if (kind == "quote") now else null,
                                signature = if (kind == "letter") authorProfile.signature else "",
                            ))
                        }
                    }
                }
            }
            result.onSuccess {
                onCreated(page)
            }.onFailure { operationError = "YouniScript couldn't create this page. Try again." }
        }
    }

    private fun createPageFromTemplate(template: WritingTemplate, onCreated: (Page) -> Unit) {
        val now = System.currentTimeMillis()
        val page = Page(
            id = UUID.randomUUID().toString(),
            title = template.title,
            body = template.body,
            createdAt = now,
            updatedAt = now,
            pageStyleId = defaultPageStyleId,
            tags = org.json.JSONArray().put("user-template").toString(),
        )
        lifecycleScope.launch {
            runCatching { writeMutex.withLock { dao.save(page) } }
                .onSuccess { onCreated(page) }
                .onFailure { operationError = "YouniScript couldn't create a page from that template. Your template is still saved." }
        }
    }

    private fun createJournal(title: String, description: String, onCreated: (Journal) -> Unit) {
        val now = System.currentTimeMillis()
        val journal = Journal(UUID.randomUUID().toString(), title, description, now, now)
        lifecycleScope.launch {
            runCatching { journalDao.saveJournal(journal) }.onSuccess { onCreated(journal) }
                .onFailure { operationError = "YouniScript couldn't create this journal. Try again." }
        }
    }

    private fun createJournalEntry(journal: Journal, onCreated: (Page) -> Unit) {
        val now = System.currentTimeMillis()
        val page = Page(UUID.randomUUID().toString(), "", "", now, now, pageStyleId = defaultPageStyleId)
        val entry = JournalEntry(UUID.randomUUID().toString(), journal.id, page.id, now)
        lifecycleScope.launch {
            runCatching {
                LibraryDatabase.get(this@MainActivity).withTransaction {
                    dao.save(page)
                    journalDao.saveEntry(entry)
                    journalDao.saveJournal(journal.copy(updatedAt = now))
                }
            }.onSuccess { onCreated(page) }
                .onFailure { operationError = "YouniScript couldn't create this journal entry. Your journal is unchanged." }
        }
    }

    private fun createBook(book: Book, authorBiography: String? = null, authorSeal: String? = null, onCreated: (Book) -> Unit) {
        val chapter = Chapter(UUID.randomUUID().toString(), book.id, "Chapter 1", book.createdAt, book.updatedAt, 0)
        val authorPageBody = listOfNotNull(
            authorBiography?.trim()?.takeIf(String::isNotBlank),
            authorSeal?.trim()?.takeIf(String::isNotBlank)?.let { "Personal seal: $it" },
        ).joinToString("\n\n")
        lifecycleScope.launch {
            runCatching { LibraryDatabase.get(this@MainActivity).withTransaction {
                bookDao.saveBook(book)
                bookDao.saveChapter(chapter)
                if (authorPageBody.isNotBlank()) bookDao.saveComponent(BookComponent(
                    id = UUID.randomUUID().toString(),
                    bookId = book.id,
                    type = "about-author",
                    title = "About the Author",
                    body = authorPageBody,
                    componentOrder = 0,
                    createdAt = book.createdAt,
                    updatedAt = book.updatedAt,
                ))
            } }.onSuccess { onCreated(book) }
                .onFailure { operationError = "YouniScript couldn't create this book. Try again." }
        }
    }

    private fun createBookPage(book: Book, chapter: Chapter, onCreated: (Page) -> Unit) {
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            runCatching {
                val page = Page(UUID.randomUUID().toString(), "", "", now, now, bookId = book.id, chapterId = chapter.id, bookOrder = bookDao.chapterPages(chapter.id).size)
                writeMutex.withLock { dao.save(page) }
                page
            }.onSuccess { onCreated(it) }
                .onFailure { operationError = "YouniScript couldn't add a page. Your book is still safe." }
        }
    }

    private fun createChapter(book: Book, chapters: List<Chapter>, onCreated: () -> Unit) {
        val now = System.currentTimeMillis()
        val chapter = Chapter(UUID.randomUUID().toString(), book.id, "Chapter ${chapters.size + 1}", now, now, chapters.size)
        lifecycleScope.launch {
            runCatching { bookDao.saveChapter(chapter) }.onSuccess { onCreated() }
                .onFailure { operationError = "YouniScript couldn't create this chapter. Your book is still safe." }
        }
    }

    private fun duplicatePage(page: Page, onDuplicated: (Page) -> Unit) {
        saveState = SaveState.Saving
        val now = System.currentTimeMillis()
        val duplicate = page.copy(
            id = UUID.randomUUID().toString(),
            title = if (page.title.isBlank()) "Copy of untitled page" else "Copy of ${page.title}",
            createdAt = now,
            updatedAt = now,
        )
        lifecycleScope.launch {
            val result = try {
                if (!autosaver.saveNow(page)) error("Original page could not be saved")
                writeMutex.withLock { dao.save(duplicate) }
                Result.success(Unit)
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
            if (result.isSuccess) {
                saveState = SaveState.Saved
                onDuplicated(duplicate)
            } else {
                saveState = SaveState.Failed
                operationError = "YouniScript couldn't make a copy of this page. Your original page is still safe."
            }
        }
    }

    private fun deletePage(page: Page, onDeleted: () -> Unit) {
        lifecycleScope.launch {
            val result = try {
                check(autosaver.saveNow(page)) { "The latest writing could not be saved" }
                writeMutex.withLock { dao.moveToTrash(page.id, System.currentTimeMillis()) }
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
            result.onSuccess {
                saveState = SaveState.Saved
                onDeleted()
            }.onFailure { operationError = "YouniScript couldn't move this page to Trash. It is still safe." }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        appLockConfigured = AppLockCrypto.isEnabled(this)
        val backgroundedAt = AppLockCrypto.backgroundedAt(this)
        val restoredLockState = if (savedInstanceState?.containsKey("app_locked") == true) savedInstanceState.getBoolean("app_locked") else null
        appLocked = appLockConfigured && (restoredLockState
            ?: AppLockCrypto.shouldLockAfterBackground(backgroundedAt, AppLockCrypto.autoLockTimeout(this), System.currentTimeMillis()))
        if (appLockConfigured) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val writingPreferences = getSharedPreferences("writing", MODE_PRIVATE)
        defaultPageStyleId = PageStyles.find(writingPreferences.getString("defaultPageStyle", "modern-paper")).id
        val authorPreferences = getSharedPreferences("author_profile", MODE_PRIVATE)
        authorProfile = AuthorProfile(
            name = authorPreferences.getString("name", "").orEmpty(),
            biography = authorPreferences.getString("biography", "").orEmpty(),
            signature = authorPreferences.getString("signature", "").orEmpty(),
            seal = authorPreferences.getString("seal", "").orEmpty(),
        )
        val templatePreferences = getSharedPreferences("writing_templates", MODE_PRIVATE)
        writingTemplates = WritingTemplateCodec.decode(templatePreferences.getString("templates", "[]"))
        val proofPreferences = getSharedPreferences("youniproof", MODE_PRIVATE)
        val dictionary = proofPreferences.getStringSet("dictionary", emptySet()).orEmpty().toSet()
        val replacements = proofPreferences.getStringSet("replacements", emptySet()).orEmpty().mapNotNull { row ->
            val parts = row.split("=>", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
        }.toMap()
        proofOptions = ProofOptions(
            spelling = proofPreferences.getBoolean("spelling", true),
            grammar = proofPreferences.getBoolean("grammar", true),
            punctuation = proofPreferences.getBoolean("punctuation", true),
            style = proofPreferences.getBoolean("style", false),
            dictionary = dictionary,
            replacements = replacements,
        )
        setContent {
            val appearancePreferences = remember { getSharedPreferences("appearance", MODE_PRIVATE) }
            var appearance by remember { mutableStateOf(appearancePreferences.getString("theme", "system") ?: "system") }
            val accessibilityPreferences = remember { getSharedPreferences("accessibility", MODE_PRIVATE) }
            var accessibilityTextSize by remember { mutableStateOf(accessibilityPreferences.getString("textSize", "system") ?: "system") }
            val interfaceTextScale = when (accessibilityTextSize) {
                "small" -> 0.9f
                "large" -> 1.15f
                "extra-large" -> 1.3f
                else -> 1f
            }
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity, interfaceTextScale) {
                Density(systemDensity.density, systemDensity.fontScale * interfaceTextScale)
            }
            val readingPreferences = remember { getSharedPreferences("reading", MODE_PRIVATE) }
            var defaultReadingMode by remember { mutableStateOf(readingPreferences.getString("defaultMode", "continuous") ?: "continuous") }
            var readingSpeed by remember { androidx.compose.runtime.mutableFloatStateOf(readingPreferences.getFloat("ttsSpeed", 1f)) }
            val darkTheme = when (appearance) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            YouniScriptTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalYouniInterfaceScale provides interfaceTextScale,
                    LocalDensity provides scaledDensity,
                ) {
                if (appLocked) {
                    AppLockScreen(
                        onUnlock = { pin ->
                            val unlocked = try { AppLockCrypto.verifyPin(this@MainActivity, pin) } catch (_: Exception) { false }
                            if (unlocked) {
                                appLocked = false
                                AppLockCrypto.setBackgroundedAt(this@MainActivity, 0L)
                            }
                            unlocked
                        },
                        onBiometric = ::showBiometricPrompt,
                    )
                } else {
                val activityContext = LocalContext.current
                var pendingEncryptedExportPassword by remember { mutableStateOf<CharArray?>(null) }
                var pendingEncryptedImportUri by remember { mutableStateOf<Uri?>(null) }
                var pendingBookExportSnapshot by remember { mutableStateOf<BookExportSnapshot?>(null) }
                var pendingPhotoPageId by remember { mutableStateOf<String?>(null) }
                var pendingAudioPageId by remember { mutableStateOf<String?>(null) }
                var audioRecordingPageId by remember { mutableStateOf<String?>(null) }
                val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    val pageId = pendingAudioPageId
                    pendingAudioPageId = null
                    if (granted) audioRecordingPageId = pageId
                    else Toast.makeText(activityContext, "Microphone permission is needed to record an audio note.", Toast.LENGTH_LONG).show()
                }
                val pagePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
                    val pageId = pendingPhotoPageId
                    pendingPhotoPageId = null
                    if (pageId != null && uris.isNotEmpty()) lifecycleScope.launch {
                        var saved = 0
                        uris.take(10).forEachIndexed { index, uri ->
                            runCatching {
                                val bytes = contentResolver.openInputStream(uri)?.use { it.readBounded(25 * 1024 * 1024) }
                                    ?: error("Could not read the selected photo.")
                                val attachment = persistPageAttachment(pageId, "image", "Photo ${index + 1}", bytes)
                                try {
                                    personalDao.insertAttachment(attachment)
                                    saved++
                                } catch (failure: Throwable) {
                                    safeAttachmentFile(attachment.localUri)?.delete()
                                    throw failure
                                }
                            }
                        }
                        Toast.makeText(activityContext, if (saved == uris.size) "Photos attached." else "$saved of ${uris.size} photos attached.", Toast.LENGTH_LONG).show()
                    }
                }
                val exportBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        try {
                            val contents = LibraryBackupCodec.encode(LibraryDatabase.get(this@MainActivity).createBackupSnapshot())
                            contentResolver.openOutputStream(uri)?.use { it.write(contents.toByteArray(Charsets.UTF_8)) }
                                ?: error("Could not open the selected destination")
                            Toast.makeText(activityContext, "Library backup exported.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't export the backup.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val exportTextPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        try {
                            writeReadableLibraryExport(uri, markdown = false)
                            Toast.makeText(activityContext, "Text export created.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't create the text export.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val exportMarkdownPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        try {
                            writeReadableLibraryExport(uri, markdown = true)
                            Toast.makeText(activityContext, "Markdown export created.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't create the Markdown export.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val exportBookPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
                    val snapshot = pendingBookExportSnapshot
                    pendingBookExportSnapshot = null
                    if (uri != null && snapshot != null) lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri)?.use(snapshot::writePdf) ?: error("Could not open the selected destination") }
                            Toast.makeText(activityContext, "PDF manuscript exported.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't create the PDF.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val exportBookEpubPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri ->
                    val snapshot = pendingBookExportSnapshot
                    pendingBookExportSnapshot = null
                    if (uri != null && snapshot != null) lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri)?.use(snapshot::writeEpub) ?: error("Could not open the selected destination") }
                            Toast.makeText(activityContext, "EPUB manuscript exported.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't create the EPUB.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val importBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        try {
                            val contents = contentResolver.openInputStream(uri)?.use { it.readBounded(50 * 1024 * 1024).toString(Charsets.UTF_8) }
                                ?: error("Could not open the selected backup")
                            val snapshot = LibraryBackupCodec.decode(contents)
                            LibraryDatabase.get(this@MainActivity).restoreBackup(snapshot)
                            Toast.makeText(activityContext, "Backup added to your library.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "Backup wasn't imported. Existing writing was left unchanged.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val encryptedExportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
                    val passphrase = pendingEncryptedExportPassword
                    pendingEncryptedExportPassword = null
                    if (uri == null || passphrase == null) {
                        passphrase?.fill('\u0000')
                    } else lifecycleScope.launch {
                        try {
                            val database = LibraryDatabase.get(this@MainActivity)
                            val snapshot = database.createBackupSnapshot()
                            val coverImages = snapshot.books.mapNotNull { book ->
                                val location = book.coverImageUri ?: return@mapNotNull null
                                val coverUri = Uri.parse(location)
                                val input = if (coverUri.scheme == "file") coverUri.path?.let { FileInputStream(File(it)) }
                                    else contentResolver.openInputStream(coverUri)
                                val bytes = input?.use { it.readBounded(30 * 1024 * 1024) }
                                    ?: error("A saved book cover could not be read. No backup was created.")
                                book.id to bytes
                            }.toMap()
                            val attachmentFiles = snapshot.attachments.associate { attachment ->
                                val file = safeAttachmentFile(attachment.localUri)
                                    ?: error("A page attachment couldn't be read from this device. No backup was created.")
                                val bytes = FileInputStream(file).use { it.readBounded(25 * 1024 * 1024) }
                                attachment.id to bytes
                            }
                            val archive = EncryptedLibraryBackupCodec.encode(snapshot, coverImages, passphrase, attachmentFiles)
                            contentResolver.openOutputStream(uri)?.use { it.write(archive) }
                                ?: error("Could not open the selected destination")
                            Toast.makeText(activityContext, "Encrypted backup created.", Toast.LENGTH_LONG).show()
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "YouniScript couldn't create the encrypted backup.", Toast.LENGTH_LONG).show()
                        } finally {
                            passphrase.fill('\u0000')
                        }
                    }
                }
                val encryptedImportPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    pendingEncryptedImportUri = uri
                }
                val pages by dao.observeAll().collectAsState(initial = emptyList())
                val books by bookDao.observeBooks().collectAsState(initial = emptyList())
                val journals by journalDao.observeJournals().collectAsState(initial = emptyList())
                val journalEntries by journalDao.observeAllEntries().collectAsState(initial = emptyList())
                val collections by personalDao.observeCollections().collectAsState(initial = emptyList())
                val glossaryTerms by personalDao.observeGlossary().collectAsState(initial = emptyList())
                val allAttachments by personalDao.observeAllAttachments().collectAsState(initial = emptyList())
                val allChapters by bookDao.observeAllChapters().collectAsState(initial = emptyList())
                val allBookComponents by bookDao.observeAllComponents().collectAsState(initial = emptyList())
                var editing by remember { mutableStateOf<Page?>(null) }
                var editingPageId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }
                var drawingPageId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedJournalId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
                var showingGlossary by rememberSaveable { mutableStateOf(false) }
                var editingCollection by remember { mutableStateOf<CollectionRecord?>(null) }
                var showCreateCollection by rememberSaveable { mutableStateOf(false) }
                var editingJournal by remember { mutableStateOf<Journal?>(null) }
                var showCreateJournal by rememberSaveable { mutableStateOf(false) }
                var bookMode by rememberSaveable { mutableStateOf("overview") }
                var readingChapterId by rememberSaveable { mutableStateOf<String?>(null) }
                var showCreateBook by rememberSaveable { mutableStateOf(false) }
                var showingSettings by rememberSaveable { mutableStateOf(false) }
                var showBookDetailsId by rememberSaveable { mutableStateOf<String?>(null) }
                var showFrontMatterId by rememberSaveable { mutableStateOf<String?>(null) }
                var editingBookDefaultStyleId by rememberSaveable { mutableStateOf<String?>(null) }
                var focusBodyOnOpen by rememberSaveable { mutableStateOf(false) }
                var pendingEditorTool by rememberSaveable { mutableStateOf<String?>(null) }
                fun openEditor(page: Page, inheritedStyleId: String? = null, focus: Boolean = false, openTool: String? = null) {
                    editingPageId = page.id
                    editingBookDefaultStyleId = inheritedStyleId
                    focusBodyOnOpen = focus
                    pendingEditorTool = openTool
                    editing = page
                }
                fun closeEditor() {
                    editing = null
                    editingPageId = null
                    focusBodyOnOpen = false
                }
                val importTextPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) lifecycleScope.launch {
                        try {
                            val bytes = contentResolver.openInputStream(uri)?.use { it.readBounded(10 * 1024 * 1024) }
                                ?: error("Could not open the selected document")
                            val body = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                            val title = uri.lastPathSegment.orEmpty().substringAfterLast('/').substringAfterLast(':').substringBeforeLast('.').take(120).ifBlank { "Imported writing" }
                            val now = System.currentTimeMillis()
                            val imported = Page(UUID.randomUUID().toString(), title, body, now, now, pageStyleId = defaultPageStyleId)
                            runCatching { writeMutex.withLock { dao.save(imported) } }
                                .onSuccess { openEditor(imported, focus = true) }
                                .onFailure { operationError = "The document couldn't be imported. Your Library is unchanged." }
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            Toast.makeText(activityContext, "The document couldn't be imported.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                LaunchedEffect(pages, editingPageId, editing) {
                    if (editing == null && editingPageId != null) {
                        pages.firstOrNull { it.id == editingPageId }?.let { restored ->
                            editingBookDefaultStyleId = allChapters.firstOrNull { it.id == restored.chapterId }?.styleOverrideId
                                ?: books.firstOrNull { it.id == restored.bookId }?.defaultPageStyleId
                                ?: editingBookDefaultStyleId
                            focusBodyOnOpen = false
                            editing = restored
                        }
                    }
                }
                val immersive = editing != null || (selectedBookId != null && bookMode == "reading")
                val visibleBookStyle = books.firstOrNull { it.id == selectedBookId }?.defaultPageStyleId
                val activeStyleId = editing?.pageStyleId ?: editingBookDefaultStyleId ?: visibleBookStyle
                val lightPage = activeStyleId != "dark-journal"
                SideEffect {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    val lightSystemBars = if (editing == null) !darkTheme else lightPage
                    controller.isAppearanceLightStatusBars = lightSystemBars
                    controller.isAppearanceLightNavigationBars = lightSystemBars
                }
                LaunchedEffect(immersive) {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (immersive) controller.hide(WindowInsetsCompat.Type.systemBars())
                    else controller.show(WindowInsetsCompat.Type.systemBars())
                }
                BackHandler(enabled = editing != null || selectedBookId != null || selectedJournalId != null || selectedCollectionId != null || showingGlossary || showingSettings) {
                    editing?.let { current -> saveAndClose(current) { closeEditor() } } ?: run {
                        if (showingSettings) showingSettings = false else if (showingGlossary) showingGlossary = false else if (selectedCollectionId != null) selectedCollectionId = null else
                        if (bookMode != "overview") bookMode = "overview" else if (selectedJournalId != null) selectedJournalId = null else selectedBookId = null
                    }
                }
                Surface(Modifier.fillMaxSize(), color = Paper) {
                    val page = editing
                    if (page == null) {
                        val bookId = selectedBookId
                        if (bookId == null) {
                            val collectionId = selectedCollectionId
                            val journalId = selectedJournalId
                            if (collectionId != null) {
                                val collection = collections.firstOrNull { it.id == collectionId }
                                val memberships by personalDao.observeCollectionItems(collectionId).collectAsState(initial = emptyList())
                                val collectionContent = buildList {
                                    pages.filterNot { it.isTrashed }.forEach { add(CollectionContent("page", it.id, it.title.ifBlank { "Untitled page" }, it.body.lineSequence().firstOrNull().orEmpty())) }
                                    books.forEach { add(CollectionContent("book", it.id, it.title, it.author)) }
                                    journals.forEach { add(CollectionContent("journal", it.id, it.title, it.description)) }
                                }
                                if (collection == null) Text("Opening collection…", Modifier.padding(24.dp), color = MutedInk)
                                else CollectionScreen(
                                    collection, memberships, collectionContent,
                                    onBack = { selectedCollectionId = null },
                                    onAdd = { item, _ -> lifecycleScope.launch { personalDao.addCollectionItem(CollectionItem(collection.id, item.type, item.id, (memberships.maxOfOrNull { it.itemOrder } ?: -1) + 1)) } },
                                    onRemove = { item -> lifecycleScope.launch { personalDao.removeCollectionItem(item.collectionId, item.contentType, item.contentId) } },
                                    onMove = { reordered -> lifecycleScope.launch { LibraryDatabase.get(this@MainActivity).withTransaction { reordered.forEachIndexed { index, item -> personalDao.saveCollectionItem(item.copy(itemOrder = index)) } } } },
                                    onOpen = { item -> when (item.type) {
                                        "page" -> pages.firstOrNull { it.id == item.id }?.let { target -> openEditor(target, allChapters.firstOrNull { it.id == target.chapterId }?.styleOverrideId ?: books.firstOrNull { it.id == target.bookId }?.defaultPageStyleId) }
                                        "book" -> { selectedCollectionId = null; selectedBookId = item.id; bookMode = "overview" }
                                        "journal" -> { selectedCollectionId = null; selectedJournalId = item.id }
                                    } },
                                    onEdit = { editingCollection = collection },
                                    onDelete = { lifecycleScope.launch { personalDao.deleteCollection(collection.id); selectedCollectionId = null } },
                                )
                            } else if (showingGlossary) {
                                GlossaryScreen(
                                    terms = glossaryTerms,
                                    pages = pages.filterNot { it.isTrashed },
                                    onBack = { showingGlossary = false },
                                    onSave = { lifecycleScope.launch { personalDao.saveGlossaryTerm(it) } },
                                    onDelete = { term -> lifecycleScope.launch { personalDao.deleteGlossaryTerm(term.id) } },
                                )
                            } else if (journalId != null) {
                                val journal = journals.firstOrNull { it.id == journalId }
                                val entries by journalDao.observeEntries(journalId).collectAsState(initial = emptyList())
                                if (journal == null) Text("Opening journal…", Modifier.padding(24.dp), color = MutedInk)
                                else JournalScreen(
                                    journal = journal,
                                    entries = entries,
                                    onBack = { selectedJournalId = null },
                                    onNewEntry = { createJournalEntry(journal) { openEditor(it, focus = true) } },
                                    onOpenEntry = { openEditor(it) },
                                    onEdit = { editingJournal = it },
                                    onArchive = { updated -> lifecycleScope.launch { journalDao.saveJournal(updated.copy(isArchived = !updated.isArchived, updatedAt = System.currentTimeMillis())) } },
                                    onDelete = { removed -> lifecycleScope.launch { journalDao.deleteJournal(removed.id); selectedJournalId = null } },
                                )
                            } else if (showingSettings) SettingsScreen(
                                onBack = { showingSettings = false },
                                selectedTheme = appearance,
                                accessibilityTextSize = accessibilityTextSize,
                                onAccessibilityTextSizeSelected = { selected ->
                                    accessibilityTextSize = selected
                                    accessibilityPreferences.edit().putString("textSize", selected).apply()
                                },
                                defaultPageStyleId = defaultPageStyleId,
                                authorProfile = authorProfile,
                                onAuthorProfileChanged = { updated ->
                                    authorProfile = updated
                                    authorPreferences.edit()
                                        .putString("name", updated.name)
                                        .putString("biography", updated.biography)
                                        .putString("signature", updated.signature)
                                        .putString("seal", updated.seal)
                                        .apply()
                                },
                                writingTemplates = writingTemplates,
                                onWritingTemplatesChanged = { updated ->
                                    writingTemplates = updated.take(100)
                                    templatePreferences.edit().putString("templates", WritingTemplateCodec.encode(writingTemplates)).apply()
                                },
                                attachments = allAttachments,
                                defaultReadingMode = defaultReadingMode,
                                readingSpeed = readingSpeed,
                                onDefaultReadingModeSelected = { selected ->
                                    defaultReadingMode = selected
                                    readingPreferences.edit().putString("defaultMode", selected).apply()
                                },
                                onReadingSpeedChanged = { selected ->
                                    readingSpeed = selected
                                    readingPreferences.edit().putFloat("ttsSpeed", selected).apply()
                                },
                                proofOptions = proofOptions,
                                onProofOptionsChanged = { updated ->
                                    proofOptions = updated
                                    proofPreferences.edit()
                                        .putBoolean("spelling", updated.spelling)
                                        .putBoolean("grammar", updated.grammar)
                                        .putBoolean("punctuation", updated.punctuation)
                                        .putBoolean("style", updated.style)
                                        .putStringSet("dictionary", updated.dictionary)
                                        .putStringSet("replacements", updated.replacements.map { (from, to) -> "$from=>$to" }.toSet())
                                        .apply()
                                },
                                onDefaultPageStyleSelected = { selected ->
                                    defaultPageStyleId = selected
                                    writingPreferences.edit().putString("defaultPageStyle", selected).apply()
                                },
                                onThemeSelected = { selected ->
                                    appearance = selected
                                    appearancePreferences.edit().putString("theme", selected).apply()
                                },
                                onExportJson = { exportBackupPicker.launch("YouniScript-library.json") },
                                onExportText = { exportTextPicker.launch("YouniScript-library.txt") },
                                onExportMarkdown = { exportMarkdownPicker.launch("YouniScript-library.md") },
                                onSelectJsonImport = { importBackupPicker.launch(arrayOf("application/json", "text/*")) },
                                onExportEncryptedBackup = { passphrase ->
                                    pendingEncryptedExportPassword?.fill('\u0000')
                                    pendingEncryptedExportPassword = passphrase
                                    encryptedExportPicker.launch("YouniScript-library.youni")
                                },
                                onSelectEncryptedBackup = { encryptedImportPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                                onImportEncryptedBackup = { passphrase ->
                                    val uri = pendingEncryptedImportUri
                                    if (uri == null) passphrase.fill('\u0000') else lifecycleScope.launch {
                                        val writtenFiles = mutableListOf<File>()
                                        try {
                                            val encrypted = contentResolver.openInputStream(uri)?.use { it.readBounded(100 * 1024 * 1024) }
                                                ?: error("Could not read the selected backup")
                                            val decoded = EncryptedLibraryBackupCodec.decode(encrypted, passphrase)
                                            val coverDirectory = File(filesDir, "book-covers").apply { mkdirs() }
                                            val coverLocations = decoded.coverImages.mapValues { (_, bytes) ->
                                                val file = File(coverDirectory, "${UUID.randomUUID()}.cover")
                                                check(file.createNewFile()) { "Could not prepare a cover image" }
                                                writtenFiles += file
                                                file.outputStream().use { it.write(bytes) }
                                                Uri.fromFile(file).toString()
                                            }
                                            val attachmentLocations = decoded.snapshot.attachments.associate { attachment ->
                                                val bytes = decoded.attachmentFiles[attachment.id] ?: error("A page attachment is missing from the encrypted backup.")
                                                val stored = persistPageAttachment(attachment.pageId, attachment.mediaType, attachment.displayName, bytes)
                                                val file = safeAttachmentFile(stored.localUri) ?: error("Could not prepare a page attachment.")
                                                writtenFiles += file
                                                attachment.id to stored.localUri
                                            }
                                            val portableSnapshot = decoded.snapshot.copy(
                                                books = decoded.snapshot.books.map { book -> book.copy(coverImageUri = coverLocations[book.id]) },
                                                attachments = decoded.snapshot.attachments.map { attachment -> attachment.copy(localUri = attachmentLocations.getValue(attachment.id)) },
                                            )
                                            LibraryDatabase.get(this@MainActivity).restoreBackup(portableSnapshot)
                                            writtenFiles.clear()
                                            pendingEncryptedImportUri = null
                                            Toast.makeText(activityContext, "Encrypted backup restored.", Toast.LENGTH_LONG).show()
                                        } catch (failure: Throwable) {
                                            if (failure is CancellationException) throw failure
                                            writtenFiles.forEach(File::delete)
                                            Toast.makeText(activityContext, "Backup wasn't restored. Existing writing was left unchanged.", Toast.LENGTH_LONG).show()
                                        } finally {
                                            passphrase.fill('\u0000')
                                        }
                                    }
                                },
                                encryptedBackupSelected = pendingEncryptedImportUri != null,
                                onDismissEncryptedBackup = { pendingEncryptedImportUri = null },
                                appLockEnabled = appLockConfigured,
                                appLockTimeout = AppLockCrypto.autoLockTimeout(this@MainActivity),
                                onSetAppPin = { current, next ->
                                    runCatching {
                                        if (current != null && !AppLockCrypto.verifyPin(this@MainActivity, current)) return@runCatching false
                                        AppLockCrypto.savePin(this@MainActivity, next)
                                        appLockConfigured = true
                                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                        true
                                    }.getOrDefault(false)
                                },
                                onDisableAppLock = { current ->
                                    val valid = runCatching { AppLockCrypto.verifyPin(this@MainActivity, current) }.getOrDefault(false)
                                    if (valid) {
                                        val cleared = AppLockCrypto.clearPin(this@MainActivity)
                                        if (cleared) {
                                            appLockConfigured = false
                                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                        }
                                        cleared
                                    } else {
                                        false
                                    }
                                },
                                onAppLockTimeoutChanged = { AppLockCrypto.updateTimeout(this@MainActivity, it) },
                                onLockNow = { flushSave(); appLocked = true },
                            ) else LibraryScreen(
                                pages = pages,
                                books = books,
                                journals = journals,
                                collections = collections,
                                journalEntries = journalEntries,
                                allChapters = allChapters,
                                error = operationError,
                                onDismissError = { operationError = null },
                                onCreate = { kind -> createPage(kind) { created -> openEditor(created, focus = true) } },
                                onCreateMedia = { type ->
                                    createPage("page") { created ->
                                        openEditor(created)
                                        when (type) {
                                            "image" -> {
                                                pendingPhotoPageId = created.id
                                                pagePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                            "drawing" -> drawingPageId = created.id
                                            "audio" -> {
                                                pendingAudioPageId = created.id
                                                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    pendingAudioPageId = null
                                                    audioRecordingPageId = created.id
                                                } else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                },
                                onCreateBook = { showCreateBook = true },
                                writingTemplates = writingTemplates,
                                onUseWritingTemplate = { template -> createPageFromTemplate(template) { openEditor(it, focus = true) } },
                                onOpenPageTool = { target, tool -> openEditor(target, openTool = tool) },
                                onCreateChapter = { targetBook ->
                                    createChapter(targetBook, allChapters.filter { it.bookId == targetBook.id }) {
                                        selectedBookId = targetBook.id
                                        bookMode = "overview"
                                    }
                                },
                                onCreateSection = { targetBook, targetChapter ->
                                    val now = System.currentTimeMillis()
                                    lifecycleScope.launch {
                                        val order = withContext(Dispatchers.IO) { bookDao.snapshotSections().count { it.chapterId == targetChapter.id } }
                                        runCatching { bookDao.saveSection(Section(UUID.randomUUID().toString(), targetBook.id, targetChapter.id, "Section ${order + 1}", order, now, now)) }
                                            .onSuccess { selectedBookId = targetBook.id; bookMode = "outline" }
                                            .onFailure { operationError = "YouniScript couldn't create the section. Your chapter remains unchanged." }
                                    }
                                },
                                onCreateJournal = { showCreateJournal = true },
                                onOpenJournal = { selectedJournalId = it.id },
                                onCreateJournalEntry = { journal -> createJournalEntry(journal) { openEditor(it, focus = true) } },
                                onOpenCollection = { selectedCollectionId = it.id },
                                onCreateCollection = { showCreateCollection = true },
                                onOpenGlossary = { showingGlossary = true },
                                onImportText = { importTextPicker.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "application/octet-stream")) },
                                onImportJson = { importBackupPicker.launch(arrayOf("application/json", "text/*")) },
                                onImportEncryptedBackup = { showingSettings = true; encryptedImportPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
                                onRestorePage = { page -> lifecycleScope.launch { dao.restoreFromTrash(page.id) } },
                                onDeletePermanently = { page -> lifecycleScope.launch { dao.deleteById(page.id) } },
                                onEmptyTrash = { lifecycleScope.launch { dao.emptyTrash() } },
                                onSettings = { showingSettings = true },
                                onOpenBook = { selectedBookId = it; bookMode = "overview"; readingChapterId = null },
                                onOpen = { openEditor(it) },
                            )
                        } else {
                            val book = books.firstOrNull { it.id == bookId }
                            val chapters by bookDao.observeChapters(bookId).collectAsState(initial = emptyList())
                            val bookPages by bookDao.observeBookPages(bookId).collectAsState(initial = emptyList())
                            val bookSections by bookDao.observeSections(bookId).collectAsState(initial = emptyList())
                            val components by bookDao.observeComponents(bookId).collectAsState(initial = emptyList())
                            if (book == null) {
                                Text("Opening book…", Modifier.padding(24.dp), color = MutedInk)
                            } else when (bookMode) {
                                "outline" -> BookOutlineScreen(
                                    book, chapters, bookPages, bookSections, components,
                                    onBack = { bookMode = "overview" },
                                    onOpenPage = { selectedPage -> openEditor(selectedPage, chapters.firstOrNull { it.id == selectedPage.chapterId }?.styleOverrideId ?: book.defaultPageStyleId) },
                                    onOpenFrontMatter = { showFrontMatterId = it.id },
                                    onOpenChapter = { chapter -> readingChapterId = chapter.id; bookMode = "reading" },
                                    onMoveChapter = { from, by ->
                                        val to = (from + by).coerceIn(0, chapters.lastIndex)
                                        if (to != from) lifecycleScope.launch {
                                            bookDao.reorderChapters(bookId, chapters.toMutableList().apply { add(to, removeAt(from)) }.map { it.id })
                                        }
                                    },
                                    onMovePage = { chapter, from, by ->
                                        val chapterPages = bookPages.filter { it.chapterId == chapter.id }.sortedBy { it.bookOrder ?: Int.MAX_VALUE }
                                        val to = (from + by).coerceIn(0, chapterPages.lastIndex)
                                        if (from != to) lifecycleScope.launch {
                                            runCatching { bookDao.reorderPages(chapter.id, chapterPages.toMutableList().apply { add(to, removeAt(from)) }.map { it.id }) }
                                        }
                                    },
                                    onMovePageToChapter = { moved, destination ->
                                        lifecycleScope.launch { runCatching { bookDao.movePageToChapter(moved.id, destination.id) } }
                                    },
                                    onMovePageToSection = { moved, section ->
                                        lifecycleScope.launch { runCatching { bookDao.placePageInSection(moved.id, section?.id) } }
                                    },
                                    onCreateSection = { chapter, title ->
                                        val now = System.currentTimeMillis()
                                        lifecycleScope.launch { runCatching { bookDao.saveSection(Section(UUID.randomUUID().toString(), bookId, chapter.id, title, bookSections.count { it.chapterId == chapter.id }, now, now)) } }
                                    },
                                    onRenameSection = { section, title ->
                                        lifecycleScope.launch { runCatching { bookDao.saveSection(section.copy(title = title, updatedAt = System.currentTimeMillis())) } }
                                    },
                                    onDeleteSection = { section -> lifecycleScope.launch { runCatching { bookDao.deleteSectionSafely(section.id) } } },
                                    onMoveSection = { chapter, from, by ->
                                        val sections = bookSections.filter { it.chapterId == chapter.id }.sortedBy { it.sectionOrder }
                                        val to = (from + by).coerceIn(0, sections.lastIndex)
                                        if (from != to) lifecycleScope.launch { runCatching { bookDao.reorderSections(chapter.id, sections.toMutableList().apply { add(to, removeAt(from)) }.map { it.id }) } }
                                    },
                                    onMoveSectionToChapter = { section, destination ->
                                        lifecycleScope.launch { runCatching { bookDao.moveSectionToChapter(section.id, destination.id) } }
                                    },
                                )
                                "reading" -> BookReadingScreen(book, chapters, bookPages, bookSections, components, readingChapterId, defaultReadingMode,
                                    onBack = { bookMode = "overview" },
                                    onOpenPage = { selectedPage -> openEditor(selectedPage, chapters.firstOrNull { it.id == selectedPage.chapterId }?.styleOverrideId ?: book.defaultPageStyleId) },
                                )
                                else -> BookOverviewScreen(
                                    book, chapters, bookPages, components, bookSections, pages.filter { it.bookId == null },
                                    onBack = { selectedBookId = null },
                                    onCreateChapter = { createChapter(book, chapters) {} },
                                    onRenameChapter = { chapter, title -> lifecycleScope.launch { bookDao.saveChapter(chapter.copy(title = title, updatedAt = System.currentTimeMillis())) } },
                                    onDuplicateChapter = { chapter -> lifecycleScope.launch {
                                        runCatching { bookDao.duplicateChapter(chapter, UUID.randomUUID().toString(), System.currentTimeMillis()) }
                                            .onFailure { operationError = "YouniScript couldn't duplicate this chapter. Its original pages remain safe." }
                                    } },
                                    onDeleteChapter = { chapter -> lifecycleScope.launch {
                                        runCatching { bookDao.deleteChapterSafely(chapter.id) }
                                            .onFailure { operationError = "YouniScript couldn't delete this chapter. Its pages remain safe." }
                                    } },
                                    onCreatePage = { chapter -> createBookPage(book, chapter) { openEditor(it, chapter.styleOverrideId ?: book.defaultPageStyleId, true) } },
                                    onContinueWriting = {
                                        val latest = bookPages.maxByOrNull { it.updatedAt }
                                        if (latest != null) openEditor(latest, chapters.firstOrNull { it.id == latest.chapterId }?.styleOverrideId ?: book.defaultPageStyleId)
                                        else if (chapters.isNotEmpty()) createBookPage(book, chapters.first()) { openEditor(it, chapters.first().styleOverrideId ?: book.defaultPageStyleId, true) }
                                        else createChapter(book, chapters) { }
                                    },
                                    onOpenPage = { selectedPage -> openEditor(selectedPage, chapters.firstOrNull { it.id == selectedPage.chapterId }?.styleOverrideId ?: book.defaultPageStyleId) },
                                    onOutline = { bookMode = "outline" },
                                    onRead = { readingChapterId = null; bookMode = "reading" },
                                    onExportPdf = {
                                        pendingBookExportSnapshot = BookExportSnapshot(book, chapters, bookPages, bookSections, components)
                                        val filename = (book.title.ifBlank { "YouniScript-book" }).replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80)
                                        exportBookPdfPicker.launch("$filename.pdf")
                                    },
                                    onExportEpub = {
                                        pendingBookExportSnapshot = BookExportSnapshot(book, chapters, bookPages, bookSections, components)
                                        val filename = (book.title.ifBlank { "YouniScript-book" }).replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80)
                                        exportBookEpubPicker.launch("$filename.epub")
                                    },
                                    onEditDetails = { showBookDetailsId = it.id },
                                    onDuplicate = {
                                        lifecycleScope.launch {
                                            val now = System.currentTimeMillis()
                                            val duplicate = book.copy(id = UUID.randomUUID().toString(), title = "Copy of ${book.title}", createdAt = now, updatedAt = now)
                                            val chapterMap = chapters.associate { it.id to UUID.randomUUID().toString() }
                                            runCatching { bookDao.duplicateBook(book, duplicate, chapterMap) }
                                                .onFailure { operationError = "YouniScript couldn't duplicate this book. The original remains safe." }
                                        }
                                    },
                                    onDelete = { lifecycleScope.launch { bookDao.deleteBookSafely(bookId); selectedBookId = null } },
                                    onAddFrontMatter = { type, title, body ->
                                        val now = System.currentTimeMillis()
                                        lifecycleScope.launch {
                                            runCatching { bookDao.saveComponent(BookComponent(UUID.randomUUID().toString(), bookId, type, title, body, components.size, now, now)) }
                                                .onFailure { operationError = "YouniScript couldn't save that front matter." }
                                        }
                                    },
                                    onOpenFrontMatter = { showFrontMatterId = it.id },
                                    onAddExistingPage = { page, chapter, section -> lifecycleScope.launch {
                                        runCatching { bookDao.addExistingPageToChapter(page.id, chapter.id, section?.id) }
                                            .onFailure { operationError = "YouniScript couldn't move that page into the book. Its original page remains safe." }
                                    } },
                                )
                            }
                        }
                    } else {
                        val personalEntry by personalDao.observeEntryForPage(page.id).collectAsState(initial = null)
                        val annotations by personalDao.observeAnnotationsFor("page", page.id).collectAsState(initial = emptyList())
                        val links by personalDao.observeLinks(page.id).collectAsState(initial = emptyList())
                        val backlinks by personalDao.observeBacklinks(page.id).collectAsState(initial = emptyList())
                        val revisions by personalDao.observeRevisions(page.id).collectAsState(initial = emptyList())
                        val attachments by personalDao.observeAttachments(page.id).collectAsState(initial = emptyList())
                        EditorScreen(
                            page = page,
                            allPages = pages,
                            personalEntry = personalEntry,
                            annotations = annotations,
                            links = links,
                            backlinks = backlinks,
                            revisions = revisions,
                            attachments = attachments,
                            toolOnEntry = pendingEditorTool,
                            openDrawingOnEntry = drawingPageId == page.id,
                            inheritedPageStyleId = editingBookDefaultStyleId,
                            saveState = saveState,
                            operationError = operationError,
                            focusBodyOnOpen = focusBodyOnOpen,
                            onDismissOperationError = { operationError = null },
                            onFocusConsumed = { focusBodyOnOpen = false },
                            onToolOnEntryConsumed = { pendingEditorTool = null },
                            onBack = { saveAndClose(page) { closeEditor() } },
                            onChange = { changed -> editing = changed; scheduleSave(changed) },
                            onFocusLeft = { changed -> flushSave(changed) },
                            onRetrySave = { flushSave(page) },
                            onDuplicate = { duplicatePage(page) { openEditor(it) } },
                            onDelete = { deletePage(page) { closeEditor() } },
                            onBookmarkToggle = { changed -> editing = changed; scheduleSave(changed) },
                            onSavePersonalEntry = { value -> lifecycleScope.launch { personalDao.savePersonalEntry(value) } },
                            onSaveAnnotation = { value -> lifecycleScope.launch { personalDao.saveAnnotation(value) } },
                            onDeleteAnnotation = { value -> lifecycleScope.launch { personalDao.deleteAnnotation(value.id) } },
                            onAddLink = { target, label ->
                                lifecycleScope.launch { runCatching { personalDao.addLink(PageLink(UUID.randomUUID().toString(), page.id, target.id, label.trim().ifBlank { target.title }, System.currentTimeMillis())) } }
                            },
                            onRemoveLink = { value -> lifecycleScope.launch { personalDao.deleteLink(value.id) } },
                            onOpenLinkedPage = { target ->
                                val inherited = allChapters.firstOrNull { it.id == target.chapterId }?.styleOverrideId
                                    ?: books.firstOrNull { it.id == target.bookId }?.defaultPageStyleId
                                openEditor(target, inherited)
                            },
                            onRestoreRevision = { revision ->
                                lifecycleScope.launch {
                                    if (autosaver.saveNow(page)) {
                                        val now = System.currentTimeMillis()
                                        val restored = page.copy(title = revision.title, body = revision.body, formatting = revision.formatting, pageStyleId = revision.pageStyleId, updatedAt = now)
                                        runCatching {
                                            LibraryDatabase.get(this@MainActivity).withTransaction {
                                                personalDao.saveRevisionIfChanged(page, now)
                                                dao.save(restored)
                                            }
                                        }.onSuccess { editing = restored }
                                            .onFailure { operationError = "This version couldn't be restored. Your current writing remains open." }
                                    } else operationError = "Save the current writing before restoring an earlier version."
                                }
                            },
                            onAttachPhotos = {
                                pendingPhotoPageId = page.id
                                pagePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onSaveDrawing = { bytes -> lifecycleScope.launch {
                                runCatching {
                                    val attachment = persistPageAttachment(page.id, "drawing", "Drawing.png", bytes)
                                    try { personalDao.insertAttachment(attachment) }
                                    catch (failure: Throwable) { safeAttachmentFile(attachment.localUri)?.delete(); throw failure }
                                }.onFailure { operationError = "YouniScript couldn't save the drawing. Your page text remains safe." }
                            } },
                            onDismissDrawing = { if (drawingPageId == page.id) drawingPageId = null },
                            onDeleteAttachment = { attachment -> lifecycleScope.launch {
                                runCatching { personalDao.deleteAttachment(attachment.id); safeAttachmentFile(attachment.localUri)?.delete() }
                                    .onFailure { operationError = "YouniScript couldn't remove that attachment." }
                            } },
                            onRenameAttachment = { attachment, name -> lifecycleScope.launch {
                                personalDao.saveAttachment(attachment.copy(displayName = name))
                            } },
                            onRecordAudio = {
                                pendingAudioPageId = page.id
                                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    pendingAudioPageId = null
                                    audioRecordingPageId = page.id
                                } else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            proofOptions = proofOptions,
                        )
                    }
                }
                if (showCreateBook) CreateBookDialog(
                    onDismiss = { showCreateBook = false },
                    onCreate = { book, authorBiography, authorSeal ->
                    showCreateBook = false
                    createBook(book, authorBiography, authorSeal) { selectedBookId = it.id; bookMode = "overview" }
                    },
                    initialAuthor = authorProfile.name,
                    authorBiography = authorProfile.biography,
                    authorSeal = authorProfile.seal,
                )
                audioRecordingPageId?.let { pageId ->
                    AudioRecordingDialog(
                        onDismiss = { audioRecordingPageId = null },
                        onSave = { bytes -> lifecycleScope.launch {
                            runCatching {
                                val attachment = persistPageAttachment(pageId, "audio", "Audio note.m4a", bytes)
                                try { personalDao.insertAttachment(attachment) }
                                catch (failure: Throwable) { safeAttachmentFile(attachment.localUri)?.delete(); throw failure }
                                Toast.makeText(activityContext, "Audio note attached.", Toast.LENGTH_SHORT).show()
                            }.onFailure { operationError = "YouniScript couldn't save that audio note." }
                        } },
                    )
                }
                if (showCreateCollection) CollectionEditorDialog(
                    existing = null,
                    onDismiss = { showCreateCollection = false },
                    onSave = { collection -> showCreateCollection = false; lifecycleScope.launch { personalDao.saveCollection(collection) } },
                )
                editingCollection?.let { collection -> CollectionEditorDialog(
                    existing = collection,
                    onDismiss = { editingCollection = null },
                    onSave = { updated -> editingCollection = null; lifecycleScope.launch { personalDao.saveCollection(updated) } },
                ) }
                if (showCreateJournal) JournalEditorDialog(
                    journal = null,
                    onDismiss = { showCreateJournal = false },
                    onSave = { title, description ->
                        showCreateJournal = false
                        createJournal(title, description) { journal ->
                            selectedJournalId = journal.id
                        }
                    },
                )
                editingJournal?.let { journal -> JournalEditorDialog(
                    journal = journal,
                    onDismiss = { editingJournal = null },
                    onSave = { title, description ->
                        editingJournal = null
                        lifecycleScope.launch { journalDao.saveJournal(journal.copy(title = title, description = description, updatedAt = System.currentTimeMillis())) }
                    },
                ) }
                books.firstOrNull { it.id == showBookDetailsId }?.let { book -> EditBookDetailsDialog(book,
                    onDismiss = { showBookDetailsId = null },
                    onSave = { updated -> showBookDetailsId = null; lifecycleScope.launch { bookDao.saveBook(updated) } },
                ) }
                allBookComponents.firstOrNull { it.id == showFrontMatterId }?.let { component -> FrontMatterEditorDialog(component,
                    onDismiss = { showFrontMatterId = null },
                    onSave = { updated -> showFrontMatterId = null; lifecycleScope.launch { bookDao.saveComponent(updated) } },
                ) }
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (AppLockCrypto.isEnabled(this)) {
            val backgroundedAt = AppLockCrypto.backgroundedAt(this)
            if (backgroundedAt > 0L && System.currentTimeMillis() - backgroundedAt >= AppLockCrypto.autoLockTimeout(this)) {
                appLocked = true
            }
            AppLockCrypto.setBackgroundedAt(this, 0L)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("app_locked", appLocked)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        flushSave()
        if (AppLockCrypto.isEnabled(this) && !isChangingConfigurations) {
            AppLockCrypto.setBackgroundedAt(this, System.currentTimeMillis())
        }
        super.onStop()
    }

    @Suppress("DEPRECATION")
    private fun showBiometricPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val builder = BiometricPrompt.Builder(this)
                .setTitle("Unlock YouniScript")
                .setSubtitle("Use biometrics to open your private library")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else {
                builder.setNegativeButton("Use app PIN", mainExecutor) { _, _ -> }
            }
            builder.build().authenticate(CancellationSignal(), mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLocked = false
                    AppLockCrypto.setBackgroundedAt(this@MainActivity, 0L)
                }
            })
        } catch (_: Exception) {
            Toast.makeText(this, "Device authentication isn't available. Use your app PIN.", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    pages: List<Page>,
    books: List<Book>,
    journals: List<Journal>,
    collections: List<CollectionRecord>,
    journalEntries: List<JournalEntryWithPage>,
    allChapters: List<Chapter>,
    writingTemplates: List<WritingTemplate>,
    onUseWritingTemplate: (WritingTemplate) -> Unit,
    onOpenPageTool: (Page, String) -> Unit,
    error: String?,
    onDismissError: () -> Unit,
    onCreate: (String) -> Unit,
    onCreateMedia: (String) -> Unit,
    onCreateBook: () -> Unit,
    onCreateChapter: (Book) -> Unit,
    onCreateSection: (Book, Chapter) -> Unit,
    onCreateJournal: () -> Unit,
    onOpenJournal: (Journal) -> Unit,
    onCreateJournalEntry: (Journal) -> Unit,
    onOpenCollection: (CollectionRecord) -> Unit,
    onCreateCollection: () -> Unit,
    onOpenGlossary: () -> Unit,
    onRestorePage: (Page) -> Unit,
    onDeletePermanently: (Page) -> Unit,
    onEmptyTrash: () -> Unit,
    onImportText: () -> Unit,
    onImportJson: () -> Unit,
    onImportEncryptedBackup: () -> Unit,
    onSettings: () -> Unit,
    onOpenBook: (String) -> Unit,
    onOpen: (Page) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var libraryActionsExpanded by remember { mutableStateOf(false) }
    var pendingPageTool by rememberSaveable { mutableStateOf<String?>(null) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var showCreateHub by rememberSaveable { mutableStateOf(false) }
    var manuscriptChooser by rememberSaveable { mutableStateOf("") }
    var sectionChooserBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var showJournalChooser by rememberSaveable { mutableStateOf(false) }
    var showingTrash by rememberSaveable { mutableStateOf(false) }
    var showTimeline by rememberSaveable { mutableStateOf(false) }
    var timelineType by rememberSaveable { mutableStateOf("All writing") }
    var timelineTypeMenu by remember { mutableStateOf(false) }
    var pageSort by rememberSaveable { mutableStateOf("Recently edited") }
    var pageType by rememberSaveable { mutableStateOf("All types") }
    var pageSortMenu by remember { mutableStateOf(false) }
    var pageTypeMenu by remember { mutableStateOf(false) }
    var confirmEmptyTrash by rememberSaveable { mutableStateOf(false) }
    var permanentDeletePage by remember { mutableStateOf<Page?>(null) }
    val libraryPages = pages.filterNot { it.isTrashed }
    val trashPages = pages.filter { it.isTrashed }
    val matchingPages = filterLibraryPages(if (showingTrash) trashPages else libraryPages, searchQuery)
    val journalPageIds = journalEntries.mapTo(mutableSetOf()) { it.page.id }
    val standalonePages = matchingPages.filter { page ->
        (if (pageType == "Journal entries" && !showingTrash) page.id in journalPageIds else page.id !in journalPageIds) &&
            (showingTrash || (showBookmarks && page.isBookmarked) || (!showBookmarks && page.bookId == null)) &&
            (showingTrash || pageType == "All types" || when (pageType) {
                "Thoughts" -> page.tags.contains("thought", true)
                "Quotes" -> page.tags.contains("quote", true)
                "Dreams" -> page.tags.contains("dream", true)
                "Letters" -> page.tags.contains("letter", true)
                "Journal entries" -> page.id in journalPageIds
                else -> true
            })
    }.let { filtered ->
        when (pageSort) {
            "Newest" -> filtered.sortedByDescending { it.createdAt }
            "Oldest" -> filtered.sortedBy { it.createdAt }
            "Alphabetical" -> filtered.sortedBy { it.title.ifBlank { it.body }.lowercase() }
            else -> filtered.sortedByDescending { it.updatedAt }
        }
    }
    val today = LocalDate.now()
    val onThisDayPages = if (searchQuery.isBlank() && !showBookmarks && !showingTrash) libraryPages.filter { page ->
        val created = Instant.ofEpochMilli(page.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        created.year < today.year && created.monthValue == today.monthValue && created.dayOfMonth == today.dayOfMonth
    }.sortedByDescending { it.createdAt }.take(2) else emptyList()
    val matchingBooks = if (showingTrash) emptyList() else filterLibraryBooks(books, allChapters, libraryPages, searchQuery)
    val matchingJournals = if (showingTrash) emptyList() else journals.filter { journal ->
        searchQuery.isBlank() || listOf(journal.title, journal.description).any { it.contains(searchQuery.trim(), ignoreCase = true) } ||
            journalEntries.any { it.journalId == journal.id && it.page.id in matchingPages.map(Page::id) }
    }
    val matchingCollections = if (showingTrash) emptyList() else collections.filter { collection ->
        searchQuery.isBlank() || listOf(collection.title, collection.description).any { it.contains(searchQuery.trim(), ignoreCase = true) }
    }
    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            "YOUNISCRIPT",
                            color = Accent,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (showingTrash) "Trash" else "Library",
                            color = Ink,
                            fontFamily = FontFamily.Serif,
                            fontSize = 23.sp,
                            lineHeight = 26.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                },
                expandedHeight = 64.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale,
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Paper),
                actions = {
                    TextButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "Open Settings" }) {
                        Text("Settings", color = Accent)
                    }
                    Box {
                        TextButton(onClick = { libraryActionsExpanded = true }) { Text("More", color = Accent) }
                        DropdownMenu(expanded = libraryActionsExpanded, onDismissRequest = { libraryActionsExpanded = false }) {
                            if (!showingTrash) DropdownMenuItem(
                                text = { Text("Timeline") },
                                onClick = { libraryActionsExpanded = false; showTimeline = true },
                            )
                            if (!showingTrash) DropdownMenuItem(
                                text = { Text(if (showBookmarks) "All pages" else "Bookmarks (${libraryPages.count { it.isBookmarked }})") },
                                onClick = { libraryActionsExpanded = false; showBookmarks = !showBookmarks },
                            )
                            if (!showingTrash) DropdownMenuItem(
                                text = { Text("Glossary") },
                                onClick = { libraryActionsExpanded = false; onOpenGlossary() },
                            )
                            DropdownMenuItem(
                                text = { Text(if (showingTrash) "Return to Library" else "Trash (${trashPages.size})") },
                                onClick = { libraryActionsExpanded = false; showingTrash = !showingTrash; showBookmarks = false; searchQuery = "" },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!showingTrash) FloatingActionButton(
                onClick = { showCreateHub = true },
                modifier = Modifier.semantics { contentDescription = "Create writing or a book" },
                containerColor = Accent,
                contentColor = Color.White,
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        },
        ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp)
                    .heightIn(min = 52.dp)
                    .background(CardPaper, RoundedCornerShape(12.dp))
                    .border(if (searchFocused) 1.5.dp else 1.dp, if (searchFocused) Accent else YouniColors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = MutedInk, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isBlank()) Text(if (showingTrash) "Search Trash" else "Search your library", color = MutedInk, style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                        modifier = Modifier.fillMaxWidth()
                            .onFocusChanged { searchFocused = it.isFocused }
                            .semantics { contentDescription = if (showingTrash) "Search Trash" else "Search pages, books, journals, and chapters" },
                    )
                }
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = { searchQuery = "" }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Clear", color = Accent)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (showingTrash) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Deleted pages can be restored or removed permanently.", Modifier.weight(1f), color = MutedInk, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { confirmEmptyTrash = true }, enabled = trashPages.isNotEmpty()) { Text("Empty Trash", color = ErrorInk) }
            }
            if (!showingTrash && onThisDayPages.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
                        .background(CardPaper, RoundedCornerShape(14.dp))
                        .border(1.dp, YouniColors.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("ON THIS DAY", color = Accent, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                    onThisDayPages.forEach { memory ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onOpen(memory) },
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(memory.createdAt)),
                                color = MutedInk,
                                fontSize = 11.sp,
                            )
                            Text(memory.title.ifBlank { memory.body.lineSequence().firstOrNull().orEmpty().ifBlank { "Untitled page" } }, color = Ink, fontFamily = FontFamily.Serif, fontSize = 17.sp, maxLines = 1)
                        }
                    }
                }
            }
            if (searchQuery.isNotBlank() && standalonePages.isEmpty() && matchingBooks.isEmpty() && matchingJournals.isEmpty() && matchingCollections.isEmpty()) {
                Text(if (showingTrash) "No trashed pages matched this search." else "No pages, books, journals, or collections found.", Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), color = MutedInk)
            } else {
            if (!showingTrash) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (matchingBooks.isEmpty()) "MAKE A BOOK" else "YOUR BOOKS", Modifier.weight(1f), color = MutedInk, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCreateBook, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+ Book", color = Accent) }
            }
            if (matchingBooks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(matchingBooks, key = { "book-${it.id}" }) { book ->
                        BookLibraryCard(book, allChapters.count { it.bookId == book.id }, libraryPages.count { it.bookId == book.id }) { onOpenBook(book.id) }
                    }
                }
            }
            if (matchingJournals.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    JournalsSection(matchingJournals, onOpenJournal)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("COLLECTIONS", Modifier.weight(1f), color = MutedInk, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCreateCollection, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+ Collection", color = Accent) }
            }
            if (matchingCollections.isNotEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matchingCollections.forEach { collection ->
                    Row(Modifier.fillMaxWidth().background(CardPaper, RoundedCornerShape(13.dp)).border(1.dp, YouniColors.border, RoundedCornerShape(13.dp)).clickable { onOpenCollection(collection) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(collection.icon.take(1).uppercase(), color = Accent, fontFamily = FontFamily.Serif, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(collection.title, color = Ink, fontFamily = FontFamily.Serif, fontSize = 18.sp, maxLines = 1)
                            if (collection.description.isNotBlank()) Text(collection.description, color = MutedInk, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (!showingTrash && searchQuery.isBlank()) Text("Gather related pages, books, or journals without copying them.", Modifier.padding(start = 22.dp, end = 22.dp, bottom = 4.dp), color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, Modifier.weight(1f), color = ErrorInk)
                    TextButton(onClick = onDismissError) { Text("Dismiss") }
                }
            }
            if (showingTrash && standalonePages.isEmpty() && searchQuery.isBlank()) {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trash is empty.", color = Ink, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                    Text("Pages moved here can be restored or permanently removed.", Modifier.padding(top = 8.dp), color = MutedInk)
                }
            } else if (!showingTrash && standalonePages.isEmpty() && matchingBooks.isEmpty() && matchingJournals.isEmpty() && matchingCollections.isEmpty() && searchQuery.isBlank() && !showBookmarks) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandMark(Modifier.padding(bottom = 24.dp))
                    Text(
                        "Your library awaits its first page.",
                        color = Ink,
                        fontFamily = FontFamily.Serif,
                        fontSize = 27.sp,
                        lineHeight = 33.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Begin with a thought, a memory, or a story.",
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(26.dp))
                    Button(
                        onClick = { onCreate("page") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    ) {
                        Text("Begin writing", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    TextButton(onClick = onCreateBook) { Text("Create a book", color = Accent) }
                }
            } else if (standalonePages.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 14.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showingTrash) "TRASH" else if (showBookmarks) "BOOKMARKS" else "YOUR PAGES", Modifier.weight(1f), color = MutedInk, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)
                        if (!showingTrash) {
                            Box {
                                TextButton(onClick = { pageTypeMenu = true }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text(pageType, color = Accent, fontSize = 11.sp) }
                                DropdownMenu(expanded = pageTypeMenu, onDismissRequest = { pageTypeMenu = false }) {
                                    listOf("All types", "Thoughts", "Quotes", "Dreams", "Letters", "Journal entries").forEach { type ->
                                        DropdownMenuItem(text = { Text(type) }, onClick = { pageType = type; showBookmarks = type == "Bookmarks"; pageTypeMenu = false })
                                    }
                                }
                            }
                            Box {
                                TextButton(onClick = { pageSortMenu = true }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Sort", color = Accent, fontSize = 11.sp) }
                                DropdownMenu(expanded = pageSortMenu, onDismissRequest = { pageSortMenu = false }) {
                                    listOf("Recently edited", "Newest", "Oldest", "Alphabetical").forEach { mode ->
                                        DropdownMenuItem(text = { Text(mode) }, onClick = { pageSort = mode; pageSortMenu = false })
                                    }
                                }
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                        contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(standalonePages, key = { it.id }) { page ->
                            val pageStyle = PageStyles.find(page.pageStyleId)
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(CardPaper, RoundedCornerShape(15.dp))
                                    .border(1.dp, YouniColors.border, RoundedCornerShape(15.dp))
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(Modifier.fillMaxWidth().then(if (showingTrash) Modifier else Modifier.clickable { onOpen(page) }), verticalAlignment = Alignment.CenterVertically) {
                                    PageStyleMiniature(pageStyle, Modifier.width(58.dp).height(76.dp))
                                    Spacer(Modifier.width(15.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(if (showingTrash) "TRASHED · ${formatDate(page.trashedAt ?: page.updatedAt)}" else "PAGE  ·  ${formatDate(page.updatedAt)}", color = Accent, fontSize = 10.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.Medium)
                                        Text(page.title.ifBlank { "Untitled page" }, color = Ink, fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 25.sp, maxLines = 1)
                                        Text(page.body.ifBlank { "A new page, ready for you." }, Modifier.padding(end = 56.dp), color = MutedInk, maxLines = 2, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp))
                                        Text(pageStyle.name, color = MutedInk, fontSize = 10.sp)
                                    }
                                }
                                if (showingTrash) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { onRestorePage(page) }) { Text("Restore", color = Accent) }
                                    TextButton(onClick = { permanentDeletePage = page }) { Text("Delete permanently", color = ErrorInk) }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchQuery.isNotBlank()) {
                        Text("No pages matched this search.", color = MutedInk)
                    } else if (showBookmarks) {
                        Text("Pages you bookmark will appear here.", color = MutedInk)
                        TextButton(onClick = { showBookmarks = false }) { Text("Show all pages", color = Accent) }
                    } else if (matchingJournals.isNotEmpty()) {
                        Text("Your writing lives here, day by day.", color = Ink, fontFamily = FontFamily.Serif, fontSize = 23.sp)
                        TextButton(onClick = { onOpenJournal(matchingJournals.first()) }) { Text("Open a journal", color = Accent) }
                    } else {
                        Text("Your books are here to grow.", color = Ink, fontFamily = FontFamily.Serif, fontSize = 23.sp)
                        TextButton(onClick = { onCreate("page") }) { Text("Write an independent page", color = Accent) }
                    }
                }
            }
            }
            }
        }
        }
    if (showTimeline) {
        val timelinePages = libraryPages.filter { page ->
            timelineType == "All writing" || when (timelineType) {
                "Book pages" -> page.bookId != null
                "Journal entries" -> journalPageIds.contains(page.id)
                "Bookmarked" -> page.isBookmarked
                else -> page.tags.contains(timelineType, ignoreCase = true)
            }
        }.sortedByDescending { it.createdAt }
        val groups = timelinePages.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1) }.toSortedMap(reverseOrder())
        AlertDialog(
            onDismissRequest = { showTimeline = false },
            title = { Text("Timeline", color = Ink, fontFamily = FontFamily.Serif) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                    Box {
                        TextButton(onClick = { timelineTypeMenu = true }) { Text(timelineType, color = Accent) }
                        DropdownMenu(expanded = timelineTypeMenu, onDismissRequest = { timelineTypeMenu = false }) {
                            listOf("All writing", "Book pages", "Journal entries", "Bookmarked", "Thought", "Quote", "Dream", "Letter").forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { timelineType = type; timelineTypeMenu = false })
                            }
                        }
                    }
                    if (timelinePages.isEmpty()) Text("Writing added to your Library will appear here by month.", color = MutedInk)
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        groups.forEach { (month, items) ->
                            item(key = "timeline-${month}") {
                                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.padding(top = 10.dp), color = Accent, style = MaterialTheme.typography.titleSmall)
                            }
                            items(items, key = { "timeline-${it.id}" }) { page ->
                                TextButton(onClick = { showTimeline = false; onOpen(page) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(page.createdAt)), color = MutedInk, style = MaterialTheme.typography.labelSmall)
                                        Text(page.title.ifBlank { "Untitled page" }, color = Ink, fontFamily = FontFamily.Serif, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTimeline = false }) { Text("Done", color = Accent) } },
            containerColor = Paper,
        )
    }
    if (showCreateHub) ModalBottomSheet(
        onDismissRequest = { showCreateHub = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Paper,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Create", color = Ink, fontFamily = FontFamily.Serif, fontSize = 28.sp)
            Text("Start with a page. You can organize it later.", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("WRITE", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                "page" to "New Page",
                "thought" to "Quick Thought",
                "journal" to "New Journal Entry",
                "letter" to "New Letter",
                "quote" to "New Quote",
                "dream" to "New Dream",
            ).forEach { (kind, label) ->
                TextButton(
                    onClick = {
                        showCreateHub = false
                        if (kind == "journal") showJournalChooser = true else onCreate(kind)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) { Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.height(4.dp))
            Text("TEMPLATES", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                "page" to "Blank page",
                "journal" to "Journal entry",
                "letter" to "Personal letter",
                "quote" to "Quote page",
                "dream" to "Dream journal",
                "research" to "Research note",
                "chapter" to "Chapter draft",
                "manuscript" to "Manuscript page",
                "family-history" to "Family history",
                "philosophy" to "Philosophy entry",
            ).forEach { (kind, label) ->
                TextButton(
                    onClick = {
                        showCreateHub = false
                        if (kind == "journal") showJournalChooser = true else onCreate(kind)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) { Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.bodyLarge) }
            }
            TextButton(
                onClick = { showCreateHub = false; onSettings() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) { Text("Manage personal templates in Settings", Modifier.fillMaxWidth(), color = MutedInk, style = MaterialTheme.typography.bodySmall) }
            if (writingTemplates.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("YOUR TEMPLATES", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                writingTemplates.forEach { template ->
                    TextButton(
                        onClick = { showCreateHub = false; onUseWritingTemplate(template) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(template.title, color = Ink, style = MaterialTheme.typography.bodyLarge)
                            template.body.takeIf(String::isNotBlank)?.let { Text(it, color = MutedInk, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("JOURNALS", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = { showCreateHub = false; onCreateJournal() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) { Text("New Journal", Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(4.dp))
            Text("ORGANIZE", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            listOf("New collection" to onCreateCollection, "Glossary terms" to onOpenGlossary).forEach { (label, action) ->
                TextButton(onClick = { showCreateHub = false; action() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium)
                }
            }
            listOf("New annotation" to "annotation", "Link to another page" to "link").forEach { (label, tool) ->
                TextButton(
                    onClick = { showCreateHub = false; pendingPageTool = tool },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) { Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.height(4.dp))
            Text("MEDIA", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            listOf("Add Photo" to "image", "New Drawing" to "drawing", "Record Audio" to "audio").forEach { (label, type) ->
                TextButton(onClick = { showCreateHub = false; onCreateMedia(type) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("BOOK", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = { showCreateHub = false; onCreateBook() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) { Text("New Book", Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            TextButton(
                onClick = { showCreateHub = false; manuscriptChooser = "chapter" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) { Text("New Chapter", Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            TextButton(
                onClick = { showCreateHub = false; manuscriptChooser = "section" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) { Text("New Section", Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(4.dp))
            Text("IMPORT", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                "TXT or Markdown" to onImportText,
                "JSON library backup" to onImportJson,
                "Encrypted Youni backup" to onImportEncryptedBackup,
            ).forEach { (label, action) ->
                TextButton(
                    onClick = { showCreateHub = false; action() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) { Text(label, Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
    if (manuscriptChooser.isNotEmpty()) AlertDialog(
        onDismissRequest = { manuscriptChooser = ""; sectionChooserBookId = null },
        title = { Text(if (sectionChooserBookId == null) "Choose a book" else "Choose a chapter") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sectionChooserBookId == null) {
                    if (books.isEmpty()) Text("Create a book before adding chapters or sections.", color = MutedInk)
                    books.forEach { book ->
                        TextButton(onClick = {
                            if (manuscriptChooser == "chapter") {
                                manuscriptChooser = ""
                                onCreateChapter(book)
                            } else if (allChapters.any { it.bookId == book.id }) sectionChooserBookId = book.id
                            else { manuscriptChooser = ""; onCreateChapter(book) }
                        }) { Text(book.title, Modifier.fillMaxWidth(), color = Ink) }
                    }
                } else {
                    val targetBook = books.firstOrNull { it.id == sectionChooserBookId }
                    allChapters.filter { it.bookId == sectionChooserBookId }.forEach { chapter ->
                        TextButton(onClick = {
                            if (targetBook != null) onCreateSection(targetBook, chapter)
                            manuscriptChooser = ""
                            sectionChooserBookId = null
                        }) { Text(chapter.title, Modifier.fillMaxWidth(), color = Ink) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { manuscriptChooser = ""; sectionChooserBookId = null }) { Text("Cancel", color = Accent) } },
        containerColor = Paper,
    )
    pendingPageTool?.let { tool -> AlertDialog(
        onDismissRequest = { pendingPageTool = null },
        title = { Text(if (tool == "annotation") "Choose a page to annotate" else "Choose the page to link from") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val available = pages.filterNot { it.isTrashed }
                if (available.isEmpty()) Text("Create a page before adding annotations or links.", color = MutedInk)
                available.forEach { target ->
                    TextButton(onClick = { pendingPageTool = null; onOpenPageTool(target, tool) }) {
                        Text(target.title.ifBlank { "Untitled page" }, Modifier.fillMaxWidth(), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { pendingPageTool = null }) { Text("Cancel", color = Accent) } },
        containerColor = Paper,
    ) }
    if (showJournalChooser) AlertDialog(
        onDismissRequest = { showJournalChooser = false },
        title = { Text("Choose a journal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val activeJournals = journals.filterNot { it.isArchived }
                if (activeJournals.isEmpty()) Text("Create a journal to begin an entry.", color = MutedInk)
                activeJournals.forEach { journal ->
                    TextButton(onClick = { showJournalChooser = false; onCreateJournalEntry(journal) }) {
                        Text(journal.title, Modifier.fillMaxWidth(), color = Ink)
                    }
                }
                TextButton(onClick = { showJournalChooser = false; onCreateJournal() }) {
                    Text("Create a journal", Modifier.fillMaxWidth(), color = Accent)
                }
            }
        },
        confirmButton = { TextButton(onClick = { showJournalChooser = false }) { Text("Cancel") } },
        containerColor = CardPaper,
    )
    if (confirmEmptyTrash) AlertDialog(
        onDismissRequest = { confirmEmptyTrash = false },
        title = { Text("Empty Trash permanently?") },
        text = { Text("${trashPages.size} ${if (trashPages.size == 1) "page" else "pages"} will be permanently deleted. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmEmptyTrash = false; onEmptyTrash() }) { Text("Empty Trash", color = ErrorInk) } },
        dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") } },
        containerColor = CardPaper,
    )
    permanentDeletePage?.let { page -> AlertDialog(
        onDismissRequest = { permanentDeletePage = null },
        title = { Text("Delete permanently?") },
        text = { Text("“${page.title.ifBlank { "Untitled page" }}” will be deleted and cannot be restored.") },
        confirmButton = { TextButton(onClick = { permanentDeletePage = null; onDeletePermanently(page) }) { Text("Delete permanently", color = ErrorInk) } },
        dismissButton = { TextButton(onClick = { permanentDeletePage = null }) { Text("Cancel") } },
        containerColor = CardPaper,
    ) }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(com.youniscript.app.R.drawable.ic_youniscript_approved_legacy),
        contentDescription = "YouniScript open-book emblem",
        modifier = modifier.size(88.dp),
    )
}

private fun formatDate(timestamp: Long): String = runCatching {
    DateTimeFormatter.ofPattern("MMM d · h:mm a")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))
}.getOrDefault("")

private fun InputStream.readBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "The selected file is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private data class EditorSnapshot(val document: FormattingDocument, val selection: TextRange)

@Composable
@Suppress("DEPRECATION")
private fun EditorScreen(
    page: Page,
    allPages: List<Page>,
    personalEntry: PersonalEntry?,
    annotations: List<AnnotationRecord>,
    links: List<PageLink>,
    backlinks: List<PageLink>,
    revisions: List<PageRevision>,
    attachments: List<MediaAttachment>,
    openDrawingOnEntry: Boolean,
    inheritedPageStyleId: String?,
    saveState: SaveState,
    operationError: String?,
    focusBodyOnOpen: Boolean,
    toolOnEntry: String?,
    onDismissOperationError: () -> Unit,
    onFocusConsumed: () -> Unit,
    onToolOnEntryConsumed: () -> Unit,
    onBack: () -> Unit,
    onChange: (Page) -> Unit,
    onFocusLeft: (Page) -> Unit,
    onRetrySave: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onBookmarkToggle: (Page) -> Unit,
    onSavePersonalEntry: (PersonalEntry) -> Unit,
    onSaveAnnotation: (AnnotationRecord) -> Unit,
    onDeleteAnnotation: (AnnotationRecord) -> Unit,
    onAddLink: (Page, String) -> Unit,
    onRemoveLink: (PageLink) -> Unit,
    onOpenLinkedPage: (Page) -> Unit,
    onRestoreRevision: (PageRevision) -> Unit,
    onAttachPhotos: () -> Unit,
    onSaveDrawing: (ByteArray) -> Unit,
    onDismissDrawing: () -> Unit,
    onDeleteAttachment: (MediaAttachment) -> Unit,
    onRenameAttachment: (MediaAttachment, String) -> Unit,
    onRecordAudio: () -> Unit,
    proofOptions: ProofOptions,
) {
    val clipboard = LocalClipboardManager.current
    val pageStyle = PageStyles.find(page.pageStyleId ?: inheritedPageStyleId)
    val titleFocus = remember(page.id) { FocusRequester() }
    val bodyFocus = remember(page.id) { FocusRequester() }
    var titleValue by remember(page.id) {
        mutableStateOf(TextFieldValue(page.title, selection = TextRange(page.title.length)))
    }
    var document by remember(page.id) { mutableStateOf(FormattingDocument.decode(page.body, page.formatting)) }
    var bodyValue by remember(page.id) {
        mutableStateOf(TextFieldValue(document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter), selection = TextRange(document.text.length)))
    }
    var formatMenu by remember(page.id) { mutableStateOf(false) }
    var styleMenu by rememberSaveable(page.id) { mutableStateOf(false) }
    var actionsMenu by remember(page.id) { mutableStateOf(false) }
    var proofDialogOpen by remember(page.id) { mutableStateOf(false) }
    var proofUndoAvailable by remember(page.id) { mutableStateOf(false) }
    var entryDetailsOpen by remember(page.id) { mutableStateOf(false) }
    var annotationsOpen by remember(page.id) { mutableStateOf(toolOnEntry == "annotation") }
    var linksOpen by remember(page.id) { mutableStateOf(toolOnEntry == "link") }
    var revisionsOpen by remember(page.id) { mutableStateOf(false) }
    var drawingOpen by remember(page.id) { mutableStateOf(false) }
    var ignoredProofIds by remember(page.id) { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember(page.id) { mutableStateOf(false) }
    var stackVersion by remember(page.id) { mutableIntStateOf(0) }
    var lastTextEditAt by remember(page.id) { mutableStateOf(0L) }
    var titleWasFocused by remember(page.id) { mutableStateOf(false) }
    var bodyWasFocused by remember(page.id) { mutableStateOf(false) }
    val undoStack = remember(page.id) { mutableListOf<EditorSnapshot>() }
    val redoStack = remember(page.id) { mutableListOf<EditorSnapshot>() }

    LaunchedEffect(page.title) {
        if (titleValue.text != page.title) titleValue = titleValue.copy(
            text = page.title,
            selection = TextRange(page.title.length),
        )
    }
    LaunchedEffect(page.id, openDrawingOnEntry) {
        if (openDrawingOnEntry) drawingOpen = true
    }
    LaunchedEffect(page.id, toolOnEntry) {
        when (toolOnEntry) {
            "annotation" -> annotationsOpen = true
            "link" -> linksOpen = true
        }
        if (toolOnEntry != null) onToolOnEntryConsumed()
    }
    LaunchedEffect(page.body, page.formatting) {
        if (document.text != page.body || document.toJson() != page.formatting) {
            document = FormattingDocument.decode(page.body, page.formatting)
            bodyValue = TextFieldValue(document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter), TextRange(document.text.length))
            undoStack.clear(); redoStack.clear(); stackVersion++
        }
    }
    LaunchedEffect(page.pageStyleId) {
        bodyValue = TextFieldValue(
            document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter),
            bodyValue.selection,
        )
    }
    LaunchedEffect(focusBodyOnOpen, page.id) {
        if (focusBodyOnOpen) {
            bodyFocus.requestFocus()
            onFocusConsumed()
        }
    }

    fun snapshot() = EditorSnapshot(document, bodyValue.selection)
    fun pushUndo() {
        undoStack += snapshot()
        if (undoStack.size > 100) undoStack.removeAt(0)
        redoStack.clear()
        stackVersion++
    }
    fun saveDocument(next: FormattingDocument, selection: TextRange = bodyValue.selection) {
        document = next
        bodyValue = TextFieldValue(
            annotatedString = next.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter),
            selection = TextRange(selection.start.coerceIn(0, next.text.length), selection.end.coerceIn(0, next.text.length)),
        )
        onChange(page.copy(body = next.text, formatting = next.toJson(), updatedAt = System.currentTimeMillis()))
    }
    fun applyInlineMark(mark: Int) {
        val selection = bodyValue.selection
        if (selection.collapsed) return
        pushUndo()
        saveDocument(document.toggleMark(selection.min, selection.max, mark), selection)
    }
    fun applyParagraph(kind: Int? = null, alignment: Int? = null) {
        pushUndo()
        val updated = document.setParagraphStyle(bodyValue.selection.min, bodyValue.selection.max, kind, alignment)
        saveDocument(updated)
        formatMenu = false
    }
    fun replaceSelection(insert: String) {
        pushUndo()
        val start = bodyValue.selection.min.coerceIn(0, document.text.length)
        val end = bodyValue.selection.max.coerceIn(start, document.text.length)
        val updatedText = document.text.substring(0, start) + insert + document.text.substring(end)
        val updated = document.edit(updatedText)
        val cursor = start + insert.length
        saveDocument(updated, TextRange(cursor))
    }
    fun applyList(numbered: Boolean) {
        val ranges = FormattingDocument.paragraphRanges(document.text, bodyValue.selection.min, bodyValue.selection.max)
        if (ranges.isEmpty()) {
            replaceSelection(if (numbered) "1. " else "• ")
            formatMenu = false
            return
        }
        pushUndo()
        var result = document.text
        var shiftBeforeCursor = 0
        val cursor = bodyValue.selection.max
        ranges.asReversed().forEachIndexed { reverseIndex, (lineStart, _) ->
            val index = ranges.lastIndex - reverseIndex
            val prefix = if (numbered) "${index + 1}. " else "• "
            result = result.substring(0, lineStart) + prefix + result.substring(lineStart)
            if (lineStart <= cursor) shiftBeforeCursor += prefix.length
        }
        val updated = document.edit(result)
        val newCursor = (cursor + shiftBeforeCursor).coerceIn(0, result.length)
        saveDocument(updated, TextRange(newCursor))
        formatMenu = false
    }
    fun applyDivider() {
        val before = if (bodyValue.selection.min > 0 && document.text[bodyValue.selection.min - 1] != '\n') "\n" else ""
        replaceSelection("${before}────────────\n")
        formatMenu = false
    }
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack += snapshot()
        val previous = undoStack.removeAt(undoStack.lastIndex)
        document = previous.document
        bodyValue = TextFieldValue(previous.document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter), previous.selection)
        stackVersion++
        onChange(page.copy(body = document.text, formatting = document.toJson(), updatedAt = System.currentTimeMillis()))
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack += snapshot()
        val next = redoStack.removeAt(redoStack.lastIndex)
        document = next.document
        bodyValue = TextFieldValue(next.document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter), next.selection)
        stackVersion++
        onChange(page.copy(body = document.text, formatting = document.toJson(), updatedAt = System.currentTimeMillis()))
    }
    fun applyProofSuggestion(suggestion: ProofSuggestion) {
        val replacement = suggestion.replacement ?: return
        val start = suggestion.start.coerceIn(0, document.text.length)
        val end = suggestion.end.coerceIn(start, document.text.length)
        pushUndo()
        val changedText = document.text.substring(0, start) + replacement + document.text.substring(end)
        saveDocument(document.edit(changedText), TextRange(start + replacement.length))
        proofUndoAvailable = true
    }
    val hasUndo = stackVersion >= 0 && undoStack.isNotEmpty()
    val hasRedo = stackVersion >= 0 && redoStack.isNotEmpty()
    val errorColor = if (pageStyle.id == "dark-journal") Color(0xFFFFB4A9) else ErrorInk
    val proofSuggestions = remember(document.text, proofOptions) { YouniProof.analyze(document.text, proofOptions) }
        .filterNot { it.id in ignoredProofIds }

    if (proofDialogOpen) AlertDialog(
        onDismissRequest = { proofDialogOpen = false },
        containerColor = CardPaper,
        title = { Text("YouniProof") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Local suggestions only. Your words stay on this device; nothing changes until you choose Replace.", color = MutedInk)
                if (proofSuggestions.isEmpty()) {
                    Text("No suggestions right now.", color = Ink)
                } else {
                    LazyColumn(Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(proofSuggestions, key = { it.id }) { suggestion ->
                            Column(Modifier.fillMaxWidth().background(YouniColors.library, RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text(suggestion.kind.name.lowercase().replace('_', ' '), color = Accent, style = MaterialTheme.typography.labelMedium)
                                Text(suggestion.message, color = Ink, style = MaterialTheme.typography.bodyMedium)
                                Text("“${suggestion.original}”" + (suggestion.replacement?.let { "  →  “$it”" } ?: ""), color = MutedInk, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    if (suggestion.replacement != null) TextButton(onClick = { applyProofSuggestion(suggestion) }) { Text("Replace") }
                                    TextButton(onClick = { ignoredProofIds = ignoredProofIds + suggestion.id }) { Text("Ignore") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (proofUndoAvailable && hasUndo) TextButton(onClick = { undo(); proofUndoAvailable = false }) { Text("Undo") }
        },
        dismissButton = { TextButton(onClick = { proofDialogOpen = false }) { Text("Done") } },
    )

    if (entryDetailsOpen && personalEntry != null) PersonalEntryDetailsDialog(
        entry = personalEntry,
        pageTitle = page.title,
        onDismiss = { entryDetailsOpen = false },
        onSave = { onSavePersonalEntry(it); entryDetailsOpen = false },
    )
    if (annotationsOpen) AnnotationManagerDialog(
        pageId = page.id,
        annotations = annotations,
        onDismiss = { annotationsOpen = false },
        onSave = onSaveAnnotation,
        onDelete = onDeleteAnnotation,
    )
    if (linksOpen) PageLinksDialog(
        page = page,
        pages = allPages,
        links = links,
        backlinks = backlinks,
        onDismiss = { linksOpen = false },
        onAdd = onAddLink,
        onOpen = onOpenLinkedPage,
        onRemove = onRemoveLink,
    )
    if (revisionsOpen) RevisionHistoryDialog(
        revisions = revisions,
        onDismiss = { revisionsOpen = false },
        onRestore = onRestoreRevision,
    )
    if (drawingOpen) DrawingEditorDialog(
        onDismiss = { drawingOpen = false; onDismissDrawing() },
        onSave = { bytes -> onSaveDrawing(bytes); drawingOpen = false; onDismissDrawing() },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = pageStyle.paper,
            titleContentColor = pageStyle.ink,
            textContentColor = pageStyle.mutedInk,
            title = { Text("Move this page to Trash?") },
            text = { Text("You can restore it from Trash or delete it permanently later.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Move to Trash", color = errorColor) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep page", color = pageStyle.ink) } },
        )
    }

    Column(
        Modifier.fillMaxSize().background(pageStyle.paper).imePadding().windowInsetsPadding(WindowInsets.displayCutout).drawBehind {
            if (pageStyle.parchmentTreatment) {
                drawRect(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), pageStyle.paper.copy(alpha = 0f)),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.maxDimension,
                    ),
                )
            }
            if (pageStyle.border) {
                drawRect(
                    pageStyle.borderColor.copy(alpha = 0.45f),
                    topLeft = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 18.dp.toPx(), size.height - 18.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Library", color = pageStyle.ink) }
            Spacer(Modifier.weight(1f))
            when (saveState) {
                SaveState.Saved -> Text("Saved", color = pageStyle.mutedInk, fontSize = 12.sp)
                SaveState.Saving -> Text("Saving…", color = pageStyle.mutedInk, fontSize = 12.sp)
                SaveState.Failed -> Text("Couldn't save", color = errorColor, fontSize = 12.sp)
            }
            if (saveState == SaveState.Failed) {
                TextButton(onClick = onRetrySave) { Text("Retry", color = errorColor) }
            }
            Box {
                TextButton(onClick = { formatMenu = true }) {
                    Text("Aa", color = pageStyle.ink, modifier = Modifier.semantics { contentDescription = "Formatting options" })
                }
                DropdownMenu(expanded = formatMenu, onDismissRequest = { formatMenu = false }, containerColor = CardPaper) {
                    DropdownMenuItem(text = { Text("Heading") }, onClick = { applyParagraph(BLOCK_HEADING) })
                    DropdownMenuItem(text = { Text("Subheading") }, onClick = { applyParagraph(BLOCK_SUBHEADING) })
                    DropdownMenuItem(text = { Text("Body text") }, onClick = { applyParagraph(BLOCK_NORMAL) })
                    DropdownMenuItem(text = { Text("Bullet list") }, onClick = { applyList(false) })
                    DropdownMenuItem(text = { Text("Numbered list") }, onClick = { applyList(true) })
                    DropdownMenuItem(text = { Text("Block quote") }, onClick = { applyParagraph(BLOCK_QUOTE) })
                    DropdownMenuItem(text = { Text("Divider") }, onClick = ::applyDivider)
                    DropdownMenuItem(text = { Text("Align left") }, onClick = { applyParagraph(alignment = ALIGN_START) })
                    DropdownMenuItem(text = { Text("Align center") }, onClick = { applyParagraph(alignment = ALIGN_CENTER) })
                    DropdownMenuItem(text = { Text("Align right") }, onClick = { applyParagraph(alignment = ALIGN_END) })
                    DropdownMenuItem(text = { Text("Justify") }, onClick = { applyParagraph(alignment = ALIGN_JUSTIFY) })
                }
            }
            TextButton(onClick = { styleMenu = true }) {
                Text("Style", color = pageStyle.ink, modifier = Modifier.semantics { contentDescription = "Page style: ${pageStyle.name}" })
            }
            TextButton(onClick = { proofDialogOpen = true }) {
                Text("Proof${proofSuggestions.size.takeIf { it > 0 }?.let { " ($it)" }.orEmpty()}", color = pageStyle.ink, modifier = Modifier.semantics { contentDescription = "YouniProof writing suggestions" })
            }
            Box {
                TextButton(onClick = { actionsMenu = true }) {
                    Text("•••", color = pageStyle.ink, modifier = Modifier.semantics { contentDescription = "More page actions" })
                }
                DropdownMenu(expanded = actionsMenu, onDismissRequest = { actionsMenu = false }, containerColor = CardPaper) {
                    DropdownMenuItem(text = { Text("Select all") }, onClick = {
                        bodyValue = bodyValue.copy(selection = TextRange(0, document.text.length)); actionsMenu = false
                    })
                    DropdownMenuItem(text = { Text("Copy") }, onClick = {
                        val selection = bodyValue.selection
                        if (!selection.collapsed) clipboard.setText(AnnotatedString(document.text.substring(selection.min, selection.max)))
                        actionsMenu = false
                    })
                    DropdownMenuItem(text = { Text("Paste") }, onClick = {
                        clipboard.getText()?.text?.let(::replaceSelection); actionsMenu = false
                    })
                    DropdownMenuItem(text = { Text("Undo") }, enabled = hasUndo, onClick = { undo(); actionsMenu = false })
                    DropdownMenuItem(text = { Text("Redo") }, enabled = hasRedo, onClick = { redo(); actionsMenu = false })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { actionsMenu = false; titleFocus.requestFocus() })
                    DropdownMenuItem(text = { Text(if (page.isBookmarked) "Remove bookmark" else "Bookmark page") }, onClick = { actionsMenu = false; onBookmarkToggle(page.copy(isBookmarked = !page.isBookmarked, updatedAt = System.currentTimeMillis())) })
                    if (personalEntry != null) DropdownMenuItem(text = { Text("Entry details") }, onClick = { actionsMenu = false; entryDetailsOpen = true })
                    DropdownMenuItem(text = { Text("Marginalia (${annotations.size})") }, onClick = { actionsMenu = false; annotationsOpen = true })
                    DropdownMenuItem(text = { Text("Page links (${links.size + backlinks.size})") }, onClick = { actionsMenu = false; linksOpen = true })
                    DropdownMenuItem(text = { Text("Version history (${revisions.size})") }, onClick = { actionsMenu = false; revisionsOpen = true })
                    DropdownMenuItem(text = { Text("Attach photos") }, onClick = { actionsMenu = false; onAttachPhotos() })
                    DropdownMenuItem(text = { Text("Create drawing") }, onClick = { actionsMenu = false; drawingOpen = true })
                    DropdownMenuItem(text = { Text("Record audio note") }, onClick = { actionsMenu = false; onRecordAudio() })
                    DropdownMenuItem(text = { Text("Duplicate page") }, onClick = { actionsMenu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete page") }, onClick = { actionsMenu = false; confirmDelete = true })
                }
            }
        }

        if (saveState == SaveState.Failed) {
            Text(
                "Your edits remain open here. Retry the save before leaving this page.",
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                color = errorColor,
                fontSize = 13.sp,
            )
        }
        if (operationError != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(operationError, Modifier.weight(1f), color = ErrorInk, fontSize = 13.sp)
                TextButton(onClick = onDismissOperationError) { Text("Dismiss") }
            }
        }
        pageStyle.runningHeader?.let { runningHeader ->
            Text(
                runningHeader,
                Modifier.fillMaxWidth().padding(start = pageStyle.horizontalMarginDp.dp, end = pageStyle.horizontalMarginDp.dp, top = 5.dp),
                color = pageStyle.mutedInk,
                fontSize = 9.sp,
                letterSpacing = 1.3.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (pageStyle.ornament) {
            Text("❧   ❧   ❧", Modifier.fillMaxWidth().padding(top = 4.dp), color = pageStyle.mutedInk, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        YouniContentTypography {
            BasicTextField(
                value = titleValue,
                onValueChange = { value ->
                    titleValue = value
                    onChange(page.copy(title = value.text, updatedAt = System.currentTimeMillis()))
                },
                modifier = Modifier.fillMaxWidth().padding(start = pageStyle.horizontalMarginDp.dp, end = pageStyle.horizontalMarginDp.dp, top = 18.dp, bottom = 12.dp)
                    .focusRequester(titleFocus)
                    .onFocusChanged {
                        if (it.isFocused) titleWasFocused = true
                        else if (titleWasFocused) {
                            titleWasFocused = false
                            onFocusLeft(page.copy(title = titleValue.text, updatedAt = System.currentTimeMillis()))
                        }
                    },
                textStyle = TextStyle(color = pageStyle.ink, fontFamily = pageStyle.titleFont, fontSize = pageStyle.titleSizeSp.sp, lineHeight = (pageStyle.titleSizeSp + 8).sp, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                decorationBox = { inner ->
                    Box {
                        if (titleValue.text.isEmpty()) Text("Untitled", color = pageStyle.mutedInk.copy(alpha = 0.72f), fontFamily = pageStyle.titleFont, fontSize = pageStyle.titleSizeSp.sp, fontWeight = FontWeight.SemiBold)
                        inner()
                    }
                },
            )
        }

        MediaAttachmentGallery(attachments = attachments, onDelete = onDeleteAttachment, onRenameAudio = onRenameAttachment)

        if (!bodyValue.selection.collapsed) {
            val selection = bodyValue.selection
            fun selectedInlineMark(mark: Int): Boolean = bodyValue.annotatedString.spanStyles.any { span ->
                span.start < selection.max && span.end > selection.min && when (mark) {
                    MARK_BOLD -> span.item.fontWeight == FontWeight.Bold
                    MARK_ITALIC -> span.item.fontStyle == FontStyle.Italic
                    MARK_UNDERLINE -> span.item.textDecoration?.contains(TextDecoration.Underline) == true
                    MARK_STRIKE -> span.item.textDecoration?.contains(TextDecoration.LineThrough) == true
                    else -> false
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormatButton("B", "Bold", FontWeight.Bold, selected = selectedInlineMark(MARK_BOLD)) { applyInlineMark(MARK_BOLD) }
                FormatButton("I", "Italic", fontStyle = FontStyle.Italic, selected = selectedInlineMark(MARK_ITALIC)) { applyInlineMark(MARK_ITALIC) }
                FormatButton("U", "Underline", decoration = TextDecoration.Underline, selected = selectedInlineMark(MARK_UNDERLINE)) { applyInlineMark(MARK_UNDERLINE) }
                FormatButton("S", "Strikethrough", decoration = TextDecoration.LineThrough, selected = selectedInlineMark(MARK_STRIKE)) { applyInlineMark(MARK_STRIKE) }
            }
        }

        Box(
            Modifier.weight(1f).fillMaxWidth()
                .padding(horizontal = pageStyle.horizontalMarginDp.dp)
                .drawBehind {
                    if (pageStyle.ruled) {
                        val spacing = 32.dp.toPx()
                        var y = spacing
                        while (y < size.height) {
                            drawLine(pageStyle.mutedInk.copy(alpha = 0.18f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 0.7f)
                            y += spacing
                        }
                    }
                },
        ) {
            YouniContentTypography {
                BasicTextField(
                    value = bodyValue,
                    onValueChange = { value ->
                        if (value.text != document.text) {
                            val now = System.currentTimeMillis()
                            if (now - lastTextEditAt > 700L) pushUndo()
                            lastTextEditAt = now
                            document = document.edit(value.text)
                            bodyValue = TextFieldValue(document.asAnnotatedString(pageStyle.headingSizeSp, pageStyle.subheadingSizeSp, pageStyle.initialLetter), value.selection)
                            onChange(page.copy(body = document.text, formatting = document.toJson(), updatedAt = now))
                        } else {
                            bodyValue = value
                        }
                    },
                    modifier = Modifier.fillMaxSize().focusRequester(bodyFocus)
                        .onFocusChanged {
                            if (it.isFocused) bodyWasFocused = true
                            else if (bodyWasFocused) {
                                bodyWasFocused = false
                                onFocusLeft(
                                    page.copy(body = document.text, formatting = document.toJson(), updatedAt = System.currentTimeMillis()),
                                )
                            }
                        },
                    textStyle = TextStyle(color = pageStyle.ink, fontFamily = pageStyle.bodyFont, fontSize = pageStyle.bodySizeSp.sp, lineHeight = pageStyle.lineHeightSp.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    decorationBox = { inner ->
                        Box {
                            if (document.text.isEmpty()) {
                                Text("Begin wherever you are…", color = pageStyle.mutedInk.copy(alpha = 0.75f), fontFamily = pageStyle.bodyFont, fontSize = pageStyle.bodySizeSp.sp)
                            }
                            inner()
                        }
                    },
                )
            }
        }
        if (pageStyle.pageNumber) {
            Text("·", Modifier.fillMaxWidth().padding(bottom = 2.dp), color = pageStyle.mutedInk, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
    }
    if (styleMenu) {
        PageStylePickerDialog(
            selectedStyleId = page.pageStyleId ?: inheritedPageStyleId,
            onSelect = { style ->
                styleMenu = false
                onChange(page.copy(pageStyleId = style.id))
            },
            onDismiss = { styleMenu = false },
        )
    }
}

@Composable
private fun FormatButton(
    label: String,
    description: String,
    weight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    decoration: TextDecoration? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description; this.selected = selected },
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(label, color = Accent, fontWeight = weight, fontStyle = fontStyle, textDecoration = decoration)
    }
}
