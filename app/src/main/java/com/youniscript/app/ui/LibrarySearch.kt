package com.youniscript.app.ui

import com.youniscript.app.data.Book
import com.youniscript.app.data.Chapter
import com.youniscript.app.data.Page
import java.util.Locale

/** Local, in-memory search over the currently stored Library records. */
fun filterLibraryPages(pages: List<Page>, query: String): List<Page> {
    val needle = query.normalizedSearchTerm()
    if (needle.isEmpty()) return pages
    return pages.filter { page ->
        listOf(page.title, page.body, page.tags).any { it.contains(needle, ignoreCase = true) }
    }
}

fun filterLibraryBooks(books: List<Book>, chapters: List<Chapter>, pages: List<Page>, query: String): List<Book> {
    val needle = query.normalizedSearchTerm()
    if (needle.isEmpty()) return books
    val chapterTitles = chapters.groupBy { it.bookId }
    val bookPages = pages.groupBy { it.bookId }
    return books.filter { book ->
        listOf(book.title, book.subtitle, book.author, book.description).any { it.contains(needle, ignoreCase = true) } ||
            chapterTitles[book.id].orEmpty().any { it.title.contains(needle, ignoreCase = true) } ||
            bookPages[book.id].orEmpty().any { page ->
                page.title.contains(needle, ignoreCase = true) || page.body.contains(needle, ignoreCase = true) || page.tags.contains(needle, ignoreCase = true)
            }
    }
}

private fun String.normalizedSearchTerm(): String = trim().lowercase(Locale.ROOT)
