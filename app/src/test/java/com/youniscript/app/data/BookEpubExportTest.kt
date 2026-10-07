package com.youniscript.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class BookEpubExportTest {
    @Test
    fun writesValidEpubOrderAndSemanticNavigation() {
        val book = Book("book-id", "A Personal Book", "A subtitle", "A. Writer", createdAt = 1, updatedAt = 2)
        val chapter = Chapter("chapter-id", book.id, "First Chapter", 1, 2, 0)
        val page = Page("page-id", "An Opening", "A thought & a memory.", 1, 2, bookId = book.id, chapterId = chapter.id, bookOrder = 0)
        val output = ByteArrayOutputStream()
        BookExportSnapshot(book, listOf(chapter), listOf(page), emptyList(), emptyList()).writeEpub(output)

        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                files[entry.name] = zip.readBytes()
            }
        }

        assertEquals("application/epub+zip", files.getValue("mimetype").toString(Charsets.US_ASCII))
        assertTrue(files.containsKey("META-INF/container.xml"))
        assertTrue(files.getValue("OEBPS/package.opf").toString(Charsets.UTF_8).contains("<spine>"))
        val nav = files.getValue("OEBPS/nav.xhtml").toString(Charsets.UTF_8)
        assertTrue(nav.contains("chapter-1.xhtml"))
        val chapterXhtml = files.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
        assertTrue(chapterXhtml.contains("A thought &amp; a memory."))
    }
}
