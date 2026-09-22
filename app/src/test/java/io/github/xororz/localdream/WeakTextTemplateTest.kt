package io.github.xororz.localdream

import io.github.xororz.localdream.data.WeakTextTemplateRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeakTextTemplateTest {

    @Test
    fun testDefaultTemplateContent() {
        val defaultContent = WeakTextTemplateRepository.DEFAULT_TEMPLATE
        assertTrue(defaultContent.contains("[quality]"))
        assertTrue(defaultContent.contains("masterpiece"))
        assertTrue(defaultContent.contains("best quality"))
    }

    @Test
    fun testTemplateEnhancementsParsing() {
        val templateContent = """
            [quality]
            masterpiece, best quality, ultra detailed

            [suffix]
            high resolution, 8k
        """.trimIndent()

        // Test parsing template quality prefix and suffix
        val prefixes = mutableListOf<String>()
        val suffixes = mutableListOf<String>()
        var currentSection = ""

        for (line in templateContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1).lowercase()
                continue
            }
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue

            when (currentSection) {
                "quality", "prefix" -> {
                    trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.let { prefixes.addAll(it) }
                }
                "suffix" -> {
                    trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.let { suffixes.addAll(it) }
                }
            }
        }

        assertEquals(listOf("masterpiece", "best quality", "ultra detailed"), prefixes)
        assertEquals(listOf("high resolution", "8k"), suffixes)
    }

    @Test
    fun testPromptFormattingAndDeduplication() {
        val rawInput = "girl, holding umbrella, masterpiece"
        val prefixEnhancements = listOf("masterpiece", "best quality")
        val suffixEnhancements = listOf("high resolution")

        val finalTags = LinkedHashSet<String>()
        prefixEnhancements.forEach { finalTags.add(it) }
        rawInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { finalTags.add(it) }
        suffixEnhancements.forEach { finalTags.add(it) }

        val formatted = finalTags.joinToString(", ")
        val expected = "masterpiece, best quality, girl, holding umbrella, high resolution"

        assertEquals(expected, formatted)
    }

    @Test
    fun testCJKContinuousPhraseExtraction() {
        // Test CJK sliding-window phrase extraction without dropping any text (e.g. 雨中的黑色连衣裙少女)
        val translations = mapOf(
            "雨中" to "rain",
            "黑色连衣裙" to "black dress",
            "少女" to "1girl",
        )
        val segment = "雨中的黑色连衣裙少女"
        val resultTags = mutableListOf<String>()
        var index = 0

        while (index < segment.length) {
            val currentChar = segment[index]
            if (currentChar.isWhitespace()) {
                index++
                continue
            }
            var matchedLength = 0
            var matchedTag: String? = null
            val maxLookahead = minOf(segment.length - index, 10)
            for (len in maxLookahead downTo 1) {
                val sub = segment.substring(index, index + len)
                val match = translations[sub]
                if (match != null) {
                    matchedLength = len
                    matchedTag = match
                    break
                }
            }
            if (matchedLength > 0 && matchedTag != null) {
                resultTags.add(matchedTag)
                index += matchedLength
            } else {
                index++
            }
        }

        val result = resultTags.joinToString(", ")
        assertEquals("rain, black dress, 1girl", result)
    }
}
