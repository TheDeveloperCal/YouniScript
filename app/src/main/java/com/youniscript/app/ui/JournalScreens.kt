package com.youniscript.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youniscript.app.data.Journal
import com.youniscript.app.data.JournalEntryWithPage
import com.youniscript.app.ui.theme.YouniColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalsSection(journals: List<Journal>, onOpen: (Journal) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("YOUR JOURNALS", color = YouniColors.mutedInk, fontSize = 10.sp, letterSpacing = 1.5.sp)
        journals.forEach { journal ->
            Column(
                Modifier.fillMaxWidth().background(YouniColors.paper, RoundedCornerShape(14.dp))
                    .border(1.dp, YouniColors.border, RoundedCornerShape(14.dp))
                    .clickable { onOpen(journal) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(journal.title, color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 20.sp)
                if (journal.isArchived) Text("ARCHIVED", color = YouniColors.naturalBrown, fontSize = 10.sp, letterSpacing = 1.1.sp)
                if (journal.description.isNotBlank()) Text(journal.description, color = YouniColors.mutedInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun JournalScreen(
    journal: Journal,
    entries: List<JournalEntryWithPage>,
    onBack: () -> Unit,
    onNewEntry: () -> Unit,
    onOpenEntry: (com.youniscript.app.data.Page) -> Unit,
    onEdit: (Journal) -> Unit,
    onArchive: (Journal) -> Unit,
    onDelete: (Journal) -> Unit,
) {
    var confirmDelete by rememberSaveable(journal.id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(journal.id) { mutableStateOf("") }
    val visibleEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries else entries.filter {
            it.page.title.contains(searchQuery, ignoreCase = true) || it.page.body.contains(searchQuery, ignoreCase = true)
        }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy · h:mm a").withZone(ZoneId.systemDefault()) }
    Column(Modifier.fillMaxSize().background(YouniColors.library).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Library", color = YouniColors.sage) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEdit(journal) }) { Text("Edit", color = YouniColors.sage) }
            TextButton(onClick = { onArchive(journal) }) { Text(if (journal.isArchived) "Restore" else "Archive", color = YouniColors.sage) }
            TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = YouniColors.error) }
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(journal.title, color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 30.sp)
            if (journal.description.isNotBlank()) Text(journal.description, color = YouniColors.mutedInk, style = MaterialTheme.typography.bodyLarge)
        }
        Button(
            onClick = onNewEntry,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YouniColors.sage, contentColor = Color.White),
        ) { Text("Write an entry") }
        if (entries.isNotEmpty()) OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            label = { Text("Search this journal") },
            singleLine = true,
        )
        if (visibleEntries.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (searchQuery.isBlank()) "A place for your days." else "No entries match this search.", color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                if (searchQuery.isBlank()) Text("Your first entry will appear here in date order.", color = YouniColors.mutedInk, modifier = Modifier.padding(top = 8.dp))
            }
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(visibleEntries, key = { it.page.id }) { entry ->
                Column(
                    Modifier.fillMaxWidth().background(YouniColors.paper, RoundedCornerShape(14.dp))
                        .border(1.dp, YouniColors.border, RoundedCornerShape(14.dp))
                        .clickable { onOpenEntry(entry.page) }.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(formatter.format(Instant.ofEpochMilli(entry.entryDate)), color = YouniColors.sage, style = MaterialTheme.typography.labelMedium)
                    Text(entry.page.title.ifBlank { "Untitled entry" }, color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 19.sp)
                    Text(entry.page.body.ifBlank { "An entry, ready to continue." }, color = YouniColors.mutedInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this journal?") },
        text = { Text("The journal and its entry links will be removed. The writing itself will remain as pages in your Library.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(journal) }) { Text("Delete journal", color = YouniColors.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep journal") } },
        containerColor = YouniColors.paper,
    )
}

@Composable
fun JournalEditorDialog(journal: Journal?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by rememberSaveable(journal?.id) { mutableStateOf(journal?.title.orEmpty()) }
    var description by rememberSaveable(journal?.id) { mutableStateOf(journal?.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (journal == null) "New journal" else "Edit journal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title.trim(), description.trim()) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = YouniColors.paper,
    )
}
