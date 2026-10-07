package com.youniscript.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import com.youniscript.app.data.AnnotationRecord
import com.youniscript.app.data.Page
import com.youniscript.app.data.PageLink
import com.youniscript.app.data.PageRevision
import com.youniscript.app.data.PersonalEntry
import com.youniscript.app.data.MediaAttachment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import java.io.ByteArrayOutputStream
import com.youniscript.app.ui.AudioAttachmentControls
import com.youniscript.app.ui.RenameAttachmentDialog

private val Kinds = listOf("Important", "Question", "Research", "Rewrite", "Final", "Keep")

@Composable
fun PersonalEntryDetailsDialog(entry: PersonalEntry, pageTitle: String, onDismiss: () -> Unit, onSave: (PersonalEntry) -> Unit) {
    var author by remember(entry.id) { mutableStateOf(entry.author) }
    var source by remember(entry.id) { mutableStateOf(entry.source) }
    var recipient by remember(entry.id) { mutableStateOf(entry.recipient) }
    var letterType by remember(entry.id) { mutableStateOf(entry.letterType) }
    var quoteDate by remember(entry.id) { mutableStateOf(entry.quoteDate?.let(::formatIsoDate).orEmpty()) }
    var writtenByMe by remember(entry.id) { mutableStateOf(entry.writtenByMe) }
    var people by remember(entry.id) { mutableStateOf(entry.people) }
    var places by remember(entry.id) { mutableStateOf(entry.places) }
    var feelings by remember(entry.id) { mutableStateOf(entry.feelings) }
    var details by remember(entry.id) { mutableStateOf(entry.details) }
    var symbols by remember(entry.id) { mutableStateOf(entry.symbols) }
    var reflection by remember(entry.id) { mutableStateOf(entry.reflection) }
    var signature by remember(entry.id) { mutableStateOf(entry.signature) }
    var dateError by remember(entry.id) { mutableStateOf(false) }
    val scroll = androidx.compose.foundation.rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${entry.kind.replaceFirstChar(Char::uppercase)} details") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pageTitle.ifBlank { "Untitled page" }, color = Color.Gray)
                when (entry.kind) {
                    "quote" -> {
                        OutlinedTextField(author, { author = it }, label = { Text("Author") }, singleLine = true)
                        OutlinedTextField(source, { source = it }, label = { Text("Source") }, singleLine = true)
                        OutlinedTextField(quoteDate, { quoteDate = it; dateError = false }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, isError = dateError)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = writtenByMe, onCheckedChange = { writtenByMe = it })
                            Text("Written by me")
                        }
                    }
                    "letter" -> {
                        OutlinedTextField(recipient, { recipient = it }, label = { Text("Recipient") }, singleLine = true)
                        OutlinedTextField(letterType, { letterType = it }, label = { Text("Letter type") }, singleLine = true)
                        OutlinedTextField(signature, { signature = it }, label = { Text("Signature") }, singleLine = true)
                    }
                    "dream" -> {
                        OutlinedTextField(people, { people = it }, label = { Text("People") })
                        OutlinedTextField(places, { places = it }, label = { Text("Places") })
                        OutlinedTextField(feelings, { feelings = it }, label = { Text("Feelings") })
                        OutlinedTextField(details, { details = it }, label = { Text("What happened / details") }, minLines = 2)
                        OutlinedTextField(symbols, { symbols = it }, label = { Text("Symbols") })
                        OutlinedTextField(reflection, { reflection = it }, label = { Text("Reflection") }, minLines = 2)
                    }
                    else -> Text("Thoughts use their page title, body, timestamp, and tags.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedDate = if (entry.kind == "quote" && quoteDate.isNotBlank()) runCatching {
                    java.time.LocalDate.parse(quoteDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrElse { dateError = true; return@TextButton } else entry.quoteDate
                onSave(entry.copy(author = author.trim(), source = source.trim(), recipient = recipient.trim(), letterType = letterType.trim().ifBlank { "general" }, quoteDate = parsedDate, writtenByMe = writtenByMe, people = people.trim(), places = places.trim(), feelings = feelings.trim(), details = details.trim(), symbols = symbols.trim(), reflection = reflection.trim(), signature = signature.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AnnotationManagerDialog(pageId: String, annotations: List<AnnotationRecord>, onDismiss: () -> Unit, onSave: (AnnotationRecord) -> Unit, onDelete: (AnnotationRecord) -> Unit) {
    var selected by remember { mutableStateOf<AnnotationRecord?>(null) }
    var kind by remember { mutableStateOf("Important") }
    var body by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var kindMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) if (selected == null) "Add marginal note" else "Edit marginal note" else "Marginalia") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editing) {
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { kindMenu = true }) { Text("$kind  ▾") }
                        DropdownMenu(kindMenu, { kindMenu = false }) { Kinds.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { kind = value; kindMenu = false }) } }
                    }
                    OutlinedTextField(body, { body = it }, label = { Text("Note") }, minLines = 3)
                } else {
                    if (annotations.isEmpty()) Text("Keep notes beside your writing without changing the page text.", color = Color.Gray)
                    LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(annotations, key = { it.id }) { note ->
                            Row(Modifier.fillMaxWidth().background(Color(0x147A805F), RoundedCornerShape(10.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(note.kind, fontSize = 12.sp); Text(note.body, maxLines = 3) }
                                TextButton(onClick = { selected = note; kind = note.kind; body = note.body; editing = true }) { Text("Edit") }
                                TextButton(onClick = { onDelete(note) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { if (editing) TextButton(onClick = {
            val now = System.currentTimeMillis()
            onSave(selected?.copy(kind = kind, body = body.trim(), updatedAt = now) ?: AnnotationRecord(UUID.randomUUID().toString(), "page", pageId, kind, body.trim(), now, now))
            editing = false; selected = null; body = ""
        }, enabled = body.isNotBlank()) { Text("Save") } else TextButton(onClick = { kind = "Important"; body = ""; selected = null; editing = true }) { Text("Add note") } },
        dismissButton = { TextButton(onClick = if (editing) { { editing = false; selected = null } } else onDismiss) { Text(if (editing) "Cancel" else "Done") } },
    )
}

@Composable
fun PageLinksDialog(page: Page, pages: List<Page>, links: List<PageLink>, backlinks: List<PageLink>, onDismiss: () -> Unit, onAdd: (Page, String) -> Unit, onOpen: (Page) -> Unit, onRemove: (PageLink) -> Unit) {
    var chosen by remember(page.id) { mutableStateOf<Page?>(null) }
    var label by remember(page.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page links") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (links.isNotEmpty()) Text("LINKS FROM THIS PAGE", fontSize = 11.sp)
                links.forEach { link ->
                    val target = pages.firstOrNull { it.id == link.targetPageId }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { target?.let(onOpen) }, modifier = Modifier.weight(1f)) { Text(link.label.ifBlank { target?.title ?: "Missing page" }, maxLines = 1) }
                        TextButton(onClick = { onRemove(link) }) { Text("Remove") }
                    }
                }
                if (backlinks.isNotEmpty()) {
                    Spacer(Modifier.padding(top = 2.dp))
                    Text("BACKLINKS", fontSize = 11.sp)
                    backlinks.forEach { link ->
                        val source = pages.firstOrNull { it.id == link.sourcePageId }
                        TextButton(onClick = { source?.let(onOpen) }) { Text(source?.title?.ifBlank { "Untitled page" } ?: "Page was deleted") }
                    }
                }
                if (chosen == null) {
                    Text("LINK TO A PAGE", fontSize = 11.sp)
                    LazyColumn(Modifier.heightIn(max = 190.dp)) {
                        items(pages.filter { it.id != page.id && !it.isTrashed && links.none { link -> link.targetPageId == it.id } }, key = { it.id }) { target ->
                            TextButton(onClick = { chosen = target; label = target.title }) { Text(target.title.ifBlank { "Untitled page" }, Modifier.fillMaxWidth()) }
                        }
                    }
                } else {
                    OutlinedTextField(label, { label = it }, label = { Text("Link label") }, singleLine = true)
                }
            }
        },
        confirmButton = { if (chosen != null) TextButton(onClick = { chosen?.let { onAdd(it, label) }; chosen = null }) { Text("Create link") } else TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { if (chosen != null) TextButton(onClick = { chosen = null }) { Text("Back") } },
    )
}

@Composable
fun RevisionHistoryDialog(revisions: List<PageRevision>, onDismiss: () -> Unit, onRestore: (PageRevision) -> Unit) {
    var selected by remember { mutableStateOf<PageRevision?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selected == null) "Version history" else "Saved version") },
        text = {
            if (selected == null) {
                if (revisions.isEmpty()) Text("Earlier saved versions appear here as you revise this page.", color = Color.Gray)
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(revisions, key = { it.id }) { revision ->
                        Column(Modifier.fillMaxWidth().background(Color(0x147A805F), RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Text(formatRevisionDate(revision.createdAt), fontSize = 12.sp)
                            Text(revision.title.ifBlank { "Untitled page" })
                            Text(revision.body.ifBlank { "Empty version" }, maxLines = 3, color = Color.Gray)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { selected = revision }) { Text("View") }
                                TextButton(onClick = { onRestore(revision) }) { Text("Restore") }
                            }
                        }
                    }
                }
            } else Column(Modifier.heightIn(max = 460.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(selected!!.title.ifBlank { "Untitled page" })
                Text(selected!!.body.ifBlank { "Empty version" })
            }
        },
        confirmButton = { TextButton(onClick = if (selected == null) onDismiss else { { selected = null } }) { Text(if (selected == null) "Done" else "Back") } },
    )
}

