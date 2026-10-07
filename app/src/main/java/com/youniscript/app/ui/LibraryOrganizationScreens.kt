package com.youniscript.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youniscript.app.data.CollectionItem
import com.youniscript.app.data.CollectionRecord
import com.youniscript.app.data.GlossaryTerm
import com.youniscript.app.data.Page
import com.youniscript.app.ui.theme.YouniColors
import androidx.compose.material3.MaterialTheme
import java.util.UUID

data class CollectionContent(val type: String, val id: String, val title: String, val detail: String)

@Composable
fun CollectionScreen(
    collection: CollectionRecord,
    memberships: List<CollectionItem>,
    content: List<CollectionContent>,
    onBack: () -> Unit,
    onAdd: (CollectionContent, Int) -> Unit,
    onRemove: (CollectionItem) -> Unit,
    onMove: (List<CollectionItem>) -> Unit,
    onOpen: (CollectionContent) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var search by rememberSaveable(collection.id) { mutableStateOf("") }
    var adding by rememberSaveable(collection.id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(collection.id) { mutableStateOf(false) }
    val byKey = content.associateBy { "${it.type}:${it.id}" }
    val resolved = memberships.mapNotNull { membership -> byKey["${membership.contentType}:${membership.contentId}"]?.let { membership to it } }
    Column(Modifier.fillMaxSize().background(YouniColors.library).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Library") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
        }
        Text(collection.icon.uppercase(), color = YouniColors.sage, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Text(collection.title, color = YouniColors.ink, style = MaterialTheme.typography.headlineMedium)
        if (collection.description.isNotBlank()) Text(collection.description, color = YouniColors.mutedInk, modifier = Modifier.padding(top = 4.dp))
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Search this collection") }, singleLine = true)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${memberships.size} saved items", Modifier.weight(1f), color = Color(0xFF686B60), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { adding = true }) { Text("Add content") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val matching = resolved.filter { (_, value) -> search.isBlank() || value.title.contains(search, true) || value.detail.contains(search, true) }
            items(matching, key = { it.first.collectionId + it.first.contentType + it.first.contentId }) { (membership, value) ->
                Row(Modifier.fillMaxWidth().background(YouniColors.paper, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onOpen(value) }) {
                        Text(value.type.uppercase(), color = YouniColors.sage, fontSize = 10.sp, letterSpacing = 1.1.sp)
                        Text(value.title.ifBlank { "Untitled" }, color = YouniColors.ink, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        if (value.detail.isNotBlank()) Text(value.detail, color = YouniColors.mutedInk, maxLines = 1)
                    }
                    val index = memberships.indexOf(membership)
                    TextButton(onClick = { if (index > 0) onMove(memberships.toMutableList().apply { add(index - 1, removeAt(index)) }) }, enabled = index > 0) { Text("↑") }
                    TextButton(onClick = { if (index < memberships.lastIndex) onMove(memberships.toMutableList().apply { add(index + 1, removeAt(index)) }) }, enabled = index < memberships.lastIndex) { Text("↓") }
                    TextButton(onClick = { onRemove(membership) }) { Text("Remove") }
                }
            }
            if (matching.isEmpty()) item { Text(if (search.isBlank()) "Add pages, books, or journals to begin." else "Nothing in this collection matches.", Modifier.padding(16.dp), color = Color(0xFF686B60)) }
        }
    }
    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text("Add to ${collection.title}") },
        text = {
            val existing = memberships.mapTo(mutableSetOf()) { "${it.contentType}:${it.contentId}" }
            val candidates = content.filterNot { "${it.type}:${it.id}" in existing }
            if (candidates.isEmpty()) Text("Everything in your Library is already here.")
            else LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(candidates, key = { "${it.type}:${it.id}" }) { item ->
                    TextButton(onClick = { onAdd(item, memberships.size); adding = false }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(item.title.ifBlank { "Untitled" }); Text(item.type.replaceFirstChar(Char::uppercase), color = Color.Gray, fontSize = 12.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { adding = false }) { Text("Done") } },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete collection?") },
        text = { Text("The collection and its membership links will be removed. The pages, books, and journals will stay in your Library.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete collection") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

@Composable
fun CollectionEditorDialog(existing: CollectionRecord?, onDismiss: () -> Unit, onSave: (CollectionRecord) -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var icon by remember(existing?.id) { mutableStateOf(existing?.icon ?: "book") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New collection" else "Edit collection") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            OutlinedTextField(icon, { icon = it }, label = { Text("Icon label") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = {
            val now = System.currentTimeMillis()
            onSave(existing?.copy(title = title.trim(), description = description.trim(), icon = icon.trim().ifBlank { "book" }, updatedAt = now)
                ?: CollectionRecord(UUID.randomUUID().toString(), title.trim(), description.trim(), icon.trim().ifBlank { "book" }, now, now))
        }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun GlossaryScreen(terms: List<GlossaryTerm>, pages: List<Page>, onBack: () -> Unit, onSave: (GlossaryTerm) -> Unit, onDelete: (GlossaryTerm) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<GlossaryTerm?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(YouniColors.library).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("‹ Library") }; Spacer(Modifier.weight(1f)); TextButton(onClick = { creating = true }) { Text("Add term") } }
        Text("Personal Glossary", color = YouniColors.ink, style = MaterialTheme.typography.headlineMedium)
        Text("Definitions in your own words, stored on this device.", color = YouniColors.mutedInk, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search terms") }, singleLine = true)
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val filtered = terms.filter { search.isBlank() || it.term.contains(search, true) || it.definition.contains(search, true) }
            items(filtered, key = { it.id }) { term ->
                Row(Modifier.fillMaxWidth().background(YouniColors.paper, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(term.term, color = YouniColors.ink, style = MaterialTheme.typography.titleMedium); Text(term.definition, color = YouniColors.mutedInk) }
                    TextButton(onClick = { editing = term }) { Text("Edit") }
                    TextButton(onClick = { onDelete(term) }) { Text("Delete") }
                }
            }
            if (filtered.isEmpty()) item { Text(if (search.isBlank()) "Add the words that belong to your world." else "No terms matched.", Modifier.padding(14.dp), color = Color(0xFF686B60)) }
        }
    }
    if (creating || editing != null) GlossaryTermEditorDialog(editing, pages, { creating = false; editing = null }, { term -> onSave(term); creating = false; editing = null })
}

@Composable
private fun GlossaryTermEditorDialog(existing: GlossaryTerm?, pages: List<Page>, onDismiss: () -> Unit, onSave: (GlossaryTerm) -> Unit) {
    var term by remember(existing?.id) { mutableStateOf(existing?.term.orEmpty()) }
    var definition by remember(existing?.id) { mutableStateOf(existing?.definition.orEmpty()) }
    var sourcePage by remember(existing?.id) { mutableStateOf(existing?.sourcePageId) }
    var sourceMenu by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (existing == null) "Add glossary term" else "Edit glossary term") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(term, { term = it }, label = { Text("Term") }, singleLine = true)
                OutlinedTextField(definition, { definition = it }, label = { Text("Definition") }, minLines = 3)
                TextButton(onClick = { sourceMenu = true }) { Text(pages.firstOrNull { it.id == sourcePage }?.title?.ifBlank { "Linked source page" } ?: "Link a page (optional)") }
            } },
            confirmButton = { TextButton(onClick = {
                val now = System.currentTimeMillis()
                onSave(existing?.copy(term = term.trim(), definition = definition.trim(), sourcePageId = sourcePage, updatedAt = now)
                    ?: GlossaryTerm(UUID.randomUUID().toString(), term.trim(), definition.trim(), sourcePage, now, now))
            }, enabled = term.isNotBlank() && definition.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
        androidx.compose.material3.DropdownMenu(sourceMenu, { sourceMenu = false }) {
            TextButton(onClick = { sourcePage = null; sourceMenu = false }) { Text("No linked page") }
            pages.filterNot { it.isTrashed }.forEach { page -> DropdownMenuItem(text = { Text(page.title.ifBlank { "Untitled page" }) }, onClick = { sourcePage = page.id; sourceMenu = false }) }
        }
    }
}
