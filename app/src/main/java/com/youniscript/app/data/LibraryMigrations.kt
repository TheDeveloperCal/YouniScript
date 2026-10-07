package com.youniscript.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object LibraryMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pages ADD COLUMN formatting TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE pages ADD COLUMN pageStyleId TEXT")
            database.execSQL("ALTER TABLE pages ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE pages ADD COLUMN collectionId TEXT")
            database.execSQL("ALTER TABLE pages ADD COLUMN bookId TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pages ADD COLUMN chapterId TEXT")
            database.execSQL("ALTER TABLE pages ADD COLUMN bookOrder INTEGER")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pages_bookId ON pages(bookId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pages_chapterId ON pages(chapterId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pages_chapterId_bookOrder ON pages(chapterId, bookOrder)")
            database.execSQL("CREATE TABLE IF NOT EXISTS books (id TEXT NOT NULL, title TEXT NOT NULL, subtitle TEXT NOT NULL, author TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, coverTemplateId TEXT NOT NULL, coverImageUri TEXT, defaultPageStyleId TEXT NOT NULL, status TEXT NOT NULL, bookOrder INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS chapters (id TEXT NOT NULL, bookId TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, chapterOrder INTEGER NOT NULL, styleOverrideId TEXT, PRIMARY KEY(id), FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_bookId ON chapters(bookId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_chapters_bookId_chapterOrder ON chapters(bookId, chapterOrder)")
            database.execSQL("CREATE TABLE IF NOT EXISTS book_components (id TEXT NOT NULL, bookId TEXT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, componentOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_book_components_bookId ON book_components(bookId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_book_components_bookId_componentOrder ON book_components(bookId, componentOrder)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pages ADD COLUMN sectionId TEXT")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pages_sectionId ON pages(sectionId)")
            database.execSQL("CREATE TABLE IF NOT EXISTS sections (id TEXT NOT NULL, bookId TEXT NOT NULL, chapterId TEXT NOT NULL, title TEXT NOT NULL, sectionOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(chapterId) REFERENCES chapters(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sections_bookId ON sections(bookId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sections_chapterId ON sections(chapterId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sections_chapterId_sectionOrder ON sections(chapterId, sectionOrder)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pages ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS journals (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isArchived INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_journals_updatedAt ON journals(updatedAt)")
            database.execSQL("CREATE TABLE IF NOT EXISTS journal_entries (id TEXT NOT NULL, journalId TEXT NOT NULL, pageId TEXT NOT NULL, entryDate INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(journalId) REFERENCES journals(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(pageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_journalId ON journal_entries(journalId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_pageId ON journal_entries(pageId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_journalId_entryDate ON journal_entries(journalId, entryDate)")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pages ADD COLUMN isTrashed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE pages ADD COLUMN trashedAt INTEGER")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pages_isTrashed_trashedAt ON pages(isTrashed, trashedAt)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, icon TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_collections_updatedAt ON collections(updatedAt)")
            database.execSQL("CREATE TABLE IF NOT EXISTS collection_items (collectionId TEXT NOT NULL, contentType TEXT NOT NULL, contentId TEXT NOT NULL, itemOrder INTEGER NOT NULL, PRIMARY KEY(collectionId, contentType, contentId), FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_collectionId_itemOrder ON collection_items(collectionId, itemOrder)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_contentType_contentId ON collection_items(contentType, contentId)")
            database.execSQL("CREATE TABLE IF NOT EXISTS annotations (id TEXT NOT NULL, contentType TEXT NOT NULL, contentId TEXT NOT NULL, kind TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_annotations_contentType_contentId ON annotations(contentType, contentId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_annotations_updatedAt ON annotations(updatedAt)")
            database.execSQL("CREATE TABLE IF NOT EXISTS page_links (id TEXT NOT NULL, sourcePageId TEXT NOT NULL, targetPageId TEXT NOT NULL, label TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(sourcePageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(targetPageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_page_links_sourcePageId ON page_links(sourcePageId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_page_links_targetPageId ON page_links(targetPageId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_page_links_sourcePageId_targetPageId ON page_links(sourcePageId, targetPageId)")
            database.execSQL("CREATE TABLE IF NOT EXISTS glossary_terms (id TEXT NOT NULL, term TEXT NOT NULL, definition TEXT NOT NULL, sourcePageId TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_glossary_terms_term ON glossary_terms(term)")
            database.execSQL("CREATE TABLE IF NOT EXISTS page_revisions (id TEXT NOT NULL, pageId TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, formatting TEXT NOT NULL, pageStyleId TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(pageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_page_revisions_pageId_createdAt ON page_revisions(pageId, createdAt)")
            database.execSQL("CREATE TABLE IF NOT EXISTS media_attachments (id TEXT NOT NULL, pageId TEXT NOT NULL, mediaType TEXT NOT NULL, localUri TEXT NOT NULL, displayName TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(pageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_media_attachments_pageId ON media_attachments(pageId)")
            database.execSQL("CREATE TABLE IF NOT EXISTS personal_entries (id TEXT NOT NULL, pageId TEXT NOT NULL, kind TEXT NOT NULL, author TEXT NOT NULL, source TEXT NOT NULL, recipient TEXT NOT NULL, letterType TEXT NOT NULL, quoteDate INTEGER, writtenByMe INTEGER NOT NULL, people TEXT NOT NULL, places TEXT NOT NULL, feelings TEXT NOT NULL, details TEXT NOT NULL, symbols TEXT NOT NULL, reflection TEXT NOT NULL, signature TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(pageId) REFERENCES pages(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_personal_entries_pageId ON personal_entries(pageId)")
        }
    }
}
