package com.youniscript.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Page::class, Book::class, Chapter::class, Section::class, BookComponent::class, Journal::class, JournalEntry::class, CollectionRecord::class, CollectionItem::class, AnnotationRecord::class, PageLink::class, GlossaryTerm::class, PageRevision::class, MediaAttachment::class, PersonalEntry::class], version = 8, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun pageDao(): PageDao
    abstract fun bookDao(): BookDao
    abstract fun journalDao(): JournalDao
    abstract fun personalLibraryDao(): PersonalLibraryDao

    companion object {
        const val DATABASE_NAME = "youniscript.db"
        @Volatile private var instance: LibraryDatabase? = null

        fun get(context: Context): LibraryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LibraryDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(LibraryMigrations.MIGRATION_1_2, LibraryMigrations.MIGRATION_2_3, LibraryMigrations.MIGRATION_3_4, LibraryMigrations.MIGRATION_4_5, LibraryMigrations.MIGRATION_5_6, LibraryMigrations.MIGRATION_6_7, LibraryMigrations.MIGRATION_7_8).build().also { instance = it }
        }
    }
}
