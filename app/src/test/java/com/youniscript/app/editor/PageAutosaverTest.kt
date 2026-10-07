package com.youniscript.app.editor

import com.youniscript.app.data.Page
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageAutosaverTest {
    private fun page(body: String) = Page("page-1", "Title", body, 1L, 1L)

    @Test
    fun editsAreDebouncedAndLatestSnapshotIsWritten() = runTest {
        val writes = mutableListOf<Page>()
        val states = mutableListOf<SaveState>()
        val autosaver = PageAutosaver(backgroundScope, { writes += it }, { states += it }, { testScheduler.currentTime })

        autosaver.schedule(page("first"))
        advanceTimeBy(200)
        autosaver.schedule(page("latest"))
        advanceTimeBy(349)
        runCurrent()
        assertTrue(writes.isEmpty())
        advanceTimeBy(1)
        runCurrent()

        assertEquals(listOf("latest"), writes.map { it.body })
        assertEquals(SaveState.Saved, states.last())
    }

    @Test
    fun flushWritesImmediately() = runTest {
        val writes = mutableListOf<Page>()
        val autosaver = PageAutosaver(backgroundScope, { writes += it }, {}, { testScheduler.currentTime })
        autosaver.schedule(page("draft"))

        val job = autosaver.flush()
        runCurrent()

        assertTrue(job?.isCompleted == true)
        assertEquals(listOf("draft"), writes.map { it.body })
    }

    @Test
    fun failureKeepsDraftAndCanBeRetried() = runTest {
        val writes = mutableListOf<Page>()
        val states = mutableListOf<SaveState>()
        var fail = true
        val autosaver = PageAutosaver(
            backgroundScope,
            { value -> if (fail) error("offline database failure") else writes += value },
            { states += it },
            { testScheduler.currentTime },
        )

        assertFalse(autosaver.saveNow(page("safe draft")))
        assertEquals(SaveState.Failed, states.last())
        fail = false
        assertTrue(autosaver.saveNow(page("safe draft")))
        assertEquals(listOf("safe draft"), writes.map { it.body })
        assertEquals(SaveState.Saved, states.last())
    }

    @Test
    fun continuousTypingStillWritesAtMaximumInterval() = runTest {
        val writes = mutableListOf<Page>()
        val autosaver = PageAutosaver(
            backgroundScope,
            { writes += it },
            {},
            { testScheduler.currentTime },
            debounceMillis = 350,
            maxIntervalMillis = 1_000,
        )

        autosaver.schedule(page("one"))
        advanceTimeBy(300)
        autosaver.schedule(page("two"))
        advanceTimeBy(300)
        autosaver.schedule(page("three"))
        advanceTimeBy(300)
        autosaver.schedule(page("four"))
        advanceTimeBy(100)
        runCurrent()

        assertEquals(listOf("four"), writes.map { it.body })
    }
}
