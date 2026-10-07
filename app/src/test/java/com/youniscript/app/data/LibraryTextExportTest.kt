package com.youniscript.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTextExportTest {
    @Test
    fun exportsManuscriptStructureAndStablePageMetadata() {
        val book = Book(id = "book-1", title = "My Manuscript", author = "A. Writer", createdAt = 1, updatedAt = 2)
        val chapter = Chapter(id = "chapter-1", bookId = book.id, title = "First Chapter", createdAt = 1, updatedAt = 2, chapterOrder = 0)
        val page = Page(
            id = "page-1", title = "The Opening", body = "Words find their place.",
            createdAt = 1, updatedAt = 2, bookId = book.id, chapterId = chapter.id, bookOrder = 0,
        )
        val snapshot = LibraryBackupSnapshot(
            pages = listOf(page), books = listOf(book), chapters = listOf(chapter),
            sections = emptyList(), components = emptyList(),
        )

        val plain = LibraryTextExport.plainText(snapshot)
        val markdown = LibraryTextExport.markdown(snapshot)

        assertTrue(plain.contains("My Manuscript"))
        assertTrue(plain.contains("First Chapter"))
        assertTrue(plain.contains("Words find their place."))
        assertTrue(plain.contains("Page ID: page-1"))
        assertTrue(markdown.contains("# My Manuscript"))
        assertTrue(markdown.contains("### First Chapter"))
        assertTrue(markdown.contains("Words find their place."))
        assertTrue(markdown.contains("**Page ID:** page-1"))
    }
}
