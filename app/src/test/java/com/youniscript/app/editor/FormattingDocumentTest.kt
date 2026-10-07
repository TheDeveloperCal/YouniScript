package com.youniscript.app.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingDocumentTest {
    @Test
    fun selectionFormattingCanBeAppliedAndRemoved() {
        val document = FormattingDocument("write what is yours")
            .toggleMark(0, 5, MARK_BOLD)

        assertEquals(listOf(InlineRun(0, 5, MARK_BOLD)), document.inlineRuns)
        assertEquals(emptyList<InlineRun>(), document.toggleMark(0, 5, MARK_BOLD).inlineRuns)
    }

    @Test
    fun insertionRetainsStylesBeforeAndAfterChangedText() {
        val original = FormattingDocument("bold tail", listOf(InlineRun(0, 4, MARK_BOLD)))
        val edited = original.edit("bold new tail")

        assertEquals("bold new tail", edited.text)
        assertEquals(listOf(InlineRun(0, 4, MARK_BOLD)), edited.inlineRuns)
    }

    @Test
    fun formattingUsesStablePageOffsetsAcrossParagraphs() {
        val original = FormattingDocument("First\nSecond")
            .setParagraphStyle(6, 12, kind = BLOCK_HEADING, alignment = ALIGN_CENTER)
        val edited = original.edit("Intro\nFirst\nSecond")

        assertEquals(ParagraphRun(12, 18, BLOCK_HEADING, ALIGN_CENTER), edited.paragraphs.single())
    }
}
