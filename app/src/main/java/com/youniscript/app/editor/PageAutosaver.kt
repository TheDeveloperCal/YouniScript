package com.youniscript.app.editor

import com.youniscript.app.data.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SaveState { Saved, Saving, Failed }

/** Debounces edits, writes periodically during long sessions, and keeps failed drafts retryable. */
class PageAutosaver(
    private val scope: CoroutineScope,
    private val writePage: suspend (Page) -> Unit,
    private val onStateChanged: (SaveState) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val debounceMillis: Long = 350L,
    private val maxIntervalMillis: Long = 1_000L,
) {
    private val writeLock = Mutex()
    private var pending: Page? = null
    private var scheduledWrite: Job? = null
    private var lastSuccessfulWrite: Long? = null
    private var pendingSince: Long? = null
    private var generation = 0L

    fun schedule(page: Page) {
        if (pending == null) pendingSince = nowMillis()
        pending = page
        onStateChanged(SaveState.Saving)
        scheduledWrite?.cancel()
        val boundary = lastSuccessfulWrite ?: pendingSince
        val elapsed = boundary?.let { nowMillis() - it }
        val wait = elapsed?.let { minOf(debounceMillis, (maxIntervalMillis - it).coerceAtLeast(0L)) }
            ?: debounceMillis
        val currentGeneration = ++generation
        scheduledWrite = scope.launch {
            delay(wait)
            persist(page, currentGeneration)
        }
    }

    fun flush(page: Page? = pending): Job? {
        val value = page ?: return null
        scheduledWrite?.cancel()
        if (pending == null) pendingSince = nowMillis()
        pending = value
        onStateChanged(SaveState.Saving)
        val currentGeneration = ++generation
        return scope.launch { persist(value, currentGeneration) }
    }

    suspend fun saveNow(page: Page): Boolean {
        scheduledWrite?.cancel()
        if (pending == null) pendingSince = nowMillis()
        pending = page
        onStateChanged(SaveState.Saving)
        val currentGeneration = ++generation
        return persist(page, currentGeneration)
    }

    fun cancelPendingFor(pageId: String) {
        if (pending?.id != pageId) return
        generation++
        scheduledWrite?.cancel()
        pending = null
        pendingSince = null
        onStateChanged(SaveState.Saved)
    }

    private suspend fun persist(page: Page, currentGeneration: Long): Boolean {
        try {
            writeLock.withLock { writePage(page) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (generation == currentGeneration && pending == page) {
                onStateChanged(SaveState.Failed)
            }
            return false
        }
        lastSuccessfulWrite = nowMillis()
        if (generation == currentGeneration && pending == page) {
            pending = null
            pendingSince = null
            onStateChanged(SaveState.Saved)
        }
        return true
    }
}
