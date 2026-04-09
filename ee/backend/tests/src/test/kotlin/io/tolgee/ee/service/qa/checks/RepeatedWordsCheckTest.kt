package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
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
    check.check(params("Hello world")).assertNoIssues()
  }

  @Test
  fun `returns empty for blank text`() {
    check.check(params("  ")).assertNoIssues()
  }

  @Test
  fun `returns empty for single word`() {
    check.check(params("Hello")).assertNoIssues()
  }

  @Test
  fun `detects simple repeated word`() {
    check.check(params("the the dog")).assertSingleIssue {
      message(QaIssueMessage.QA_REPEATED_WORD)
      param("word", "the")
    }
  }

  @Test
  fun `detects three times repeated word`() {
    check.check(params("the the the dog")).assertIssues {
      issue {
        message(QaIssueMessage.QA_REPEATED_WORD)
        param("word", "the")
      }
      issue {
        message(QaIssueMessage.QA_REPEATED_WORD)
        param("word", "the")
      }
    }
  }

  @Test
  fun `detects case-insensitive repetition`() {
    check.check(params("The the dog")).assertSingleIssue {
      param("word", "the")
    }
  }

  @Test
  fun `detects multiple repeated pairs`() {
    check.check(params("the the is is good")).assertIssues {
      issue { param("word", "the") }
      issue { param("word", "is") }
    }
  }

  @Test
  fun `detects repetition with punctuation between words`() {
    check.check(params("word. word again")).assertSingleIssue {
      param("word", "word")
    }
  }

  @Test
  fun `detects repetition across newline`() {
    check.check(params("hello\nhello world")).assertSingleIssue {
      param("word", "hello")
    }
  }

  @Test
  fun `works without base text`() {
    check.check(params("the the dog", base = null)).assertSingleIssue {
      message(QaIssueMessage.QA_REPEATED_WORD)
    }
  }

  @Test
  fun `works with base text present`() {
    check.check(params("the the dog", base = "something")).assertSingleIssue {
      message(QaIssueMessage.QA_REPEATED_WORD)
    }
  }

  @Test
  fun `reports correct positions for simple case`() {
    check.check(params("the the dog")).assertSingleIssue {
      position(3, 7)
      replacement("")
    }
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
    check.check(params("100 100 items")).assertNoIssues()
  }

  @Test
  fun `detects single-char word repeats`() {
    check.check(params("a a thing")).assertSingleIssue {
      param("word", "a")
    }
  }

  @Test
  fun `detects words with digits`() {
    check.check(params("test1 test1 value")).assertSingleIssue {
      param("word", "test1")
    }
  }

  @Test
  fun `does not flag different words`() {
    check.check(params("hello world foo bar")).assertNoIssues()
  }

  @Test
  fun `all types are REPEATED_WORDS`() {
    check.check(params("the the is is")).assertAllHaveType(QaCheckType.REPEATED_WORDS)
  }
}
