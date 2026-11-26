package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.formats.capitalizesAllNouns
import io.tolgee.formats.isWrittenInCaselessScript
import io.tolgee.formats.javaLocaleOf
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class CharacterCaseMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.CHARACTER_CASE_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    if (isWrittenInCaselessScript(params.languageTag)) return emptyList()
    if (params.baseLanguageTag?.let(::isWrittenInCaselessScript) == true) return emptyList()

    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, isPluralVariant ->
      checkVariant(text, baseText, isPluralVariant, params.languageTag)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
    isPluralVariant: Boolean,
    languageTag: String,
  ): List<QaCheckResult> {
    return filterResultsInBlockedRanges(detectCaseMismatch(text, baseText, isPluralVariant, languageTag), text)
  }

  private fun detectCaseMismatch(
    text: String,
    baseText: String?,
    isPluralVariant: Boolean,
    languageTag: String,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val (baseFirstIndex, baseFirstChar) = firstLetter(base) ?: return emptyList()
    val (textFirstIndex, textFirstChar) = firstLetter(text) ?: return emptyList()

    val shouldCapitalize = baseFirstChar.isUpperCase() && textFirstChar.isLowerCase()
    val shouldLowercase = baseFirstChar.isLowerCase() && textFirstChar.isUpperCase()
    if (!shouldCapitalize && !shouldLowercase) return emptyList()

    if (!isFirstLetterCaseMeaningful(base, baseFirstIndex, isPluralVariant)) return emptyList()
    if (!isFirstLetterCaseMeaningful(text, textFirstIndex, isPluralVariant)) return emptyList()
    if (startsWithLowercaseAbbreviation(base) || isInBlockedRange(base, baseFirstIndex)) return emptyList()

    val locale = javaLocaleOf(languageTag)

    if (shouldCapitalize) {
      return listOf(
        QaCheckResult(
          type = QaCheckType.CHARACTER_CASE_MISMATCH,
          message = QaIssueMessage.QA_CASE_CAPITALIZE,
          replacement = textFirstChar.toString().uppercase(locale),
          positionStart = textFirstIndex,
          positionEnd = textFirstIndex + 1,
        ),
      )
    }

    if (capitalizesAllNouns(languageTag) || startsWithAcronym(text, textFirstIndex)) return emptyList()

    return listOf(
      QaCheckResult(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_LOWERCASE,
        replacement = textFirstChar.toString().lowercase(locale),
        positionStart = textFirstIndex,
        positionEnd = textFirstIndex + 1,
      ),
    )
  }

  private fun isFirstLetterCaseMeaningful(
    text: String,
    firstLetterIndex: Int,
    isPluralVariant: Boolean,
  ): Boolean {
    if (startsWithPlaceholder(text, isPluralVariant)) return false
    return !hasFixedCase(text, firstLetterIndex)
  }

  companion object {
    private val LOWERCASE_ABBREVIATIONS = listOf("e.g.", "i.e.", "etc.", "vs.")

    /** `index.html`, `refresh_token`, `name@example.com` */
    private val CODE_LITERAL_REGEX = Regex("""[\p{L}\p{N}][._@][\p{L}\p{N}]""")

    fun firstLetter(text: String): Pair<Int, Char>? {
      val index = text.indexOfFirst { it.isLetter() }
      if (index < 0) return null
      return index to text[index]
    }

    private fun startsWithPlaceholder(
      text: String,
      isPluralVariant: Boolean,
    ): Boolean {
      val start = text.indexOfFirst { !it.isWhitespace() }
      return extractArgs(text, isPluralVariant)?.any { it.positionStart == start } ?: false
    }

    private fun startsWithLowercaseAbbreviation(text: String): Boolean {
      val trimmed = text.trimStart()
      return LOWERCASE_ABBREVIATIONS.any { trimmed.startsWith(it) }
    }

    private fun hasFixedCase(
      text: String,
      firstLetterIndex: Int,
    ): Boolean {
      if (CODE_LITERAL_REGEX.containsMatchIn(whitespaceDelimitedWordAt(text, firstLetterIndex))) return true
      return hasInternalCapital(letterRunAt(text, firstLetterIndex))
    }

    private fun startsWithAcronym(
      text: String,
      firstLetterIndex: Int,
    ): Boolean {
      val word = letterRunAt(text, firstLetterIndex)
      return word.length > 1 && word.none { it.isLowerCase() }
    }

    private fun letterRunAt(
      text: String,
      index: Int,
    ): String {
      return text.substring(index).takeWhile { it.isLetterOrDigit() }
    }

    /** `iManage`, `vLLM`, `GitHub`, but not `API` or `Hello` */
    private fun hasInternalCapital(word: String): Boolean {
      return word.any { it.isLowerCase() } && word.drop(1).any { it.isUpperCase() }
    }

    private fun whitespaceDelimitedWordAt(
      text: String,
      index: Int,
    ): String {
      val before = text.substring(0, index).takeLastWhile { !it.isWhitespace() }
      val after = text.substring(index).takeWhile { !it.isWhitespace() }
      return before + after
    }
  }
}
