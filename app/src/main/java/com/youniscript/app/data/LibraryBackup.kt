package com.youniscript.app.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Portable JSON snapshot for the local writing records currently supported by Room. */
data class LibraryBackupSnapshot(
    val pages: List<Page>,
    val books: List<Book>,
    val chapters: List<Chapter>,
    val sections: List<Section>,
    val components: List<BookComponent>,
    val journals: List<Journal> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val collections: List<CollectionRecord> = emptyList(),
    val collectionItems: List<CollectionItem> = emptyList(),
    val annotations: List<AnnotationRecord> = emptyList(),
    val links: List<PageLink> = emptyList(),
    val glossary: List<GlossaryTerm> = emptyList(),
    val revisions: List<PageRevision> = emptyList(),
    val attachments: List<MediaAttachment> = emptyList(),
    val personalEntries: List<PersonalEntry> = emptyList(),
) {
    fun validate() {
        requireUnique(pages.map { it.id }, "page")
        requireUnique(books.map { it.id }, "book")
        requireUnique(chapters.map { it.id }, "chapter")
        requireUnique(sections.map { it.id }, "section")
        requireUnique(components.map { it.id }, "book component")
        requireUnique(journals.map { it.id }, "journal")
        requireUnique(journalEntries.map { it.id }, "journal entry")
        requireUnique(journalEntries.map { it.pageId }, "journal entry page")
        requireUnique(collections.map { it.id }, "collection")
        requireUnique(collectionItems.map { "${it.collectionId}:${it.contentType}:${it.contentId}" }, "collection membership")
        requireUnique(annotations.map { it.id }, "annotation")
        requireUnique(links.map { it.id }, "page link")
        requireUnique(glossary.map { it.id }, "glossary term")
        requireUnique(glossary.map { it.term.lowercase() }, "glossary spelling")
        requireUnique(revisions.map { it.id }, "revision")
        requireUnique(attachments.map { it.id }, "attachment")
        requireUnique(personalEntries.map { it.id }, "personal entry")
        requireUnique(personalEntries.map { it.pageId }, "personal entry page")
        val bookById = books.associateBy { it.id }
        val chapterById = chapters.associateBy { it.id }
        val sectionById = sections.associateBy { it.id }
        val journalById = journals.associateBy { it.id }
        val collectionById = collections.associateBy { it.id }
        require(chapters.all { it.bookId in bookById }) { "A chapter refers to a missing book" }
        require(sections.all { section ->
            section.bookId in bookById && chapterById[section.chapterId]?.bookId == section.bookId
        }) { "A section refers to a missing or different book chapter" }
        require(components.all { it.bookId in bookById }) { "A book component refers to a missing book" }
        require(journalEntries.all { it.journalId in journalById && pages.any { page -> page.id == it.pageId } }) { "A journal entry refers to missing content" }
        val pageIds = pages.mapTo(mutableSetOf()) { it.id }
        require(collectionItems.all { it.collectionId in collectionById && it.contentType in setOf("page", "book", "journal") && when (it.contentType) {
            "page" -> it.contentId in pageIds
            "book" -> it.contentId in bookById
            else -> it.contentId in journalById
        } }) { "A collection membership refers to missing content" }
        require(annotations.all { annotation -> when (annotation.contentType) {
            "page" -> annotation.contentId in pageIds
            "book" -> annotation.contentId in bookById
            "journal" -> annotation.contentId in journalById
            else -> false
        } }) { "An annotation refers to missing or unsupported content" }
        require(links.all { it.sourcePageId in pageIds && it.targetPageId in pageIds && it.sourcePageId != it.targetPageId }) { "A page link refers to missing or identical content" }
        require(glossary.all { it.sourcePageId == null || it.sourcePageId in pageIds }) { "A glossary term refers to a missing page" }
        require(revisions.all { it.pageId in pageIds } && attachments.all { it.pageId in pageIds } && personalEntries.all { it.pageId in pageIds }) { "Page metadata refers to missing content" }
        require(pages.all { page ->
            if (page.bookId == null) page.chapterId == null && page.sectionId == null
            else {
                val chapter = page.chapterId?.let(chapterById::get)
                chapter?.bookId == page.bookId && (page.sectionId == null || sectionById[page.sectionId]?.let {
                    it.bookId == page.bookId && it.chapterId == page.chapterId
                } == true)
            }
        }) { "A page has an invalid manuscript relationship" }
        chapters.groupBy { it.bookId }.values.forEach { requireDistinctBy(it, { row -> row.chapterOrder }, "chapter order") }
        sections.groupBy { it.chapterId }.values.forEach { requireDistinctBy(it, { row -> row.sectionOrder }, "section order") }
        components.groupBy { it.bookId }.values.forEach { requireDistinctBy(it, { row -> row.componentOrder }, "component order") }
    }

    private fun requireUnique(ids: List<String>, label: String) {
        require(ids.none(String::isBlank) && ids.toSet().size == ids.size) { "The archive contains a missing or duplicate $label ID" }
    }

    private fun <T, K> requireDistinctBy(rows: List<T>, key: (T) -> K, label: String) {
        require(rows.map(key).toSet().size == rows.size) { "The archive contains duplicate $label values" }
    }
}

