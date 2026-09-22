package io.github.xororz.localdream.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class WeakTextTemplateState(
    val imported: Boolean = false,
    val fileName: String? = null,
    val templateContent: String = WeakTextTemplateRepository.DEFAULT_TEMPLATE,
)

class WeakTextTemplateRepository private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val templateFile = File(context.filesDir, TEMPLATE_FILE_NAME)

    private val _state = MutableStateFlow(readStateFromDisk())
    val state: StateFlow<WeakTextTemplateState> = _state.asStateFlow()

    private fun readStateFromDisk(): WeakTextTemplateState {
        val imported = templateFile.exists() && templateFile.length() > 0
        val fileName = if (imported) prefs.getString(KEY_FILE_NAME, null) else null
        val content = if (imported) {
            runCatching { templateFile.readText() }.getOrDefault(DEFAULT_TEMPLATE)
        } else {
            DEFAULT_TEMPLATE
        }
        return WeakTextTemplateState(
            imported = imported,
            fileName = fileName,
            templateContent = content.ifBlank { DEFAULT_TEMPLATE },
        )
    }

    suspend fun importTemplate(uri: Uri, displayName: String?): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val contentBuilder = StringBuilder()
            var lineCount = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                    lines.forEach { line ->
                        contentBuilder.append(line).append("\n")
                        lineCount++
                    }
                }
            } ?: return@withContext ImportResult.Error("Cannot open model file")

            val content = contentBuilder.toString().trim()
            if (content.isBlank()) {
                return@withContext ImportResult.Error("Empty file")
            }

            templateFile.writeText(content)
            prefs.edit {
                putString(KEY_FILE_NAME, displayName ?: TEMPLATE_FILE_NAME)
            }
            _state.value = readStateFromDisk()
            ImportResult.Success(lineCount)
        }.getOrElse {
            templateFile.delete()
            ImportResult.Error(it.message ?: "Unknown error")
        }
    }

    fun clearTemplate() {
        templateFile.delete()
        prefs.edit {
            remove(KEY_FILE_NAME)
        }
        _state.value = readStateFromDisk()
    }

    fun getTemplateContent(): String {
        return state.value.templateContent
    }

    /**
     * Executes local weak text model / prompt strengthening and translation.
     * Takes typed written language text, translates non-English words to English tags
     * using sliding-window phrase matching and dictionary lookups,
     * applies weak text model rules, enriches quality/style tags, and returns
     * ONLY the corrected prompt without dropping any user text.
     */
    suspend fun strengthenAndTranslatePrompt(
        inputText: String,
        tagRepository: TagAutocompleteRepository? = null,
    ): String = withContext(Dispatchers.Default) {
        val rawInput = inputText.trim()
        if (rawInput.isBlank()) return@withContext rawInput

        val template = getTemplateContent()

        // 1. Split input into segments by comma or newline
        val rawSegments = rawInput.split(Regex("[,，\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val processedTags = mutableListOf<String>()

        for (segment in rawSegments) {
            if (containsNonAsciiLetterLocal(segment)) {
                val translated = translateSegment(segment, tagRepository)
                if (translated.isNotBlank()) {
                    processedTags.add(translated)
                } else {
                    processedTags.add(segment)
                }
            } else {
                processedTags.add(segment)
            }
        }

        // 2. Parse template rules for quality prefix/suffix or tag enhancements
        val (prefixEnhancements, suffixEnhancements) = parseTemplateEnhancements(template)

        val finalTags = LinkedHashSet<String>()

        // Add prefix quality tags from weak text model template
        prefixEnhancements.forEach { tag -> if (tag.isNotBlank()) finalTags.add(tag.trim()) }

        // Add translated and strengthened input tags
        processedTags.forEach { tag ->
            tag.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { singleTag ->
                finalTags.add(singleTag)
            }
        }

        // Add suffix quality tags from weak text model template
        suffixEnhancements.forEach { tag -> if (tag.isNotBlank()) finalTags.add(tag.trim()) }

        // Join into corrected prompt
        finalTags.joinToString(", ")
    }

    private suspend fun translateSegment(
        segment: String,
        tagRepository: TagAutocompleteRepository?,
    ): String {
        // Step A: Try direct dictionary translation of the whole segment
        if (tagRepository != null) {
            val suggestions = tagRepository.suggest(segment, limit = 5)
            val directMatch = suggestions.firstOrNull {
                it.matchType == TagMatchType.Translation || it.matchType == TagMatchType.Prefix
            }?.replacementTag ?: suggestions.firstOrNull()?.replacementTag

            if (directMatch != null) {
                return tagUnderscoresToSpacesLocal(directMatch)
            }
        }

        val builtInDirect = COMMON_TRANSLATIONS[segment]
        if (builtInDirect != null) {
            return builtInDirect
        }

        // Step B: Longest-match sliding window extraction over CJK / non-ASCII text
        val translatedTags = mutableListOf<String>()
        var index = 0

        while (index < segment.length) {
            // Skip leading whitespace or punctuation
            val currentChar = segment[index]
            if (currentChar.isWhitespace() || currentChar == '，' || currentChar == '、') {
                index++
                continue
            }

            var matchedLength = 0
            var matchedEnglishTag: String? = null

            // Search for the longest matching phrase starting at `index` (up to max 10 chars)
            val maxLookahead = minOf(segment.length - index, 10)
            for (len in maxLookahead downTo 1) {
                val sub = segment.substring(index, index + len)

                // 1) Check TagAutocompleteRepository
                if (tagRepository != null && containsNonAsciiLetterLocal(sub)) {
                    val sug = tagRepository.suggest(sub, limit = 3)
                    val match = sug.firstOrNull {
                        it.matchType == TagMatchType.Translation || it.matchType == TagMatchType.Prefix
                    }?.replacementTag
                    if (match != null) {
                        matchedLength = len
                        matchedEnglishTag = tagUnderscoresToSpacesLocal(match)
                        break
                    }
                }

                // 2) Check built-in translation table
                val commonMatch = COMMON_TRANSLATIONS[sub]
                if (commonMatch != null) {
                    matchedLength = len
                    matchedEnglishTag = commonMatch
                    break
                }
            }

            if (matchedLength > 0 && matchedEnglishTag != null) {
                translatedTags.add(matchedEnglishTag)
                index += matchedLength
            } else {
                // Collect unmatched English or unmapped word/character
                var end = index + 1
                while (end < segment.length) {
                    val nextChar = segment[end]
                    if (nextChar.isWhitespace() || nextChar == '，' || nextChar == '、') break
                    // Check if a known dictionary phrase starts at `end`
                    var knownPhraseStarts = false
                    val maxCheck = minOf(segment.length - end, 10)
                    for (chk in maxCheck downTo 1) {
                        val candidate = segment.substring(end, end + chk)
                        if (COMMON_TRANSLATIONS.containsKey(candidate)) {
                            knownPhraseStarts = true
                            break
                        }
                    }
                    if (knownPhraseStarts) break
                    end++
                }
                val unmatchedToken = segment.substring(index, end).trim()
                if (unmatchedToken.isNotBlank() && unmatchedToken != "的" && unmatchedToken != "在") {
                    translatedTags.add(unmatchedToken)
                }
                index = end
            }
        }

        return if (translatedTags.isNotEmpty()) {
            translatedTags.joinToString(", ")
        } else {
            segment
        }
    }

    private fun parseTemplateEnhancements(templateContent: String): Pair<List<String>, List<String>> {
        val prefixes = mutableListOf<String>()
        val suffixes = mutableListOf<String>()

        val lines = templateContent.lines()
        var currentSection = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1).lowercase()
                continue
            }
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue

            when (currentSection) {
                "prefix", "quality", "strengthen", "rules" -> {
                    trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.let { prefixes.addAll(it) }
                }
                "suffix", "ending" -> {
                    trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.let { suffixes.addAll(it) }
                }
                else -> {
                    // If no section header, comma-separated quality tags are treated as default prefix tags
                    if (!trimmed.contains("=") && !trimmed.contains(":")) {
                        trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.let { prefixes.addAll(it) }
                    }
                }
            }
        }

        // If no template rules were extracted, use default quality prefix
        if (prefixes.isEmpty() && suffixes.isEmpty()) {
            prefixes.addAll(listOf("masterpiece", "best quality", "highly detailed"))
        }

        return Pair(prefixes, suffixes)
    }

    private fun containsNonAsciiLetterLocal(value: String): Boolean {
        return value.any { it.code > 127 && it.isLetter() }
    }

    private fun tagUnderscoresToSpacesLocal(tag: String): String {
        if ('_' !in tag) return tag
        val sb = StringBuilder(tag.length)
        var i = 0
        while (i < tag.length) {
            val c = tag[i]
            when {
                c == '\\' && i + 1 < tag.length && tag[i + 1] == '_' -> {
                    sb.append('_')
                    i += 2
                }
                c == '_' -> {
                    sb.append(' ')
                    i++
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    companion object {
        private const val PREFS_NAME = "weak_text_template_prefs"
        private const val TEMPLATE_FILE_NAME = "weak_template.txt"
        private const val KEY_FILE_NAME = "template_file_name"

        val DEFAULT_TEMPLATE = """
            # Weak Text Model & Template for Prompt Strengthening & Translation
            # Rules:
            # - Translate written language to English Stable Diffusion tags
            # - Strengthen prompt quality and detail
            # - Output strictly the corrected prompt only

            [quality]
            masterpiece, best quality, highly detailed

            [rules]
            translate_to_english=true
            strict_corrected_prompt_only=true
        """.trimIndent()

        // Common offline fallback translation mappings for prompt written language terms
        private val COMMON_TRANSLATIONS = mapOf(
            "黑色连衣裙" to "black dress",
            "白色连衣裙" to "white dress",
            "红色连衣裙" to "red dress",
            "蓝色连衣裙" to "blue dress",
            "哥特风" to "gothic style",
            "哥特" to "gothic",
            "拿雨伞" to "holding umbrella",
            "拿着伞" to "holding umbrella",
            "雨伞" to "holding umbrella",
            "下雨" to "rain",
            "雨中" to "rain",
            "少女" to "1girl",
            "女孩" to "1girl",
            "女性" to "1girl",
            "少年" to "1boy",
            "男孩" to "1boy",
            "男性" to "1boy",
            "雨" to "rain",
            "伞" to "umbrella",
            "连衣裙" to "dress",
            "长发" to "long hair",
            "短发" to "short hair",
            "黑发" to "black hair",
            "金发" to "blonde hair",
            "银发" to "silver hair",
            "蓝眼睛" to "blue eyes",
            "红眼睛" to "red eyes",
            "笑" to "smiling",
            "微笑" to "smiling",
            "站立" to "standing",
            "坐着" to "sitting",
            "风景" to "scenery",
            "夜景" to "night sky",
            "星空" to "starry sky",
            "森林" to "forest",
            "海洋" to "ocean",
            "二次元" to "anime",
            "动漫" to "anime style",
            "写实" to "realistic",
            "高清" to "highres",
            "精细" to "detailed",
        )

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: WeakTextTemplateRepository? = null

        fun getInstance(context: Context): WeakTextTemplateRepository = instance ?: synchronized(this) {
            instance ?: WeakTextTemplateRepository(context.applicationContext).also {
                instance = it
            }
        }
    }
}
