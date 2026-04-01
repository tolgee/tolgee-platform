package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.checks.language.filterLanguageToolFalsePositives
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LanguageToolResultFilterTest {
  private fun result(
    start: Int?,
    end: Int?,
  ) = QaCheckResult(
    type = QaCheckType.SPELLING,
    message = QaIssueMessage.QA_SPELLING_ERROR,
    positionStart = start,
    positionEnd = end,
    params = if (start != null) mapOf("word" to "test") else null,
  )

  @Test
  fun `returns empty for empty results`() {
    val filtered = filterLanguageToolFalsePositives(emptyList(), "Hello {name}")
    assertThat(filtered).isEmpty()
  }

  @Test
  fun `keeps all results when text has no placeholders or tags`() {
    val results = listOf(result(0, 4))
    val filtered = filterLanguageToolFalsePositives(results, "Helo world")
    assertThat(filtered).hasSize(1)
  }

  @Test
  fun `filters result overlapping ICU placeholder`() {
    // "Hello {name} world" — {name} is at positions 6..12
    val text = "Hello {name} world"
    val results = listOf(
      result(0, 5), // "Hello" — no overlap, keep
      result(6, 12), // "{name}" — overlaps placeholder, filter
      result(13, 18), // "world" — no overlap, keep
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(2)
    assertThat(filtered.map { it.positionStart }).containsExactly(0, 13)
  }

  @Test
  fun `filters result overlapping HTML tag`() {
    // "Click <b>here</b> now" — <b> at 6..9, </b> at 13..17
    val text = "Click <b>here</b> now"
    val results = listOf(
      result(0, 5), // "Click" — keep
      result(6, 9), // "<b>" — overlaps tag, filter
      result(9, 13), // "here" — keep
      result(13, 17), // "</b>" — overlaps tag, filter
      result(18, 21), // "now" — keep
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(3)
    assertThat(filtered.map { it.positionStart }).containsExactly(0, 9, 18)
  }

  @Test
  fun `filters result overlapping URL`() {
    // "Visit https://example.com today"
    val text = "Visit https://example.com today"
    val results = listOf(
      result(0, 5), // "Visit" — keep
      result(6, 25), // "https://example.com" — overlaps URL, filter
      result(26, 31), // "today" — keep
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(2)
    assertThat(filtered.map { it.positionStart }).containsExactly(0, 26)
  }

  @Test
  fun `keeps results with null positions`() {
    val text = "Hello {name}"
    val results = listOf(result(null, null))
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(1)
  }

  @Test
  fun `filters partial overlap with placeholder`() {
    // "ab{x}cd" — {x} at positions 2..5
    val text = "ab{x}cd"
    val results = listOf(
      result(1, 4), // "b{x" — partially overlaps, filter
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).isEmpty()
  }

  @Test
  fun `keeps adjacent but non-overlapping result`() {
    // "ab{x}cd" — {x} at positions 2..5
    val text = "ab{x}cd"
    val results = listOf(
      result(0, 2), // "ab" — adjacent to placeholder start, keep
      result(5, 7), // "cd" — adjacent to placeholder end, keep
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(2)
  }

  @Test
  fun `handles text with multiple blocked range types`() {
    // Mix of placeholder, HTML, and URL
    val text = "Hello {name}, click <a href=\"url\">https://example.com</a>"
    val results = listOf(
      result(0, 5), // "Hello" — keep
      result(6, 12), // "{name}" — placeholder, filter
      result(20, 36), // '<a href="url">' — HTML tag, filter
      result(36, 55), // "https://example.com" — URL, filter
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(1)
    assertThat(filtered[0].positionStart).isEqualTo(0)
  }

  @Test
  fun `returns same list when no blocked ranges exist`() {
    val results = listOf(result(0, 5), result(6, 11))
    val filtered = filterLanguageToolFalsePositives(results, "Hello world")
    assertThat(filtered).hasSize(2)
  }

  @Test
  fun `handles non-ICU text gracefully`() {
    // extractArgs returns null for non-ICU text — should still filter HTML/URLs
    val text = "Check <b>this</b>"
    val results = listOf(
      result(6, 9), // "<b>" — HTML tag, filter
      result(9, 13), // "this" — keep
    )
    val filtered = filterLanguageToolFalsePositives(results, text)
    assertThat(filtered).hasSize(1)
    assertThat(filtered[0].positionStart).isEqualTo(9)
  }
}