object LibraryBackupCodec {
    private const val FORMAT = "youniscript-library"
    private const val VERSION = 1

    fun encode(snapshot: LibraryBackupSnapshot): String {
        snapshot.validate()
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("pages", JSONArray().apply { snapshot.pages.forEach { put(it.toJson()) } })
            .put("books", JSONArray().apply { snapshot.books.forEach { put(it.toJson()) } })
            .put("chapters", JSONArray().apply { snapshot.chapters.forEach { put(it.toJson()) } })
            .put("sections", JSONArray().apply { snapshot.sections.forEach { put(it.toJson()) } })
            .put("components", JSONArray().apply { snapshot.components.forEach { put(it.toJson()) } })
            .put("journals", JSONArray().apply { snapshot.journals.forEach { put(it.toJson()) } })
            .put("journalEntries", JSONArray().apply { snapshot.journalEntries.forEach { put(it.toJson()) } })
            .put("collections", JSONArray().apply { snapshot.collections.forEach { put(it.toJson()) } })
            .put("collectionItems", JSONArray().apply { snapshot.collectionItems.forEach { put(it.toJson()) } })
            .put("annotations", JSONArray().apply { snapshot.annotations.forEach { put(it.toJson()) } })
            .put("links", JSONArray().apply { snapshot.links.forEach { put(it.toJson()) } })
            .put("glossary", JSONArray().apply { snapshot.glossary.forEach { put(it.toJson()) } })
            .put("revisions", JSONArray().apply { snapshot.revisions.forEach { put(it.toJson()) } })
            .put("attachments", JSONArray().apply { snapshot.attachments.forEach { put(it.toJson()) } })
            .put("personalEntries", JSONArray().apply { snapshot.personalEntries.forEach { put(it.toJson()) } })
            .toString(2)
    }

    fun decode(source: String): LibraryBackupSnapshot {
        require(source.length <= 50_000_000) { "The backup is larger than this app can safely import" }
        val root = JSONObject(source)
        require(root.getString("format") == FORMAT) { "This is not a YouniScript library backup" }
        require(root.getInt("version") == VERSION) { "This backup version is not supported by this app" }
        val snapshot = LibraryBackupSnapshot(
            pages = root.getJSONArray("pages").decodeEach { it.toPage() },
            books = root.getJSONArray("books").decodeEach { it.toBook() },
            chapters = root.getJSONArray("chapters").decodeEach { it.toChapter() },
            sections = root.getJSONArray("sections").decodeEach { it.toSection() },
            components = root.getJSONArray("components").decodeEach { it.toComponent() },
            journals = root.optJSONArray("journals")?.decodeEach { it.toJournal() }.orEmpty(),
            journalEntries = root.optJSONArray("journalEntries")?.decodeEach { it.toJournalEntry() }.orEmpty(),
            collections = root.optJSONArray("collections")?.decodeEach { it.toCollection() }.orEmpty(),
            collectionItems = root.optJSONArray("collectionItems")?.decodeEach { it.toCollectionItem() }.orEmpty(),
            annotations = root.optJSONArray("annotations")?.decodeEach { it.toAnnotation() }.orEmpty(),
            links = root.optJSONArray("links")?.decodeEach { it.toLink() }.orEmpty(),
            glossary = root.optJSONArray("glossary")?.decodeEach { it.toGlossaryTerm() }.orEmpty(),
            revisions = root.optJSONArray("revisions")?.decodeEach { it.toRevision() }.orEmpty(),
            attachments = root.optJSONArray("attachments")?.decodeEach { it.toAttachment() }.orEmpty(),
            personalEntries = root.optJSONArray("personalEntries")?.decodeEach { it.toPersonalEntry() }.orEmpty(),
        )
        snapshot.validate()
        return snapshot
    }

