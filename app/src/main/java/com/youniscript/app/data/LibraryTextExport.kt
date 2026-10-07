package com.youniscript.app.data

import com.youniscript.app.editor.BLOCK_HEADING
import com.youniscript.app.editor.BLOCK_QUOTE
import com.youniscript.app.editor.BLOCK_SUBHEADING
import com.youniscript.app.editor.FormattingDocument
import com.youniscript.app.editor.MARK_BOLD
import com.youniscript.app.editor.MARK_ITALIC
import com.youniscript.app.editor.MARK_STRIKE
import com.youniscript.app.editor.MARK_UNDERLINE
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Human-readable, offline export of writing and its text metadata. Media bytes stay in the encrypted archive. */
object LibraryTextExport {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    fun plainText(snapshot: LibraryBackupSnapshot): String = render(snapshot, markdown = false)

    fun markdown(snapshot: LibraryBackupSnapshot): String = render(snapshot, markdown = true)

    private fun render(snapshot: LibraryBackupSnapshot, markdown: Boolean): String = buildString {
        snapshot.validate()
        heading(1, "YouniScript Library", markdown)
        line("Exported ${timestamp(System.currentTimeMillis())}", markdown)

        val chapters = snapshot.chapters.groupBy { it.bookId }
        val sections = snapshot.sections.groupBy { it.chapterId }
        val components = snapshot.components.groupBy { it.bookId }
        val pagesById = snapshot.pages.associateBy { it.id }
        val entriesByJournal = snapshot.journalEntries.groupBy { it.journalId }

        snapshot.books.sortedBy { it.bookOrder }.forEach { book ->
            blank()
            heading(2, book.title.ifBlank { "Untitled book" }, markdown)
            metadata("Book ID", book.id, markdown)
            book.subtitle.takeIf(String::isNotBlank)?.let { metadata("Subtitle", it, markdown) }
            book.author.takeIf(String::isNotBlank)?.let { metadata("Author", it, markdown) }
            book.description.takeIf(String::isNotBlank)?.let { line(it, markdown) }
            components[book.id].orEmpty().sortedBy { it.componentOrder }.forEach { component ->
                blank()
                heading(3, component.title.ifBlank { component.type.replace('-', ' ').replaceFirstChar(Char::uppercase) }, markdown)
                line(component.body, markdown)
            }
            chapters[book.id].orEmpty().sortedBy { it.chapterOrder }.forEach { chapter ->
                blank()
                heading(3, chapter.title, markdown)
                val chapterPages = snapshot.pages.filter { it.bookId == book.id && it.chapterId == chapter.id && !it.isTrashed }
                sections[chapter.id].orEmpty().sortedBy { it.sectionOrder }.forEach { section ->
                    heading(4, section.title, markdown)
                    chapterPages.filter { it.sectionId == section.id }.sortedBy { it.bookOrder }.forEach { appendPage(it, markdown, headingLevel = 5) }
                }
                chapterPages.filter { it.sectionId == null }.sortedBy { it.bookOrder }.forEach { appendPage(it, markdown) }
            }
        }

        snapshot.journals.sortedBy { it.createdAt }.forEach { journal ->
            blank()
            heading(2, "Journal · ${journal.title.ifBlank { "Untitled journal" }}", markdown)
            metadata("Journal ID", journal.id, markdown)
            journal.description.takeIf(String::isNotBlank)?.let { line(it, markdown) }
            entriesByJournal[journal.id].orEmpty().sortedBy { it.entryDate }.forEach { entry ->
                pagesById[entry.pageId]?.takeUnless { it.isTrashed }?.let { appendPage(it, markdown, entry.entryDate, headingLevel = 3) }
            }
        }

        val journalPageIds = snapshot.journalEntries.mapTo(mutableSetOf()) { it.pageId }
        snapshot.pages.filter { !it.isTrashed && it.bookId == null && it.id !in journalPageIds }
            .sortedBy { it.updatedAt }.forEach { appendPage(it, markdown, headingLevel = 2) }

        val trashedPages = snapshot.pages.filter { it.isTrashed }.sortedBy { it.trashedAt ?: it.updatedAt }
        if (trashedPages.isNotEmpty()) {
            blank()
            heading(2, "Trash", markdown)
            trashedPages.forEach { page ->
                appendPage(page, markdown, headingLevel = 3)
                page.trashedAt?.let { metadata("Moved to Trash", timestamp(it), markdown) }
            }
        }

        snapshot.collections.sortedBy { it.title.lowercase() }.forEach { collection ->
            blank()
            heading(2, "Collection · ${collection.title}", markdown)
            collection.description.takeIf(String::isNotBlank)?.let { line(it, markdown) }
            snapshot.collectionItems.filter { it.collectionId == collection.id }.sortedBy { it.itemOrder }.forEach { item ->
                val title = when (item.contentType) {
                    "page" -> pagesById[item.contentId]?.title
                    "book" -> snapshot.books.firstOrNull { it.id == item.contentId }?.title
                    else -> snapshot.journals.firstOrNull { it.id == item.contentId }?.title
                }
                line("${item.contentType}: ${title ?: item.contentId}", markdown)
            }
        }

        if (snapshot.glossary.isNotEmpty()) {
            blank()
            heading(2, "Glossary", markdown)
            snapshot.glossary.sortedBy { it.term.lowercase() }.forEach { term ->
                if (markdown) append("- **${escapeMarkdown(term.term)}** — ${escapeMarkdown(term.definition)}\n")
                else append("${term.term}: ${term.definition}\n")
            }
        }
        if (snapshot.annotations.isNotEmpty()) {
            blank()
            heading(2, "Marginalia", markdown)
            snapshot.annotations.sortedBy { it.createdAt }.forEach { note ->
                val target = when (note.contentType) {
                    "page" -> pagesById[note.contentId]?.title
                    "book" -> snapshot.books.firstOrNull { it.id == note.contentId }?.title
                    else -> snapshot.journals.firstOrNull { it.id == note.contentId }?.title
                } ?: note.contentId
                line("${note.kind} · $target · ${timestamp(note.createdAt)}", markdown)
                line(note.body, markdown)
                blank()
            }
        }
        if (snapshot.links.isNotEmpty()) {
            blank()
            heading(2, "Page links", markdown)
            snapshot.links.forEach { link ->
                val from = pagesById[link.sourcePageId]?.title ?: link.sourcePageId
                val to = pagesById[link.targetPageId]?.title ?: link.targetPageId
                line("$from → $to${link.label.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}", markdown)
            }
        }
        if (snapshot.attachments.isNotEmpty()) {
            blank()
            heading(2, "Attachment index", markdown)
            snapshot.attachments.sortedBy { it.createdAt }.forEach { attachment ->
                val owner = pagesById[attachment.pageId]?.title ?: attachment.pageId
                line("${attachment.mediaType} · ${attachment.displayName} · $owner", markdown)
            }
            line("Attachment bytes are not included in TXT/Markdown exports. Use an encrypted .ysbackup to carry supported media.", markdown)
        }
        if (snapshot.revisions.isNotEmpty()) {
            blank()
            heading(2, "Earlier revisions", markdown)
            snapshot.revisions.sortedWith(compareBy<PageRevision> { it.pageId }.thenBy { it.createdAt }).forEach { revision ->
                blank()
                heading(3, revision.title.ifBlank { "Untitled revision" }, markdown)
                metadata("Page ID", revision.pageId, markdown)
                metadata("Saved", timestamp(revision.createdAt), markdown)
                appendBody(revision.body, revision.formatting, markdown)
            }
        }
        if (snapshot.personalEntries.isNotEmpty()) {
            blank()
            heading(2, "Entry details", markdown)
            snapshot.personalEntries.forEach { entry ->
                val pageTitle = pagesById[entry.pageId]?.title ?: entry.pageId
                heading(3, "${entry.kind.replaceFirstChar(Char::uppercase)} · $pageTitle", markdown)
                listOf(
                    "Author" to entry.author, "Source" to entry.source, "Recipient" to entry.recipient,
                    "Letter type" to entry.letterType, "People" to entry.people, "Places" to entry.places,
                    "Feelings" to entry.feelings, "Details" to entry.details, "Symbols" to entry.symbols,
                    "Reflection" to entry.reflection, "Signature" to entry.signature,
                ).filter { it.second.isNotBlank() }.forEach { (label, value) -> metadata(label, value, markdown) }
            }
        }
    }.trimEnd() + "\n"

