package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.formats.isWrittenInNonLatinScript
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class BracketsMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.BRACKETS_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val bracketChars = getBracketChars(params.icuPlaceholders)
    val latinGlossesExpected = isWrittenInNonLatinScript(params.languageTag)
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, _ ->
      checkVariant(text, baseText, bracketChars, latinGlossesExpected)
    }
  }

  private fun checkVariant(
    rawText: String,
    baseText: String?,
    bracketChars: CharArray,
    latinGlossesExpected: Boolean,
  ): List<QaCheckResult> {
    val rawBase = baseText ?: return emptyList()
    if (rawBase.isBlank()) return emptyList()
    if (rawText.isBlank()) return emptyList()

    val (base, textWithGlosses) =
      withoutUnmatchedOptionalSuffixes(fullWidthBracketsToAscii(rawBase), fullWidthBracketsToAscii(rawText))
    val text = withoutAddedLatinGlosses(textWithGlosses, base, latinGlossesExpected)

    val baseCounts = countBrackets(base, bracketChars)
    val textCounts = countBrackets(text, bracketChars)

    val results = mutableListOf<QaCheckResult>()
    val missingBrackets = mutableListOf<Pair<Char, Int>>() // bracket to missing count

    for (bracket in bracketChars) {
      val baseCount = baseCounts[bracket] ?: 0
      val textCount = textCounts[bracket] ?: 0

      if (textCount > baseCount) {
        // Extra brackets in translation — report each extra occurrence (they have unique positions)
        val positions = findBracketPositions(text, bracket)
        for (pos in positions.drop(baseCount)) {
          results.add(
            QaCheckResult(
              type = QaCheckType.BRACKETS_MISMATCH,
              message = QaIssueMessage.QA_BRACKETS_EXTRA,
              replacement = null,
              positionStart = pos,
              positionEnd = pos + 1,
              params = mapOf("bracket" to bracket.toString()),
            ),
          )
        }
      }

      if (textCount < baseCount) {
        missingBrackets.add(bracket to (baseCount - textCount))
      }
    }

    // Aggregate all missing brackets into a single issue to avoid identity conflicts
    if (missingBrackets.isNotEmpty()) {
      val description = missingBrackets.joinToString(", ") { (b, n) -> if (n > 1) "$b ×$n" else b.toString() }
      val totalMissing = missingBrackets.sumOf { it.second }
      results.add(
        QaCheckResult(
          type = QaCheckType.BRACKETS_MISMATCH,
          message = QaIssueMessage.QA_BRACKETS_MISSING,
          replacement = null,
          positionStart = null,
          positionEnd = null,
          params = mapOf("brackets" to description, "count" to totalMissing.toString()),
        ),
      )
    }

    return results
  }

  companion object {
    private val ALL_BRACKET_CHARS = charArrayOf('(', ')', '[', ']', '{', '}')
    private val NON_ICU_BRACKET_CHARS = charArrayOf('(', ')', '[', ']')

    private val FULL_WIDTH_BRACKETS =
      mapOf('（' to '(', '）' to ')', '［' to '[', '］' to ']', '｛' to '{', '｝' to '}')

    /** `Associé(e)`, `file(s)`, `을(를)`, `{name}(이)가` */
    private val OPTIONAL_SUFFIX_REGEX =
      Regex(
        """(?<=[\p{L}}>])\((?:[\p{L}&&[^\p{IsHan}\p{IsHiragana}\p{IsKatakana}\p{IsThai}\p{IsHangul}]]{1,3}|\p{IsHangul})\)""",
      )

    private val LATIN_GLOSS_REGEX = Regex("""\((?=[^()]*\p{IsLatin})[\p{IsLatin}\p{N}\s.,&/'_+-]+\)""")

    private fun fullWidthBracketsToAscii(text: String): String {
      return String(CharArray(text.length) { FULL_WIDTH_BRACKETS[text[it]] ?: text[it] })
    }

    private fun withoutUnmatchedOptionalSuffixes(
      base: String,
      text: String,
    ): Pair<String, String> {
      if (roundBracketCounts(base) == roundBracketCounts(text)) return base to text
      return replaceWithSpaces(base, OPTIONAL_SUFFIX_REGEX.findAll(base)) to
        replaceWithSpaces(text, OPTIONAL_SUFFIX_REGEX.findAll(text))
    }

    private fun withoutAddedLatinGlosses(
      text: String,
      base: String,
      latinGlossesExpected: Boolean,
    ): String {
      if (!latinGlossesExpected) return text

      val (baseOpening, baseClosing) = roundBracketCounts(base)
      val (textOpening, textClosing) = roundBracketCounts(text)
      val extraPairs = minOf(textOpening - baseOpening, textClosing - baseClosing)
      if (extraPairs <= 0) return text

      val addedGlosses = LATIN_GLOSS_REGEX.findAll(text).filter { it.value !in base }.toList()
      return replaceWithSpaces(text, addedGlosses.takeLast(extraPairs).asSequence())
    }

    private fun roundBracketCounts(text: String): Pair<Int, Int> {
      return text.count { it == '(' } to text.count { it == ')' }
    }

    private fun replaceWithSpaces(
      text: String,
      matches: Sequence<MatchResult>,
    ): String {
      val chars = text.toCharArray()
      matches.forEach { chars.fill(' ', it.range.first, it.range.last + 1) }
      return String(chars)
    }

    fun getBracketChars(icuPlaceholders: Boolean): CharArray =
      if (icuPlaceholders) NON_ICU_BRACKET_CHARS else ALL_BRACKET_CHARS

    private fun countBrackets(
      text: String,
      bracketChars: CharArray,
    ): Map<Char, Int> {
      val counts = mutableMapOf<Char, Int>()
      for (ch in text) {
        if (ch in bracketChars) {
          counts[ch] = (counts[ch] ?: 0) + 1
        }
      }
      return counts
    }

    private fun findBracketPositions(
      text: String,
      bracket: Char,
    ): List<Int> {
      return text.indices.filter { text[it] == bracket }
    }
  }
}
