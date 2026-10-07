package com.youniscript.app.editor

/** Local deterministic writing checks. No remote service or language model is involved. */
enum class ProofKind { SPELLING, GRAMMAR, PUNCTUATION, CAPITALIZATION, STYLE, CLARITY }

data class ProofSuggestion(
    val id: String,
    val kind: ProofKind,
    val start: Int,
    val end: Int,
    val original: String,
    val replacement: String?,
    val message: String,
)

data class ProofOptions(
    val spelling: Boolean = true,
    val grammar: Boolean = true,
    val punctuation: Boolean = true,
    val style: Boolean = false,
    val dictionary: Set<String> = emptySet(),
    val replacements: Map<String, String> = emptyMap(),
)

object YouniProof {
    private val commonTypos = mapOf(
        "becuase" to "because", "recieve" to "receive", "teh" to "the",
        "definately" to "definitely", "seperate" to "separate", "wich" to "which",
        "occured" to "occurred", "untill" to "until", "thier" to "their",
    )
    private val words = Regex("[A-Za-z][A-Za-z’']*")
    private val doubledWord = Regex("\\b([A-Za-z’']+)(\\s+)\\1\\b", RegexOption.IGNORE_CASE)
    private val grammarRules = listOf(
        Regex("\\b(He|She|It)\\s+go\\b", RegexOption.IGNORE_CASE) to "goes",
        Regex("\\b(They|We|You)\\s+was\\b", RegexOption.IGNORE_CASE) to "were",
        Regex("\\b(He|She|It)\\s+don't\\b", RegexOption.IGNORE_CASE) to "doesn't",
    )

    fun analyze(text: String, options: ProofOptions = ProofOptions()): List<ProofSuggestion> {
        if (text.isBlank()) return emptyList()
        val found = mutableListOf<ProofSuggestion>()
        val protected = quotedRanges(text)
        fun add(kind: ProofKind, start: Int, end: Int, replacement: String?, message: String) {
            if (start < 0 || end < start || end > text.length) return
            val original = text.substring(start, end)
            found += ProofSuggestion("${kind.name}:$start:$original", kind, start, end, original, replacement, message)
        }

        if (options.spelling) {
            words.findAll(text).forEach { match ->
                if (protected.any { match.range.first in it }) return@forEach
                val word = match.value
                val lower = word.lowercase()
                if (lower in options.dictionary.map(String::lowercase)) return@forEach
                val custom = options.replacements.entries.firstOrNull { it.key.equals(word, true) }?.value
                val correction = custom ?: commonTypos[lower] ?: return@forEach
                add(ProofKind.SPELLING, match.range.first, match.range.last + 1, preserveCase(word, correction), "Possible spelling correction")
            }
        }
        if (options.grammar) {
            grammarRules.forEach { (pattern, replacement) ->
                pattern.findAll(text).forEach { match ->
                    if (protected.any { match.range.first in it }) return@forEach
                    val subject = match.groupValues[1]
                    val proposed = subject + " " + preserveCase(match.value.substringAfterLast(' '), replacement)
                    add(ProofKind.GRAMMAR, match.range.first, match.range.last + 1, proposed, "Check subject and verb agreement")
                }
            }
        }
        if (options.punctuation) {
            Regex(" {2,}").findAll(text).forEach { add(ProofKind.PUNCTUATION, it.range.first, it.range.last + 1, " ", "Extra spaces") }
            Regex("\\s+([,;:.!?])").findAll(text).forEach { match ->
                val punctuation = match.groupValues[1]
                add(ProofKind.PUNCTUATION, match.range.first, match.range.last + 1, punctuation, "Remove the space before punctuation")
            }
            Regex("([!?.,])\\1+").findAll(text).forEach { match ->
                add(ProofKind.PUNCTUATION, match.range.first, match.range.last + 1, match.value.first().toString(), "Repeated punctuation")
            }
            val trimmedEnd = text.trimEnd().length
            if (trimmedEnd > 0 && text.substring(0, trimmedEnd).count { it.isLetter() } >= 12 && text[trimmedEnd - 1] !in ".!?…" && text[trimmedEnd - 1] != '"') {
                add(ProofKind.PUNCTUATION, trimmedEnd, trimmedEnd, ".", "This writing may need ending punctuation")
            }
            Regex("\\bi\\b").findAll(text).forEach { match ->
                if (protected.any { match.range.first in it }) return@forEach
                add(ProofKind.CAPITALIZATION, match.range.first, match.range.last + 1, "I", "Capitalize the first-person pronoun")
            }
        }
        if (options.style) {
            doubledWord.findAll(text).forEach { match ->
                add(ProofKind.STYLE, match.range.first, match.range.last + 1, match.groupValues[1], "Possible repeated word")
            }
        }
        if (options.style || options.grammar) {
            sentenceRanges(text).forEach { (start, end) ->
                val sentence = text.substring(start, end)
                if (sentence.trim().split(Regex("\\s+")).size >= 36) {
                    add(ProofKind.CLARITY, start, end, null, "This sentence is long; consider splitting it if that fits your voice")
                }
            }
        }
        return found.distinctBy { it.id }.sortedWith(compareBy(ProofSuggestion::start, ProofSuggestion::end))
    }

    private fun quotedRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var opening = -1
        text.forEachIndexed { index, character ->
            if (character == '"') {
                if (opening < 0) opening = index else { ranges += opening..index; opening = -1 }
            }
        }
        return ranges
    }

    private fun sentenceRanges(text: String): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        var start = 0
        text.forEachIndexed { index, char ->
            if (char in ".!?\n") {
                val end = index + 1
                if (text.substring(start, end).isNotBlank()) ranges += start to end
                start = end
            }
        }
        if (start < text.length && text.substring(start).isNotBlank()) ranges += start to text.length
        return ranges
    }

    private fun preserveCase(source: String, replacement: String): String = when {
        source.isEmpty() -> replacement
        source.all(Char::isUpperCase) -> replacement.uppercase()
        source.first().isUpperCase() -> replacement.replaceFirstChar(Char::uppercase)
        else -> replacement
    }
}