    private fun StringBuilder.appendPage(page: Page, markdown: Boolean, journalDate: Long? = null, headingLevel: Int = 4) {
        blank()
        heading(headingLevel, page.title.ifBlank { "Untitled page" }, markdown)
        metadata("Page ID", page.id, markdown)
        metadata("Created", timestamp(page.createdAt), markdown)
        metadata("Updated", timestamp(page.updatedAt), markdown)
        page.pageStyleId?.takeIf(String::isNotBlank)?.let { metadata("Page style", it, markdown) }
        journalDate?.let { metadata("Journal date", timestamp(it), markdown) }
        val tags = runCatching { org.json.JSONArray(page.tags).let { array -> (0 until array.length()).map { array.optString(it) } } }.getOrDefault(emptyList())
        tags.takeIf { it.isNotEmpty() }?.let { metadata("Tags", it.joinToString(", "), markdown) }
        appendBody(page.body, page.formatting, markdown)
    }

    private fun StringBuilder.appendBody(body: String, formatting: String, markdown: Boolean) {
        if (body.isBlank()) return
        blank()
        append(if (markdown) renderMarkdownBody(body, formatting) else body)
        append('\n')
    }

    private fun renderMarkdownBody(body: String, formatting: String): String {
        val document = FormattingDocument.decode(body, formatting)
        val marks = IntArray(body.length)
        document.inlineRuns.forEach { run ->
            for (index in run.start.coerceAtLeast(0) until run.end.coerceAtMost(body.length)) marks[index] = marks[index] or run.marks
        }
        val paragraphs = document.paragraphs.associateBy { it.start }
        val output = StringBuilder(body.length + 32)
        var activeMarks = 0
        var lineStart = true
        body.forEachIndexed { index, char ->
            if (lineStart) {
                when (paragraphs[index]?.kind) {
                    BLOCK_HEADING -> output.append("# ")
                    BLOCK_SUBHEADING -> output.append("## ")
                    BLOCK_QUOTE -> output.append("> ")
                }
                lineStart = false
            }
            val nextMarks = marks[index]
            if (nextMarks != activeMarks) {
                closeMarks(output, activeMarks)
                openMarks(output, nextMarks)
                activeMarks = nextMarks
            }
            if (char == '\n') {
                closeMarks(output, activeMarks)
                activeMarks = 0
                output.append(char)
                lineStart = true
            } else output.append(escapeMarkdownChar(char))
        }
        closeMarks(output, activeMarks)
        return output.toString()
    }

