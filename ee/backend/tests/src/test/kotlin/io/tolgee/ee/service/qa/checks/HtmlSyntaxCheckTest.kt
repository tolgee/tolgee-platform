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
  fun `void element with matching closing tag - no issue`() {
    check.check(params("<br></br>")).assertNoIssues()
  }

  @Test
  fun `multiple void element pairs - no issue`() {
    check.check(params("<br></br>More text<hr></hr>")).assertNoIssues()
  }

  @Test
  fun `void open inside real wrapper - no issue`() {
    check.check(params("<b><br></b>")).assertNoIssues()
  }

  @Test
  fun `void wrapping real content - no issue`() {
    check.check(params("<br><b>text</b></br>")).assertNoIssues()
  }

  @Test
  fun `void open with attributes and matching close - no issue`() {
    check.check(params("""<img src="photo.jpg"></img>""")).assertNoIssues()
  }

  @Test
  fun `extra unmatched void open is tolerated`() {
    // Consistent with a bare `<br>` being accepted.
    check.check(params("<br><br></br>")).assertNoIssues()
  }

  @Test
  fun `interleaved distinct void names - no issue`() {
    // Regression guard: the stack must be keyed per-name so two different void names
    // can be open and close simultaneously.
    check.check(params("<br><hr></br></hr>")).assertNoIssues()
    check.check(params("<br><hr></hr></br>")).assertNoIssues()
  }

  @Test
  fun `void element detection is case-insensitive`() {
    // In HtmlSyntaxCheck, void-element detection is case-insensitive.
    // Leftover uppercase/mixed-case void openers are tolerated like lowercase ones.
    check.check(params("<BR>")).assertNoIssues()
    check.check(params("<Br></Br>")).assertNoIssues()
    check.check(params("<IMG src=\"x\">")).assertNoIssues()
  }

  @Test
  fun `void leftover does not mask unclosed non-void`() {
    // Leftover void opener is tolerated, but a genuinely unclosed real tag still errors.
    check.check(params("<br><b>text</br>")).assertSingleIssue {
      message(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
      param("tag", "<b>")
    }
  }

  @Test
  fun `stray closing void tag is flagged`() {
    // Asymmetric leniency: a close tag with no opener is meaningless
    // even for Tolgee component interpolation, so it still errors.
    check.check(params("</br>")).assertSingleIssue {
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
