package com.youniscript.app.data

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import com.youniscript.app.editor.FormattingDocument
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BookExportSnapshot(
    val book: Book,
    val chapters: List<Chapter>,
    val pages: List<Page>,
    val sections: List<Section>,
    val components: List<BookComponent>,
)

private data class ExportSection(val title: String, val body: String, val kind: Kind) {
    enum class Kind { TITLE, FRONT, CONTENTS, CHAPTER, PAGE }
}

private fun BookExportSnapshot.sectionsForExport(): List<ExportSection> = buildList {
    add(ExportSection(book.title.ifBlank { "Untitled book" }, listOf(book.subtitle, book.author.takeIf(String::isNotBlank)?.let { "by $it" }.orEmpty()).filter(String::isNotBlank).joinToString("\n\n"), ExportSection.Kind.TITLE))
    components.sortedBy { it.componentOrder }.forEach { add(ExportSection(it.title, it.body, ExportSection.Kind.FRONT)) }
    val orderedChapters = chapters.sortedBy { it.chapterOrder }
    add(ExportSection("Contents", orderedChapters.mapIndexed { i, chapter -> "${i + 1}. ${chapter.title}" }.joinToString("\n"), ExportSection.Kind.CONTENTS))
    orderedChapters.forEachIndexed { chapterIndex, chapter ->
        add(ExportSection("Chapter ${chapterIndex + 1} · ${chapter.title}", "", ExportSection.Kind.CHAPTER))
        val chapterSections = sections.filter { it.chapterId == chapter.id }.sortedBy { it.sectionOrder }
        pages.filter { it.chapterId == chapter.id }
            .sortedWith(compareBy<Page> { p -> if (p.sectionId == null) -1 else chapterSections.indexOfFirst { it.id == p.sectionId } }.thenBy { it.bookOrder ?: Int.MAX_VALUE })
            .forEach { page ->
                val sectionTitle = chapterSections.firstOrNull { it.id == page.sectionId }?.title
                val title = page.title.ifBlank { sectionTitle.orEmpty() }
                val body = FormattingDocument.decode(page.body, page.formatting).text
                add(ExportSection(title, body, ExportSection.Kind.PAGE))
            }
    }
}

/** Writes a paginated, searchable PDF using Android's built-in PDF renderer. */
fun BookExportSnapshot.writePdf(output: OutputStream) {
    val document = PdfDocument()
    val pageWidth = 612
    val pageHeight = 792
    val margin = 54f
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(35, 48, 38); typeface = Typeface.create("serif", Typeface.BOLD); textSize = 28f }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(48, 57, 45); typeface = Typeface.create("serif", Typeface.BOLD); textSize = 20f }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(43, 42, 36); typeface = Typeface.create("serif", Typeface.NORMAL); textSize = 12f }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(104, 111, 96); textSize = 9f }
    fun beginPage() {
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        page!!.canvas.drawColor(android.graphics.Color.rgb(250, 247, 238))
        y = margin
    }
    fun finishPage() {
        val current = page ?: return
        current.canvas.drawText(pageNumber.toString(), pageWidth / 2f, pageHeight - 25f, footerPaint.apply { textAlign = Paint.Align.CENTER })
        footerPaint.textAlign = Paint.Align.LEFT
        document.finishPage(current)
        page = null
    }
    fun ensureRoom(height: Float) {
        if (page == null) beginPage()
        if (y + height > pageHeight - margin) { finishPage(); beginPage() }
    }
    fun drawWrapped(text: String, paint: Paint, width: Float, lineHeight: Float) {
        text.split('\n').forEach { paragraph ->
            if (paragraph.isBlank()) { y += lineHeight * 0.7f; return@forEach }
            var line = ""
            paragraph.split(Regex("\\s+")).forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && paint.measureText(candidate) > width) {
                    ensureRoom(lineHeight)
                    page!!.canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                    line = word
                } else line = candidate
            }
            if (line.isNotEmpty()) { ensureRoom(lineHeight); page!!.canvas.drawText(line, margin, y, paint); y += lineHeight }
        }
    }
    try {
        beginPage()
        page!!.canvas.drawColor(android.graphics.Color.rgb(25, 48, 34))
        book.coverImageUri?.let { uriString ->
            val bitmap = runCatching {
                val uri = Uri.parse(uriString)
                val stream = if (uri.scheme == "file") uri.path?.let(::FileInputStream) else null
                stream?.use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (bitmap != null) {
                val maxWidth = 240f
                val maxHeight = 300f
                val factor = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
                val width = bitmap.width * factor
                val height = bitmap.height * factor
                page!!.canvas.drawBitmap(bitmap, null, android.graphics.RectF((pageWidth - width) / 2, 100f, (pageWidth + width) / 2, 100f + height), Paint(Paint.ANTI_ALIAS_FLAG))
                bitmap.recycle()
            }
        }
        val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(247, 241, 224); textAlign = Paint.Align.CENTER; typeface = Typeface.create("serif", Typeface.BOLD); textSize = 31f }
        page!!.canvas.drawText(book.title.ifBlank { "Untitled book" }.take(36), pageWidth / 2f, pageHeight * .43f, coverPaint)
        coverPaint.textSize = 15f; coverPaint.typeface = Typeface.create("serif", Typeface.NORMAL)
        if (book.subtitle.isNotBlank()) page!!.canvas.drawText(book.subtitle.take(52), pageWidth / 2f, pageHeight * .49f, coverPaint)
        if (book.author.isNotBlank()) page!!.canvas.drawText(book.author.take(48), pageWidth / 2f, pageHeight * .62f, coverPaint)
        finishPage()
        sectionsForExport().drop(1).forEach { section ->
            if (section.kind == ExportSection.Kind.CHAPTER || section.kind == ExportSection.Kind.FRONT || section.kind == ExportSection.Kind.CONTENTS) {
                if (page != null && y > margin) finishPage()
                beginPage()
                drawWrapped(section.title, headingPaint, pageWidth - 2 * margin, 28f)
                y += 12f
            } else {
                if (section.title.isNotBlank()) { ensureRoom(30f); drawWrapped(section.title, headingPaint, pageWidth - 2 * margin, 26f); y += 6f }
                drawWrapped(section.body, bodyPaint, pageWidth - 2 * margin, 17f)
                y += 18f
            }
        }
        if (page != null) finishPage()
        document.writeTo(output)
    } finally { document.close() }
}