@Composable
fun MediaAttachmentGallery(attachments: List<MediaAttachment>, onDelete: (MediaAttachment) -> Unit, onRenameAudio: (MediaAttachment, String) -> Unit) {
    if (attachments.isEmpty()) return
    var renameTarget by remember { mutableStateOf<MediaAttachment?>(null) }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ATTACHMENTS", color = Color(0xFF78866E), fontSize = 10.sp, letterSpacing = 1.4.sp)
        attachments.forEach { attachment ->
            Row(Modifier.fillMaxWidth().background(Color(0x147A805F), RoundedCornerShape(10.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (attachment.mediaType == "image" || attachment.mediaType == "drawing") {
                    val context = LocalContext.current
                    val bitmap by produceState<android.graphics.Bitmap?>(null, attachment.localUri) {
                        value = runCatching {
                            val uri = android.net.Uri.parse(attachment.localUri)
                            if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path) else context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        }.getOrNull()
                    }
                    bitmap?.let { Image(it.asImageBitmap(), attachment.displayName, Modifier.width(74.dp).height(74.dp), contentScale = ContentScale.Crop) }
                }
                Text(attachment.displayName, Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 2, color = Color(0xFF3A3B34))
                if (attachment.mediaType == "audio") {
                    AudioAttachmentControls(attachment.localUri)
                    TextButton(onClick = { renameTarget = attachment }) { Text("Rename") }
                }
                TextButton(onClick = { onDelete(attachment) }) { Text("Remove") }
            }
        }
    }
    renameTarget?.let { attachment ->
        RenameAttachmentDialog(attachment.displayName, onDismiss = { renameTarget = null }) { newName ->
            val extension = attachment.displayName.substringAfterLast('.', "m4a")
            onRenameAudio(attachment, "$newName.$extension")
            renameTarget = null
        }
    }
}

