package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RepeatedWordsCheckTest {
  private val check = RepeatedWordsCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = null,
    languageTag = "en",
  )

  @Test
  fun `returns empty for no repeated words`() {
    val results = check.check(params("Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for blank text`() {
    val results = check.check(params("  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for single word`() {
    val results = check.check(params("Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects simple repeated word`() {
    val results = check.check(params("the the dog"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_REPEATED_WORD)
    assertThat(results[0].params).containsEntry("word", "the")
  }

  @Test
  fun `detects three times repeated word`() {
    val results = check.check(params("the the the dog"))
    assertThat(results).hasSize(2)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_REPEATED_WORD)
    assertThat(results[0].params).containsEntry("word", "the")
    assertThat(results[1].message).isEqualTo(QaIssueMessage.QA_REPEATED_WORD)
    assertThat(results[1].params).containsEntry("word", "the")
  }

  @Test
  fun `detects case-insensitive repetition`() {
    val results = check.check(params("The the dog"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("word", "the")
  }

  @Test
  fun `detects multiple repeated pairs`() {
    val results = check.check(params("the the is is good"))
    assertThat(results).hasSize(2)
    val words = results.map { it.params?.get("word") }
    assertThat(words).containsExactly("the", "is")
  }

  @Test
  fun `detects repetition with punctuation between words`() {
    // Tokenizer extracts words ignoring punctuation, so "word." and "word" both yield "word"
    val results = check.check(params("word. word again"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("word", "word")
  }

  @Test
  fun `detects repetition across newline`() {
    val results = check.check(params("hello\nhello world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("word", "hello")
  }

  @Test
  fun `works without base text`() {
    val results = check.check(params("the the dog", base = null))
    assertThat(results).hasSize(1)
  }

  @Test
  fun `works with base text present`() {
    val results = check.check(params("the the dog", base = "something"))
    assertThat(results).hasSize(1)
  }

  @Test
  fun `reports correct positions for simple case`() {
    // "the the dog"
    //  012 3456 789...
    // First "the" at 0..2, second "the" at 4..6
    // positionStart = 3 (after first "the"), positionEnd = 7 (after second "the")
    val results = check.check(params("the the dog"))
    assertThat(results).hasSize(1)
    assertThat(results[0].positionStart).isEqualTo(3)
    assertThat(results[0].positionEnd).isEqualTo(7)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `applying replacement fixes the text`() {
    val text = "the the dog"
    val result = check.check(params(text))[0]
    val fixed = text.substring(0, result.positionStart!!) + result.replacement + text.substring(result.positionEnd!!)
    assertThat(fixed).isEqualTo("the dog")
  }

  @Test
  fun `does not flag pure numbers`() {
    val results = check.check(params("100 100 items"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects single-char word repeats`() {
    val results = check.check(params("a a thing"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("word", "a")
  }

  @Test
  fun `detects words with digits`() {
    val results = check.check(params("test1 test1 value"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("word", "test1")
  }

  @Test
  fun `does not flag different words`() {
    val results = check.check(params("hello world foo bar"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `all types are REPEATED_WORDS`() {
    val results = check.check(params("the the is is"))
    assertThat(results).allMatch { it.type == QaCheckType.REPEATED_WORDS }
  }
}
