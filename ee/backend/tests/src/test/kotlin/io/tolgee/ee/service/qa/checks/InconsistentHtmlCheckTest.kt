package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class InconsistentHtmlCheckTest {
  private val check = InconsistentHtmlCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "de",
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("<b>Hello</b>")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("<b>Hallo</b>", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "<b>Hello</b>")).assertNoIssues()
  }

  @Test
  fun `returns empty when tags match`() {
    check.check(params("<b>Hallo</b>", "<b>Hello</b>")).assertNoIssues()
  }

  @Test
  fun `returns empty when neither has tags`() {
    check.check(params("Hallo", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when attributes differ but tag names match`() {
    check
      .check(
        params(
          """<a href="/de/hilfe">Hilfe</a>""",
          """<a href="/en/help">Help</a>""",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `detects missing tags`() {
    check.check(params("Hallo", "<b>Hello</b>")).assertIssues {
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        noReplacement()
        noPosition()
        param("tag", "<b>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        noReplacement()
        noPosition()
        param("tag", "</b>")
      }
    }
  }

  @Test
  fun `detects extra tags`() {
    check.check(params("<b>Hallo</b>", "Hello")).assertIssues {
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        replacement("")
        position(0, 3)
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        replacement("")
        position(8, 12)
      }
    }
  }

  @Test
  fun `detects mixed - different tags`() {
    check.check(params("<i>Hallo</i>", "<b>Hello</b>")).assertIssues {
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        param("tag", "<i>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        param("tag", "</i>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        param("tag", "<b>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        param("tag", "</b>")
      }
    }
  }

  @Test
  fun `handles self-closing tags`() {
    check.check(params("Line1 Line2", "Line1<br/>Line2")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_TAG_MISSING)
      param("tag", "<br/>")
    }
  }

  @Test
  fun `handles duplicate tags`() {
    check.check(params("<b>Hallo</b>", "<b><b>Hello</b></b>")).assertIssues {
      issue { message(QaIssueMessage.QA_HTML_TAG_MISSING) }
      issue { message(QaIssueMessage.QA_HTML_TAG_MISSING) }
    }
  }

  @Test
  fun `handles void elements without slash`() {
    check.check(params("text", "<br>text")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_TAG_MISSING)
      param("tag", "<br>")
    }
  }

  @Test
  fun `custom tag names`() {
    check.check(params("<link>klicken</link>", "<link>click</link>")).assertNoIssues()
  }

  @Test
  fun `custom tag name mismatch`() {
    check.check(params("<bold>Hallo</bold>", "<link>click</link>")).assertIssues {
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        param("tag", "<bold>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_EXTRA)
        param("tag", "</bold>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        param("tag", "<link>")
      }
      issue {
        message(QaIssueMessage.QA_HTML_TAG_MISSING)
        param("tag", "</link>")
      }
    }
  }

  @Test
  fun `all types are INCONSISTENT_HTML`() {
    check.check(params("<i>Hallo</i>", "<b>Hello</b>")).assertAllHaveType(QaCheckType.INCONSISTENT_HTML)
  }
}
