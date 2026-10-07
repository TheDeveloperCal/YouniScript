package com.youniscript.app.data

import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryBackupSnapshotTest {
    @Test
    fun acceptsAConsistentWritingAndManuscriptSnapshot() {
        snapshot().validate()
    }

    @Test
    fun rejectsPagesWhoseSectionBelongsToAnotherChapter() {
        val original = snapshot()
        val otherChapter = Chapter("chapter-other", "book", "Other", 1L, 1L, 1)
        val otherSection = Section("section-other", "book", otherChapter.id, "Other section", 0, 1L, 1L)
        original.copy(
            chapters = original.chapters + otherChapter,
            sections = original.sections + otherSection,
            pages = original.pages.map { it.copy(sectionId = otherSection.id) },
        ).let { assertThrows(IllegalArgumentException::class.java) { it.validate() } }
    }

    @Test
    fun rejectsDuplicateChapterOrderWithinOneBook() {
        val original = snapshot()
        val secondChapter = Chapter("chapter-2", "book", "Second", 1L, 1L, 0)
        original.copy(chapters = original.chapters + secondChapter)
            .let { assertThrows(IllegalArgumentException::class.java) { it.validate() } }
    }

    @Test
    fun acceptsJournalStructureAndEntryToPageIdentityForBackup() {
        val journal = Journal("journal", "Morning Pages", "Private reflections", 1L, 3L)
        val entry = JournalEntry("entry", journal.id, "page", 2L)
        val source = snapshot().copy(journals = listOf(journal), journalEntries = listOf(entry))
        source.validate()
        assertEquals("journal", source.journals.single().id)
        assertEquals(source.pages.single().id, source.journalEntries.single().pageId)
    }

    @Test
    fun rejectsJournalEntryWithMissingJournalOrPage() {
        val invalid = snapshot().copy(journalEntries = listOf(JournalEntry("entry", "missing", "page", 1L)))
        assertThrows(IllegalArgumentException::class.java) { invalid.validate() }
    }

    @Test
    fun acceptsManyToManyCollectionsAndIndependentPageMetadata() {
        val first = CollectionRecord("collection-one", "Memories", createdAt = 1L, updatedAt = 1L)
        val second = CollectionRecord("collection-two", "Research", createdAt = 1L, updatedAt = 1L)
        val annotatedPage = AnnotationRecord("note", "page", "page", "Question", "Check this date", 1L, 2L)
        val link = PageLink("link", "page", "other-page", "Related thought", 1L)
        val otherPage = Page("other-page", "Related", "Another thought", 1L, 1L)
        val revision = PageRevision("revision", "page", "Earlier title", "Earlier words", "{}", null, 1L)
        val entry = PersonalEntry("entry", "page", "quote", author = "Author", source = "Book")
        snapshot().copy(
            pages = snapshot().pages + otherPage,
            collections = listOf(first, second),
            collectionItems = listOf(CollectionItem(first.id, "page", "page", 0), CollectionItem(second.id, "page", "page", 0)),
            annotations = listOf(annotatedPage),
            links = listOf(link),
            glossary = listOf(GlossaryTerm("term", "Freedom", "A personal definition", "page", 1L, 1L)),
            revisions = listOf(revision),
            personalEntries = listOf(entry),
        ).validate()
    }

    @Test
    fun rejectsMetadataWhoseStablePageReferenceIsMissing() {
        val invalid = snapshot().copy(
            revisions = listOf(PageRevision("revision", "missing-page", "Old", "Old words", "{}", null, 1L)),
        )
        assertThrows(IllegalArgumentException::class.java) { invalid.validate() }
    }

    private fun snapshot() = LibraryBackupSnapshot(
        pages = listOf(Page("page", "A page", "Saved words", 1L, 2L, pageStyleId = "literary", bookId = "book", chapterId = "chapter", sectionId = "section", bookOrder = 0)),
        books = listOf(Book("book", "A book", createdAt = 1L, updatedAt = 2L)),
        chapters = listOf(Chapter("chapter", "book", "Chapter 1", 1L, 2L, 0)),
        sections = listOf(Section("section", "book", "chapter", "Opening", 0, 1L, 2L)),
        components = listOf(BookComponent("component", "book", "preface", "Preface", "Words", 0, 1L, 2L)),
    )
}
