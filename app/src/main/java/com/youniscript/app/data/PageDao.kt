package com.youniscript.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): Page?

    @Query("SELECT * FROM pages ORDER BY updatedAt DESC")
    suspend fun snapshotPages(): List<Page>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPagesForRestore(pages: List<Page>)

    @Upsert
    suspend fun save(page: Page)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pages SET isTrashed = 1, trashedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long)

    @Query("UPDATE pages SET isTrashed = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM pages WHERE isTrashed = 1")
    suspend fun emptyTrash()
}
