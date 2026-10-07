package com.youniscript.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages", indices = [androidx.room.Index("bookId"), androidx.room.Index("chapterId"), androidx.room.Index("sectionId"), androidx.room.Index(value = ["chapterId", "bookOrder"]), androidx.room.Index(value = ["isTrashed", "trashedAt"])])
data class Page(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val formatting: String = "",
    val pageStyleId: String? = null,
    val tags: String = "[]",
    val collectionId: String? = null,
    val bookId: String? = null,
    val chapterId: String? = null,
    val sectionId: String? = null,
    val bookOrder: Int? = null,
    val isBookmarked: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
)
