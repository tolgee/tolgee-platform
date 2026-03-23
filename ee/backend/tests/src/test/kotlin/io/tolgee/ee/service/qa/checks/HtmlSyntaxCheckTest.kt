package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class HtmlSyntaxCheckTest {
  private val check = HtmlSyntaxCheck()

  private fun params(text: String) =
    QaCheckParams(
      baseText = null,
      text = text,
      baseLanguageTag = null,
      languageTag = "en",
    )

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when no HTML tags present`() {
    check.check(params("Hello world")).assertNoIssues()
  }

  @Test
  fun `returns empty when all tags properly paired`() {
    check.check(params("<b>bold</b>")).assertNoIssues()
  }

  @Test
  fun `returns empty for nested properly paired tags`() {
    check.check(params("<div><b>text</b></div>")).assertNoIssues()
  }

  @Test
  fun `returns empty for self-closing tags`() {
    check.check(params("<br/>")).assertNoIssues()
    check.check(params("<br />")).assertNoIssues()
  }

  @Test
  fun `returns empty for void elements without slash`() {
    check.check(params("<br>")).assertNoIssues()
    check.check(params("<hr>")).assertNoIssues()
    check.check(params("<img>")).assertNoIssues()
    check.check(params("""<img src="photo.jpg">""")).assertNoIssues()
  }

  @Test
  fun `detects unclosed tag`() {
    check.check(params("<b>text")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
      noReplacement()
      position(0, 3)
      param("tag", "<b>")
    }
  }

  @Test
  fun `detects unopened closing tag`() {
    check.check(params("text</b>")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNOPENED_TAG)
      replacement("")
      position(4, 8)
      param("tag", "</b>")
    }
  }

  @Test
  fun `detects multiple unclosed tags`() {
    check.check(params("<b><i>text")).assertIssues {
      issue { message(QaIssueMessage.QA_HTML_UNCLOSED_TAG) }
      issue { message(QaIssueMessage.QA_HTML_UNCLOSED_TAG) }
    }
  }

  @Test
  fun `detects multiple unopened tags`() {
    check.check(params("text</i></b>")).assertIssues {
      issue { message(QaIssueMessage.QA_HTML_UNOPENED_TAG) }
      issue { message(QaIssueMessage.QA_HTML_UNOPENED_TAG) }
    }
  }

  @Test
  fun `handles misnested but paired tags leniently`() {
    check.check(params("<b><i>text</b></i>")).assertNoIssues()
  }

  @Test
  fun `handles mixed valid and invalid`() {
    check.check(params("<b>bold</b><i>unclosed")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
      param("tag", "<i>")
    }
  }

  @Test
  fun `does not require base text`() {
    check.check(params("<b>text")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
    }
  }

  @Test
  fun `handles custom tag names`() {
    check.check(params("<myComponent>text")).assertSingleIssue {
      param("tag", "<myComponent>")
    }
  }

  @Test
  fun `void element with closing tag - closing is orphaned`() {
    check.check(params("<br></br>")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNOPENED_TAG)
      param("tag", "</br>")
    }
  }

  @Test
  fun `all types are HTML_SYNTAX`() {
    check.check(params("<b>text</i>")).assertAllHaveType(QaCheckType.HTML_SYNTAX)
  }

  @Test
  fun `position of unclosed tag points to opening tag`() {
    check.check(params("Hello <b>world")).assertSingleIssue {
      position(6, 9)
    }
  }

  @Test
  fun `position of unopened tag points to closing tag`() {
    check.check(params("Hello world</b>")).assertSingleIssue {
      position(11, 15)
    }
  }
}
