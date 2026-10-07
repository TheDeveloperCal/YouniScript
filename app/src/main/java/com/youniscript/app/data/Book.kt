package com.youniscript.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String = "",
    val author: String = "",
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val coverTemplateId: String = "minimal",
    val coverImageUri: String? = null,
    val defaultPageStyleId: String = "modern-paper",
    val status: String = "draft",
    val bookOrder: Int = 0,
)

@Entity(
    tableName = "chapters",
    indices = [androidx.room.Index("bookId"), androidx.room.Index(value = ["bookId", "chapterOrder"], unique = true)],
    foreignKeys = [androidx.room.ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = androidx.room.ForeignKey.CASCADE,
    )],
)
data class Chapter(
    @PrimaryKey val id: String,
    val bookId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val chapterOrder: Int,
    val styleOverrideId: String? = null,
)

@Entity(
    tableName = "sections",
    indices = [androidx.room.Index("bookId"), androidx.room.Index("chapterId"), androidx.room.Index(value = ["chapterId", "sectionOrder"], unique = true)],
    foreignKeys = [androidx.room.ForeignKey(
        entity = Chapter::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = androidx.room.ForeignKey.CASCADE,
    )],
)
data class Section(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val title: String,
    val sectionOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "book_components",
    indices = [androidx.room.Index("bookId"), androidx.room.Index(value = ["bookId", "componentOrder"], unique = true)],
    foreignKeys = [androidx.room.ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = androidx.room.ForeignKey.CASCADE,
    )],
)
data class BookComponent(
    @PrimaryKey val id: String,
    val bookId: String,
    /** title-page, dedication, preface, introduction, appendix, notes, bibliography, about-author, or closing */
    val type: String,
    val title: String,
    val body: String = "",
    val componentOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