    private fun <T> JSONArray.decodeEach(decode: (JSONObject) -> T): List<T> = (0 until length()).map { decode(getJSONObject(it)) }

    private fun Page.toJson() = JSONObject().put("id", id).put("title", title).put("body", body)
        .put("createdAt", createdAt).put("updatedAt", updatedAt).put("formatting", formatting)
        .putNullable("pageStyleId", pageStyleId).put("tags", tags).putNullable("collectionId", collectionId)
        .putNullable("bookId", bookId).putNullable("chapterId", chapterId).putNullable("sectionId", sectionId)
        .putNullable("bookOrder", bookOrder).put("isBookmarked", isBookmarked)
        .put("isTrashed", isTrashed).putNullable("trashedAt", trashedAt)

    private fun Book.toJson() = JSONObject().put("id", id).put("title", title).put("subtitle", subtitle)
        .put("author", author).put("description", description).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("coverTemplateId", coverTemplateId).putNullable("coverImageUri", coverImageUri)
        .put("defaultPageStyleId", defaultPageStyleId).put("status", status).put("bookOrder", bookOrder)

    private fun Chapter.toJson() = JSONObject().put("id", id).put("bookId", bookId).put("title", title)
        .put("createdAt", createdAt).put("updatedAt", updatedAt).put("chapterOrder", chapterOrder)
        .putNullable("styleOverrideId", styleOverrideId)

    private fun Section.toJson() = JSONObject().put("id", id).put("bookId", bookId).put("chapterId", chapterId)
        .put("title", title).put("sectionOrder", sectionOrder).put("createdAt", createdAt).put("updatedAt", updatedAt)

    private fun BookComponent.toJson() = JSONObject().put("id", id).put("bookId", bookId).put("type", type)
        .put("title", title).put("body", body).put("componentOrder", componentOrder)
        .put("createdAt", createdAt).put("updatedAt", updatedAt)

    private fun Journal.toJson() = JSONObject().put("id", id).put("title", title).put("description", description)
        .put("createdAt", createdAt).put("updatedAt", updatedAt).put("isArchived", isArchived)

    private fun JournalEntry.toJson() = JSONObject().put("id", id).put("journalId", journalId)
        .put("pageId", pageId).put("entryDate", entryDate)

    private fun CollectionRecord.toJson() = JSONObject().put("id", id).put("title", title).put("description", description).put("icon", icon).put("createdAt", createdAt).put("updatedAt", updatedAt)
    private fun CollectionItem.toJson() = JSONObject().put("collectionId", collectionId).put("contentType", contentType).put("contentId", contentId).put("itemOrder", itemOrder)
    private fun AnnotationRecord.toJson() = JSONObject().put("id", id).put("contentType", contentType).put("contentId", contentId).put("kind", kind).put("body", body).put("createdAt", createdAt).put("updatedAt", updatedAt)
    private fun PageLink.toJson() = JSONObject().put("id", id).put("sourcePageId", sourcePageId).put("targetPageId", targetPageId).put("label", label).put("createdAt", createdAt)
    private fun GlossaryTerm.toJson() = JSONObject().put("id", id).put("term", term).put("definition", definition).putNullable("sourcePageId", sourcePageId).put("createdAt", createdAt).put("updatedAt", updatedAt)
    private fun PageRevision.toJson() = JSONObject().put("id", id).put("pageId", pageId).put("title", title).put("body", body).put("formatting", formatting).putNullable("pageStyleId", pageStyleId).put("createdAt", createdAt)
    private fun MediaAttachment.toJson() = JSONObject().put("id", id).put("pageId", pageId).put("mediaType", mediaType).put("localUri", localUri).put("displayName", displayName).put("createdAt", createdAt)
    private fun PersonalEntry.toJson() = JSONObject().put("id", id).put("pageId", pageId).put("kind", kind).put("author", author).put("source", source).put("recipient", recipient).put("letterType", letterType).putNullable("quoteDate", quoteDate).put("writtenByMe", writtenByMe).put("people", people).put("places", places).put("feelings", feelings).put("details", details).put("symbols", symbols).put("reflection", reflection).put("signature", signature)

