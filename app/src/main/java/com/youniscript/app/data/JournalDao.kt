package com.youniscript.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals ORDER BY isArchived, updatedAt DESC")
    fun observeJournals(): Flow<List<Journal>>

    @Query("SELECT * FROM journals WHERE id = :id LIMIT 1")
    suspend fun findJournal(id: String): Journal?

    @Query("SELECT pages.*, journal_entries.entryDate, journal_entries.journalId FROM pages INNER JOIN journal_entries ON pages.id = journal_entries.pageId WHERE journal_entries.journalId = :journalId AND pages.isTrashed = 0 ORDER BY journal_entries.entryDate DESC")
    fun observeEntries(journalId: String): Flow<List<JournalEntryWithPage>>

    @Query("SELECT pages.*, journal_entries.entryDate, journal_entries.journalId FROM pages INNER JOIN journal_entries ON pages.id = journal_entries.pageId WHERE pages.isTrashed = 0 ORDER BY journal_entries.entryDate DESC")
    fun observeAllEntries(): Flow<List<JournalEntryWithPage>>

    @Query("SELECT * FROM journals ORDER BY updatedAt DESC")
    suspend fun snapshotJournals(): List<Journal>

    @Query("SELECT * FROM journal_entries ORDER BY entryDate")
    suspend fun snapshotEntries(): List<JournalEntry>

    @Upsert
    suspend fun saveJournal(journal: Journal)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournal(journal: Journal)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalRecords(journals: List<Journal>)

    @Upsert
    suspend fun saveEntry(entry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<JournalEntry>)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: String)
}