private data class DrawingStroke(val points: List<Offset>, val color: Color, val widthFraction: Float)

@Composable
fun DrawingEditorDialog(onDismiss: () -> Unit, onSave: (ByteArray) -> Unit) {
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    val redo = remember { mutableStateListOf<DrawingStroke>() }
    var currentPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var color by remember { mutableStateOf(Color(0xFF263A2C)) }
    var erasing by remember { mutableStateOf(false) }
    var widthFraction by remember { mutableStateOf(0.006f) }
    val palette = listOf(Color(0xFF1F3326), Color(0xFF292820), Color(0xFF69513C), Color(0xFF48556A))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drawing") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(min = 380.dp, max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        palette.forEachIndexed { index, swatch ->
                            Box(
                                Modifier.padding(2.dp).background(swatch, CircleShape)
                                    .border(if (!erasing && color == swatch) 3.dp else 1.dp, if (!erasing && color == swatch) Color(0xFFA9B997) else Color(0xFFD5D3CC), CircleShape)
                                    .size(30.dp)
                                    .clickable { color = swatch; erasing = false }
                                    .semantics { contentDescription = "Ink color ${index + 1}" },
                            )
                        }
                    }
                    TextButton(onClick = { erasing = !erasing }) { Text(if (erasing) "Pen" else "Erase", color = if (erasing) Color(0xFFA9B997) else Color.White) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (strokes.isNotEmpty()) redo.add(strokes.removeAt(strokes.lastIndex)) }) { Text("Undo") }
                    TextButton(onClick = { if (redo.isNotEmpty()) strokes.add(redo.removeAt(redo.lastIndex)) }) { Text("Redo") }
                    TextButton(onClick = { strokes.clear(); redo.clear(); currentPoints = emptyList() }) { Text("Clear") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.003f to "Fine", 0.006f to "Medium", 0.012f to "Broad").forEach { (value, name) ->
                        TextButton(onClick = { widthFraction = value }) { Text(name, color = if (widthFraction == value) Color(0xFF526147) else Color.Gray) }
                    }
                }
                Canvas(
                    Modifier.fillMaxWidth().weight(1f).background(Color(0xFFF5F2EA), RoundedCornerShape(8.dp))
                        .pointerInput(color, erasing, widthFraction) {
                            detectDragGestures(
                                onDragStart = { start -> currentPoints = listOf(Offset(start.x / size.width, start.y / size.height)) },
                                onDrag = { change, _ ->
                                    currentPoints = currentPoints + Offset(change.position.x / size.width, change.position.y / size.height)
                                    change.consume()
                                },
                                onDragEnd = { if (currentPoints.isNotEmpty()) { strokes.add(DrawingStroke(currentPoints, if (erasing) Color(0xFFF5F2EA) else color, widthFraction)); currentPoints = emptyList(); redo.clear() } },
                                onDragCancel = { currentPoints = emptyList() },
                            )
                        },
                ) {
                    fun drawStroke(stroke: DrawingStroke) {
                        if (stroke.points.isEmpty()) return
                        val path = Path().apply {
                            moveTo(stroke.points.first().x * size.width, stroke.points.first().y * size.height)
                            stroke.points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                        }
                        drawPath(path, stroke.color, style = Stroke(stroke.widthFraction * size.width, cap = StrokeCap.Round))
                        if (stroke.points.size == 1) drawCircle(stroke.color, stroke.widthFraction * size.width / 2, Offset(stroke.points[0].x * size.width, stroke.points[0].y * size.height))
                    }
                    strokes.forEach(::drawStroke)
                    if (currentPoints.isNotEmpty()) drawStroke(DrawingStroke(currentPoints, if (erasing) Color(0xFFF5F2EA) else color, widthFraction))
                }
                Text("Draw with your finger. Strokes stay attached to this page on this device.", color = Color.Gray, fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = { onSave(renderDrawingPng(strokes.toList())); onDismiss() }, enabled = strokes.isNotEmpty()) { Text("Save drawing") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun renderDrawingPng(strokes: List<DrawingStroke>): ByteArray {
    val bitmap = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(245, 242, 234))
    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val path = AndroidPath().apply {
            moveTo(stroke.points.first().x * bitmap.width, stroke.points.first().y * bitmap.height)
            stroke.points.drop(1).forEach { lineTo(it.x * bitmap.width, it.y * bitmap.height) }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = stroke.widthFraction * bitmap.width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
        if (stroke.points.size == 1) canvas.drawCircle(stroke.points[0].x * bitmap.width, stroke.points[0].y * bitmap.height, paint.strokeWidth / 2f, paint)
    }
    return ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle(); output.toByteArray() }
}

private fun formatIsoDate(timestamp: Long): String = runCatching {
    DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
}.getOrDefault("")

private fun formatRevisionDate(timestamp: Long): String = runCatching {
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
}.getOrDefault("")