    private fun JSONObject.toPage() = Page(getString("id"), getString("title"), getString("body"), getLong("createdAt"), getLong("updatedAt"),
        getString("formatting"), nullableString("pageStyleId"), getString("tags"), nullableString("collectionId"),
        nullableString("bookId"), nullableString("chapterId"), nullableString("sectionId"), nullableInt("bookOrder"), optBoolean("isBookmarked", false),
        optBoolean("isTrashed", false), nullableLong("trashedAt"))

    private fun JSONObject.toBook() = Book(getString("id"), getString("title"), getString("subtitle"), getString("author"), getString("description"),
        getLong("createdAt"), getLong("updatedAt"), getString("coverTemplateId"), nullableString("coverImageUri"),
        getString("defaultPageStyleId"), getString("status"), getInt("bookOrder"))

    private fun JSONObject.toChapter() = Chapter(getString("id"), getString("bookId"), getString("title"), getLong("createdAt"), getLong("updatedAt"),
        getInt("chapterOrder"), nullableString("styleOverrideId"))

    private fun JSONObject.toSection() = Section(getString("id"), getString("bookId"), getString("chapterId"), getString("title"),
        getInt("sectionOrder"), getLong("createdAt"), getLong("updatedAt"))

    private fun JSONObject.toComponent() = BookComponent(getString("id"), getString("bookId"), getString("type"), getString("title"),
        getString("body"), getInt("componentOrder"), getLong("createdAt"), getLong("updatedAt"))

    private fun JSONObject.toJournal() = Journal(getString("id"), getString("title"), getString("description"),
        getLong("createdAt"), getLong("updatedAt"), optBoolean("isArchived", false))

    private fun JSONObject.toJournalEntry() = JournalEntry(getString("id"), getString("journalId"), getString("pageId"), getLong("entryDate"))
    private fun JSONObject.toCollection() = CollectionRecord(getString("id"), getString("title"), getString("description"), getString("icon"), getLong("createdAt"), getLong("updatedAt"))
    private fun JSONObject.toCollectionItem() = CollectionItem(getString("collectionId"), getString("contentType"), getString("contentId"), getInt("itemOrder"))
    private fun JSONObject.toAnnotation() = AnnotationRecord(getString("id"), getString("contentType"), getString("contentId"), getString("kind"), getString("body"), getLong("createdAt"), getLong("updatedAt"))
    private fun JSONObject.toLink() = PageLink(getString("id"), getString("sourcePageId"), getString("targetPageId"), getString("label"), getLong("createdAt"))
    private fun JSONObject.toGlossaryTerm() = GlossaryTerm(getString("id"), getString("term"), getString("definition"), nullableString("sourcePageId"), getLong("createdAt"), getLong("updatedAt"))
    private fun JSONObject.toRevision() = PageRevision(getString("id"), getString("pageId"), getString("title"), getString("body"), getString("formatting"), nullableString("pageStyleId"), getLong("createdAt"))
    private fun JSONObject.toAttachment() = MediaAttachment(getString("id"), getString("pageId"), getString("mediaType"), getString("localUri"), getString("displayName"), getLong("createdAt"))
    private fun JSONObject.toPersonalEntry() = PersonalEntry(getString("id"), getString("pageId"), getString("kind"), getString("author"), getString("source"), getString("recipient"), getString("letterType"), nullableLong("quoteDate"), getBoolean("writtenByMe"), getString("people"), getString("places"), getString("feelings"), getString("details"), getString("symbols"), getString("reflection"), getString("signature"))

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject = put(name, value ?: JSONObject.NULL)
    private fun JSONObject.putNullable(name: String, value: Int?): JSONObject = put(name, value ?: JSONObject.NULL)
    private fun JSONObject.putNullable(name: String, value: Long?): JSONObject = put(name, value ?: JSONObject.NULL)
    private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else getString(name)
    private fun JSONObject.nullableInt(name: String): Int? = if (isNull(name)) null else getInt(name)
    private fun JSONObject.nullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)
}