/** Writes a dependency-free EPUB 3 archive with ordered front matter, navigation, and chapters. */
fun BookExportSnapshot.writeEpub(output: OutputStream) {
    val sections = sectionsForExport()
    val chapterSections = sections.filter { it.kind == ExportSection.Kind.CHAPTER }
    val chapterFiles = chapterSections.indices.map { "chapter-${it + 1}.xhtml" }
    val title = xml(book.title.ifBlank { "Untitled book" })
    val entries = linkedMapOf<String, ByteArray>()
    entries["mimetype"] = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
    entries["META-INF/container.xml"] = """<?xml version="1.0" encoding="UTF-8"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/package.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""".toByteArray()
    entries["OEBPS/styles.css"] = "body{font-family:serif;line-height:1.65;margin:6%;color:#292820}h1,h2{text-align:center;line-height:1.25}section{break-after:page}.frontmatter{font-style:normal}.page{margin:0 0 2em}.page-title{font-size:1.15em;font-weight:bold}".toByteArray()
    val contentNames = mutableListOf("title.xhtml")
    sections.filter { it.kind == ExportSection.Kind.FRONT || it.kind == ExportSection.Kind.CONTENTS }.forEachIndexed { index, section ->
        val filename = if (section.kind == ExportSection.Kind.CONTENTS) "contents.xhtml" else "front-${index + 1}.xhtml"
        contentNames += filename
        entries["OEBPS/$filename"] = xhtml(section.title, section.body, "frontmatter")
    }
    entries["OEBPS/title.xhtml"] = xhtml(book.title, listOf(book.subtitle, book.author).filter(String::isNotBlank).joinToString("\n\n"), "title-page")
    var coverHref: String? = null
    book.coverImageUri?.let { uriString ->
        val bytes = runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") uri.path?.let(::File)?.readBytes() else null
        }.getOrNull()
        if (bytes != null) {
            val mime = when {
                bytes.size > 3 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
                bytes.size > 12 && bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) && bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> "image/webp"
                bytes.size > 5 && bytes.copyOfRange(0, 3).contentEquals("GIF".toByteArray()) -> "image/gif"
                else -> "image/jpeg"
            }
            val filename = when (mime) { "image/png" -> "cover.png"; "image/webp" -> "cover.webp"; "image/gif" -> "cover.gif"; else -> "cover.jpg" }
            entries["OEBPS/$filename"] = bytes
            coverHref = filename
            entries["OEBPS/cover.xhtml"] = """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>Cover</title><meta name="viewport" content="width=device-width, height=device-height"/><link rel="stylesheet" href="styles.css"/></head><body><section><img src="$filename" alt="${xml(book.title)}" style="display:block;max-width:100%;max-height:90vh;margin:auto"/></section></body></html>""".toByteArray()
            contentNames.add(0, "cover.xhtml")
        }
    }
    chapterSections.forEachIndexed { index, chapter ->
        val nextChapter = chapterSections.getOrNull(index + 1)
        val start = sections.indexOf(chapter) + 1
        val end = if (nextChapter == null) sections.size else sections.indexOf(nextChapter)
        val body = buildString {
            append("<h1>").append(xml(chapter.title.substringAfter(" · "))).append("</h1>")
            sections.subList(start, end).filter { it.kind == ExportSection.Kind.PAGE }.forEach { page ->
                append("<section class=\"page\">")
                if (page.title.isNotBlank()) append("<h2 class=\"page-title\">").append(xml(page.title)).append("</h2>")
                page.body.split("\n\n").filter(String::isNotBlank).forEach { append("<p>").append(xml(it).replace("\n", "<br/>" )).append("</p>") }
                append("</section>")
            }
        }
        contentNames += chapterFiles[index]
        entries["OEBPS/${chapterFiles[index]}"] = xhtmlRaw(chapter.title, body)
    }
    val navItems = buildString {
        append("<li><a href=\"title.xhtml\">Title</a></li>")
        sections.filter { it.kind == ExportSection.Kind.FRONT || it.kind == ExportSection.Kind.CONTENTS }.forEachIndexed { index, section ->
            val filename = if (section.kind == ExportSection.Kind.CONTENTS) "contents.xhtml" else "front-${index + 1}.xhtml"
            append("<li><a href=\"").append(filename).append("\">").append(xml(section.title)).append("</a></li>")
        }
        chapterSections.forEachIndexed { index, chapter -> append("<li><a href=\"").append(chapterFiles[index]).append("\">").append(xml(chapter.title.substringAfter(" · "))).append("</a></li>") }
    }
    entries["OEBPS/nav.xhtml"] = """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title><link rel="stylesheet" href="styles.css"/></head><body><nav epub:type="toc"><h1>Contents</h1><ol>$navItems</ol></nav></body></html>""".toByteArray()
    val manifest = buildString {
        append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")
        append("<item id=\"css\" href=\"styles.css\" media-type=\"text/css\"/>")
        if (coverHref != null) {
            val mime = when (coverHref.substringAfterLast('.')) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; else -> "image/jpeg" }
            append("<item id=\"cover-image\" href=\"").append(coverHref).append("\" media-type=\"").append(mime).append("\" properties=\"cover-image\"/>")
            append("<item id=\"cover-page\" href=\"cover.xhtml\" media-type=\"application/xhtml+xml\"/>")
        }
        contentNames.forEachIndexed { index, filename -> append("<item id=\"c$index\" href=\"").append(filename).append("\" media-type=\"application/xhtml+xml\"/>") }
    }
    val spine = contentNames.mapIndexed { index, _ -> "<itemref idref=\"c$index\"/>" }.joinToString("")
    val coverMeta = if (coverHref == null) "" else "<meta name=\"cover\" content=\"cover-image\"/>"
    val epubSpine = if (coverHref == null) spine else "<itemref idref=\"cover-page\" linear=\"no\"/>$spine"
    entries["OEBPS/package.opf"] = """<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id" xml:lang="en"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="book-id">${xml(book.id)}</dc:identifier><dc:title>$title</dc:title><dc:creator>${xml(book.author)}</dc:creator><dc:language>en</dc:language>$coverMeta</metadata><manifest>$manifest</manifest><spine>$epubSpine</spine></package>""".toByteArray()
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            val entry = ZipEntry(name)
            if (name == "mimetype") entry.method = ZipEntry.STORED.also { entry.size = bytes.size.toLong(); entry.compressedSize = bytes.size.toLong(); entry.crc = java.util.zip.CRC32().apply { update(bytes) }.value }
            zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry()
        }
    }
}

private fun xhtml(title: String, body: String, cssClass: String): ByteArray = xhtmlRaw(title, body.split("\n\n").filter(String::isNotBlank).joinToString("") { "<p>${xml(it).replace("\n", "<br/>")}</p>" }, cssClass)

private fun xhtmlRaw(title: String, body: String, cssClass: String = "chapter"): ByteArray = """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(title)}</title><link rel="stylesheet" href="styles.css"/></head><body class="$cssClass"><h1>${xml(title)}</h1>$body</body></html>""".toByteArray()

private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
