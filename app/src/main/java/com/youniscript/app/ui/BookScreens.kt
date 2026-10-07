package com.youniscript.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.youniscript.app.data.Book
import com.youniscript.app.data.BookComponent
import com.youniscript.app.data.Chapter
import com.youniscript.app.data.Page
import com.youniscript.app.data.Section
import com.youniscript.app.editor.FormattingDocument
import com.youniscript.app.ui.PageStyles
import com.youniscript.app.ui.theme.YouniColors
import com.youniscript.app.ui.theme.YouniContentTypography
import java.util.UUID
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Ink @Composable get() = YouniColors.ink
private val Muted @Composable get() = YouniColors.mutedInk
private val Paper @Composable get() = YouniColors.library
private val Accent @Composable get() = YouniColors.sage

fun coverTemplate(id: String): BookCoverTemplate = BookCoverTemplates.all.firstOrNull { it.id == id } ?: BookCoverTemplates.all.first()

@Composable
fun BookLibraryCard(book: Book, chapterCount: Int, pageCount: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(YouniColors.paper)
            .border(1.dp, YouniColors.border, RoundedCornerShape(15.dp)).clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book.title.ifBlank { "Untitled book" }, book.subtitle, book.author, coverTemplate(book.coverTemplateId), Modifier.width(82.dp).height(116.dp), book.coverImageUri)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("BOOK  ·  ${book.status.uppercase()}", color = Accent, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            Text(book.title.ifBlank { "Untitled book" }, color = Ink, fontFamily = FontFamily.Serif, fontSize = 21.sp, lineHeight = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) Text(book.author, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("$chapterCount ${if (chapterCount == 1) "chapter" else "chapters"} · $pageCount ${if (pageCount == 1) "page" else "pages"}", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun CreateBookDialog(
    onDismiss: () -> Unit,
    onCreate: (Book, String?, String?) -> Unit,
    initialAuthor: String = "",
    authorBiography: String = "",
    authorSeal: String = "",
) {
    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf(initialAuthor) }
    var includeBiography by rememberSaveable { mutableStateOf(false) }
    var includeSeal by rememberSaveable { mutableStateOf(false) }
    var templateId by rememberSaveable { mutableStateOf("minimal") }
    var styleId by rememberSaveable { mutableStateOf("modern-paper") }
    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var imageCopyFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val copy = runCatching { withContext(Dispatchers.IO) { copyCoverToPrivateStorage(context, uri) } }
                copy.onSuccess { imageUri = it; imageCopyFailed = false }
                    .onFailure { imageCopyFailed = true }
            }
        }
    }
    val now = rememberSaveable { System.currentTimeMillis() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Paper)
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Create your book", color = Ink, fontFamily = FontFamily.Serif, fontSize = 27.sp)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(subtitle, { subtitle = it }, Modifier.fillMaxWidth(), label = { Text("Subtitle · optional") }, singleLine = true)
            OutlinedTextField(author, { author = it }, Modifier.fillMaxWidth(), label = { Text("Author · optional") }, singleLine = true)
            if (authorBiography.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeBiography, onCheckedChange = { includeBiography = it })
                    Text("Add my biography to an About the Author page", color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (authorSeal.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeSeal, onCheckedChange = { includeSeal = it })
                    Text("Include my personal seal on that page", color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("COVER", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (imageUri == null) "Choose a cover image" else "Change cover image", color = Accent) }
            if (imageCopyFailed) Text("That image couldn't be saved. Choose another image.", color = YouniColors.error, style = MaterialTheme.typography.bodySmall)
            BookCoverTemplates.all.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { template ->
                        TextButton(
                            onClick = { templateId = template.id },
                            modifier = Modifier.weight(1f).border(1.dp, if (templateId == template.id) Accent else YouniColors.border, RoundedCornerShape(10.dp)),
                        ) { Text(template.name, color = if (templateId == template.id) Accent else Ink) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            BookCover(title.ifBlank { "Your title" }, subtitle, author, coverTemplate(templateId), Modifier.align(Alignment.CenterHorizontally).width(128.dp).height(178.dp), imageUri)
            Text("DEFAULT PAGE STYLE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("modern-paper" to "Modern", "classic-book" to "Classic", "literary" to "Literary", "parchment" to "Parchment", "ancient-manuscript" to "Ancient").forEach { (id, name) ->
                    TextButton(onClick = { styleId = id }, modifier = Modifier.border(1.dp, if (styleId == id) Accent else YouniColors.border, RoundedCornerShape(10.dp))) { Text(name, color = if (styleId == id) Accent else Ink, maxLines = 1) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) }
                Button(onClick = {
                    val book = Book(UUID.randomUUID().toString(), title.trim(), subtitle.trim(), author.trim(), createdAt = now, updatedAt = now, coverTemplateId = templateId, defaultPageStyleId = styleId)
                    onCreate(
                        book.copy(coverImageUri = imageUri),
                        authorBiography.takeIf { includeBiography },
                        authorSeal.takeIf { includeSeal },
                    )
                }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create book") }
            }
        }
    }
}