suspend fun LibraryDatabase.createBackupSnapshot(): LibraryBackupSnapshot = withTransaction {
    LibraryBackupSnapshot(
        pages = pageDao().snapshotPages(),
        books = bookDao().snapshotBooks(),
        chapters = bookDao().snapshotChapters(),
        sections = bookDao().snapshotSections(),
        components = bookDao().snapshotComponents(),
        journals = journalDao().snapshotJournals(),
        journalEntries = journalDao().snapshotEntries(),
        collections = personalLibraryDao().snapshotCollections(),
        collectionItems = personalLibraryDao().snapshotCollectionItems(),
        annotations = personalLibraryDao().snapshotAnnotations(),
        links = personalLibraryDao().snapshotLinks(),
        glossary = personalLibraryDao().snapshotGlossary(),
        revisions = personalLibraryDao().snapshotRevisions(),
        attachments = personalLibraryDao().snapshotAttachments(),
        personalEntries = personalLibraryDao().snapshotPersonalEntries(),
    ).also { it.validate() }
}

/** Imports a validated backup additively. Existing records are never overwritten or deleted. */
suspend fun LibraryDatabase.restoreBackup(snapshot: LibraryBackupSnapshot) = withTransaction {
    snapshot.validate()
    val existingPages = pageDao().snapshotPages().map { it.id }.toSet()
    val existingBooks = bookDao().snapshotBooks().map { it.id }.toSet()
    val existingChapters = bookDao().snapshotChapters().map { it.id }.toSet()
    val existingSections = bookDao().snapshotSections().map { it.id }.toSet()
    val existingComponents = bookDao().snapshotComponents().map { it.id }.toSet()
    val existingJournals = journalDao().snapshotJournals().map { it.id }.toSet()
    val existingJournalEntryIds = journalDao().snapshotEntries().map { it.id }.toSet()
    val personalDao = personalLibraryDao()
    val existingCollections = personalDao.snapshotCollections().map { it.id }.toSet()
    val existingAnnotations = personalDao.snapshotAnnotations().map { it.id }.toSet()
    val existingLinks = personalDao.snapshotLinks().map { it.id }.toSet()
    val existingGlossary = personalDao.snapshotGlossary().map { it.id }.toSet()
    val existingRevisions = personalDao.snapshotRevisions().map { it.id }.toSet()
    val existingAttachments = personalDao.snapshotAttachments().map { it.id }.toSet()
    val existingPersonalEntries = personalDao.snapshotPersonalEntries().map { it.id }.toSet()
    require(snapshot.pages.none { it.id in existingPages } && snapshot.books.none { it.id in existingBooks } &&
        snapshot.chapters.none { it.id in existingChapters } && snapshot.sections.none { it.id in existingSections } &&
        snapshot.components.none { it.id in existingComponents } && snapshot.journals.none { it.id in existingJournals } &&
        snapshot.journalEntries.none { it.id in existingJournalEntryIds } && snapshot.collections.none { it.id in existingCollections } &&
        snapshot.annotations.none { it.id in existingAnnotations } && snapshot.links.none { it.id in existingLinks } &&
        snapshot.glossary.none { it.id in existingGlossary } && snapshot.revisions.none { it.id in existingRevisions } &&
        snapshot.attachments.none { it.id in existingAttachments } && snapshot.personalEntries.none { it.id in existingPersonalEntries }) {
        "This backup contains IDs that already exist here. Nothing was imported."
    }
    bookDao().insertBooks(snapshot.books)
    bookDao().insertChapters(snapshot.chapters)
    bookDao().insertSections(snapshot.sections)
    bookDao().insertComponents(snapshot.components)
    pageDao().insertPagesForRestore(snapshot.pages)
    journalDao().insertJournalRecords(snapshot.journals)
    journalDao().insertEntries(snapshot.journalEntries)
    personalDao.insertCollections(snapshot.collections)
    personalDao.insertCollectionItems(snapshot.collectionItems)
    personalDao.insertAnnotations(snapshot.annotations)
    personalDao.insertLinks(snapshot.links)
    personalDao.insertGlossaryTerms(snapshot.glossary)
    personalDao.insertRevisions(snapshot.revisions)
    personalDao.insertAttachments(snapshot.attachments)
    personalDao.insertPersonalEntries(snapshot.personalEntries)
}
