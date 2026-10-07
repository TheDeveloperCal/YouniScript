package com.youniscript.app.ui

import com.youniscript.app.data.Book
import com.youniscript.app.data.Chapter
import com.youniscript.app.data.Page
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySearchTest {
    @Test
    fun pageSearchIsLocalCaseInsensitiveAndIncludesBodyAndTags() {
        val pages = listOf(
            page("1", title = "A Quiet Morning", body = "First light over the garden"),
            page("2", title = "Untitled", body = "A LETTER for my family", tags = "[family]"),
            page("3", title = "A walk", body = "The road was empty"),
        )

        assertEquals(listOf("2"), filterLibraryPages(pages, " letter ").map { it.id })
        assertEquals(listOf("2"), filterLibraryPages(pages, "FAMILY").map { it.id })
        assertEquals(listOf("1", "2", "3"), filterLibraryPages(pages, "").map { it.id })
    }

    @Test
    fun bookSearchIncludesBookDetailsAndChapterTitles() {
        val books = listOf(book("a", "My Philosophy"), book("b", "Letters"))
        val chapters = listOf(chapter("c1", "a", "Freedom"), chapter("c2", "b", "Home"))

        val manuscriptPage = page("p", "Notes", "a sentence about home", "[]").copy(bookId = "b")
        assertEquals(listOf("a"), filterLibraryBooks(books, chapters, listOf(manuscriptPage), "freedom").map { it.id })
        assertEquals(listOf("b"), filterLibraryBooks(books, chapters, listOf(manuscriptPage), "HOME").map { it.id })
    }

    private fun page(id: String, title: String, body: String, tags: String = "[]") =
        Page(id, title, body, 1L, 1L, tags = tags)

    private fun book(id: String, title: String) = Book(id, title, createdAt = 1L, updatedAt = 1L)
    private fun chapter(id: String, bookId: String, title: String) = Chapter(id, bookId, title, 1L, 1L, 0)
}
