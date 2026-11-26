package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class BracketsMismatchCheckTest {
  private val check = BracketsMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
    icuPlaceholders: Boolean = true,
    languageTag: String = "cs",
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = languageTag,
    icuPlaceholders = icuPlaceholders,
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello (world)", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when brackets match`() {
    check.check(params("Ahoj (svete)", "Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when no brackets in either`() {
    check.check(params("Ahoj svete", "Hello world")).assertNoIssues()
  }

  @Test
  fun `detects missing bracket in translation`() {
    // Missing brackets are aggregated into a single issue
    check.check(params("Ahoj svete", "Hello (world)")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
      noReplacement()
      noPosition()
      param("brackets", "(, )")
      param("count", "2")
    }
  }

  @Test
  fun `detects extra bracket in translation`() {
    val results = check.check(params("Ahoj (svete)", "Hello world"))
    results.assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
        position(5, 6)
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `detects different bracket counts`() {
    // Translation has 3 ( and 3 ), source has 1 ( and 1 ) -> 2 extra of each
    check.check(params("Ahoj (svete) (a) (b)", "Hello (world)")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `handles multiple bracket types`() {
    // Missing brackets are aggregated into a single issue
    check.check(params("Ahoj [svete]", "Hello (world) [test]")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
      param("brackets", "(, )")
      param("count", "2")
    }
  }

  @Test
  fun `handles curly braces when ICU is disabled`() {
    check.check(params("Ahoj svete", "Hello {world}", icuPlaceholders = false)).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
      param("brackets", "{, }")
      param("count", "2")
    }
  }

  @Test
  fun `all results have BRACKETS_MISMATCH type`() {
    check.check(params("Ahoj svete", "Hello (world)")).assertAllHaveType(QaCheckType.BRACKETS_MISMATCH)
  }

  // ICU placeholder tests

  @Test
  fun `ignores curly braces when ICU is enabled`() {
    check.check(params("{count} polozek", "{count} items")).assertNoIssues()
  }

  @Test
  fun `ignores curly braces with different ICU args when ICU is enabled`() {
    check.check(params("{name} ma {count} polozek", "{name} has {count, number} items")).assertNoIssues()
  }

  @Test
  fun `still detects round bracket mismatch alongside ICU placeholders`() {
    check.check(params("{count} (extra) polozek", "{count} items")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `checks curly braces when ICU is disabled`() {
    check.check(params("Ahoj {svete}", "Hello world", icuPlaceholders = false)).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "{")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "}")
      }
    }
  }

  @Test
  fun `curly brace mismatch detected when ICU is disabled`() {
    check.check(params("Ahoj svete", "Hello {world}", icuPlaceholders = false)).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
      param("brackets", "{, }")
      param("count", "2")
    }
  }

  @Test
  fun `treats full-width brackets as equivalent`() {
    check
      .check(params("部門 ID （クライアント ID）の必須設定", "Department ID (Client ID) requirement", languageTag = "ja"))
      .assertNoIssues()
    check.check(params("項目［任意］", "Item [optional]", languageTag = "ja")).assertNoIssues()
  }

  @Test
  fun `reports position of an extra full-width bracket`() {
    check.check(params("こんにちは（", "Hello", languageTag = "ja")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_EXTRA)
      position(5, 6)
      param("bracket", "(")
    }
  }

  @Test
  fun `ignores inclusive and optional-plural suffixes in translation`() {
    check.check(params("Associé(e)", "Partner", languageTag = "fr")).assertNoIssues()
    check
      .check(
        params(
          "Dra og slipp filen(e) din(e) til en av variablene",
          "Drag and drop your file(s) onto one of the variables",
          languageTag = "nb",
        ),
      ).assertNoIssues()
    check
      .check(
        params(
          "<name></name> 가 <field></field> 을(를) 업데이트했습니다.",
          "<name></name> updated <field></field>",
          languageTag = "ko",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `ignores optional-plural suffix present only in base`() {
    check.check(params("המסמך/ים", "document(s)", languageTag = "he")).assertNoIssues()
  }

  @Test
  fun `still flags a short bracketed word that is not attached to a word`() {
    check.check(params("Ahoj (ne)", "Hello")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        position(5, 6)
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        position(8, 9)
      }
    }
  }

  @Test
  fun `still flags a long bracketed suffix`() {
    check.check(params("Ahoj(svete)", "Hello")).assertIssues {
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
    }
  }

  @Test
  fun `allows a Latin acronym gloss in a right-to-left translation`() {
    check
      .check(params("إرشادات Acme لاتفاقية عدم الإفصاح (NDA)", "Acme NDA Guidelines", languageTag = "ar"))
      .assertNoIssues()
    check.check(params("הסכם סודיות (NDA)", "Non-disclosure agreement", languageTag = "he")).assertNoIssues()
  }

  @Test
  fun `allows a Latin acronym gloss in other non-Latin scripts`() {
    check.check(params("保密协议（NDA）", "Non-disclosure agreement", languageTag = "zh-Hans")).assertNoIssues()
    check
      .check(
        params("Соглашение о неразглашении (NDA)", "Non-disclosure agreement", languageTag = "ru"),
      ).assertNoIssues()
  }

  @Test
  fun `still flags a Latin gloss in a Latin-script translation`() {
    check.check(params("Dohoda o mlčenlivosti (NDA)", "Non-disclosure agreement")).assertIssues {
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
    }
  }

  @Test
  fun `still flags extra brackets with non-Latin content in right-to-left translation`() {
    check.check(params("مرحبا (عالم)", "Hello", languageTag = "ar")).assertIssues {
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
    }
  }

  @Test
  fun `still flags missing brackets in right-to-left translation that keeps a Latin gloss`() {
    check.check(params("اتفاقية (NDA)", "Agreement (NDA) (draft)", languageTag = "ar")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
    }
  }

  @Test
  fun `still flags an extra non-Latin pair next to a Latin gloss the base also has`() {
    check.check(params("وقع (اتفاقية) (NDA)", "Sign (NDA)", languageTag = "ar")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `keeps short parentheticals in scripts written without spaces`() {
    check.check(params("设置（可选）", "Settings (optional)", languageTag = "zh-Hans")).assertNoIssues()
    check.check(params("设置(可选)", "Settings (optional)", languageTag = "zh-Hans")).assertNoIssues()
    check.check(params("名前(必須)", "Name (required)", languageTag = "ja")).assertNoIssues()
    check.check(params("이름(선택)", "Name (optional)", languageTag = "ko")).assertNoIssues()
    check.check(params("ชื่อ(ไทย)", "Name (Thai)", languageTag = "th")).assertNoIssues()
  }

  @Test
  fun `still flags a missing pair next to optional suffixes`() {
    check.check(params("Soubor(y) k nahrání", "File(s) to upload (optional)")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
    }
  }

  @Test
  fun `treats full-width curly braces as equivalent when ICU placeholders are disabled`() {
    check.check(params("こんにちは｛名前｝", "Hello {name}", icuPlaceholders = false, languageTag = "ja")).assertNoIssues()
    check.check(params("こんにちは｛名前", "Hello {name}", icuPlaceholders = false, languageTag = "ja")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_MISSING)
    }
  }

  @Test
  fun `ignores a base optional suffix next to a parenthetical in a script written without spaces`() {
    check.check(params("删除文件（可选）", "Delete file(s) (optional)", languageTag = "zh-Hans")).assertNoIssues()
    check.check(params("멤버(선택)", "Member(s) (optional)", languageTag = "ko")).assertNoIssues()
  }

  @Test
  fun `reports the position of an extra parenthetical in a script written without spaces`() {
    check.check(params("设置（可选）（新）", "Settings (optional)", languageTag = "zh-Hans")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        position(6, 7)
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        position(8, 9)
      }
    }
  }

  @Test
  fun `ignores Korean particle pairs attached to a placeholder or tag`() {
    check.check(params("{name}(이)가 삭제되었습니다", "{name} was deleted", languageTag = "ko")).assertNoIssues()
    check.check(params("<b>{path}</b>(으)로 이동", "Move to <b>{path}</b>", languageTag = "ko")).assertNoIssues()
  }

  @Test
  fun `still flags a Latin gloss for a language tag without a known script`() {
    check.check(params("Ahoj (svete)", "Hello", languageTag = "cz")).assertIssues {
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
      issue { message(QaIssueMessage.QA_BRACKETS_EXTRA) }
    }
  }
}
