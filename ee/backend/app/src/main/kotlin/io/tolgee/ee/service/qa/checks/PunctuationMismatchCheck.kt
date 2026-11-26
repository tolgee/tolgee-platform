package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.formats.languageCodeOf
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class PunctuationMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.PUNCTUATION_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, _ ->
      checkVariant(text, baseText, params.baseLanguageTag, params.languageTag)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
    baseLanguageTag: String?,
    languageTag: String,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val textTrimmed = withoutTrailingInvisibleChars(text)

    val basePunct = trailingPunctuation(withoutTrailingInvisibleChars(base), baseLanguageTag)
    val textPunct = trailingPunctuation(textTrimmed, languageTag)

    val expectedAscii = basePunct?.let { toAsciiPunctuation(it, baseLanguageTag) }
    val textAscii = textPunct?.let { toAsciiPunctuation(it, languageTag) }
    if (expectedAscii == textAscii) return emptyList()

    val expected = expectedAscii?.let { localizePunctuation(it, languageTag).toString() }
    val isAlreadyWrittenAsSuggested = expected == textPunct?.toString()
    if (isAlreadyWrittenAsSuggested) return emptyList()

    val textEnd = textTrimmed.length
    return when {
      expected != null && textPunct == null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_ADD,
            replacement = expected,
            positionStart = textEnd,
            positionEnd = textEnd,
            params = mapOf("punctuation" to expected),
          ),
        )
      }

      expected == null && textPunct != null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_REMOVE,
            replacement = "",
            positionStart = textEnd - 1,
            positionEnd = textEnd,
            params = mapOf("punctuation" to textPunct.toString()),
          ),
        )
      }

      expected != null && textPunct != null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_REPLACE,
            replacement = expected,
            positionStart = textEnd - 1,
            positionEnd = textEnd,
            params = mapOf("punctuation" to textPunct.toString(), "expected" to expected),
          ),
        )
      }

      else -> {
        emptyList()
      }
    }
  }

  companion object {
    private val PUNCTUATION_CHARS = setOf('.', ',', '!', '?', ':', ';')

    private const val GREEK = "el"
    private const val GREEK_QUESTION_MARK = '\u037E'

    private val ASCII_PUNCTUATION_EQUIVALENTS =
      mapOf(
        '。' to '.',
        '．' to '.',
        '｡' to '.',
        '۔' to '.',
        '।' to '.',
        '։' to '.',
        '።' to '.',
        '？' to '?',
        '؟' to '?',
        GREEK_QUESTION_MARK to '?',
        '！' to '!',
        '，' to ',',
        '、' to ',',
        '،' to ',',
        '：' to ':',
        '；' to ';',
        '؛' to ';',
      )

    private val CHINESE_PUNCTUATION = mapOf('.' to '。', ',' to '，', '?' to '？', '!' to '！', ':' to '：', ';' to '；')
    private val ARABIC_PUNCTUATION = mapOf('?' to '؟', ',' to '،', ';' to '؛')

    private val LOCALIZED_PUNCTUATION =
      mapOf(
        "zh" to CHINESE_PUNCTUATION,
        "ja" to CHINESE_PUNCTUATION + (',' to '、'),
        "ar" to ARABIC_PUNCTUATION,
        "fa" to ARABIC_PUNCTUATION,
        "ur" to ARABIC_PUNCTUATION + ('.' to '۔'),
        GREEK to mapOf('?' to ';'),
      )

    /** Right-to-left text often ends with an invisible direction mark after the punctuation. */
    private fun withoutTrailingInvisibleChars(text: String): String {
      return text.trimEnd { it.isWhitespace() || it.category == CharCategory.FORMAT }
    }

    fun trailingPunctuation(
      text: String,
      languageTag: String?,
    ): Char? {
      return text.lastOrNull()?.takeIf { toAsciiPunctuation(it, languageTag) in PUNCTUATION_CHARS }
    }

    /** Greek writes the question mark as `;`. */
    private fun toAsciiPunctuation(
      char: Char,
      languageTag: String?,
    ): Char {
      if (char == ';' && languageOf(languageTag) == GREEK) return '?'
      return ASCII_PUNCTUATION_EQUIVALENTS[char] ?: char
    }

    private fun localizePunctuation(
      asciiChar: Char,
      languageTag: String,
    ): Char {
      return LOCALIZED_PUNCTUATION[languageOf(languageTag)]?.get(asciiChar) ?: asciiChar
    }

    private fun languageOf(languageTag: String?): String? {
      return languageTag?.let(::languageCodeOf)
    }
  }
}
