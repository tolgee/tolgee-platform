package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InconsistentPlaceholdersCheckTest {
  private val check = InconsistentPlaceholdersCheck()

  private fun params(
    text: String,
    base: String? = null,
    isPlural: Boolean = false,
    textVariants: Map<String, String>? = null,
    baseTextVariants: Map<String, String>? = null,
    textVariantOffsets: Map<String, Int>? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
    isPlural = isPlural,
    textVariants = textVariants,
    baseTextVariants = baseTextVariants,
    textVariantOffsets = textVariantOffsets,
  )

  @Test
  fun `returns empty when base is null`() {
    val results = check.check(params("Hello {name}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Hello {name}", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Hello {name}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when all placeholders match`() {
    val results = check.check(params("Ahoj {name}", "Hello {name}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no placeholders in either`() {
    val results = check.check(params("Ahoj", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing placeholder`() {
    val results = check.check(params("Ahoj", "Hello {name}"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
    assertThat(results[0].params).containsEntry("placeholder", "name")
    assertThat(results[0].replacement).isNull()
  }

  @Test
  fun `detects extra placeholder with position and replacement`() {
    // "Ahoj {extra}" — {extra} starts at index 5, ends at 12
    val results = check.check(params("Ahoj {extra}", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
    assertThat(results[0].params).containsEntry("placeholder", "extra")
    assertThat(results[0].positionStart).isEqualTo(5)
    assertThat(results[0].positionEnd).isEqualTo(12)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects both missing and extra`() {
    // "Ahoj {nom}" — {nom} starts at 5, ends at 10
    val results = check.check(params("Ahoj {nom}", "Hello {name}"))
    assertThat(results).hasSize(2)

    val missingResult = results.first { it.message == QaIssueMessage.QA_PLACEHOLDERS_MISSING }
    assertThat(missingResult.params).containsEntry("placeholder", "name")
    assertThat(missingResult.positionStart).isEqualTo(0)
    assertThat(missingResult.positionEnd).isEqualTo(0)
    assertThat(missingResult.replacement).isNull()

    val extraResult = results.first { it.message == QaIssueMessage.QA_PLACEHOLDERS_EXTRA }
    assertThat(extraResult.params).containsEntry("placeholder", "nom")
    assertThat(extraResult.positionStart).isEqualTo(5)
    assertThat(extraResult.positionEnd).isEqualTo(10)
    assertThat(extraResult.replacement).isEqualTo("")
  }

  @Test
  fun `reports position for each occurrence of duplicate extra placeholder`() {
    // "Ahoj {foo} a {foo}" — first {foo} at 5-10, second at 13-18
    val results = check.check(params("Ahoj {foo} a {foo}", "Hello"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_PLACEHOLDERS_EXTRA }
    assertThat(results).allMatch { it.replacement == "" }
    val positions = results.map { it.positionStart to it.positionEnd }.toSet()
    assertThat(positions).containsExactlyInAnyOrder(5 to 10, 13 to 18)
  }

  @Test
  fun `handles numbered placeholders`() {
    val results = check.check(params("Ahoj {1}", "Hello {0} and {1}"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
    assertThat(results[0].params).containsEntry("placeholder", "0")
  }

  @Test
  fun `handles typed placeholders`() {
    val results = check.check(params("Ahoj", "Hello {count, number}"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("placeholder", "count")
  }

  @Test
  fun `handles styled placeholders`() {
    val results = check.check(params("Cena", "Price {price, number, currency}"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("placeholder", "price")
  }

  @Test
  fun `handles multiple matching placeholders`() {
    val results =
      check.check(
        params(
          "Ahoj {name}, mate {count, number} zprav",
          "Hello {name}, you have {count, number} messages",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles nested select args`() {
    val results =
      check.check(
        params(
          "{gender, select, male {On} female {Ona} other {Ono}}",
          "{gender, select, male {He has {count}} female {She has {count}} other {They have {count}}}",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
    assertThat(results[0].params).containsEntry("placeholder", "count")
  }

  @Test
  fun `returns empty when parse fails on base`() {
    val results = check.check(params("Hello {name}", "Hello {unclosed"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when parse fails on text`() {
    val results = check.check(params("Hello {unclosed", "Hello {name}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles escaped braces`() {
    val results = check.check(params("Ahoj '{'not a placeholder'}'", "Hello '{'not a placeholder'}'"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `ignores hash in standalone text`() {
    // # is a literal when not inside a plural context
    val results = check.check(params("# items by {author}", "# items by {author}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `all types are INCONSISTENT_PLACEHOLDERS`() {
    val results = check.check(params("Ahoj {nom}", "Hello {name}"))
    assertThat(results).allMatch { it.type == QaCheckType.INCONSISTENT_PLACEHOLDERS }
  }

  @Test
  fun `works with plural variants`() {
    val results =
      check.check(
        params(
          text = "{count, plural, one {# polozka} other {# polozek}}",
          base = "{count, plural, one {# item by {author}} other {# items by {author}}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# item by {author}", "other" to "# items by {author}"),
          textVariantOffsets = mapOf("one" to 24, "other" to 39),
        ),
      )
    // Both variants are missing {author}
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_PLACEHOLDERS_MISSING }
    assertThat(results).allMatch { it.params?.get("placeholder") == "author" }
    // Verify plural variant is set
    assertThat(results.map { it.pluralVariant }).containsExactlyInAnyOrder("one", "other")
  }

  @Test
  fun `detects missing placeholder in one plural variant only`() {
    val results =
      check.check(
        params(
          text = "{count, plural, one {# polozka od {author}} other {# polozek}}",
          base = "{count, plural, one {# item by {author}} other {# items by {author}}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka od {author}", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# item by {author}", "other" to "# items by {author}"),
          textVariantOffsets = mapOf("one" to 24, "other" to 51),
        ),
      )
    // Only "other" variant is missing {author}
    assertThat(results).hasSize(1)
    assertThat(results[0].pluralVariant).isEqualTo("other")
    assertThat(results[0].params).containsEntry("placeholder", "author")
  }
}