    private fun openMarks(output: StringBuilder, marks: Int) {
        if (marks and MARK_BOLD != 0) output.append("**")
        if (marks and MARK_ITALIC != 0) output.append('*')
        if (marks and MARK_STRIKE != 0) output.append("~~")
        if (marks and MARK_UNDERLINE != 0) output.append("<u>")
    }

    private fun closeMarks(output: StringBuilder, marks: Int) {
        if (marks and MARK_UNDERLINE != 0) output.append("</u>")
        if (marks and MARK_STRIKE != 0) output.append("~~")
        if (marks and MARK_ITALIC != 0) output.append('*')
        if (marks and MARK_BOLD != 0) output.append("**")
    }

    private fun escapeMarkdownChar(char: Char): String = when (char) {
        '\\', '*', '_', '`', '[', ']', '#', '~' -> "\\$char"
        '<' -> "&lt;"
        '>' -> "&gt;"
        else -> char.toString()
    }

    private fun StringBuilder.heading(level: Int, value: String, markdown: Boolean) {
        val title = value.ifBlank { "Untitled" }
        if (markdown) append("${"#".repeat(level.coerceIn(1, 6))} ${escapeMarkdown(title)}\n")
        else append("$title\n${"=".repeat(title.length.coerceIn(3, 72))}\n")
    }

    private fun StringBuilder.metadata(label: String, value: String, markdown: Boolean) {
        if (markdown) append("> **${escapeMarkdown(label)}:** ${escapeMarkdown(value)}\n")
        else append("$label: $value\n")
    }

    private fun StringBuilder.line(value: String, markdown: Boolean) {
        if (value.isBlank()) return
        append(if (markdown) escapeMarkdown(value) else value)
        append('\n')
    }

    private fun StringBuilder.blank() {
        if (isNotEmpty() && last() != '\n') append('\n')
        if (isNotEmpty() && getOrNull(length - 2) != '\n') append('\n')
    }

    private fun timestamp(value: Long): String = runCatching { timestampFormat.format(Instant.ofEpochMilli(value)) }.getOrDefault(value.toString())

    private fun escapeMarkdown(value: String): String = buildString(value.length) {
        value.forEach { char -> append(escapeMarkdownChar(char)) }
    }
}