@Composable
fun BookOverviewScreen(
    book: Book,
    chapters: List<Chapter>,
    pages: List<Page>,
    components: List<BookComponent>,
    sections: List<Section>,
    availablePages: List<Page>,
    onBack: () -> Unit,
    onCreateChapter: () -> Unit,
    onRenameChapter: (Chapter, String) -> Unit,
    onDuplicateChapter: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onCreatePage: (Chapter) -> Unit,
    onContinueWriting: () -> Unit,
    onOpenPage: (Page) -> Unit,
    onOutline: () -> Unit,
    onRead: () -> Unit,
    onExportPdf: () -> Unit,
    onExportEpub: () -> Unit,
    onEditDetails: (Book) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddFrontMatter: (String, String, String) -> Unit,
    onOpenFrontMatter: (BookComponent) -> Unit,
    onAddExistingPage: (Page, Chapter, Section?) -> Unit,
) {
    var showDelete by remember(book.id) { mutableStateOf(false) }
    var showFrontMatter by remember(book.id) { mutableStateOf(false) }
    var showAddExisting by remember(book.id) { mutableStateOf(false) }
    var showExportMenu by remember(book.id) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Paper).windowInsetsPadding(WindowInsets.safeDrawing), contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Library", color = Ink) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOutline, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Outline", color = Accent) }
                TextButton(onClick = onRead, modifier = Modifier.semantics { contentDescription = "Read book full screen" }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Read", color = Accent) }
                Box {
                    TextButton(onClick = { showExportMenu = true }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("More", color = Accent) }
                    androidx.compose.material3.DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Export as PDF") }, onClick = { showExportMenu = false; onExportPdf() })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Export as EPUB") }, onClick = { showExportMenu = false; onExportEpub() })
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                BookCover(book.title.ifBlank { "Untitled book" }, book.subtitle, book.author, coverTemplate(book.coverTemplateId), Modifier.width(124.dp).height(174.dp), book.coverImageUri)
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(book.title.ifBlank { "Untitled book" }, color = Ink, fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 29.sp)
                    if (book.subtitle.isNotBlank()) Text(book.subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
                    if (book.author.isNotBlank()) Text("by ${book.author}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Text("${chapters.size} ${if (chapters.size == 1) "chapter" else "chapters"} · ${pages.size} ${if (pages.size == 1) "page" else "pages"}", color = Muted, fontSize = 12.sp)
                    val lastModified = maxOf(book.updatedAt, pages.maxOfOrNull { it.updatedAt } ?: 0L, components.maxOfOrNull { it.updatedAt } ?: 0L, chapters.maxOfOrNull { it.updatedAt } ?: 0L, sections.maxOfOrNull { it.updatedAt } ?: 0L)
                    Text("Updated ${java.time.Instant.ofEpochMilli(lastModified).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))}", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onContinueWriting, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Continue writing") }
                TextButton(onClick = { onEditDetails(book) }) { Text("Edit book", color = Ink) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showFrontMatter = true }) { Text("+ Front matter", color = Accent) }
                TextButton(onClick = { showAddExisting = true }, enabled = availablePages.isNotEmpty()) { Text("Add existing page", color = Accent) }
                TextButton(onClick = onDuplicate) { Text("Duplicate", color = Ink) }
                TextButton(onClick = { showDelete = true }) { Text("Delete", color = YouniColors.error) }
            }
        }
        if (components.isNotEmpty()) item {
            Text("FRONT MATTER", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
            components.forEach { component ->
                TextButton(onClick = { onOpenFrontMatter(component) }) { Text("${component.title}  ›", color = Ink) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CHAPTERS", Modifier.weight(1f), color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCreateChapter) { Text("+ Add chapter", color = Accent) }
            }
        }
        items(chapters, key = { it.id }) { chapter ->
            var menuOpen by remember(chapter.id) { mutableStateOf(false) }
            var renameOpen by remember(chapter.id) { mutableStateOf(false) }
            var deleteOpen by remember(chapter.id) { mutableStateOf(false) }
            var chapterTitle by remember(chapter.id, chapter.title) { mutableStateOf(chapter.title) }
            val chapterPages = pages.filter { it.chapterId == chapter.id }
            val chapterSections = sections.filter { it.chapterId == chapter.id }.sortedBy { it.sectionOrder }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(YouniColors.paper).border(1.dp, YouniColors.border, RoundedCornerShape(14.dp)).padding(15.dp)) {
                Text("CHAPTER ${chapters.indexOf(chapter) + 1}", color = Accent, fontSize = 10.sp, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.title, Modifier.weight(1f).padding(top = 4.dp, bottom = 6.dp), color = Ink, fontFamily = FontFamily.Serif, fontSize = 20.sp)
                    Box {
                        TextButton(onClick = { menuOpen = true }) { Text("•••", color = Muted) }
                        androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Rename chapter") }, onClick = { menuOpen = false; renameOpen = true })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Duplicate chapter") }, onClick = { menuOpen = false; onDuplicateChapter(chapter) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Delete chapter") }, onClick = { menuOpen = false; deleteOpen = true })
                        }
                    }
                }
                chapterPages.filter { it.sectionId == null }.forEachIndexed { index, page ->
                    TextButton(onClick = { onOpenPage(page) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("${index + 1}.  ${page.title.ifBlank { "Untitled page" }}", Modifier.fillMaxWidth(), color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                chapterSections.forEach { section ->
                    Text("§ ${section.title}", Modifier.padding(start = 8.dp, top = 3.dp), color = Accent, fontSize = 12.sp)
                    chapterPages.filter { it.sectionId == section.id }.forEach { page ->
                        TextButton(onClick = { onOpenPage(page) }, modifier = Modifier.padding(start = 14.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)) {
                            Text("↳ ${page.title.ifBlank { "Untitled page" }}", Modifier.fillMaxWidth(), color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                TextButton(onClick = { onCreatePage(chapter) }) { Text("+ Add page", color = Accent) }
            }
            if (renameOpen) AlertDialog(
                onDismissRequest = { renameOpen = false },
                title = { Text("Rename chapter") },
                text = { OutlinedTextField(chapterTitle, { chapterTitle = it }, label = { Text("Chapter title") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { renameOpen = false; onRenameChapter(chapter, chapterTitle.trim()) }) { Text("Save", color = Accent) } },
                dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Cancel", color = Ink) } },
                containerColor = Paper,
            )
            if (deleteOpen) AlertDialog(
                onDismissRequest = { deleteOpen = false },
                title = { Text("Delete this chapter?") },
                text = { Text("Its pages will be kept as independent pages in your Library.") },
                confirmButton = { TextButton(onClick = { deleteOpen = false; onDeleteChapter(chapter) }) { Text("Delete chapter", color = YouniColors.error) } },
                dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Keep chapter", color = Ink) } },
                containerColor = Paper,
            )
        }
    }
    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete this book?") },
        text = { Text("The book will be removed. Its pages and front matter will be kept in your Library as independent pages.") },
        confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text("Delete book", color = YouniColors.error) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep book", color = Ink) } },
        containerColor = Paper,
    )
    if (showFrontMatter) FrontMatterDialog(onDismiss = { showFrontMatter = false }, onSave = { type, title, body -> showFrontMatter = false; onAddFrontMatter(type, title, body) })
    if (showAddExisting) AddExistingPageDialog(availablePages, chapters, sections,
        onDismiss = { showAddExisting = false },
        onAdd = { page, chapter, section -> showAddExisting = false; onAddExistingPage(page, chapter, section) },
    )
}

@Composable
private fun AddExistingPageDialog(pages: List<Page>, chapters: List<Chapter>, sections: List<Section>, onDismiss: () -> Unit, onAdd: (Page, Chapter, Section?) -> Unit) {
    var pageId by rememberSaveable { mutableStateOf<String?>(null) }
    var chapterId by rememberSaveable(chapters.firstOrNull()?.id) { mutableStateOf(chapters.firstOrNull()?.id) }
    var sectionId by rememberSaveable(chapterId) { mutableStateOf<String?>(null) }
    var chapterMenu by remember { mutableStateOf(false) }
    var sectionMenu by remember { mutableStateOf(false) }
    val chosenPage = pages.firstOrNull { it.id == pageId }
    val chosenChapter = chapters.firstOrNull { it.id == chapterId }
    val chapterSections = sections.filter { it.chapterId == chapterId }.sortedBy { it.sectionOrder }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add existing page") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Choose a page. It will move into this book with its existing identity and writing.", color = Muted, style = MaterialTheme.typography.bodySmall)
                pages.forEach { page ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { pageId = page.id }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = page.id == pageId, onClick = { pageId = page.id })
                        Column {
                            Text(page.title.ifBlank { "Untitled page" }, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(page.body.replace('\n', ' ').take(72), color = Muted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Text("CHAPTER", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Box {
                    TextButton(onClick = { chapterMenu = true }, enabled = chapters.isNotEmpty()) { Text(chosenChapter?.title ?: "Choose a chapter", color = Accent) }
                    androidx.compose.material3.DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                        chapters.forEach { chapter -> androidx.compose.material3.DropdownMenuItem(text = { Text(chapter.title) }, onClick = { chapterId = chapter.id; chapterMenu = false }) }
                    }
                }
                Text("SECTION · OPTIONAL", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Box {
                    TextButton(onClick = { sectionMenu = true }) { Text(chapterSections.firstOrNull { it.id == sectionId }?.title ?: "No section", color = Accent) }
                    androidx.compose.material3.DropdownMenu(expanded = sectionMenu, onDismissRequest = { sectionMenu = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("No section") }, onClick = { sectionId = null; sectionMenu = false })
                        chapterSections.forEach { section -> androidx.compose.material3.DropdownMenuItem(text = { Text(section.title) }, onClick = { sectionId = section.id; sectionMenu = false }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (chosenPage != null && chosenChapter != null) onAdd(chosenPage, chosenChapter, chapterSections.firstOrNull { it.id == sectionId }) }, enabled = chosenPage != null && chosenChapter != null) { Text("Add page", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } },
        containerColor = Paper,
    )
}

@Composable
private fun FrontMatterDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var type by remember { mutableStateOf("dedication") }
    var title by remember { mutableStateOf("Dedication") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add front matter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("dedication" to "Dedication", "preface" to "Preface", "introduction" to "Introduction").forEach { (value, label) ->
                        TextButton(onClick = { type = value; title = label }) { Text(label, color = if (type == value) Accent else Ink, fontSize = 12.sp) }
                    }
                }
                OutlinedTextField(body, { body = it }, label = { Text("Write something · optional") }, minLines = 3)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(type, title, body) }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } },
        containerColor = Paper,
    )
}

@Composable
fun BookOutlineScreen(
    book: Book,
    chapters: List<Chapter>,
    pages: List<Page>,
    sections: List<Section>,
    components: List<BookComponent>,
    onBack: () -> Unit,
    onOpenPage: (Page) -> Unit,
    onOpenFrontMatter: (BookComponent) -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onMoveChapter: (Int, Int) -> Unit,
    onMovePage: (Chapter, Int, Int) -> Unit,
    onMovePageToChapter: (Page, Chapter) -> Unit,
    onMovePageToSection: (Page, Section?) -> Unit,
    onCreateSection: (Chapter, String) -> Unit,
    onRenameSection: (Section, String) -> Unit,
    onDeleteSection: (Section) -> Unit,
    onMoveSection: (Chapter, Int, Int) -> Unit,
    onMoveSectionToChapter: (Section, Chapter) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Book", color = Ink) }
            Text("Outline", color = Ink, fontFamily = FontFamily.Serif, fontSize = 23.sp)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text(book.title, color = Ink, fontFamily = FontFamily.Serif, fontSize = 21.sp, modifier = Modifier.padding(vertical = 9.dp)) }
            item { Text("Front matter", color = Muted, fontSize = 12.sp) }
            item { Text("Title page", color = Ink, modifier = Modifier.padding(start = 20.dp, top = 5.dp, bottom = 5.dp)) }
            items(components, key = { "outline-component-${it.id}" }) { component ->
                TextButton(onClick = { onOpenFrontMatter(component) }, contentPadding = PaddingValues(start = 20.dp, top = 3.dp, bottom = 3.dp)) { Text("${component.title}  ›", Modifier.fillMaxWidth(), color = Ink) }
            }
            item { Text("Table of contents · generated", color = Ink, modifier = Modifier.padding(start = 20.dp, top = 5.dp, bottom = 5.dp)) }
            item { Spacer(Modifier.height(8.dp)); Text("Chapters", color = Muted, fontSize = 12.sp) }
            items(chapters, key = { it.id }) { chapter ->
                var addingSection by remember(chapter.id) { mutableStateOf(false) }
                var newSectionTitle by rememberSaveable(chapter.id) { mutableStateOf("") }
                var renamingSection by remember(chapter.id) { mutableStateOf<Section?>(null) }
                var deletingSection by remember(chapter.id) { mutableStateOf<Section?>(null) }
                val chapterSections = sections.filter { it.chapterId == chapter.id }.sortedBy { it.sectionOrder }
                Column(Modifier.fillMaxWidth().padding(start = 12.dp).clip(RoundedCornerShape(12.dp)).background(YouniColors.paper).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onOpenChapter(chapter) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("${chapters.indexOf(chapter) + 1}. ${chapter.title}", Modifier.fillMaxWidth(), color = Ink, fontFamily = FontFamily.Serif, fontSize = 18.sp) }
                        TextButton(onClick = { onMoveChapter(chapters.indexOf(chapter), -1) }, enabled = chapters.indexOf(chapter) > 0) { Text("↑", color = Accent) }
                        TextButton(onClick = { onMoveChapter(chapters.indexOf(chapter), 1) }, enabled = chapters.indexOf(chapter) < chapters.lastIndex) { Text("↓", color = Accent) }
                    }
                    TextButton(onClick = { addingSection = true }) { Text("+ Add section", color = Accent) }
                    if (addingSection) AlertDialog(
                        onDismissRequest = { addingSection = false },
                        title = { Text("Add section") },
                        text = { OutlinedTextField(newSectionTitle, { newSectionTitle = it }, label = { Text("Section title") }, singleLine = true) },
                        confirmButton = { TextButton(onClick = { onCreateSection(chapter, newSectionTitle.trim()); newSectionTitle = ""; addingSection = false }, enabled = newSectionTitle.isNotBlank()) { Text("Create", color = Accent) } },
                        dismissButton = { TextButton(onClick = { addingSection = false }) { Text("Cancel", color = Ink) } },
                        containerColor = Paper,
                    )
                    renamingSection?.let { section -> AlertDialog(
                        onDismissRequest = { renamingSection = null },
                        title = { Text("Rename section") },
                        text = { OutlinedTextField(newSectionTitle, { newSectionTitle = it }, label = { Text("Section title") }, singleLine = true) },
                        confirmButton = { TextButton(onClick = { onRenameSection(section, newSectionTitle.trim()); newSectionTitle = ""; renamingSection = null }, enabled = newSectionTitle.isNotBlank()) { Text("Save", color = Accent) } },
                        dismissButton = { TextButton(onClick = { renamingSection = null }) { Text("Cancel", color = Ink) } },
                        containerColor = Paper,
                    ) }
                    deletingSection?.let { section -> AlertDialog(
                        onDismissRequest = { deletingSection = null },
                        title = { Text("Delete section?") },
                        text = { Text("Pages in ${section.title} will stay in this chapter without a section.") },
                        confirmButton = { TextButton(onClick = { onDeleteSection(section); deletingSection = null }) { Text("Delete section", color = YouniColors.error) } },
                        dismissButton = { TextButton(onClick = { deletingSection = null }) { Text("Keep section", color = Ink) } },
                        containerColor = Paper,
                    ) }
                    val chapterPages = pages.filter { it.chapterId == chapter.id }.sortedBy { it.bookOrder ?: Int.MAX_VALUE }
                    @Composable fun renderPage(page: Page, indent: androidx.compose.ui.unit.Dp) {
                        var menuOpen by remember(page.id) { mutableStateOf(false) }
                        val globalIndex = chapterPages.indexOfFirst { it.id == page.id }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onOpenPage(page) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(start = indent, top = 3.dp, bottom = 3.dp)) {
                                Text("${globalIndex + 1}. ${page.title.ifBlank { "Untitled page" }}", Modifier.fillMaxWidth(), color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { onMovePage(chapter, globalIndex, -1) }, enabled = globalIndex > 0) { Text("↑", color = Accent) }
                            TextButton(onClick = { onMovePage(chapter, globalIndex, 1) }, enabled = globalIndex in 0 until chapterPages.lastIndex) { Text("↓", color = Accent) }
                            Box {
                                TextButton(onClick = { menuOpen = true }) { Text("Move", color = Accent, fontSize = 11.sp) }
                                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    if (page.sectionId != null) androidx.compose.material3.DropdownMenuItem(text = { Text("Remove from section") }, onClick = { menuOpen = false; onMovePageToSection(page, null) })
                                    chapterSections.filterNot { it.id == page.sectionId }.forEach { section ->
                                        androidx.compose.material3.DropdownMenuItem(text = { Text("To ${section.title}") }, onClick = { menuOpen = false; onMovePageToSection(page, section) })
                                    }
                                    chapters.filterNot { it.id == chapter.id }.forEach { destination ->
                                        androidx.compose.material3.DropdownMenuItem(text = { Text("To ${destination.title}") }, onClick = { menuOpen = false; onMovePageToChapter(page, destination) })
                                    }
                                }
                            }
                        }
                    }
                    chapterSections.forEachIndexed { sectionIndex, section ->
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("§ ${section.title}", Modifier.weight(1f), color = Accent, fontWeight = FontWeight.Medium)
                            TextButton(onClick = { onMoveSection(chapter, sectionIndex, -1) }, enabled = sectionIndex > 0) { Text("↑", color = Accent) }
                            TextButton(onClick = { onMoveSection(chapter, sectionIndex, 1) }, enabled = sectionIndex < chapterSections.lastIndex) { Text("↓", color = Accent) }
                            var menuOpen by remember(section.id) { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { menuOpen = true }, modifier = Modifier.semantics { contentDescription = "Actions for section ${section.title}" }) { Text("•••", color = Muted) }
                                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("Rename section") }, onClick = { menuOpen = false; newSectionTitle = section.title; renamingSection = section })
                                    chapters.filterNot { it.id == chapter.id }.forEach { destination ->
                                        androidx.compose.material3.DropdownMenuItem(text = { Text("Move to ${destination.title}") }, onClick = { menuOpen = false; onMoveSectionToChapter(section, destination) })
                                    }
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("Delete section") }, onClick = { menuOpen = false; deletingSection = section })
                                }
                            }
                        }
                        chapterPages.filter { it.sectionId == section.id }.forEach { page -> renderPage(page, 18.dp) }
                    }
                    chapterPages.filter { it.sectionId == null }.forEach { page -> renderPage(page, 19.dp) }
                }
            }
        }
    }
}

