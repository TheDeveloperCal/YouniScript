package com.youniscript.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalLibraryDao {
    @Query("SELECT * FROM collections ORDER BY updatedAt DESC") fun observeCollections(): Flow<List<CollectionRecord>>
    @Query("SELECT * FROM collections ORDER BY updatedAt DESC") suspend fun snapshotCollections(): List<CollectionRecord>
    @Query("SELECT * FROM collection_items ORDER BY collectionId, itemOrder") suspend fun snapshotCollectionItems(): List<CollectionItem>
    @Upsert suspend fun saveCollection(collection: CollectionRecord)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCollections(items: List<CollectionRecord>)
    @Query("DELETE FROM collections WHERE id = :id") suspend fun deleteCollection(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addCollectionItem(item: CollectionItem)
    @Upsert suspend fun saveCollectionItem(item: CollectionItem)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCollectionItems(items: List<CollectionItem>)
    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND contentType = :contentType AND contentId = :contentId") suspend fun removeCollectionItem(collectionId: String, contentType: String, contentId: String)
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY itemOrder") fun observeCollectionItems(collectionId: String): Flow<List<CollectionItem>>

    @Query("SELECT * FROM annotations ORDER BY updatedAt DESC") fun observeAnnotations(): Flow<List<AnnotationRecord>>
    @Query("SELECT * FROM annotations WHERE contentType = :contentType AND contentId = :contentId ORDER BY updatedAt DESC") fun observeAnnotationsFor(contentType: String, contentId: String): Flow<List<AnnotationRecord>>
    @Query("SELECT * FROM annotations ORDER BY updatedAt DESC") suspend fun snapshotAnnotations(): List<AnnotationRecord>
    @Upsert suspend fun saveAnnotation(annotation: AnnotationRecord)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAnnotations(items: List<AnnotationRecord>)
    @Query("DELETE FROM annotations WHERE id = :id") suspend fun deleteAnnotation(id: String)

    @Query("SELECT * FROM page_links WHERE sourcePageId = :pageId ORDER BY createdAt DESC") fun observeLinks(pageId: String): Flow<List<PageLink>>
    @Query("SELECT * FROM page_links WHERE targetPageId = :pageId ORDER BY createdAt DESC") fun observeBacklinks(pageId: String): Flow<List<PageLink>>
    @Query("SELECT * FROM page_links ORDER BY createdAt") suspend fun snapshotLinks(): List<PageLink>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addLink(link: PageLink)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertLinks(items: List<PageLink>)
    @Query("DELETE FROM page_links WHERE id = :id") suspend fun deleteLink(id: String)

    @Query("SELECT * FROM glossary_terms ORDER BY term COLLATE NOCASE") fun observeGlossary(): Flow<List<GlossaryTerm>>
    @Query("SELECT * FROM glossary_terms ORDER BY term COLLATE NOCASE") suspend fun snapshotGlossary(): List<GlossaryTerm>
    @Upsert suspend fun saveGlossaryTerm(term: GlossaryTerm)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGlossaryTerms(items: List<GlossaryTerm>)
    @Query("DELETE FROM glossary_terms WHERE id = :id") suspend fun deleteGlossaryTerm(id: String)

    @Query("SELECT * FROM page_revisions WHERE pageId = :pageId ORDER BY createdAt DESC") fun observeRevisions(pageId: String): Flow<List<PageRevision>>
    @Query("SELECT * FROM page_revisions WHERE pageId = :pageId ORDER BY createdAt DESC LIMIT 1") suspend fun latestRevision(pageId: String): PageRevision?
    @Query("SELECT * FROM page_revisions ORDER BY pageId, createdAt") suspend fun snapshotRevisions(): List<PageRevision>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRevision(revision: PageRevision)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRevisions(items: List<PageRevision>)
    @Query("DELETE FROM page_revisions WHERE id = :id") suspend fun deleteRevision(id: String)

    @Query("SELECT * FROM media_attachments WHERE pageId = :pageId ORDER BY createdAt") fun observeAttachments(pageId: String): Flow<List<MediaAttachment>>
    @Query("SELECT * FROM media_attachments ORDER BY createdAt") fun observeAllAttachments(): Flow<List<MediaAttachment>>
    @Query("SELECT * FROM media_attachments ORDER BY createdAt") suspend fun snapshotAttachments(): List<MediaAttachment>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAttachment(attachment: MediaAttachment)
    @Upsert suspend fun saveAttachment(attachment: MediaAttachment)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAttachments(items: List<MediaAttachment>)
    @Query("DELETE FROM media_attachments WHERE id = :id") suspend fun deleteAttachment(id: String)

    @Query("SELECT * FROM personal_entries WHERE pageId = :pageId LIMIT 1") suspend fun entryForPage(pageId: String): PersonalEntry?
    @Query("SELECT * FROM personal_entries WHERE pageId = :pageId LIMIT 1") fun observeEntryForPage(pageId: String): Flow<PersonalEntry?>
    @Query("SELECT * FROM personal_entries ORDER BY pageId") suspend fun snapshotPersonalEntries(): List<PersonalEntry>
    @Upsert suspend fun savePersonalEntry(entry: PersonalEntry)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPersonalEntries(items: List<PersonalEntry>)
    @Query("DELETE FROM personal_entries WHERE pageId = :pageId") suspend fun deletePersonalEntry(pageId: String)

    @Transaction
    suspend fun saveRevisionIfChanged(page: Page, timestamp: Long) {
        val latest = latestRevision(page.id)
        if (latest?.let { it.title == page.title && it.body == page.body && it.formatting == page.formatting && it.pageStyleId == page.pageStyleId } == true) return
        insertRevision(PageRevision(java.util.UUID.randomUUID().toString(), page.id, page.title, page.body, page.formatting, page.pageStyleId, timestamp))
    }
}
