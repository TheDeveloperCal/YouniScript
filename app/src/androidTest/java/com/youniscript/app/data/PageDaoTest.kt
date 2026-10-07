package com.youniscript.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.youniscript.app.editor.ALIGN_CENTER
import com.youniscript.app.editor.BLOCK_HEADING
import com.youniscript.app.editor.FormattingDocument
import com.youniscript.app.editor.MARK_BOLD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PageDaoTest {
    private lateinit var database: LibraryDatabase
    private lateinit var dao: PageDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertCreatesAndUpdatesPage() = runBlocking {
        val original = Page("page-1", "First title", "First draft", 10L, 10L)
        dao.save(original)
        assertEquals(original, dao.findById(original.id))

        val revised = original.copy(title = "Revised title", body = "Saved writing", updatedAt = 20L)
        dao.save(revised)
        assertEquals(revised, dao.findById(original.id))
    }

    @Test
    fun duplicateCanBeRemovedWithoutRemovingOriginal() = runBlocking {
        val original = Page("original", "Original", "Keep this text", 1L, 1L)
        val duplicate = original.copy(id = "duplicate", title = "Copy of Original")
        dao.save(original)
        dao.save(duplicate)

        dao.deleteById(duplicate.id)

        assertEquals(original, dao.findById(original.id))
        assertEquals(null, dao.findById(duplicate.id))
    }

    @Test
    fun formattingMetadataRoundTripsWithTheBodyText() {
        val document = FormattingDocument("A heading\nBody text")
            .toggleMark(0, 1, MARK_BOLD)
            .setParagraphStyle(0, 9, kind = BLOCK_HEADING, alignment = ALIGN_CENTER)

        assertEquals(document, FormattingDocument.decode(document.text, document.toJson()))
        assertEquals(1, document.asAnnotatedString().paragraphStyles.size)
    }

    @Test
    fun libraryFlowOrdersPagesByMostRecentlyUpdated() = runBlocking {
        val older = Page("older", "Older", "", 1L, 10L)
        val newer = Page("newer", "Newer", "", 2L, 20L)
        dao.save(older)
        dao.save(newer)

        assertEquals(listOf(newer, older), dao.observeAll().first())
        val revisedOlder = older.copy(title = "Recently revised", updatedAt = 30L)
        dao.save(revisedOlder)
        assertEquals(listOf(revisedOlder, newer), dao.observeAll().first())
    }

    @Test
    fun savedPageSurvivesClosingAndReopeningTheDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "persistence-${UUID.randomUUID()}.db"
        val page = Page("persistent-page", "Persistence", "Saved across a database reopen.", 10L, 20L)
        val first = Room.databaseBuilder(context, LibraryDatabase::class.java, databaseName).build()
        first.pageDao().save(page)
        first.close()

        val reopened = Room.databaseBuilder(context, LibraryDatabase::class.java, databaseName).build()
        try {
            assertEquals(page, reopened.pageDao().findById(page.id))
            reopened.pageDao().deleteById(page.id)
        } finally {
            reopened.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun bookmarkStatePersistsWithoutChangingPageText() = runBlocking {
        val original = Page("bookmark-page", "A saved thought", "Keep this paragraph.", 3L, 4L)
        dao.save(original.copy(isBookmarked = true))
        val bookmarked = dao.findById(original.id)
        assertEquals(true, bookmarked?.isBookmarked)
        assertEquals(original.title, bookmarked?.title)
        assertEquals(original.body, bookmarked?.body)
        dao.save(requireNotNull(bookmarked).copy(isBookmarked = false))
        assertEquals(false, dao.findById(original.id)?.isBookmarked)
    }

    @Test
    fun journalEntryUsesStablePageAndDeletingJournalPreservesWriting() = runBlocking {
        val journal = Journal("journal-one", "Daily Journal", "Personal notes", 10L, 10L)
        val page = Page("journal-page", "A day", "Writing remains in its own page record.", 11L, 12L)
        val entry = JournalEntry("entry-one", journal.id, page.id, 11L)
        database.journalDao().saveJournal(journal)
        dao.save(page)
        database.journalDao().saveEntry(entry)

        val persisted = database.journalDao().observeEntries(journal.id).first().single()
        assertEquals(page.id, persisted.page.id)
        assertEquals(entry.entryDate, persisted.entryDate)
        database.journalDao().deleteJournal(journal.id)
        assertEquals(page, dao.findById(page.id))
        assertEquals(0, database.journalDao().observeEntries(journal.id).first().size)
    }

    @Test
    fun pageCanBeMovedToTrashAndRestoredWithWritingIntact() = runBlocking {
        val original = Page("trash-page", "Important", "Text remains intact.", 1L, 2L)
        dao.save(original)
        dao.moveToTrash(original.id, 10L)
        val trashed = requireNotNull(dao.findById(original.id))
        assertEquals(true, trashed.isTrashed)
        assertEquals(10L, trashed.trashedAt)
        assertEquals(original.body, trashed.body)
        dao.restoreFromTrash(original.id)
        val restored = requireNotNull(dao.findById(original.id))
        assertEquals(false, restored.isTrashed)
        assertEquals(null, restored.trashedAt)
        assertEquals(original.body, restored.body)
    }

    @Test
    fun specializedEntryCollectionsAnnotationsLinksAndRevisionsPersist() = runBlocking {
        val personal = database.personalLibraryDao()
        val page = Page("personal-page", "A Quote", "Keep the original words.", 10L, 10L)
        val target = Page("target-page", "Related page", "Linked writing.", 11L, 11L)
        dao.save(page); dao.save(target)
        val entry = PersonalEntry("quote-metadata", page.id, "quote", author = "A Writer", source = "A Book", quoteDate = 10L)
        personal.savePersonalEntry(entry)
        assertEquals(entry, personal.observeEntryForPage(page.id).first())

        val one = CollectionRecord("collection-one", "Memories", createdAt = 1L, updatedAt = 1L)
        val two = CollectionRecord("collection-two", "Research", createdAt = 1L, updatedAt = 1L)
        personal.saveCollection(one); personal.saveCollection(two)
        personal.addCollectionItem(CollectionItem(one.id, "page", page.id, 0))
        personal.addCollectionItem(CollectionItem(two.id, "page", page.id, 0))
        assertEquals(1, personal.observeCollectionItems(one.id).first().size)
        assertEquals(1, personal.observeCollectionItems(two.id).first().size)

        val note = AnnotationRecord("annotation-one", "page", page.id, "Question", "Check this source.", 1L, 1L)
        personal.saveAnnotation(note)
        val link = PageLink("link-one", page.id, target.id, "Related page", 1L)
        personal.addLink(link)
        val revision = PageRevision("revision-one", page.id, page.title, page.body, page.formatting, page.pageStyleId, 1L)
        personal.insertRevision(revision)
        val attachment = MediaAttachment("attachment-one", page.id, "drawing", "file:///private/drawing.png", "Drawing.png", 2L)
        personal.insertAttachment(attachment)
        assertEquals(note, personal.observeAnnotationsFor("page", page.id).first().single())
        assertEquals(link, personal.observeLinks(page.id).first().single())
        assertEquals(link, personal.observeBacklinks(target.id).first().single())
        assertEquals(revision, personal.observeRevisions(page.id).first().single())
        assertEquals(attachment, personal.observeAttachments(page.id).first().single())
    }

    @Test
    fun backupRoundTripPreservesPageBookmarkState() {
        val source = LibraryBackupSnapshot(
            pages = listOf(Page("bookmarked-page", "A saved thought", "Keep this paragraph.", 3L, 4L, isBookmarked = true)),
            books = emptyList(), chapters = emptyList(), sections = emptyList(), components = emptyList(),
        )
        val restored = LibraryBackupCodec.decode(LibraryBackupCodec.encode(source))
        assertEquals(true, restored.pages.single().isBookmarked)
        assertEquals(source.pages.single().body, restored.pages.single().body)
    }

    @Test
    fun encryptedBackupRoundTripsDrawingAttachmentBytes() {
        val page = Page("page-media", "Sketch", "A note beside the drawing", 1L, 2L)
        val attachment = MediaAttachment("drawing-1", page.id, "drawing", "file:///private/drawing.png", "Drawing.png", 3L)
        val snapshot = LibraryBackupSnapshot(
            pages = listOf(page), books = emptyList(), chapters = emptyList(), sections = emptyList(), components = emptyList(),
            attachments = listOf(attachment),
        )
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 1, 2, 3)
        val password = "a-long-private-passphrase".toCharArray()
        try {
            EncryptedLibraryBackupCodec.encode(snapshot, emptyMap(), password)
            throw AssertionError("Missing page-media asset should reject the backup")
        } catch (_: IllegalArgumentException) {
            // A page attachment must be embedded rather than left as a device-local URI.
        }
        val archive = EncryptedLibraryBackupCodec.encode(snapshot, emptyMap(), password, mapOf(attachment.id to bytes))
        val decoded = EncryptedLibraryBackupCodec.decode(archive, password)
        assertArrayEquals(bytes, decoded.attachmentFiles.getValue(attachment.id))
        assertEquals("youniscript-backup-media:${attachment.id}", decoded.snapshot.attachments.single().localUri)
    }

    @Test
    fun versionOneMigrationPreservesPageAndAddsOptionalMetadata() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            "CREATE TABLE pages (id TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))",
        )
        legacy.execSQL(
            "INSERT INTO pages (id, title, body, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>("legacy-page", "A preserved title", "A preserved manuscript paragraph.", 100L, 200L),
        )
        legacy.version = 1
        legacy.close()

        val upgraded = Room.databaseBuilder(context, LibraryDatabase::class.java, databaseName)
            .addMigrations(LibraryMigrations.MIGRATION_1_2, LibraryMigrations.MIGRATION_2_3, LibraryMigrations.MIGRATION_3_4, LibraryMigrations.MIGRATION_4_5, LibraryMigrations.MIGRATION_5_6, LibraryMigrations.MIGRATION_6_7, LibraryMigrations.MIGRATION_7_8)
            .build()
        try {
            val page = upgraded.pageDao().findById("legacy-page")
            assertEquals("A preserved title", page?.title)
            assertEquals("A preserved manuscript paragraph.", page?.body)
            assertEquals(100L, page?.createdAt)
            assertEquals(200L, page?.updatedAt)
            assertEquals("", page?.formatting)
            assertEquals("[]", page?.tags)
            assertEquals(null, page?.pageStyleId)
            assertEquals(null, page?.collectionId)
            assertEquals(null, page?.bookId)
            assertEquals(null, page?.chapterId)
            assertEquals(null, page?.sectionId)
            assertEquals(null, page?.bookOrder)
            assertEquals(false, page?.isBookmarked)
            assertEquals(false, page?.isTrashed)
            assertEquals(null, page?.trashedAt)
        } finally {
            upgraded.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun versionTwoMigrationPreservesPageIdentityTextAndStyle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-v2-${UUID.randomUUID()}.db"
        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacy.execSQL("CREATE TABLE pages (id TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, formatting TEXT NOT NULL, pageStyleId TEXT, tags TEXT NOT NULL, collectionId TEXT, bookId TEXT, PRIMARY KEY(id))")
        legacy.execSQL("INSERT INTO pages VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf<Any?>("phase3-id", "Preserved title", "Preserved paragraph", 10L, 20L, "format-data", "ancient-manuscript", "[]", null, null))
        legacy.version = 2
        legacy.close()

        val upgraded = Room.databaseBuilder(context, LibraryDatabase::class.java, databaseName)
            .addMigrations(LibraryMigrations.MIGRATION_2_3, LibraryMigrations.MIGRATION_3_4, LibraryMigrations.MIGRATION_4_5, LibraryMigrations.MIGRATION_5_6, LibraryMigrations.MIGRATION_6_7, LibraryMigrations.MIGRATION_7_8)
            .build()
        try {
            val page = upgraded.pageDao().findById("phase3-id")
            assertEquals("phase3-id", page?.id)
            assertEquals("Preserved title", page?.title)
            assertEquals("Preserved paragraph", page?.body)
            assertEquals("ancient-manuscript", page?.pageStyleId)
            assertEquals("format-data", page?.formatting)
            assertEquals(null, page?.chapterId)
            assertEquals(null, page?.sectionId)
            assertEquals(null, page?.bookOrder)
            assertEquals(false, page?.isBookmarked)
        } finally {
            upgraded.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun movingAndDeletingChapterPreservesPageIdentityAndContent() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("book-a", "A book", createdAt = 1L, updatedAt = 1L)
        val first = Chapter("ch-a", book.id, "First", 1L, 1L, 0)
        val second = Chapter("ch-b", book.id, "Second", 1L, 1L, 1)
        val page = Page("stable-page", "Memory", "Keep these words", 1L, 1L, bookId = book.id, chapterId = first.id, bookOrder = 0)
        bookDao.saveBook(book)
        bookDao.saveChapter(first)
        bookDao.saveChapter(second)
        dao.save(page)

        bookDao.movePageToChapter(page.id, second.id)
        assertEquals(page.copy(chapterId = second.id), dao.findById(page.id))
        bookDao.deleteChapterSafely(second.id)
        assertEquals(page.copy(bookId = null, chapterId = null, bookOrder = null), dao.findById(page.id))
    }

    @Test
    fun duplicateBookCreatesIndependentPageAndChapterIds() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("original-book", "Original", createdAt = 1L, updatedAt = 1L)
        val chapter = Chapter("original-chapter", book.id, "Chapter 1", 1L, 1L, 0)
        val page = Page("original-page", "Title", "Independent copy", 1L, 1L, bookId = book.id, chapterId = chapter.id, bookOrder = 0)
        bookDao.saveBook(book)
        bookDao.saveChapter(chapter)
        dao.save(page)
        val copy = book.copy(id = "copy-book", title = "Copy")
        bookDao.duplicateBook(book, copy, mapOf(chapter.id to "copy-chapter"))

        val copiedPage = bookDao.observeBookPages(copy.id).first().single()
        assertEquals("copy-book", copiedPage.bookId)
        assertEquals("copy-chapter", copiedPage.chapterId)
        assertEquals("Independent copy", copiedPage.body)
        assertEquals(false, copiedPage.id == page.id)
        assertEquals(page, dao.findById(page.id))
    }

    @Test
    fun existingPageCanBeAddedAndMovedBetweenSectionsWithoutChangingItsIdentityOrContent() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("section-book", "Section book", createdAt = 1L, updatedAt = 1L)
        val chapter = Chapter("section-chapter", book.id, "Chapter", 1L, 1L, 0)
        val nextChapter = Chapter("section-chapter-next", book.id, "Next chapter", 1L, 1L, 1)
        val firstSection = Section("section-a", book.id, chapter.id, "First section", 0, 1L, 1L)
        val secondSection = Section("section-b", book.id, chapter.id, "Second section", 1, 1L, 1L)
        val original = Page("stable-independent-page", "Memory", "Exact writing and metadata", 10L, 20L, formatting = "formatting", pageStyleId = "literary", tags = "[\"keep\"]")
        bookDao.saveBook(book)
        bookDao.saveChapter(chapter)
        bookDao.saveChapter(nextChapter)
        bookDao.saveSection(firstSection)
        bookDao.saveSection(secondSection)
        dao.save(original)

        bookDao.addExistingPageToChapter(original.id, chapter.id, firstSection.id)
        val added = dao.findById(original.id)
        assertEquals(original.id, added?.id)
        assertEquals(original.body, added?.body)
        assertEquals(original.formatting, added?.formatting)
        assertEquals(original.pageStyleId, added?.pageStyleId)
        assertEquals(original.tags, added?.tags)
        assertEquals(book.id, added?.bookId)
        assertEquals(chapter.id, added?.chapterId)
        assertEquals(firstSection.id, added?.sectionId)

        bookDao.placePageInSection(original.id, secondSection.id)
        assertEquals(secondSection.id, dao.findById(original.id)?.sectionId)
        bookDao.moveSectionToChapter(secondSection.id, nextChapter.id)
        assertEquals(nextChapter.id, dao.findById(original.id)?.chapterId)
        assertEquals(secondSection.id, dao.findById(original.id)?.sectionId)
        assertEquals(nextChapter.id, bookDao.findSection(secondSection.id)?.chapterId)
        bookDao.deleteSectionSafely(secondSection.id)
        assertEquals(null, dao.findById(original.id)?.sectionId)
        assertEquals(nextChapter.id, dao.findById(original.id)?.chapterId)
        assertEquals(original.body, dao.findById(original.id)?.body)
        assertEquals(listOf(firstSection.id), bookDao.sections(chapter.id).map { it.id })
    }

    @Test
    fun sectionReorderingPersistsExplicitOrder() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("ordered-section-book", "Order", createdAt = 1L, updatedAt = 1L)
        val chapter = Chapter("ordered-section-chapter", book.id, "Chapter", 1L, 1L, 0)
        val first = Section("ordered-a", book.id, chapter.id, "A", 0, 1L, 1L)
        val second = Section("ordered-b", book.id, chapter.id, "B", 1, 1L, 1L)
        bookDao.saveBook(book)
        bookDao.saveChapter(chapter)
        bookDao.saveSection(first)
        bookDao.saveSection(second)
        bookDao.reorderSections(chapter.id, listOf(second.id, first.id))
        assertEquals(listOf(second.id, first.id), bookDao.sections(chapter.id).map { it.id })
    }

    @Test
    fun deletingBookPreservesPagesAndConvertsFrontMatterToStandalonePages() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("delete-safe-book", "Delete-safe book", createdAt = 1L, updatedAt = 1L)
        val chapter = Chapter("delete-safe-chapter", book.id, "Chapter", 1L, 1L, 0)
        val page = Page("delete-safe-page", "Body", "Keep the manuscript text", 1L, 1L, bookId = book.id, chapterId = chapter.id)
        val dedication = BookComponent("delete-safe-frontmatter", book.id, "dedication", "Dedication", "For my family", 0, 1L, 2L)
        bookDao.saveBook(book)
        bookDao.saveChapter(chapter)
        bookDao.saveComponent(dedication)
        dao.save(page)

        bookDao.deleteBookSafely(book.id)

        assertEquals(page.copy(bookId = null, chapterId = null, sectionId = null, bookOrder = null), dao.findById(page.id))
        assertEquals(Page(dedication.id, dedication.title, dedication.body, dedication.createdAt, dedication.updatedAt), dao.findById(dedication.id))
        assertEquals(null, bookDao.findBook(book.id))
    }

    @Test
    fun libraryBackupRoundTripsManuscriptDataAndRejectsExistingIdsWithoutOverwriting() = runBlocking {
        val bookDao = database.bookDao()
        val book = Book("backup-book", "Backup manuscript", "A subtitle", "An author", "Private notes", 10L, 20L,
            coverTemplateId = "literary", coverImageUri = "content://test-cover", defaultPageStyleId = "classic-book", status = "draft")
        val chapter = Chapter("backup-chapter", book.id, "Opening", 10L, 20L, 0)
        val section = Section("backup-section", book.id, chapter.id, "First section", 0, 10L, 20L)
        val page = Page("backup-page", "A title", "A body that must survive.", 10L, 20L,
            formatting = "formatting-data", pageStyleId = "ancient-manuscript", tags = "[\"memory\"]",
            bookId = book.id, chapterId = chapter.id, sectionId = section.id, bookOrder = 0)
        val component = BookComponent("backup-preface", book.id, "preface", "Preface", "Preface text", 0, 10L, 20L)
        bookDao.saveBook(book)
        bookDao.saveChapter(chapter)
        bookDao.saveSection(section)
        bookDao.saveComponent(component)
        dao.save(page)

        val snapshot = database.createBackupSnapshot()
        val coverBytes = byteArrayOf(3, 1, 4, 1, 5, 9)
        val decoded = EncryptedLibraryBackupCodec.decode(
            EncryptedLibraryBackupCodec.encode(snapshot, mapOf(book.id to coverBytes), "a-long-private-passphrase".toCharArray()),
            "a-long-private-passphrase".toCharArray(),
        )
        val decodedSnapshot = decoded.snapshot.copy(books = decoded.snapshot.books.map { it.copy(coverImageUri = book.coverImageUri) })
        assertEquals(snapshot, decodedSnapshot)
        assertEquals(true, decoded.coverImages.getValue(book.id).contentEquals(coverBytes))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val restored = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            restored.restoreBackup(decodedSnapshot)
            assertEquals(page, restored.pageDao().findById(page.id))
            assertEquals(book, restored.bookDao().findBook(book.id))
            assertEquals(listOf(section), restored.bookDao().sections(chapter.id))
            assertEquals(listOf(component), restored.bookDao().components(book.id))
            val beforeRejectedImport = restored.createBackupSnapshot()
            val failure = runCatching { restored.restoreBackup(decodedSnapshot) }.exceptionOrNull()
            assertEquals(true, failure is IllegalArgumentException)
            assertEquals(beforeRejectedImport, restored.createBackupSnapshot())
        } finally {
            restored.close()
        }
    }
}
