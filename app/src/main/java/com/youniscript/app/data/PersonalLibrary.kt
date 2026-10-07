package com.youniscript.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections", indices = [Index("updatedAt")])
data class CollectionRecord(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val icon: String = "book",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "contentType", "contentId"],
    indices = [Index(value = ["collectionId", "itemOrder"]), Index(value = ["contentType", "contentId"])],
    foreignKeys = [ForeignKey(entity = CollectionRecord::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE)],
)
data class CollectionItem(
    val collectionId: String,
    val contentType: String,
    val contentId: String,
    val itemOrder: Int,
)

@Entity(
    tableName = "annotations",
    indices = [Index(value = ["contentType", "contentId"]), Index("updatedAt")],
)
data class AnnotationRecord(
    @PrimaryKey val id: String,
    val contentType: String,
    val contentId: String,
    val kind: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "page_links",
    indices = [Index("sourcePageId"), Index("targetPageId"), Index(value = ["sourcePageId", "targetPageId"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["sourcePageId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["targetPageId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PageLink(
    @PrimaryKey val id: String,
    val sourcePageId: String,
    val targetPageId: String,
    val label: String,
    val createdAt: Long,
)

@Entity(tableName = "glossary_terms", indices = [Index(value = ["term"], unique = true)])
data class GlossaryTerm(
    @PrimaryKey val id: String,
    val term: String,
    val definition: String,
    val sourcePageId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "page_revisions", indices = [Index(value = ["pageId", "createdAt"])], foreignKeys = [ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["pageId"], onDelete = ForeignKey.CASCADE)])
data class PageRevision(
    @PrimaryKey val id: String,
    val pageId: String,
    val title: String,
    val body: String,
    val formatting: String,
    val pageStyleId: String?,
    val createdAt: Long,
)

@Entity(tableName = "media_attachments", indices = [Index("pageId")], foreignKeys = [ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["pageId"], onDelete = ForeignKey.CASCADE)])
data class MediaAttachment(
    @PrimaryKey val id: String,
    val pageId: String,
    val mediaType: String,
    val localUri: String,
    val displayName: String,
    val createdAt: Long,
)

@Entity(tableName = "personal_entries", indices = [Index("pageId", unique = true)], foreignKeys = [ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["pageId"], onDelete = ForeignKey.CASCADE)])
data class PersonalEntry(
    @PrimaryKey val id: String,
    val pageId: String,
    val kind: String,
    val author: String = "",
    val source: String = "",
    val recipient: String = "",
    val letterType: String = "general",
    val quoteDate: Long? = null,
    val writtenByMe: Boolean = false,
    val people: String = "",
    val places: String = "",
    val feelings: String = "",
    val details: String = "",
    val symbols: String = "",
    val reflection: String = "",
    val signature: String = "",
)
