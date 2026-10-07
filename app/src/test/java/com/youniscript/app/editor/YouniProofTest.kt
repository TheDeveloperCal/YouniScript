package com.youniscript.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouniProofTest {
    @Test fun commonSpellingProducesSuggestionWithoutChangingText() {
        val text = "I stayed becuase the room was quiet."
        val suggestions = YouniProof.analyze(text)
        val spelling = suggestions.single { it.kind == ProofKind.SPELLING }
        assertEquals("becuase", spelling.original)
        assertEquals("because", spelling.replacement)
        // Analysis returns proposals only; the source remains caller-owned.
        assertEquals("I stayed becuase the room was quiet.", text)
    }

    @Test fun personalDictionarySuppressesKnownWord() {
        val suggestions = YouniProof.analyze("My name is YouniScript.", ProofOptions(dictionary = setOf("youniscript")))
        assertTrue(suggestions.none { it.original.equals("YouniScript", true) })
    }

    @Test fun customReplacementIsSuggestedButQuotedTextIsProtected() {
        val text = "Outside and \"inside\""
        val suggestions = YouniProof.analyze(text, ProofOptions(replacements = mapOf("and" to "&")))
        assertEquals(1, suggestions.count { it.kind == ProofKind.SPELLING })
        assertEquals("and", suggestions.single { it.kind == ProofKind.SPELLING }.original)
    }

    @Test fun grammarSuggestionPreservesSubjectAndCapitalization() {
        val suggestion = YouniProof.analyze("He go home.").single { it.kind == ProofKind.GRAMMAR }
        assertEquals("He go", suggestion.original)
        assertEquals("He goes", suggestion.replacement)
    }

    @Test fun punctuationAndCapitalizationAreSuggestions() {
        val suggestions = YouniProof.analyze("I saw it  !  i went home yesterday")
        assertTrue(suggestions.any { it.kind == ProofKind.PUNCTUATION && it.message == "Extra spaces" })
        assertTrue(suggestions.any { it.kind == ProofKind.CAPITALIZATION && it.original == "i" })
        assertTrue(suggestions.any { it.kind == ProofKind.PUNCTUATION && it.message.contains("ending punctuation") })
    }

    @Test fun styleChecksCanBeDisabled() {
        val suggestions = YouniProof.analyze("The the path is long.", ProofOptions(style = false, grammar = false))
        assertTrue(suggestions.none { it.kind == ProofKind.STYLE || it.kind == ProofKind.CLARITY })
    }
}
