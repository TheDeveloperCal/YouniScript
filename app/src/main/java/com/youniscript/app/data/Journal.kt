package com.youniscript.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "journals", indices = [Index("updatedAt")])
data class Journal(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "journal_entries",
    indices = [Index("journalId"), Index(value = ["pageId"], unique = true), Index(value = ["journalId", "entryDate"])],
    foreignKeys = [
        ForeignKey(entity = Journal::class, parentColumns = ["id"], childColumns = ["journalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["pageId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class JournalEntry(
    @PrimaryKey val id: String,
    val journalId: String,
    val pageId: String,
    val entryDate: Long,
)

data class JournalEntryWithPage(
    @Embedded val page: Page,
    val entryDate: Long,
    val journalId: String,
)
