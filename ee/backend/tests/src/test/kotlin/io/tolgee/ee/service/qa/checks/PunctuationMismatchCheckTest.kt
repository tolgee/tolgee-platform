package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class PunctuationMismatchCheckTest {
  private val check = PunctuationMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
    baseLanguageTag: String? = null,
    languageTag: String = "en",
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = baseLanguageTag,
    languageTag = languageTag,
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello.", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when punctuation matches`() {
    check.check(params("Bonjour.", "Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when neither has punctuation`() {
    check.check(params("Bonjour", "Hello")).assertNoIssues()
  }

  @Test
  fun `detects missing punctuation - add`() {
    check.check(params("Bonjour", "Hello.")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_ADD)
      param("punctuation", ".")
      replacement(".")
    }
  }

  @Test
  fun `detects extra punctuation - remove`() {
    check.check(params("Bonjour!", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REMOVE)
      param("punctuation", "!")
      replacement("")
    }
  }

  @Test
  fun `detects different punctuation - replace`() {
    check.check(params("Bonjour!", "Hello.")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REPLACE)
      param("punctuation", "!")
      param("expected", ".")
      replacement(".")
    }
  }

  @Test
  fun `ignores trailing whitespace`() {
    check.check(params("Bonjour.  ", "Hello.  ")).assertNoIssues()
  }

  @Test
  fun `handles all punctuation types`() {
    for (punct in listOf('.', ',', '!', '?', ':', ';')) {
      check.check(params("Bonjour", "Hello$punct")).assertSingleIssue {
        param("punctuation", punct.toString())
      }
    }
  }

  @Test
  fun `all types are PUNCTUATION_MISMATCH`() {
    check.check(params("Bonjour!", "Hello.")).assertAllHaveType(QaCheckType.PUNCTUATION_MISMATCH)
  }

  @Test
  fun `treats full-width CJK punctuation as equivalent`() {
    check.check(params("どの組織にも所属していません。", "You are not a member of any organization.")).assertNoIssues()
    check.check(params("出了点问题。请稍后再试。", "Something went wrong. Please try again later.")).assertNoIssues()
    check.check(params("よろしいですか？", "Are you sure?")).assertNoIssues()
    check.check(params("完了！", "Done!")).assertNoIssues()
    check.check(params("次の項目：", "The following items:")).assertNoIssues()
    check.check(params("まず、", "First,")).assertNoIssues()
    check.check(params("まず，", "First,")).assertNoIssues()
    check.check(params("一つ目；", "First;")).assertNoIssues()
  }

  @Test
  fun `treats Arabic punctuation as equivalent`() {
    check.check(params("هل أنت متأكد؟", "Are you sure?")).assertNoIssues()
    check.check(params("أولاً،", "First,")).assertNoIssues()
    check.check(params("أولاً؛", "First;")).assertNoIssues()
  }

  @Test
  fun `treats half-width and full-width full stops as equivalent`() {
    check.check(params("完了．", "Done.")).assertNoIssues()
    check.check(params("完了｡", "Done.")).assertNoIssues()
  }

  @Test
  fun `treats full stops of other scripts as equivalent`() {
    check.check(params("ہو گیا۔", "Done.", languageTag = "ur")).assertNoIssues()
    check.check(params("हो गया।", "Done.", languageTag = "hi")).assertNoIssues()
    check.check(params("Պատրաստ է։", "Done.", languageTag = "hy")).assertNoIssues()
    check.check(params("ተከናውኗል።", "Done.", languageTag = "am")).assertNoIssues()
  }

  @Test
  fun `treats the Greek question mark as a question mark`() {
    check.check(params("Είστε σίγουροι;", "Are you sure?", languageTag = "el")).assertNoIssues()
    check.check(params("Είστε σίγουροι\u037E", "Are you sure?", languageTag = "el")).assertNoIssues()
  }

  @Test
  fun `does not treat a semicolon as a question mark outside Greek`() {
    check.check(params("Jste si jistí;", "Are you sure?", languageTag = "cs")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REPLACE)
      replacement("?")
    }
  }

  @Test
  fun `treats a Greek base question mark as a question mark`() {
    check.check(params("Are you sure?", "Είστε σίγουροι;", baseLanguageTag = "el")).assertNoIssues()
  }

  @Test
  fun `suggests punctuation of the translation language when replacing`() {
    check.check(params("よろしいですか。", "Are you sure?", languageTag = "ja")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REPLACE)
      replacement("？")
      position(7, 8)
      param("punctuation", "。")
      param("expected", "？")
    }
  }

  @Test
  fun `suggests punctuation of the translation language when adding`() {
    listOf(
      Triple("ja", "こんにちは", "。"),
      Triple("zh-Hans", "你好", "。"),
      Triple("ur", "ہو گیا", "۔"),
      Triple("cs", "Ahoj", "."),
    ).forEach { (tag, text, expected) ->
      check.check(params(text, "Hello.", languageTag = tag)).assertSingleIssue {
        message(QaIssueMessage.QA_PUNCTUATION_ADD)
        replacement(expected)
        param("punctuation", expected)
      }
    }
    listOf(
      Triple("ar", "هل أنت متأكد", "؟"),
      Triple("fa", "مطمئنی", "؟"),
      Triple("el", "Είστε σίγουροι", ";"),
      Triple("ko", "확실합니까", "?"),
    ).forEach { (tag, text, expected) ->
      check.check(params(text, "Are you sure?", languageTag = tag)).assertSingleIssue {
        message(QaIssueMessage.QA_PUNCTUATION_ADD)
        replacement(expected)
      }
    }
    check.check(params("まず", "First,", languageTag = "ja")).assertSingleIssue { replacement("、") }
    check.check(params("首先", "First,", languageTag = "zh")).assertSingleIssue { replacement("，") }
  }

  @Test
  fun `suggests ASCII punctuation when the base uses localized punctuation`() {
    check.check(params("Hello", "こんにちは。", baseLanguageTag = "ja")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_ADD)
      replacement(".")
    }
  }

  @Test
  fun `still flags localized punctuation missing in base`() {
    check.check(params("こんにちは。", "Hello", languageTag = "ja")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REMOVE)
      position(5, 6)
      param("punctuation", "。")
    }
  }

  @Test
  fun `accepts a Greek semicolon for a base semicolon`() {
    check.check(params("Επιλέξτε ένα;", "Choose one;", languageTag = "el")).assertNoIssues()
  }

  @Test
  fun `understands language tags written with an underscore`() {
    check.check(params("Είστε σίγουροι;", "Are you sure?", languageTag = "el_GR")).assertNoIssues()
    check.check(params("你好", "Hello.", languageTag = "zh_CN")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_ADD)
      replacement("。")
    }
  }

  @Test
  fun `suggests a question mark for a semicolon that translates a Greek question`() {
    check.check(params("Are you sure;", "Είστε σίγουροι;", baseLanguageTag = "el")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REPLACE)
      replacement("?")
      param("punctuation", ";")
      param("expected", "?")
    }
  }

  @Test
  fun `ignores an invisible direction mark after the punctuation`() {
    check.check(params("هل أنت متأكد؟\u200F", "Are you sure?", languageTag = "ar")).assertNoIssues()
    check.check(params("בטוח?\u200F ", "Are you sure?", languageTag = "he")).assertNoIssues()
  }

  @Test
  fun `adds missing punctuation before a trailing direction mark`() {
    check.check(params("مرحبا\u200F", "Hello.", languageTag = "ar")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_ADD)
      position(5, 5)
    }
  }
}
