package com.youniscript.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY bookOrder, updatedAt DESC")
    fun observeBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findBook(id: String): Book?

    @Query("SELECT * FROM books ORDER BY bookOrder, updatedAt DESC")
    suspend fun snapshotBooks(): List<Book>

    @Query("SELECT * FROM chapters ORDER BY bookId, chapterOrder")
    suspend fun snapshotChapters(): List<Chapter>

    @Query("SELECT * FROM sections ORDER BY bookId, chapterId, sectionOrder")
    suspend fun snapshotSections(): List<Section>

    @Query("SELECT * FROM book_components ORDER BY bookId, componentOrder")
    suspend fun snapshotComponents(): List<BookComponent>

    @Query("SELECT (SELECT COUNT(*) FROM books) + (SELECT COUNT(*) FROM chapters) + (SELECT COUNT(*) FROM sections) + (SELECT COUNT(*) FROM book_components)")
    suspend fun snapshotManuscriptCount(): Int

    @Upsert
    suspend fun saveBook(book: Book)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterOrder")
    fun observeChapters(bookId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters ORDER BY bookId, chapterOrder")
    fun observeAllChapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterOrder")
    suspend fun chapters(bookId: String): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun findChapter(chapterId: String): Chapter?

    @Upsert
    suspend fun saveChapter(chapter: Chapter)

    @Query("SELECT * FROM sections WHERE bookId = :bookId ORDER BY chapterId, sectionOrder")
    fun observeSections(bookId: String): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY sectionOrder")
    suspend fun sections(chapterId: String): List<Section>

    @Query("SELECT * FROM sections WHERE id = :sectionId LIMIT 1")
    suspend fun findSection(sectionId: String): Section?

    @Upsert
    suspend fun saveSection(section: Section)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSections(sections: List<Section>)

    @Query("UPDATE sections SET sectionOrder = sectionOrder + 100000 WHERE chapterId = :chapterId")
    suspend fun liftSectionOrders(chapterId: String)

    @Query("UPDATE sections SET sectionOrder = :order WHERE id = :sectionId")
    suspend fun setSectionOrder(sectionId: String, order: Int)

    @Query("UPDATE sections SET bookId = :bookId, chapterId = :chapterId, sectionOrder = :order WHERE id = :sectionId")
    suspend fun moveSectionRow(sectionId: String, bookId: String, chapterId: String, order: Int)

    @Query("UPDATE pages SET bookId = :bookId, chapterId = :chapterId WHERE sectionId = :sectionId")
    suspend fun moveSectionPages(sectionId: String, bookId: String, chapterId: String)

    @Query("SELECT * FROM pages WHERE sectionId = :sectionId AND isTrashed = 0 ORDER BY COALESCE(bookOrder, 2147483647), updatedAt DESC")
    suspend fun pagesInSection(sectionId: String): List<Page>

    @Query("UPDATE pages SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun ungroupSectionPages(sectionId: String)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun deleteSectionRow(sectionId: String)

    @Transaction
    suspend fun reorderSections(chapterId: String, sectionIds: List<String>) {
        val existing = sections(chapterId).map { it.id }.toSet()
        require(sectionIds.size == existing.size && sectionIds.toSet() == existing) {
            "Section order must include each section exactly once"
        }
        liftSectionOrders(chapterId)
        sectionIds.forEachIndexed { index, id -> setSectionOrder(id, index) }
    }

    @Transaction
    suspend fun moveSectionToChapter(sectionId: String, targetChapterId: String) {
        val section = findSection(sectionId) ?: error("Section does not exist")
        val target = findChapter(targetChapterId) ?: error("Chapter does not exist")
        require(section.bookId == target.bookId) { "A section can only move within its current book" }
        if (section.chapterId == targetChapterId) return
        val sourceChapterId = section.chapterId
        liftSectionOrders(sourceChapterId)
        sections(sourceChapterId).filterNot { it.id == sectionId }.forEachIndexed { index, remaining -> setSectionOrder(remaining.id, index) }
        moveSectionRow(sectionId, target.bookId, targetChapterId, sections(targetChapterId).size)
        val targetPages = chapterPages(targetChapterId).size
        moveSectionPages(sectionId, target.bookId, targetChapterId)
        pagesInSection(sectionId).forEachIndexed { index, page -> setPageOrder(page.id, targetPages + index) }
    }

    @Transaction
    suspend fun deleteSectionSafely(sectionId: String) {
        val section = findSection(sectionId) ?: return
        ungroupSectionPages(sectionId)
        deleteSectionRow(sectionId)
        liftSectionOrders(section.chapterId)
        sections(section.chapterId).forEachIndexed { index, remaining -> setSectionOrder(remaining.id, index) }
    }

    @Query("UPDATE chapters SET chapterOrder = chapterOrder + 100000 WHERE bookId = :bookId")
    suspend fun liftChapterOrders(bookId: String)

    @Query("UPDATE chapters SET chapterOrder = :order WHERE id = :chapterId")
    suspend fun setChapterOrder(chapterId: String, order: Int)

    @Query("SELECT * FROM book_components WHERE bookId = :bookId ORDER BY componentOrder")
    fun observeComponents(bookId: String): Flow<List<BookComponent>>

    @Query("SELECT * FROM book_components ORDER BY bookId, componentOrder")
    fun observeAllComponents(): Flow<List<BookComponent>>

    @Upsert
    suspend fun saveComponent(component: BookComponent)

    @Query("SELECT * FROM pages WHERE bookId = :bookId AND isTrashed = 0 ORDER BY COALESCE(bookOrder, 2147483647), updatedAt DESC")
    fun observeBookPages(bookId: String): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId AND isTrashed = 0 ORDER BY COALESCE(bookOrder, 2147483647), updatedAt DESC")
    fun observeChapterPages(chapterId: String): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId AND isTrashed = 0 ORDER BY COALESCE(bookOrder, 2147483647), updatedAt DESC")
    suspend fun chapterPages(chapterId: String): List<Page>

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun findPage(pageId: String): Page?

    @Query("UPDATE pages SET bookId = :bookId, chapterId = :chapterId, sectionId = NULL, bookOrder = :order WHERE id = :pageId")
    suspend fun placePage(pageId: String, bookId: String?, chapterId: String?, order: Int?)

    @Query("UPDATE pages SET sectionId = :sectionId WHERE id = :pageId")
    suspend fun setPageSection(pageId: String, sectionId: String?)

    @Query("UPDATE pages SET bookOrder = :order WHERE id = :pageId")
    suspend fun setPageOrder(pageId: String, order: Int)

    @Query("UPDATE pages SET bookOrder = bookOrder + 100000 WHERE chapterId = :chapterId")
    suspend fun liftPageOrders(chapterId: String)

    @Transaction
    suspend fun reorderPages(chapterId: String, pageIds: List<String>) {
        val existing = chapterPages(chapterId).map { it.id }.toSet()
        require(pageIds.size == existing.size && pageIds.toSet() == existing) {
            "Page order must include each page exactly once"
        }
        liftPageOrders(chapterId)
        pageIds.forEachIndexed { index, id -> setPageOrder(id, index) }
    }

    @Transaction
    suspend fun movePageToChapter(pageId: String, targetChapterId: String) {
        val page = findPage(pageId) ?: error("Page does not exist")
        val target = findChapter(targetChapterId) ?: error("Chapter does not exist")
        require(page.bookId == target.bookId) { "A page can only move within its current book" }
        val sourceId = page.chapterId
        if (sourceId == targetChapterId) return
        val oldOrder = sourceId?.let { chapterPages(it).filterNot { old -> old.id == pageId }.map { it.id } }.orEmpty()
        val newOrder = chapterPages(targetChapterId).map { it.id }.filterNot { it == pageId } + pageId
        placePage(pageId, target.bookId, targetChapterId, newOrder.lastIndex)
        reorderPages(targetChapterId, newOrder)
        if (sourceId != null && oldOrder.isNotEmpty()) reorderPages(sourceId, oldOrder)
    }

    @Transaction
    suspend fun placePageInSection(pageId: String, sectionId: String?) {
        val page = findPage(pageId) ?: error("Page does not exist")
        if (sectionId == null) {
            setPageSection(pageId, null)
            return
        }
        val section = findSection(sectionId) ?: error("Section does not exist")
        require(page.bookId == section.bookId && page.chapterId == section.chapterId) {
            "A page and its section must belong to the same chapter"
        }
        setPageSection(pageId, sectionId)
    }

    @Transaction
    suspend fun addExistingPageToChapter(pageId: String, targetChapterId: String, sectionId: String? = null) {
        val page = findPage(pageId) ?: error("Page does not exist")
        require(page.bookId == null && page.chapterId == null) { "Only a standalone page can be added here" }
        val chapter = findChapter(targetChapterId) ?: error("Chapter does not exist")
        val section = sectionId?.let { findSection(it) ?: error("Section does not exist") }
        require(section == null || (section.bookId == chapter.bookId && section.chapterId == chapter.id)) {
            "The selected section must belong to the target chapter"
        }
        val nextOrder = chapterPages(targetChapterId).size
        placePage(pageId, chapter.bookId, chapter.id, nextOrder)
        if (sectionId != null) setPageSection(pageId, sectionId)
    }

    @Query("UPDATE pages SET bookId = NULL, chapterId = NULL, sectionId = NULL, bookOrder = NULL WHERE chapterId = :chapterId")
    suspend fun detachChapterPages(chapterId: String)

    @Query("UPDATE pages SET bookId = NULL, chapterId = NULL, sectionId = NULL, bookOrder = NULL WHERE bookId = :bookId")
    suspend fun detachBookPages(bookId: String)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterRow(chapterId: String)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookRow(bookId: String)

    @Transaction
    suspend fun deleteChapterSafely(chapterId: String) {
        val bookId = findChapter(chapterId)?.bookId ?: return
        detachChapterPages(chapterId)
        deleteChapterRow(chapterId)
        liftChapterOrders(bookId)
        chapters(bookId).forEachIndexed { index, chapter -> setChapterOrder(chapter.id, index) }
    }

    @Transaction
    suspend fun duplicateChapter(chapter: Chapter, duplicateId: String, now: Long) {
        val copiedChapter = chapter.copy(id = duplicateId, title = "${chapter.title} · Copy", createdAt = now, updatedAt = now, chapterOrder = chapters(chapter.bookId).size)
        saveChapter(copiedChapter)
        val sectionMap = sections(chapter.id).associate { it.id to java.util.UUID.randomUUID().toString() }
        if (sectionMap.isNotEmpty()) insertSections(sections(chapter.id).mapIndexed { index, section ->
            section.copy(id = sectionMap.getValue(section.id), chapterId = duplicateId, createdAt = now, updatedAt = now, sectionOrder = index)
        })
        val pages = chapterPages(chapter.id)
        if (pages.isNotEmpty()) insertPages(pages.mapIndexed { index, page ->
            page.copy(id = java.util.UUID.randomUUID().toString(), chapterId = duplicateId, sectionId = page.sectionId?.let(sectionMap::get), createdAt = now, updatedAt = now, bookOrder = index)
        })
    }

    @Transaction
    suspend fun deleteBookSafely(bookId: String) {
        val preservedFrontMatter = components(bookId).map { component ->
            Page(
                id = component.id,
                title = component.title,
                body = component.body,
                createdAt = component.createdAt,
                updatedAt = component.updatedAt,
            )
        }
        detachBookPages(bookId)
        if (preservedFrontMatter.isNotEmpty()) insertPages(preservedFrontMatter)
        deleteBookRow(bookId)
    }

    @Transaction
    suspend fun reorderChapters(bookId: String, chapterIds: List<String>) {
        val existing = chapters(bookId).map { it.id }.toSet()
        require(chapterIds.size == existing.size && chapterIds.toSet() == existing) {
            "Chapter order must include each chapter exactly once"
        }
        liftChapterOrders(bookId)
        chapterIds.forEachIndexed { index, id -> setChapterOrder(id, index) }
    }

    @Query("SELECT * FROM pages WHERE bookId = :bookId AND isTrashed = 0")
    suspend fun pagesInBook(bookId: String): List<Page>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooks(books: List<Book>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertComponents(components: List<BookComponent>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPages(pages: List<Page>)

    @Transaction
    suspend fun duplicateBook(sourceBook: Book, duplicateBook: Book, chapterMap: Map<String, String>) {
        saveBook(duplicateBook)
        val oldChapters = chapters(sourceBook.id)
        val sectionMap = mutableMapOf<String, String>()
        if (oldChapters.isNotEmpty()) insertChapters(oldChapters.mapIndexed { index, chapter ->
            chapter.copy(id = chapterMap.getValue(chapter.id), bookId = duplicateBook.id, createdAt = duplicateBook.createdAt, updatedAt = duplicateBook.updatedAt, chapterOrder = index)
        })
        oldChapters.forEach { chapter -> sections(chapter.id).forEach { section -> sectionMap[section.id] = java.util.UUID.randomUUID().toString() } }
        val copiedSections = oldChapters.flatMap { chapter -> sections(chapter.id) }.map { section ->
            section.copy(id = sectionMap.getValue(section.id), bookId = duplicateBook.id, chapterId = chapterMap.getValue(section.chapterId), createdAt = duplicateBook.createdAt, updatedAt = duplicateBook.updatedAt)
        }
        if (copiedSections.isNotEmpty()) insertSections(copiedSections)
        val components = mutableListOf<BookComponent>()
        // Components are copied with fresh identities and preserve their typed content.
        components += _duplicateComponents(sourceBook.id, duplicateBook.id, duplicateBook.createdAt)
        if (components.isNotEmpty()) insertComponents(components)
        val oldPages = pagesInBook(sourceBook.id)
        if (oldPages.isNotEmpty()) insertPages(oldPages.map { page ->
            page.copy(
                id = java.util.UUID.randomUUID().toString(), bookId = duplicateBook.id,
                chapterId = page.chapterId?.let(chapterMap::get), sectionId = page.sectionId?.let(sectionMap::get), createdAt = duplicateBook.createdAt,
                updatedAt = duplicateBook.updatedAt,
            )
        })
    }

    @Query("SELECT * FROM book_components WHERE bookId = :bookId ORDER BY componentOrder")
    suspend fun components(bookId: String): List<BookComponent>

    suspend fun _duplicateComponents(sourceId: String, targetId: String, now: Long): List<BookComponent> =
        components(sourceId).map { it.copy(id = java.util.UUID.randomUUID().toString(), bookId = targetId, createdAt = now, updatedAt = now) }
}