private data class ReaderLeaf(
    val title: String,
    val body: String,
    val chapterTitle: String? = null,
    val chapterId: String? = null,
    val styleId: String? = null,
    val page: Page? = null,
    val formattedBody: AnnotatedString? = null,
    val coverImageUri: String? = null,
)

val ReadingModeOptions = listOf(
    "continuous" to "Continuous scroll",
    "paginated" to "Paginated",
    "spread" to "Two-page spread",
    "focus" to "Focus reading",
    "presentation" to "Presentation reading",
    "immersive" to "Immersive reading",
    "sepia" to "Sepia / paper",
    "dark" to "Dark reading",
    "centered" to "Typewriter / centered line",
)

@Composable
fun BookReadingScreen(book: Book, chapters: List<Chapter>, pages: List<Page>, sections: List<Section>, components: List<BookComponent>, initialChapterId: String?, defaultReadingMode: String = "continuous", onBack: () -> Unit, onOpenPage: (Page) -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("reading", Context.MODE_PRIVATE) }
    val orderedChapters = remember(chapters) { chapters.sortedBy { it.chapterOrder } }
    val leaves = remember(book, orderedChapters, pages, sections, components) {
        buildList {
            add(ReaderLeaf(book.title.ifBlank { "Untitled book" }, listOf(book.subtitle, book.author.takeIf(String::isNotBlank)?.let { "by $it" }.orEmpty()).filter(String::isNotBlank).joinToString("\n\n"), coverImageUri = book.coverImageUri))
            components.sortedBy { it.componentOrder }.forEach { add(ReaderLeaf(it.title, it.body)) }
            add(ReaderLeaf("Contents", orderedChapters.mapIndexed { i, chapter -> "${i + 1}. ${chapter.title}" }.joinToString("\n")))
            orderedChapters.forEach { chapter ->
                val chapterSections = sections.filter { it.chapterId == chapter.id }.sortedBy { it.sectionOrder }
                val chapterPages = pages.filter { it.chapterId == chapter.id }.sortedWith(
                    compareBy<Page> { page -> if (page.sectionId == null) -1 else chapterSections.indexOfFirst { it.id == page.sectionId } }
                        .thenBy { it.bookOrder ?: Int.MAX_VALUE },
                )
                if (chapterPages.isEmpty()) add(ReaderLeaf(chapter.title, "", "CHAPTER ${orderedChapters.indexOf(chapter) + 1}", chapter.id, chapter.styleOverrideId ?: book.defaultPageStyleId))
                chapterPages.forEach { page ->
                    val styleId = page.pageStyleId ?: chapter.styleOverrideId ?: book.defaultPageStyleId
                    val style = PageStyles.find(styleId)
                    val document = FormattingDocument.decode(page.body, page.formatting)
                    val section = chapterSections.firstOrNull { it.id == page.sectionId }?.title
                    add(ReaderLeaf(page.title.ifBlank { section.orEmpty() }, document.text, chapter.title, chapter.id, styleId, page, document.asAnnotatedString(style.headingSizeSp, style.subheadingSizeSp, style.initialLetter)))
                }
            }
        }
    }
    val initialLeaf = remember(leaves, initialChapterId, book.id) {
        if (initialChapterId != null) leaves.indexOfFirst { it.chapterId == initialChapterId }.takeIf { it >= 0 }
            ?: preferences.getInt("position.${book.id}", 0).coerceIn(0, (leaves.size - 1).coerceAtLeast(0))
        else preferences.getInt("position.${book.id}", 0).coerceIn(0, (leaves.size - 1).coerceAtLeast(0))
    }
    var mode by rememberSaveable(book.id) { mutableStateOf(preferences.getString("mode.${book.id}", defaultReadingMode) ?: defaultReadingMode) }
    var savedPosition by rememberSaveable(book.id) { mutableIntStateOf(initialLeaf) }
    val scope = rememberCoroutineScope()
    var showModes by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showSpeechScope by remember { mutableStateOf(false) }
    var immersiveChromeVisible by rememberSaveable(book.id) { mutableStateOf(false) }
    var speed by rememberSaveable { androidx.compose.runtime.mutableFloatStateOf(preferences.getFloat("ttsSpeed", 1f)) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialLeaf)
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 720
    val twoPage = mode == "spread" && wide
    val pagerState = rememberPagerState(initialPage = if (twoPage) initialLeaf / 2 else initialLeaf) {
        if (twoPage) ((leaves.size + 1) / 2).coerceAtLeast(1) else leaves.size.coerceAtLeast(1)
    }
    val visiblePosition = if (mode == "paginated" || mode == "spread") pagerState.currentPage * if (twoPage) 2 else 1 else listState.firstVisibleItemIndex
    val tts = remember(context) { mutableStateOf<TextToSpeech?>(null) }
    val speechLines = remember(book.id) { mutableStateListOf<String>() }
    var ttsReady by remember(book.id) { mutableStateOf(false) }
    var speaking by remember(book.id) { mutableStateOf(false) }
    var speechIndex by rememberSaveable(book.id) { mutableIntStateOf(0) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(context, book.id) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post { ttsReady = status == TextToSpeech.SUCCESS && (tts.value?.isLanguageAvailable(Locale.getDefault()) ?: TextToSpeech.LANG_MISSING_DATA) >= TextToSpeech.LANG_AVAILABLE }
        }
        tts.value = engine
        engine.language = Locale.getDefault()
        engine.setSpeechRate(speed)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { mainHandler.post { speaking = true } }
            override fun onDone(utteranceId: String?) {
                val completed = utteranceId?.removePrefix("youni-")?.toIntOrNull() ?: return
                mainHandler.post {
                    speechIndex = (completed + 1).coerceAtMost(speechLines.size)
                    if (speechIndex >= speechLines.size) speaking = false
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { mainHandler.post { speaking = false } }
            override fun onError(utteranceId: String?, errorCode: Int) { mainHandler.post { speaking = false } }
        })
        onDispose { engine.stop(); engine.shutdown(); if (tts.value === engine) tts.value = null }
    }
    val startSpeech: (Int) -> Unit = { from ->
        tts.value?.let { engine ->
            engine.stop()
            engine.setSpeechRate(speed)
            val safeStart = from.coerceIn(0, speechLines.size)
            for (index in safeStart until speechLines.size) {
                val line = speechLines[index].take(3500)
                if (line.isNotBlank()) engine.speak(line, if (index == safeStart) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "youni-$index")
            }
            speechIndex = safeStart
            speaking = speechLines.isNotEmpty() && safeStart < speechLines.size
        }
    }
    LaunchedEffect(visiblePosition, book.id) {
        savedPosition = visiblePosition.coerceAtMost((leaves.size - 1).coerceAtLeast(0))
        preferences.edit().putInt("position.${book.id}", savedPosition).apply()
    }
    LaunchedEffect(mode, book.id) { preferences.edit().putString("mode.${book.id}", mode).apply() }
    LaunchedEffect(speed) { preferences.edit().putFloat("ttsSpeed", speed).apply(); tts.value?.setSpeechRate(speed) }
    LaunchedEffect(initialChapterId, mode) {
        val target = if (initialChapterId != null) leaves.indexOfFirst { it.chapterId == initialChapterId }.takeIf { it >= 0 } ?: savedPosition else savedPosition
        if (mode == "paginated" || mode == "spread") pagerState.scrollToPage(if (mode == "spread" && wide) target / 2 else target)
        else listState.scrollToItem(target.coerceIn(0, (leaves.size - 1).coerceAtLeast(0)))
    }
    val navigateToChapter: (Chapter) -> Unit = { chapter ->
        val target = leaves.indexOfFirst { it.chapterId == chapter.id }.coerceAtLeast(0)
        if (mode == "paginated" || mode == "spread") scope.launch { pagerState.animateScrollToPage(if (mode == "spread" && wide) target / 2 else target) }
        else scope.launch { listState.animateScrollToItem(target) }
        showChapters = false
    }
    val background = when (mode) { "dark" -> Color(0xFF202620); "sepia" -> Color(0xFFF0E5CE); else -> Paper }
    val ink = if (mode == "dark") Color(0xFFECE9DD) else Ink
    val muted = if (mode == "dark") Color(0xFFB7C1AA) else Muted
    Column(Modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (mode != "immersive" || immersiveChromeVisible) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("‹ Close", color = ink) }
            TextButton(onClick = { showChapters = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Contents", color = muted) }
            Spacer(Modifier.weight(1f))
            Text("${(visiblePosition + 1).coerceAtMost(leaves.size)} / ${leaves.size}", color = muted, fontSize = 12.sp)
            Box {
                TextButton(onClick = { showModes = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Aa", color = ink, fontSize = 18.sp) }
                androidx.compose.material3.DropdownMenu(expanded = showModes, onDismissRequest = { showModes = false }) {
                    ReadingModeOptions.forEach { (id, label) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (id == "spread" && !wide) "$label · wide screens" else label) },
                            onClick = {
                                savedPosition = if (mode == "paginated" || mode == "spread") pagerState.currentPage * if (mode == "spread" && wide) 2 else 1 else listState.firstVisibleItemIndex
                                preferences.edit().putInt("position.${book.id}", savedPosition).apply()
                                mode = id
                                immersiveChromeVisible = false
                                showModes = false
                            },
                        )
                    }
                }
            }
        }
        androidx.compose.material3.DropdownMenu(expanded = showChapters, onDismissRequest = { showChapters = false }) {
            orderedChapters.forEachIndexed { index, chapter ->
                androidx.compose.material3.DropdownMenuItem(text = { Text("${index + 1}. ${chapter.title}") }, onClick = { navigateToChapter(chapter) })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                TextButton(onClick = { showSpeechScope = true }, enabled = ttsReady) { Text(if (speaking) "Reading aloud" else "Read aloud", color = muted) }
                androidx.compose.material3.DropdownMenu(expanded = showSpeechScope, onDismissRequest = { showSpeechScope = false }) {
                    listOf("This page", "This chapter", "Entire book").forEachIndexed { scopeIndex, label ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(label) }, onClick = {
                            val start = visiblePosition.coerceIn(0, (leaves.size - 1).coerceAtLeast(0))
                            val current = leaves.getOrNull(start)
                            val selected = when (scopeIndex) {
                                0 -> listOfNotNull(current)
                                1 -> leaves.filter { it.chapterId != null && it.chapterId == current?.chapterId }
                                else -> leaves
                            }
                            speechLines.clear()
                            selected.forEach { leaf ->
                                listOf(leaf.chapterTitle, leaf.title, leaf.body).filterNotNull().filter(String::isNotBlank).forEach { speechLines.addAll(splitSpeechLine(it)) }
                            }
                            startSpeech(0)
                            showSpeechScope = false
                        })
                    }
                }
            }
            if (speechLines.isNotEmpty()) {
                TextButton(onClick = { tts.value?.stop(); speaking = false }) { Text("Pause", color = muted) }
                TextButton(onClick = { startSpeech(speechIndex) }, enabled = !speaking && speechIndex < speechLines.size) { Text("Resume", color = muted) }
                TextButton(onClick = { tts.value?.stop(); speechIndex = 0; speaking = false }) { Text("Stop", color = muted) }
                TextButton(onClick = { speed = (speed - 0.1f).coerceAtLeast(0.6f) }) { Text("−", color = muted) }
                Text("${"%.1f".format(Locale.getDefault(), speed)}×", color = muted, fontSize = 12.sp)
                TextButton(onClick = { speed = (speed + 0.1f).coerceAtMost(1.8f) }) { Text("+", color = muted) }
            }
        }
        }
        if (mode == "paginated" || mode == "spread") {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { it }) { pageIndex ->
                val first = pageIndex * if (twoPage) 2 else 1
                Row(Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 14.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReaderLeafView(leaves.getOrNull(first), mode, twoPage, onOpenPage, Modifier.weight(1f))
                    if (twoPage) ReaderLeafView(leaves.getOrNull(first + 1), mode, false, onOpenPage, Modifier.weight(1f))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(), state = listState,
                contentPadding = PaddingValues(horizontal = if (mode == "focus" || mode == "centered") 18.dp else 26.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (mode == "presentation") 36.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(leaves.size, key = { "reader-${book.id}-$it" }) { index ->
                    ReaderLeafView(
                        leaves[index], mode, wide, onOpenPage,
                        Modifier.fillMaxWidth().widthIn(max = if (wide) 760.dp else 620.dp),
                        onImmersiveTap = if (mode == "immersive") { { immersiveChromeVisible = !immersiveChromeVisible } } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderLeafView(leaf: ReaderLeaf?, mode: String, spread: Boolean, onOpenPage: (Page) -> Unit, modifier: Modifier = Modifier, onImmersiveTap: (() -> Unit)? = null) {
    if (leaf == null) return
    val coverBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = leaf.coverImageUri) {
        val coverLocation = leaf.coverImageUri
        if (coverLocation == null) { value = null; return@produceState }
        val uri = Uri.parse(coverLocation)
        value = withContext(Dispatchers.IO) {
            if (uri.scheme != "file") null else runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(uri.path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1100 || bounds.outHeight / sample > 1500) sample *= 2
                BitmapFactory.decodeFile(uri.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }
    val style = PageStyles.find(leaf.styleId ?: "modern-paper")
    val dark = mode == "dark"
    val sepia = mode == "sepia"
    val paper = when { dark -> Color(0xFF202620); sepia -> Color(0xFFF0E5CE); else -> style.paper }
    val ink = when { dark -> Color(0xFFECE9DD); sepia -> Color(0xFF392F22); else -> style.ink }
    val scale = when (mode) { "presentation" -> 1.3f; "focus", "centered" -> 1.15f; else -> 1f }
    val centered = mode == "centered" || leaf.chapterTitle == null
    Column(
        modifier.clip(RoundedCornerShape(if (mode == "focus" || mode == "centered") 0.dp else 5.dp))
            .background(paper).then(if (onImmersiveTap != null) Modifier.clickable(onClick = onImmersiveTap) else if (leaf.page != null) Modifier.clickable { onOpenPage(leaf.page) } else Modifier)
            .padding(horizontal = if (spread) 22.dp else style.horizontalMarginDp.dp, vertical = if (mode == "presentation") 40.dp else 24.dp)
            .then(if (mode == "presentation" || mode == "centered") Modifier.fillMaxSize() else Modifier),
        verticalArrangement = if (mode == "presentation" || mode == "centered") Arrangement.Center else Arrangement.spacedBy(16.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        coverBitmap?.let { bitmap ->
            Image(bitmap.asImageBitmap(), contentDescription = "${leaf.title} cover", modifier = Modifier.fillMaxWidth().heightIn(max = if (mode == "presentation") 380.dp else 250.dp).clip(RoundedCornerShape(4.dp)))
        }
        YouniContentTypography {
            leaf.chapterTitle?.let { Text(it.uppercase(), color = if (dark) Color(0xFFB7C1AA) else Accent, fontSize = 11.sp, letterSpacing = 1.8.sp) }
            if (leaf.title.isNotBlank()) Text(leaf.title, color = ink, fontFamily = style.titleFont, fontSize = (style.titleSizeSp * scale).sp, lineHeight = ((style.titleSizeSp + 6) * scale).sp, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
            if (leaf.body.isNotBlank()) Text(leaf.formattedBody ?: AnnotatedString(leaf.body), color = ink, fontFamily = style.bodyFont, fontSize = (style.bodySizeSp * scale).sp, lineHeight = (style.lineHeightSp * scale).sp, textAlign = if (mode == "centered" || leaf.chapterTitle == null) TextAlign.Center else TextAlign.Start)
        }
    }
}

private fun splitSpeechLine(text: String): List<String> {
    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    text.split(Regex("\\s+")).filter(String::isNotBlank).forEach { word ->
        if (current.isNotEmpty() && current.length + word.length + 1 > 3000) {
            chunks += current.toString()
            current = StringBuilder()
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(word.take(3000))
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

@Composable
fun EditBookDetailsDialog(book: Book, onDismiss: () -> Unit, onSave: (Book) -> Unit) {
    var title by rememberSaveable(book.id) { mutableStateOf(book.title) }
    var subtitle by rememberSaveable(book.id) { mutableStateOf(book.subtitle) }
    var author by rememberSaveable(book.id) { mutableStateOf(book.author) }
    var description by rememberSaveable(book.id) { mutableStateOf(book.description) }
    var status by rememberSaveable(book.id) { mutableStateOf(book.status) }
    var template by rememberSaveable(book.id) { mutableStateOf(book.coverTemplateId) }
    var style by rememberSaveable(book.id) { mutableStateOf(book.defaultPageStyleId) }
    var imageUri by rememberSaveable(book.id) { mutableStateOf(book.coverImageUri) }
    var imageCopyFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val copy = runCatching { withContext(Dispatchers.IO) { copyCoverToPrivateStorage(context, uri) } }
                copy.onSuccess { imageUri = it; imageCopyFailed = false }
                    .onFailure { imageCopyFailed = true }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Subtitle") }, singleLine = true)
                OutlinedTextField(author, { author = it }, label = { Text("Author") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description · optional") }, minLines = 2)
                Text("STATUS", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("draft" to "Draft", "finished" to "Finished", "archived" to "Archived").forEach { (id, label) ->
                        TextButton(onClick = { status = id }) { Text(label, color = if (status == id) Accent else Ink, fontSize = 12.sp) }
                    }
                }
                TextButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (imageUri == null) "Choose cover image" else "Change cover image", color = Accent) }
                if (imageCopyFailed) Text("That image couldn't be saved. Choose another image.", color = YouniColors.error, style = MaterialTheme.typography.bodySmall)
                BookCover(title, subtitle, author, coverTemplate(template), Modifier.align(Alignment.CenterHorizontally).width(110.dp).height(155.dp), imageUri)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { BookCoverTemplates.all.forEach { t -> TextButton(onClick = { template = t.id }) { Text(t.name.take(1), color = if (template == t.id) Accent else Ink) } } }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { listOf("modern-paper" to "Modern", "classic-book" to "Classic", "literary" to "Literary", "parchment" to "Parchment", "ancient-manuscript" to "Ancient", "medieval-manuscript" to "Medieval", "dark-journal" to "Dark").forEach { (id, label) -> TextButton(onClick = { style = id }) { Text(label, color = if (style == id) Accent else Ink, fontSize = 11.sp) } } }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(book.copy(title = title.trim(), subtitle = subtitle.trim(), author = author.trim(), description = description.trim(), status = status, coverTemplateId = template, coverImageUri = imageUri, defaultPageStyleId = style, updatedAt = System.currentTimeMillis())) }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } },
        containerColor = Paper,
    )
}

private fun copyCoverToPrivateStorage(context: android.content.Context, source: Uri): String {
    val directory = java.io.File(context.filesDir, "book-covers").apply { mkdirs() }
    val file = java.io.File(directory, "${UUID.randomUUID()}.cover")
    try {
        val input = context.contentResolver.openInputStream(source) ?: error("Image can't be opened")
        input.use { image ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = image.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 30 * 1024 * 1024) { "Image is too large" }
                    output.write(buffer, 0, count)
                }
                require(total > 0) { "Image is empty" }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Image format isn't supported" }
        return Uri.fromFile(file).toString()
    } catch (failure: Throwable) {
        file.delete()
        throw failure
    }
}

@Composable
fun FrontMatterEditorDialog(component: BookComponent, onDismiss: () -> Unit, onSave: (BookComponent) -> Unit) {
    var title by rememberSaveable(component.id) { mutableStateOf(component.title) }
    var body by rememberSaveable(component.id) { mutableStateOf(component.body) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(component.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Heading") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Text") }, minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(component.copy(title = title, body = body, updatedAt = System.currentTimeMillis())) }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } },
        containerColor = Paper,
    )
}
