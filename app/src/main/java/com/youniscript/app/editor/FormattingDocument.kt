package com.youniscript.app.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

const val MARK_BOLD = 1
const val MARK_ITALIC = 2
const val MARK_UNDERLINE = 4
const val MARK_STRIKE = 8

const val BLOCK_NORMAL = 0
const val BLOCK_HEADING = 1
const val BLOCK_SUBHEADING = 2
const val BLOCK_QUOTE = 3

const val ALIGN_START = 0
const val ALIGN_CENTER = 1
const val ALIGN_END = 2
const val ALIGN_JUSTIFY = 3

data class InlineRun(val start: Int, val end: Int, val marks: Int)
data class ParagraphRun(val start: Int, val end: Int, val kind: Int, val alignment: Int)

data class FormattingDocument(
    val text: String,
    val inlineRuns: List<InlineRun> = emptyList(),
    val paragraphs: List<ParagraphRun> = emptyList(),
) {
    fun toJson(): String {
        if (inlineRuns.isEmpty() && paragraphs.isEmpty()) return ""
        val root = JSONObject()
        root.put("v", 1)
        val inline = JSONArray()
        inlineRuns.forEach { run ->
            inline.put(JSONObject().put("s", run.start).put("e", run.end).put("m", run.marks))
        }
        val blocks = JSONArray()
        paragraphs.forEach { run ->
            blocks.put(JSONObject().put("s", run.start).put("e", run.end).put("k", run.kind).put("a", run.alignment))
        }
        root.put("inline", inline).put("paragraphs", blocks)
        return root.toString()
    }

    fun asAnnotatedString(
        headingSizeSp: Int = 26,
        subheadingSizeSp: Int = 21,
        decorativeInitial: Boolean = false,
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        if (decorativeInitial && text.isNotEmpty()) {
            builder.addStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold), 0, 1)
        }
        inlineRuns.forEach { run ->
            val start = run.start.coerceIn(0, text.length)
            val end = run.end.coerceIn(start, text.length)
            if (start == end) return@forEach
            val style = SpanStyle(
                fontWeight = if (run.marks and MARK_BOLD != 0) FontWeight.Bold else null,
                fontStyle = if (run.marks and MARK_ITALIC != 0) FontStyle.Italic else null,
                textDecoration = when {
                    run.marks and (MARK_UNDERLINE or MARK_STRIKE) == (MARK_UNDERLINE or MARK_STRIKE) -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                    )
                    run.marks and MARK_UNDERLINE != 0 -> TextDecoration.Underline
                    run.marks and MARK_STRIKE != 0 -> TextDecoration.LineThrough
                    else -> null
                },
            )
            builder.addStyle(style, start, end)
        }
        paragraphs.forEach { run ->
            val start = run.start.coerceIn(0, text.length)
            val end = run.end.coerceIn(start, text.length)
            if (start == end) return@forEach
            val alignment = when (run.alignment) {
                ALIGN_CENTER -> TextAlign.Center
                ALIGN_END -> TextAlign.End
                ALIGN_JUSTIFY -> TextAlign.Justify
                else -> TextAlign.Start
            }
            val indent = if (run.kind == BLOCK_QUOTE) TextIndent(firstLine = 14.sp, restLine = 14.sp) else TextIndent.None
            builder.addStyle(ParagraphStyle(textAlign = alignment, textIndent = indent), start, end)
            val span = when (run.kind) {
                BLOCK_HEADING -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = headingSizeSp.sp)
                BLOCK_SUBHEADING -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = subheadingSizeSp.sp)
                BLOCK_QUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
                else -> null
            }
            if (span != null) builder.addStyle(span, start, end)
        }
        return builder.toAnnotatedString()
    }

    fun toggleMark(start: Int, end: Int, mark: Int): FormattingDocument {
        val rangeStart = start.coerceIn(0, text.length)
        val rangeEnd = end.coerceIn(rangeStart, text.length)
        if (rangeStart == rangeEnd) return this
        val masks = IntArray(text.length)
        inlineRuns.forEach { run ->
            for (i in run.start.coerceAtLeast(0)..<run.end.coerceAtMost(text.length)) masks[i] = run.marks
        }
        val remove = (rangeStart..<rangeEnd).all { masks[it] and mark != 0 }
        for (i in rangeStart..<rangeEnd) {
            masks[i] = if (remove) masks[i] and mark.inv() else masks[i] or mark
        }
        return copy(inlineRuns = compress(masks))
    }

    fun setParagraphStyle(start: Int, end: Int, kind: Int? = null, alignment: Int? = null): FormattingDocument {
        val selected = paragraphRanges(text, start, end)
        if (selected.isEmpty()) return this
        val styles = paragraphs.associateBy { it.start }.toMutableMap()
        selected.forEach { (paragraphStart, paragraphEnd) ->
            val previous = styles[paragraphStart] ?: ParagraphRun(paragraphStart, paragraphEnd, BLOCK_NORMAL, ALIGN_START)
            val updated = previous.copy(
                end = paragraphEnd,
                kind = kind ?: previous.kind,
                alignment = alignment ?: previous.alignment,
            )
            if (updated.kind == BLOCK_NORMAL && updated.alignment == ALIGN_START) styles.remove(paragraphStart)
            else styles[paragraphStart] = updated
        }
        return copy(paragraphs = styles.values.sortedBy { it.start })
    }

    /** Reattaches styles around the smallest changed text region, including multiline edits. */
    fun edit(newText: String): FormattingDocument {
        if (newText == text) return this
        var prefix = 0
        while (prefix < text.length && prefix < newText.length && text[prefix] == newText[prefix]) prefix++
        var suffix = 0
        while (suffix < text.length - prefix && suffix < newText.length - prefix &&
            text[text.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) suffix++
        val oldEnd = text.length - suffix
        val newEnd = newText.length - suffix
        val delta = newText.length - text.length

        fun marksAt(position: Int): Int = inlineRuns.firstOrNull { position >= it.start && position < it.end }?.marks ?: 0
        val inheritedMarks = when {
            prefix > 0 -> marksAt(prefix - 1)
            text.isNotEmpty() -> marksAt(prefix.coerceAtMost(text.lastIndex))
            else -> 0
        }
        val masks = IntArray(newText.length)
        for (i in newText.indices) {
            val oldIndex = when {
                i < prefix -> i
                i >= newEnd -> i - delta
                else -> -1
            }
            masks[i] = if (oldIndex >= 0) marksAt(oldIndex) else inheritedMarks
        }

        fun blockAt(position: Int): ParagraphRun? = paragraphs.firstOrNull { position >= it.start && position < it.end }
        val newParagraphs = paragraphRanges(newText, 0, newText.length).mapNotNull { (start, end) ->
            val source = when {
                start < prefix -> start
                start >= newEnd -> start - delta
                else -> prefix.coerceAtMost(text.length)
            }
            val block = blockAt(source) ?: return@mapNotNull null
            block.copy(start = start, end = end)
        }
        return FormattingDocument(newText, compress(masks), newParagraphs)
    }

    companion object {
        fun decode(text: String, json: String): FormattingDocument {
            if (json.isBlank()) return FormattingDocument(text)
            return runCatching {
                val root = JSONObject(json)
                if (root.optInt("v", 1) > 1) return FormattingDocument(text)
                val inline = buildList {
                    val array = root.optJSONArray("inline") ?: JSONArray()
                    for (i in 0 until array.length()) {
                        val value = array.getJSONObject(i)
                        val start = value.optInt("s").coerceIn(0, text.length)
                        val end = value.optInt("e").coerceIn(start, text.length)
                        val marks = value.optInt("m") and (MARK_BOLD or MARK_ITALIC or MARK_UNDERLINE or MARK_STRIKE)
                        if (start < end && marks != 0) add(InlineRun(start, end, marks))
                    }
                }
                val blocks = buildList {
                    val array = root.optJSONArray("paragraphs") ?: JSONArray()
                    for (i in 0 until array.length()) {
                        val value = array.getJSONObject(i)
                        val start = value.optInt("s").coerceIn(0, text.length)
                        val end = value.optInt("e").coerceIn(start, text.length)
                        val kind = value.optInt("k").coerceIn(BLOCK_NORMAL, BLOCK_QUOTE)
                        val alignment = value.optInt("a").coerceIn(ALIGN_START, ALIGN_JUSTIFY)
                        if (start < end && (kind != BLOCK_NORMAL || alignment != ALIGN_START)) {
                            add(ParagraphRun(start, end, kind, alignment))
                        }
                    }
                }
                FormattingDocument(text, inline, blocks)
            }.getOrDefault(FormattingDocument(text))
        }

        private fun compress(masks: IntArray): List<InlineRun> {
            val runs = mutableListOf<InlineRun>()
            var start = 0
            while (start < masks.size) {
                val mark = masks[start]
                var end = start + 1
                while (end < masks.size && masks[end] == mark) end++
                if (mark != 0) runs += InlineRun(start, end, mark)
                start = end
            }
            return runs
        }

        fun paragraphRanges(text: String, selectionStart: Int, selectionEnd: Int): List<Pair<Int, Int>> {
            if (text.isEmpty()) return emptyList()
            val left = selectionStart.coerceIn(0, text.length)
            val right = selectionEnd.coerceIn(left, text.length)
            val lastSelected = if (left == right) left else right - 1
            val result = mutableListOf<Pair<Int, Int>>()
            var start = 0
            while (start < text.length) {
                val newline = text.indexOf('\n', start)
                val end = if (newline < 0) text.length else newline + 1
                val includes = if (left == right) {
                    (left >= start && left < end) || (left == text.length && end == text.length)
                } else {
                    lastSelected >= start && left < end
                }
                if (includes) result += start to end
                start = end
            }
            return result
        }
    }
}
