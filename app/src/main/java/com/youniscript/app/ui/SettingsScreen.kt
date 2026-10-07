package com.youniscript.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youniscript.app.ui.theme.YouniColors
import com.youniscript.app.ui.PageStyles
import com.youniscript.app.editor.ProofOptions
import com.youniscript.app.security.AppLockCrypto
import com.youniscript.app.data.MediaAttachment
import com.youniscript.app.data.AuthorProfile
import com.youniscript.app.data.WritingTemplate
import java.util.UUID
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.animation.ValueAnimator
import android.view.accessibility.AccessibilityManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected

private data class SettingsSection(val title: String, val details: List<String>)
private data class StorageSummary(val databaseBytes: Long, val imageBytes: Long, val audioBytes: Long, val drawingBytes: Long, val largeAttachments: List<Pair<String, Long>>, val orphanFiles: List<File>, val missingFiles: Int)

private fun Long.readableBytes(): String = when {
    this >= 1024L * 1024L -> "${"%.1f".format(this / (1024.0 * 1024.0))} MB"
    this >= 1024L -> "${"%.0f".format(this / 1024.0)} KB"
    else -> "$this bytes"
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    selectedTheme: String,
    accessibilityTextSize: String,
    onAccessibilityTextSizeSelected: (String) -> Unit,
    onThemeSelected: (String) -> Unit,
    defaultPageStyleId: String,
    onDefaultPageStyleSelected: (String) -> Unit,
    authorProfile: AuthorProfile,
    onAuthorProfileChanged: (AuthorProfile) -> Unit,
    writingTemplates: List<WritingTemplate>,
    onWritingTemplatesChanged: (List<WritingTemplate>) -> Unit,
    attachments: List<MediaAttachment>,
    defaultReadingMode: String,
    readingSpeed: Float,
    onDefaultReadingModeSelected: (String) -> Unit,
    onReadingSpeedChanged: (Float) -> Unit,
    proofOptions: ProofOptions,
    onProofOptionsChanged: (ProofOptions) -> Unit,
    onExportJson: () -> Unit,
    onExportText: () -> Unit,
    onExportMarkdown: () -> Unit,
    onSelectJsonImport: () -> Unit,
    onExportEncryptedBackup: (CharArray) -> Unit,
    onSelectEncryptedBackup: () -> Unit,
    onImportEncryptedBackup: (CharArray) -> Unit,
    encryptedBackupSelected: Boolean,
    onDismissEncryptedBackup: () -> Unit,
    appLockEnabled: Boolean,
    appLockTimeout: Long,
    onSetAppPin: (CharArray?, CharArray) -> Boolean,
    onDisableAppLock: (CharArray) -> Boolean,
    onAppLockTimeoutChanged: (Long) -> Unit,
    onLockNow: () -> Unit,
) {
    var exportPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showExportPassword by remember { mutableStateOf(false) }
    var importPassword by remember(encryptedBackupSelected) { mutableStateOf("") }
    var showDefaultStyleMenu by remember { mutableStateOf(false) }
    var showReadingModeMenu by remember { mutableStateOf(false) }
    var storageSummary by remember { mutableStateOf<StorageSummary?>(null) }
    var confirmOrphanCleanup by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WritingTemplate?>(null) }
    var templateTitle by remember { mutableStateOf("") }
    var templateBody by remember { mutableStateOf("") }
    var templateEditorOpen by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<WritingTemplate?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf<String?>(null) }
    val settingsListState = rememberLazyListState()
    LaunchedEffect(selectedSection) { settingsListState.scrollToItem(0) }
    var storageRefresh by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(attachments, storageRefresh) {
        storageSummary = withContext(Dispatchers.IO) {
            val database = context.getDatabasePath("youniscript.db")
            val databaseBytes = listOf(database, File(database.path + "-wal"), File(database.path + "-shm")).filter(File::isFile).sumOf(File::length)
            val root = File(context.filesDir, "page-media").canonicalFile
            val referenced = attachments.mapNotNull { attachment ->
                runCatching {
                    val uri = Uri.parse(attachment.localUri)
                    require(uri.scheme == "file")
                    File(uri.path ?: error("missing path")).canonicalFile.also { require(it.path.startsWith(root.path + File.separator)) }
                }.getOrNull()
            }.toSet()
            val allFiles = if (root.exists()) root.walkTopDown().filter(File::isFile).toList() else emptyList()
            val orphanFiles = allFiles.filter { it.canonicalFile !in referenced }
            val byType = attachments.groupBy { it.mediaType }
            fun sizeFor(type: String) = byType[type].orEmpty().sumOf { item ->
                runCatching { val uri = Uri.parse(item.localUri); if (uri.scheme == "file") File(uri.path!!).length() else 0L }.getOrDefault(0L)
            }
            val largeAttachments = attachments.mapNotNull { item ->
                val size = runCatching { val uri = Uri.parse(item.localUri); if (uri.scheme == "file") File(uri.path!!).length() else 0L }.getOrDefault(0L)
                if (size >= 1024 * 1024) item.displayName to size else null
            }.sortedByDescending { it.second }.take(5)
            StorageSummary(databaseBytes, sizeFor("image"), sizeFor("audio"), sizeFor("drawing"), largeAttachments, orphanFiles, attachments.count { item ->
                runCatching { val uri = Uri.parse(item.localUri); uri.scheme == "file" && !File(uri.path!!).isFile }.getOrDefault(false)
            })
        }
    }
    var dictionaryTerm by remember { mutableStateOf("") }
    var dictionarySearch by rememberSaveable { mutableStateOf("") }
    var replacementSearch by rememberSaveable { mutableStateOf("") }
    var editingDictionaryWord by remember { mutableStateOf<String?>(null) }
    var editedDictionaryWord by remember { mutableStateOf("") }
    var replacementEditingFrom by remember { mutableStateOf<String?>(null) }
    var replacementFrom by remember { mutableStateOf("") }
    var replacementTo by remember { mutableStateOf("") }
    val sections = listOf(
        SettingsSection("Appearance", listOf("Choose the app theme. Page styles remain independent.")),
        SettingsSection("Writing", listOf("Pages save automatically on this device while you write and when you leave the editor.")),
        SettingsSection("YouniProof", listOf("Local rule-based suggestions. Writing is not sent to a server; corrections are never applied without your choice.")),
        SettingsSection("Personal Dictionary", listOf("Words you add here are skipped by spelling suggestions.")),
        SettingsSection("Reading", listOf("Choose how books open. Your place and per-book reading mode are saved on this device.")),
        SettingsSection("Page Styles", listOf("Choose from nine page styles in the editor. A style changes presentation and keeps your writing intact.")),
        SettingsSection("Templates", listOf("Create reusable starting pages for the kinds of writing you return to.")),
        SettingsSection("Security", listOf(if (appLockEnabled) "PIN and device authentication are available. The app lock hides content in Recents; the on-device database itself is not encrypted." else "Your library is stored locally and requires no account or network connection. A PIN app lock is available; the on-device database itself is not encrypted.")),
        SettingsSection("Storage & Backups", listOf("TXT, Markdown, and JSON exports are unencrypted. Text exports contain readable writing and metadata, not media bytes. Encrypted .youni files include supported page media and book covers. Import never overwrites existing records.")),
        SettingsSection("Accessibility", listOf("Text size follows Android's system text scaling. TalkBack and system navigation remain available.")),
        SettingsSection("About", listOf("YouniScript", "Write what is yours.")),
    )
    Column(Modifier.fillMaxSize().background(YouniColors.library).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
            Text("‹ Library", color = YouniColors.ink, style = MaterialTheme.typography.titleMedium)
        }
        val selectedSettings = sections.firstOrNull { it.title == selectedSection }
        val showingLicense = selectedSection == "License"
        val visibleSections = selectedSettings?.let(::listOf).orEmpty()
        LazyColumn(
            Modifier.fillMaxSize(),
            state = settingsListState,
            contentPadding = PaddingValues(start = 22.dp, top = 4.dp, end = 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedSettings == null && !showingLicense) {
                item { Text("Settings", color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 31.sp, modifier = Modifier.semantics { heading() }) }
                items(sections, key = { "menu-${it.title}" }) { section ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { selectedSection = section.title }.padding(vertical = 13.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(section.title, color = YouniColors.ink, style = MaterialTheme.typography.titleMedium)
                            Text(section.details.firstOrNull().orEmpty(), color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", color = YouniColors.sage, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            } else {
                item {
                    TextButton(onClick = { selectedSection = if (showingLicense) "About" else null }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                        Text(if (showingLicense) "‹ About" else "‹ Settings", color = YouniColors.ink, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(if (showingLicense) "License" else selectedSettings!!.title, color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 27.sp, lineHeight = 32.sp, modifier = Modifier.semantics { heading() })
                }
            }
            if (showingLicense) item(key = "gpl-license") { LicenseContent() }
            items(visibleSections, key = { "content-${it.title}" }) { section ->
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (section.title == "Appearance") {
                        listOf("light" to "Light", "dark" to "Dark", "system" to "System Default").forEach { (value, label) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onThemeSelected(value) }.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, Modifier.alignByBaseline(), color = YouniColors.ink, style = MaterialTheme.typography.bodyLarge)
                                RadioButton(selected = selectedTheme == value, onClick = { onThemeSelected(value) })
                            }
                        }
                    }
                    if (section.title == "Page Styles") {
                        TextButton(onClick = { showDefaultStyleMenu = true }) {
                            Text("Default for new pages · ${PageStyles.find(defaultPageStyleId).name}", color = YouniColors.ink)
                        }
                        DropdownMenu(expanded = showDefaultStyleMenu, onDismissRequest = { showDefaultStyleMenu = false }) {
                            PageStyles.all.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.name) },
                                    onClick = { onDefaultPageStyleSelected(style.id); showDefaultStyleMenu = false },
                                )
                            }
                        }
                    }
                    if (section.title == "Writing") {
                        Text("Author profile", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                        Text("Keep a private author name, biography, signature, and personal seal on this device.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            authorProfile.name,
                            { onAuthorProfileChanged(authorProfile.copy(name = it)) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Author name") },
                            supportingText = { Text("Used as the suggested author on new books.") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            authorProfile.biography,
                            { onAuthorProfileChanged(authorProfile.copy(biography = it)) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Short biography · optional") },
                            minLines = 2,
                            maxLines = 4,
                        )
                        OutlinedTextField(
                            authorProfile.signature,
                            { onAuthorProfileChanged(authorProfile.copy(signature = it)) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Letter signature · optional") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            authorProfile.seal,
                            { onAuthorProfileChanged(authorProfile.copy(seal = it)) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Personal seal or initials · optional") },
                            supportingText = { Text("A simple text seal for personal identity; it is not a cryptographic signature.") },
                            singleLine = true,
                        )
                        val sealLabel = authorProfile.seal.trim().ifBlank {
                            authorProfile.name.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "Y" }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                            Box(
                                Modifier.size(76.dp).border(1.dp, YouniColors.sage, CircleShape).background(YouniColors.library, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(sealLabel, color = YouniColors.ink, fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    if (section.title == "Templates") {
                        Text("These templates stay on this device and can be selected from Create. A template creates a separate editable page.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            editingTemplate = null
                            templateTitle = ""
                            templateBody = ""
                            templateEditorOpen = true
                        }) { Text("+ New template", color = YouniColors.ink) }
                        writingTemplates.forEach { template ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(template.title, color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                                if (template.body.isNotBlank()) Text(template.body, color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                Row {
                                    TextButton(onClick = {
                                        editingTemplate = template
                                        templateTitle = template.title
                                        templateBody = template.body
                                        templateEditorOpen = true
                                    }) { Text("Edit", color = YouniColors.sage) }
                                    TextButton(onClick = { deletingTemplate = template }) { Text("Delete", color = YouniColors.error) }
                                }
                            }
                        }
                    }
                    if (section.title == "Reading") {
                        Box {
                            TextButton(onClick = { showReadingModeMenu = true }) {
                                Text("Default · ${ReadingModeOptions.firstOrNull { it.first == defaultReadingMode }?.second ?: "Continuous scroll"}", color = YouniColors.ink)
                            }
                            DropdownMenu(expanded = showReadingModeMenu, onDismissRequest = { showReadingModeMenu = false }) {
                                ReadingModeOptions.forEach { (id, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { onDefaultReadingModeSelected(id); showReadingModeMenu = false })
                                }
                            }
                        }
                        Text("Read aloud speed · ${"%.1f".format(readingSpeed)}×", color = YouniColors.ink, style = MaterialTheme.typography.bodyLarge)
                        androidx.compose.material3.Slider(value = readingSpeed, onValueChange = onReadingSpeedChanged, valueRange = 0.6f..1.8f, steps = 11)
                    }
                    if (section.title == "YouniProof") {
                        listOf(
                            Triple("spelling", "Spelling suggestions", proofOptions.spelling),
                            Triple("grammar", "Grammar suggestions", proofOptions.grammar),
                            Triple("punctuation", "Punctuation and capitalization", proofOptions.punctuation),
                            Triple("style", "Repeated words and long sentences", proofOptions.style),
                        ).forEach { (key, title, enabled) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(title, Modifier.alignByBaseline(), color = YouniColors.ink, style = MaterialTheme.typography.bodyLarge)
                                Switch(enabled, onCheckedChange = { value ->
                                    onProofOptionsChanged(when (key) {
                                        "spelling" -> proofOptions.copy(spelling = value)
                                        "grammar" -> proofOptions.copy(grammar = value)
                                        "punctuation" -> proofOptions.copy(punctuation = value)
                                        else -> proofOptions.copy(style = value)
                                    })
                                })
                            }
                        }
                        Text("Suggestions are local and deterministic. Nothing is sent to a server or applied without your choice.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { selectedSection = "Personal Dictionary" }) { Text("Personal Dictionary", color = YouniColors.sage) }
                        TextButton(onClick = { selectedSection = "Personal Dictionary"; dictionarySearch = ""; replacementSearch = "" }) { Text("Custom Replacements", color = YouniColors.sage) }
                    }
                    if (section.title == "Personal Dictionary") {
                        Text("Words and replacements stay on this device and are used by local YouniProof suggestions.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                        Text("WORDS", color = YouniColors.sage, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp, modifier = Modifier.semantics { heading() })
                        OutlinedTextField(dictionarySearch, { dictionarySearch = it }, Modifier.fillMaxWidth(), label = { Text("Search words") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(dictionaryTerm, { dictionaryTerm = it }, label = { Text("Add a word") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val word = dictionaryTerm.trim()
                                if (word.isNotEmpty()) onProofOptionsChanged(proofOptions.copy(dictionary = proofOptions.dictionary + word))
                                dictionaryTerm = ""
                            }, enabled = dictionaryTerm.isNotBlank()) { Text("Add") }
                        }
                        val visibleWords = proofOptions.dictionary.filter { it.contains(dictionarySearch.trim(), ignoreCase = true) }.sortedBy(String::lowercase)
                        if (visibleWords.isEmpty()) {
                            Text(if (proofOptions.dictionary.isEmpty()) "Your personal dictionary is empty. Add a name, place, or word you use." else "No words match this search.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                        }
                        visibleWords.forEach { word ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(word, Modifier.weight(1f), color = YouniColors.ink, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { editingDictionaryWord = word; editedDictionaryWord = word }) { Text("Edit", color = YouniColors.sage) }
                                TextButton(onClick = { onProofOptionsChanged(proofOptions.copy(dictionary = proofOptions.dictionary - word)) }) { Text("Delete", color = YouniColors.error) }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = YouniColors.divider, modifier = Modifier.padding(vertical = 6.dp))
                        Text("CUSTOM REPLACEMENTS", color = YouniColors.sage, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp, modifier = Modifier.semantics { heading() })
                        OutlinedTextField(replacementSearch, { replacementSearch = it }, Modifier.fillMaxWidth(), label = { Text("Search replacements") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(replacementFrom, { replacementFrom = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(replacementTo, { replacementTo = it }, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                val from = replacementFrom.trim()
                                if (from.isNotEmpty()) {
                                    val updated = proofOptions.replacements - listOfNotNull(replacementEditingFrom)
                                    onProofOptionsChanged(proofOptions.copy(replacements = updated + (from to replacementTo)))
                                }
                                replacementEditingFrom = null
                                replacementFrom = ""; replacementTo = ""
                            }, enabled = replacementFrom.isNotBlank()) { Text(if (replacementEditingFrom == null) "Add replacement" else "Save replacement") }
                            if (replacementEditingFrom != null) TextButton(onClick = { replacementEditingFrom = null; replacementFrom = ""; replacementTo = "" }) { Text("Cancel") }
                        }
                        val visibleReplacements = proofOptions.replacements.toSortedMap(String.CASE_INSENSITIVE_ORDER).filter { (from, to) ->
                            val query = replacementSearch.trim()
                            query.isEmpty() || from.contains(query, true) || to.contains(query, true)
                        }
                        if (visibleReplacements.isEmpty()) Text(if (proofOptions.replacements.isEmpty()) "No custom replacements yet. For example, teh → the." else "No replacements match this search.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                        visibleReplacements.forEach { (from, to) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("$from → $to", Modifier.weight(1f), color = YouniColors.ink, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { replacementEditingFrom = from; replacementFrom = from; replacementTo = to }) { Text("Edit", color = YouniColors.sage) }
                                TextButton(onClick = { onProofOptionsChanged(proofOptions.copy(replacements = proofOptions.replacements - from)) }) { Text("Delete", color = YouniColors.error) }
                            }
                        }
                    }
                    if (section.title == "Accessibility") {
                        val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                        val systemScale = context.resources.configuration.fontScale
                        AccessibilityContent(
                            selectedTextSize = accessibilityTextSize,
                            onTextSizeSelected = onAccessibilityTextSizeSelected,
                            systemTextScale = systemScale,
                            screenReaderEnabled = manager?.isTouchExplorationEnabled == true,
                            highContrastEnabled = android.provider.Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1,
                            systemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled(),
                            onOpenReading = { selectedSection = "Reading" },
                            onOpenSystemAccessibility = {
                                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                                    .onFailure { Toast.makeText(context, "Android accessibility settings are unavailable.", Toast.LENGTH_LONG).show() }
                            },
                        )
                    }
                    if (section.title == "About") {
                        AboutContent(onOpenPermissions = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            runCatching { context.startActivity(intent) }
                        }, onOpenLicense = { selectedSection = "License" }, onDonate = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/DeveloperCal/"))
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "No browser is available to open Liberapay.", Toast.LENGTH_LONG).show() }
                        })
                    }
                    if (section.title == "Storage & Backups") {
                        Column {
                            val summary = storageSummary
                            if (summary != null) {
                                Text("On this device", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                                Text("Library database  ·  ${summary.databaseBytes.readableBytes()}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                                Text("Images  ·  ${summary.imageBytes.readableBytes()}    Audio  ·  ${summary.audioBytes.readableBytes()}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                                Text("Drawings  ·  ${summary.drawingBytes.readableBytes()}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyMedium)
                                if (summary.largeAttachments.isNotEmpty()) {
                                    Text("Largest attachments", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                                    summary.largeAttachments.forEach { (name, size) -> Text("$name  ·  ${size.readableBytes()}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall) }
                                }
                                if (summary.missingFiles > 0) Text("${summary.missingFiles} attachment file(s) are unavailable. Their library records remain.", color = YouniColors.error, style = MaterialTheme.typography.bodySmall)
                                if (summary.orphanFiles.isNotEmpty()) {
                                    Text("${summary.orphanFiles.size} unreferenced private file(s) · ${summary.orphanFiles.sumOf(File::length).readableBytes()}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { confirmOrphanCleanup = true }) { Text("Review and remove unreferenced files", color = YouniColors.error) }
                                }
                            } else Text("Calculating local storage…", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showExportPassword = true }) { Text("Create encrypted backup", color = YouniColors.ink) }
                                TextButton(onClick = onSelectEncryptedBackup) { Text("Restore backup", color = YouniColors.ink) }
                            }
                            Text("Exports are saved to the folder you choose in Android's document picker.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onExportJson) { Text("Export JSON (unencrypted)", color = YouniColors.mutedInk) }
                                TextButton(onClick = onSelectJsonImport) { Text("Import JSON", color = YouniColors.mutedInk) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onExportText) { Text("Export TXT", color = YouniColors.mutedInk) }
                                TextButton(onClick = onExportMarkdown) { Text("Export Markdown", color = YouniColors.mutedInk) }
    }
}
                    }
                    if (section.title == "Security") {
                        AppLockSettings(
                            pinConfigured = appLockEnabled,
                            autoLockTimeout = appLockTimeout,
                            onSetPin = onSetAppPin,
                            onDisable = onDisableAppLock,
                            onTimeoutChanged = onAppLockTimeoutChanged,
                            onLockNow = onLockNow,
                        )
                    }
                }
            }
        }
    }
    if (templateEditorOpen) AlertDialog(
        onDismissRequest = { templateEditorOpen = false },
        title = { Text(if (editingTemplate == null) "New writing template" else "Edit writing template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(templateTitle, { templateTitle = it.take(80) }, label = { Text("Template name") }, singleLine = true)
                OutlinedTextField(templateBody, { templateBody = it.take(8000) }, label = { Text("Starting text · optional") }, minLines = 4, maxLines = 8)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val saved = WritingTemplate(editingTemplate?.id ?: UUID.randomUUID().toString(), templateTitle.trim(), templateBody)
                val next = if (editingTemplate == null) writingTemplates + saved else writingTemplates.map { if (it.id == saved.id) saved else it }
                onWritingTemplatesChanged(next)
                templateEditorOpen = false
            }, enabled = templateTitle.isNotBlank()) { Text("Save", color = YouniColors.sage) }
        },
        dismissButton = { TextButton(onClick = { templateEditorOpen = false }) { Text("Cancel", color = YouniColors.ink) } },
        containerColor = YouniColors.paper,
    )
    editingDictionaryWord?.let { original -> AlertDialog(
        onDismissRequest = { editingDictionaryWord = null },
        title = { Text("Edit dictionary word") },
        text = { OutlinedTextField(editedDictionaryWord, { editedDictionaryWord = it }, label = { Text("Word") }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = {
                val updated = editedDictionaryWord.trim()
                if (updated.isNotEmpty()) onProofOptionsChanged(proofOptions.copy(dictionary = (proofOptions.dictionary - original) + updated))
                editingDictionaryWord = null
            }, enabled = editedDictionaryWord.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editingDictionaryWord = null }) { Text("Cancel") } },
        containerColor = YouniColors.paper,
    ) }
    deletingTemplate?.let { template -> AlertDialog(
        onDismissRequest = { deletingTemplate = null },
        title = { Text("Delete template?") },
        text = { Text("${template.title} will be removed. Pages already created from it will stay in your Library.") },
        confirmButton = { TextButton(onClick = { onWritingTemplatesChanged(writingTemplates.filterNot { it.id == template.id }); deletingTemplate = null }) { Text("Delete", color = YouniColors.error) } },
        dismissButton = { TextButton(onClick = { deletingTemplate = null }) { Text("Keep", color = YouniColors.ink) } },
        containerColor = YouniColors.paper,
    ) }
    if (showExportPassword) AlertDialog(
        onDismissRequest = { showExportPassword = false; exportPassword = ""; confirmPassword = "" },
        title = { Text("Encrypt your backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a passphrase with at least 12 characters. YouniScript does not store it; you will need it to restore this file.", color = YouniColors.mutedInk)
                OutlinedTextField(exportPassword, { exportPassword = it }, label = { Text("Passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val password = exportPassword
                showExportPassword = false
                exportPassword = ""
                confirmPassword = ""
                onExportEncryptedBackup(password.toCharArray())
            }, enabled = exportPassword.length >= 12 && exportPassword == confirmPassword) { Text("Choose file", color = YouniColors.sage) }
        },
        dismissButton = { TextButton(onClick = { showExportPassword = false; exportPassword = ""; confirmPassword = "" }) { Text("Cancel") } },
    )
    if (confirmOrphanCleanup) {
        val candidates = storageSummary?.orphanFiles.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmOrphanCleanup = false },
            title = { Text("Remove unreferenced files?") },
            text = { Text("${candidates.size} private file(s), ${candidates.sumOf(File::length).readableBytes()}, are not referenced by a media attachment record. Only those files will be removed. Your pages and attached media records will not change.", color = YouniColors.mutedInk) },
            confirmButton = { TextButton(onClick = {
                confirmOrphanCleanup = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val safeRoot = File(context.filesDir, "page-media").canonicalFile
                        candidates.forEach { file -> runCatching { val canonical = file.canonicalFile; require(canonical.path.startsWith(safeRoot.path + File.separator)); canonical.delete() } }
                    }
                    storageRefresh++
                }
            }) { Text("Remove files", color = YouniColors.error) } },
            dismissButton = { TextButton(onClick = { confirmOrphanCleanup = false }) { Text("Cancel", color = YouniColors.ink) } },
        )
    }
    if (encryptedBackupSelected) AlertDialog(
        onDismissRequest = onDismissEncryptedBackup,
        title = { Text("Restore encrypted backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the passphrase used when the backup was created.", color = YouniColors.mutedInk)
                OutlinedTextField(importPassword, { importPassword = it }, label = { Text("Passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = {
            TextButton(onClick = { val password = importPassword; importPassword = ""; onImportEncryptedBackup(password.toCharArray()) }, enabled = importPassword.isNotEmpty()) {
                Text("Restore", color = YouniColors.sage)
            }
        },
        dismissButton = { TextButton(onClick = onDismissEncryptedBackup) { Text("Cancel") } },
    )
}

@Composable
private fun AboutContent(onOpenPermissions: () -> Unit, onOpenLicense: () -> Unit, onDonate: () -> Unit) {
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            (info.versionName ?: "Unknown") to PackageInfoCompat.getLongVersionCode(info)
        }.getOrDefault("Unknown" to 0L)
    }
    val principles = listOf(
        "PRIVATE BY DESIGN" to "Your library stays on this device unless you export or back it up.",
        "OFFLINE FIRST" to "Writing and library tools work without an internet connection.",
        "NO ACCOUNT REQUIRED" to "Use your personal library without creating an account.",
        "YOUR WORK, YOUR LIBRARY" to "Your pages, books, journals, and memories are yours to keep.",
    )
    val features = listOf(
        "¶" to "Pages & rich text",
        "▤" to "Books & manuscripts",
        "◷" to "Journals",
        "◇" to "Collections",
        "✓" to "YouniProof",
        "Aa" to "Personal Dictionary",
        "☆" to "Bookmarks & annotations",
        "↗" to "Cross-links",
        "↺" to "Revisions",
        "◉" to "Reading modes",
        "♪" to "Read Aloud",
        "▧" to "Local media",
        "⌑" to "Encrypted backups",
        "⇄" to "Export & import",
    )

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(com.youniscript.app.R.drawable.ic_youniscript_approved_legacy),
                contentDescription = "YouniScript open-book emblem",
                modifier = Modifier.size(84.dp),
            )
            Text("YouniScript", color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 31.sp)
            Text("Write what is yours.", color = YouniColors.sage, style = MaterialTheme.typography.titleMedium)
            Text(
                "Version ${appVersion.first}  ·  Build ${appVersion.second}",
                color = YouniColors.mutedInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            AboutSectionTitle("YOUR PERSONAL LIBRARY")
            Text(
                "A private space for your writing, books, journals, memories, and ideas.",
                color = YouniColors.secondaryInk,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AboutSectionTitle("WHAT GUIDES US")
            Column(
                Modifier.fillMaxWidth()
                    .background(YouniColors.elevatedPaper, RoundedCornerShape(16.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(16.dp)),
            ) {
                principles.forEachIndexed { index, (title, description) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, color = YouniColors.sage, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                        Text(description, color = YouniColors.secondaryInk, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
                    }
                    if (index != principles.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            Modifier.padding(horizontal = 15.dp),
                            color = YouniColors.divider,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AboutSectionTitle("BUILT FOR YOUR WRITING")
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                features.forEach { (symbol, label) ->
                    Row(
                        Modifier.fillMaxWidth(0.48f)
                            .background(YouniColors.elevatedPaper, RoundedCornerShape(12.dp))
                            .border(1.dp, YouniColors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(symbol, color = YouniColors.sage, style = MaterialTheme.typography.titleSmall)
                        Text(label, color = YouniColors.ink, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionTitle("PRIVACY & PERMISSIONS")
            Row(
                Modifier.fillMaxWidth()
                    .background(YouniColors.elevatedPaper, RoundedCornerShape(12.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPermissions)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("App permissions", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                    Text("Review microphone access in Android settings.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
                }
                Text("›", color = YouniColors.sage, style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "Your library is stored locally. YouniScript has no account or internet access.",
                color = YouniColors.mutedInk,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionTitle("LICENSE")
            Row(
                Modifier.fillMaxWidth()
                    .background(YouniColors.elevatedPaper, RoundedCornerShape(12.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onOpenLicense)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("GNU General Public License v3.0", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                    Text("View License", color = YouniColors.sage, style = MaterialTheme.typography.bodySmall)
                }
                Text("›", color = YouniColors.sage, style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "YouniScript source code is licensed under the GNU General Public License v3.0. The YouniScript name, logo, icon, splash artwork, and other branding are not licensed under the GPL and remain the property of DeveloperCal.",
                color = YouniColors.mutedInk,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionTitle("SUPPORT YOUNISCRIPT")
            Text("If you find YouniScript useful, you can support its continued development.", color = YouniColors.secondaryInk, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
            Button(onClick = onDonate, modifier = Modifier.fillMaxWidth()) { Text("Donate") }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Made for people who have something to say.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
            Text("YouniScript", color = YouniColors.sage, fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun LicenseContent() {
    val context = LocalContext.current
    val license = remember(context) {
        runCatching { context.assets.open("gpl-3.0.txt").bufferedReader().use { it.readText() } }
            .getOrDefault("The bundled GNU GPLv3 text could not be loaded. The complete license is available at https://www.gnu.org/licenses/gpl-3.0.html")
    }
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("YouniScript source code · GPL-3.0", color = YouniColors.sage, style = MaterialTheme.typography.titleSmall)
        Text(license, color = YouniColors.secondaryInk, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
    }
}

@Composable
private fun AccessibilityContent(
    selectedTextSize: String,
    onTextSizeSelected: (String) -> Unit,
    systemTextScale: Float,
    screenReaderEnabled: Boolean,
    highContrastEnabled: Boolean,
    systemAnimationsEnabled: Boolean,
    onOpenReading: () -> Unit,
    onOpenSystemAccessibility: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(
            "Make YouniScript easier and more comfortable to read, write, and navigate.",
            color = YouniColors.secondaryInk,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutSectionTitle("TEXT")
            Text("App text size", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
            listOf("system" to "System Default", "small" to "Small", "large" to "Large", "extra-large" to "Extra Large").forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.RadioButton) { onTextSizeSelected(value) }
                        .semantics(mergeDescendants = true) { selected = selectedTextSize == value },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, Modifier.weight(1f), color = YouniColors.ink, style = MaterialTheme.typography.bodyLarge)
                    RadioButton(selected = selectedTextSize == value, onClick = { onTextSizeSelected(value) })
                }
            }
            Text(
                "System text size · ${String.format(java.util.Locale.getDefault(), "%.1f×", systemTextScale)}",
                color = YouniColors.mutedInk,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("YouniScript also respects Android's system text size.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
            Column(
                Modifier.fillMaxWidth().background(YouniColors.elevatedPaper, RoundedCornerShape(14.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Aa", color = YouniColors.sage, fontFamily = FontFamily.Serif, fontSize = 30.sp)
                Text("This is how text will appear in YouniScript.", color = YouniColors.ink, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionTitle("TOUCH TARGETS")
            Text("Settings categories use full-width rows. Primary actions use Android's standard touch areas where practical.", color = YouniColors.secondaryInk, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionTitle("SCREEN READER")
            Text(
                "YouniScript provides labels, roles, selection states, and navigation information for supported controls when Android screen readers such as TalkBack are enabled.",
                color = YouniColors.secondaryInk,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
            Text("Touch exploration · ${if (screenReaderEnabled) "On" else "Off"}", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenSystemAccessibility) { Text("Android Accessibility Settings", color = YouniColors.sage) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionTitle("CONTRAST")
            Text(
                "YouniScript uses theme-aware forest, sage, parchment, and ink colors. Android high-contrast text is ${if (highContrastEnabled) "enabled" else "not enabled"}.",
                color = YouniColors.secondaryInk,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
            Text("No separate app contrast switch is provided.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionTitle("MOTION")
            Text(
                "Android system animations are ${if (systemAnimationsEnabled) "enabled" else "disabled"}. YouniScript has no separate motion switch.",
                color = YouniColors.secondaryInk,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AboutSectionTitle("READING")
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .background(YouniColors.elevatedPaper, RoundedCornerShape(12.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onOpenReading)
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Reading Settings", color = YouniColors.ink, style = MaterialTheme.typography.titleSmall)
                    Text("Choose reading mode and Read Aloud speed. Page styles and reader presentation control text appearance.", color = YouniColors.mutedInk, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                }
                Text("›", color = YouniColors.sage, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(title, color = YouniColors.sage, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp, modifier = Modifier.semantics { heading() })
}
