package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.ee.service.qa.checks.lines.SeparatorType
import io.tolgee.ee.service.qa.checks.lines.UnmatchedNewlinesCheck
import io.tolgee.ee.service.qa.checks.lines.extractStructure
import io.tolgee.ee.service.qa.checks.lines.splitLines
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnmatchedNewlinesCheckTest {
  private val check = UnmatchedNewlinesCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when structures match - no empty lines`() {
    check.check(params("Hola\nMundo", "Hello\nWorld")).assertNoIssues()
  }

  @Test
  fun `returns empty when structures match - with empty lines`() {
    check.check(params("Hola\n\nMundo", "Hello\n\nWorld")).assertNoIssues()
  }

  @Test
  fun `returns empty when structures match - multiple empty lines`() {
    check.check(params("A\n\n\nB", "X\n\n\nY")).assertNoIssues()
  }

  @Test
  fun `returns empty when structures match - leading and trailing`() {
    check.check(params("\nHola\n", "\nHello\n")).assertNoIssues()
  }

  // --- Gap mismatch cases (content block counts match) ---

  @Test
  fun `detects missing empty line in gap`() {
    check.check(params("X\n\nY", "A\n\n\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      replacement("\n")
      param("count", "1")
      position(3, 3)
    }
  }

  @Test
  fun `detects extra empty line in gap`() {
    check.check(params("X\n\n\nY", "A\n\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_EXTRA)
      replacement("")
      param("count", "1")
      position(3, 4)
    }
  }

  @Test
  fun `detects multiple missing empty lines`() {
    check.check(params("X\n\nY", "A\n\n\n\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      replacement("\n\n")
      param("count", "2")
    }
  }

  @Test
  fun `detects issues at different positions`() {
    check.check(params("X\n\nY\n\n\nZ", "A\n\n\nB\n\nC")).assertIssues {
      issue {
        message(QaIssueMessage.QA_NEWLINES_MISSING)
        param("count", "1")
      }
      issue {
        message(QaIssueMessage.QA_NEWLINES_EXTRA)
        param("count", "1")
      }
    }
  }

  @Test
  fun `detects missing leading empty lines`() {
    check.check(params("Hola", "\n\nHello")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      position(0, 0)
      replacement("\n\n")
      param("count", "2")
    }
  }

  @Test
  fun `detects extra leading empty lines`() {
    check.check(params("\n\nHola", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_EXTRA)
      position(0, 2)
      replacement("")
      param("count", "2")
    }
  }

  @Test
  fun `detects missing trailing empty lines`() {
    check.check(params("Hola", "Hello\n")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      replacement("\n")
      param("count", "1")
    }
  }

  @Test
  fun `detects extra trailing empty lines`() {
    check.check(params("Hola\n\n", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_EXTRA)
      replacement("")
      param("count", "2")
    }
  }

  // --- Structure mismatch cases (content block counts differ) ---

  @Test
  fun `detects too few content blocks`() {
    check.check(params("Hola\nMundo", "Hello\n\nWorld")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_TOO_FEW_SECTIONS)
      noReplacement()
      noPosition()
      param("expected", "2")
      param("actual", "1")
    }
  }

  @Test
  fun `detects too many content blocks`() {
    check.check(params("Hola\n\nMundo", "Hello\nWorld")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_TOO_MANY_SECTIONS)
      noReplacement()
      noPosition()
      param("expected", "1")
      param("actual", "2")
    }
  }

  @Test
  fun `all types are UNMATCHED_NEWLINES`() {
    check.check(params("X\n\nY\n\n\nZ", "A\n\n\nB\n\nC")).assertAllHaveType(QaCheckType.UNMATCHED_NEWLINES)
  }

  // --- Different line separators (check level) ---

  @Test
  fun `returns empty when CRLF structures match`() {
    check.check(params("Hola\r\n\r\nMundo", "Hello\r\n\r\nWorld")).assertNoIssues()
  }

  @Test
  fun `returns empty when CR structures match`() {
    check.check(params("Hola\r\rMundo", "Hello\r\rWorld")).assertNoIssues()
  }

  @Test
  fun `detects missing empty line with CRLF`() {
    check.check(params("X\r\nY", "A\r\n\r\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_TOO_FEW_SECTIONS)
    }
  }

  @Test
  fun `detects extra empty line with CRLF`() {
    check.check(params("X\r\n\r\n\r\nY", "A\r\n\r\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_EXTRA)
      replacement("")
      param("count", "1")
    }
  }

  @Test
  fun `replacement uses detected separator for CRLF`() {
    check.check(params("X\r\n\r\nY", "A\r\n\r\n\r\nB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      replacement("\r\n")
    }
  }

  @Test
  fun `replacement uses detected separator for CR`() {
    check.check(params("X\r\rY", "A\r\r\rB")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_MISSING)
      replacement("\r")
    }
  }

  @Test
  fun `detects structure mismatch with CRLF`() {
    check.check(params("Hola\r\n\r\nMundo", "Hello\r\nWorld")).assertSingleIssue {
      message(QaIssueMessage.QA_NEWLINES_TOO_MANY_SECTIONS)
    }
  }

  // --- splitLines unit tests (not QA check assertions — keep raw assertThat) ---

  @Test
  fun `splitLines - simple LF`() {
    val lines = splitLines("Hello\nWorld")
    assertThat(lines).hasSize(2)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.LF)
    assertThat(lines[1].text).isEqualTo("World")
    assertThat(lines[1].index).isEqualTo(6)
    assertThat(lines[1].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - simple CRLF`() {
    val lines = splitLines("Hello\r\nWorld")
    assertThat(lines).hasSize(2)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.CRLF)
    assertThat(lines[1].text).isEqualTo("World")
    assertThat(lines[1].index).isEqualTo(7)
    assertThat(lines[1].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - simple CR`() {
    val lines = splitLines("Hello\rWorld")
    assertThat(lines).hasSize(2)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.CR)
    assertThat(lines[1].text).isEqualTo("World")
    assertThat(lines[1].index).isEqualTo(6)
    assertThat(lines[1].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - no separators`() {
    val lines = splitLines("Hello")
    assertThat(lines).hasSize(1)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - empty string`() {
    val lines = splitLines("")
    assertThat(lines).hasSize(1)
    assertThat(lines[0].text).isEqualTo("")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - trailing LF produces extra empty line`() {
    val lines = splitLines("Hello\n")
    assertThat(lines).hasSize(2)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].type).isEqualTo(SeparatorType.LF)
    assertThat(lines[1].text).isEqualTo("")
    assertThat(lines[1].index).isEqualTo(6)
    assertThat(lines[1].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - trailing CRLF produces extra empty line`() {
    val lines = splitLines("Hello\r\n")
    assertThat(lines).hasSize(2)
    assertThat(lines[0].text).isEqualTo("Hello")
    assertThat(lines[0].type).isEqualTo(SeparatorType.CRLF)
    assertThat(lines[1].text).isEqualTo("")
    assertThat(lines[1].index).isEqualTo(7)
    assertThat(lines[1].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - multiple CRLF with empty lines`() {
    val lines = splitLines("A\r\n\r\nB")
    assertThat(lines).hasSize(3)
    assertThat(lines[0].text).isEqualTo("A")
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[0].type).isEqualTo(SeparatorType.CRLF)
    assertThat(lines[1].text).isEqualTo("")
    assertThat(lines[1].index).isEqualTo(3)
    assertThat(lines[1].type).isEqualTo(SeparatorType.CRLF)
    assertThat(lines[2].text).isEqualTo("B")
    assertThat(lines[2].index).isEqualTo(5)
    assertThat(lines[2].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - multiple CR with empty lines`() {
    val lines = splitLines("A\r\rB")
    assertThat(lines).hasSize(3)
    assertThat(lines[0].text).isEqualTo("A")
    assertThat(lines[0].type).isEqualTo(SeparatorType.CR)
    assertThat(lines[1].text).isEqualTo("")
    assertThat(lines[1].index).isEqualTo(2)
    assertThat(lines[1].type).isEqualTo(SeparatorType.CR)
    assertThat(lines[2].text).isEqualTo("B")
    assertThat(lines[2].index).isEqualTo(3)
    assertThat(lines[2].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  @Test
  fun `splitLines - consecutive LFs`() {
    val lines = splitLines("\n\n\n")
    assertThat(lines).hasSize(4)
    assertThat(lines).allMatch { it.text == "" }
    assertThat(lines[0].index).isEqualTo(0)
    assertThat(lines[1].index).isEqualTo(1)
    assertThat(lines[2].index).isEqualTo(2)
    assertThat(lines[3].index).isEqualTo(3)
    assertThat(lines[3].type).isEqualTo(SeparatorType.UNKNOWN)
  }

  // --- extractStructure with different separators ---

  @Test
  fun `extractStructure - CRLF separator`() {
    val s = extractStructure("Hello\r\n\r\nWorld")
    assertThat(s.separatorType).isEqualTo(SeparatorType.CRLF)
    assertThat(s.gaps).hasSize(3)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(1)
    assertThat(s.gaps[2].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - CR separator`() {
    val s = extractStructure("Hello\r\rWorld")
    assertThat(s.separatorType).isEqualTo(SeparatorType.CR)
    assertThat(s.gaps).hasSize(3)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(1)
    assertThat(s.gaps[2].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - CRLF endIndex tracks correctly`() {
    val s = extractStructure("A\r\n\r\n\r\nB")
    assertThat(s.separatorType).isEqualTo(SeparatorType.CRLF)
    assertThat(s.gaps).hasSize(3)
    assertThat(s.gaps[1].lineCount).isEqualTo(2)
    assertThat(s.gaps[1].endIndex).isEqualTo(7)
  }

  // --- extractStructure unit tests ---

  @Test
  fun `extractStructure - no empty lines`() {
    val s = extractStructure("Hello\nWorld")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(2)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - single empty line between content`() {
    val s = extractStructure("Hello\n\nWorld")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(3)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(1)
    assertThat(s.gaps[2].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - multiple consecutive empty lines`() {
    val s = extractStructure("My\nText\n\n\nMore text")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(3)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(2)
    assertThat(s.gaps[2].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - complex structure`() {
    val s = extractStructure("My\nText\n\n\nMore text\n\nEven more text")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(4)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(2)
    assertThat(s.gaps[2].lineCount).isEqualTo(1)
    assertThat(s.gaps[3].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - leading empty lines`() {
    val s = extractStructure("\n\nHello")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(2)
    assertThat(s.gaps[0].lineCount).isEqualTo(2)
    assertThat(s.gaps[1].lineCount).isEqualTo(0)
  }

  @Test
  fun `extractStructure - trailing empty line`() {
    val s = extractStructure("Hello\n")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(2)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(1)
  }

  @Test
  fun `extractStructure - only empty lines`() {
    val s = extractStructure("\n\n")
    assertThat(s.separatorType).isEqualTo(SeparatorType.LF)
    assertThat(s.gaps).hasSize(1)
    assertThat(s.gaps[0].lineCount).isEqualTo(3)
  }

  @Test
  fun `extractStructure - just text`() {
    val s = extractStructure("Just some text")
    assertThat(s.separatorType).isEqualTo(SeparatorType.UNKNOWN)
    assertThat(s.gaps).hasSize(2)
    assertThat(s.gaps[0].lineCount).isEqualTo(0)
    assertThat(s.gaps[1].lineCount).isEqualTo(0)
  }
}
