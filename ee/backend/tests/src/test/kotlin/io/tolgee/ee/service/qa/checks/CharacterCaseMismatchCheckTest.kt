package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class CharacterCaseMismatchCheckTest {
  private val check = CharacterCaseMismatchCheck()

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
    check.check(params("hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("hello", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when case matches - both upper`() {
    check.check(params("Bonjour", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when case matches - both lower`() {
    check.check(params("bonjour", "hello")).assertNoIssues()
  }

  @Test
  fun `detects lowercase when base is capitalized`() {
    check.check(params("bonjour", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("B")
      position(0, 1)
    }
  }

  @Test
  fun `detects uppercase when base is lowercase`() {
    check.check(params("Bonjour", "hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
      replacement("b")
      position(0, 1)
    }
  }

  @Test
  fun `skips leading non-letter characters`() {
    check.check(params("  123 bonjour", "  123 Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      position(6, 7)
    }
  }

  @Test
  fun `returns empty when no letters in text`() {
    check.check(params("123", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when no letters in base`() {
    check.check(params("Hello", "123")).assertNoIssues()
  }

  @Test
  fun `uses Turkish locale for uppercase conversion`() {
    check.check(params("istanbul", "Hello", languageTag = "tr")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("\u0130") // Turkish capital I with dot
    }
  }

  @Test
  fun `uses Turkish locale for lowercase conversion`() {
    check.check(params("Istanbul", "hello", languageTag = "tr")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
      replacement("\u0131") // Turkish lowercase dotless i
    }
  }

  @Test
  fun `all types are CHARACTER_CASE_MISMATCH`() {
    check.check(params("bonjour", "Hello")).assertAllHaveType(QaCheckType.CHARACTER_CASE_MISMATCH)
  }

  @Test
  fun `does not flag first letter inside an HTML tag`() {
    // First letter is the `b` of the <b> tag — intentionally not re-checked against
    // the real first word; we only filter, we don't advance to the next letter.
    check.check(params("<b>hello</b>", "Hello")).assertNoIssues()
  }

  @Test
  fun `does not flag first letter inside an ICU placeholder`() {
    check.check(params("{name} world", "Hello")).assertNoIssues()
  }

  @Test
  fun `does not flag first letter inside a URL`() {
    check.check(params("https://example.com is here", "Hello")).assertNoIssues()
  }

  @Test
  fun `flags genuine mismatch when first letter is outside blocked ranges`() {
    check.check(params("bonjour <b>x</b>", "Hello world")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("B")
      position(0, 1)
    }
  }

  @Test
  fun `does not compare case when the base starts with a placeholder`() {
    check.check(params("Zaznaczono {count} słowo", "{count} word highlighted", languageTag = "pl")).assertNoIssues()
  }

  @Test
  fun `does not compare case when the translation starts with a placeholder`() {
    check.check(params("{count} élément ignoré", "Skipped {count} item", languageTag = "fr")).assertNoIssues()
  }

  @Test
  fun `does not compare case when a plural variant starts with the number`() {
    check
      .check(
        QaCheckParams(
          baseText = "{count, plural, one {# word highlighted} other {# words highlighted}}",
          text = "{count, plural, one {Zaznaczono # słowo} other {Zaznaczono # słów}}",
          baseLanguageTag = "en",
          languageTag = "pl",
          isPlural = true,
          textVariants = mapOf("one" to "Zaznaczono # słowo", "other" to "Zaznaczono # słów"),
          baseTextVariants = mapOf("one" to "# word highlighted", "other" to "# words highlighted"),
          textVariantOffsets = mapOf("one" to 21, "other" to 48),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `does not use a base letter inside a placeholder or HTML tag`() {
    check.check(params("Napsal „{author}“", "\"{author}\" wrote")).assertNoIssues()
    check.check(params("Položky", "<b>items</b>")).assertNoIssues()
  }

  @Test
  fun `allows capitalized German and Luxembourgish nouns`() {
    check.check(params("Zeilen", "rows", languageTag = "de-DE")).assertNoIssues()
    check.check(params("Zeilen", "rows", languageTag = "lb")).assertNoIssues()
  }

  @Test
  fun `still flags lowercase German translation of a capitalized base`() {
    check.check(params("anzeigen", "Show", languageTag = "de-DE")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("A")
      position(0, 1)
    }
  }

  @Test
  fun `does not flag when the base starts with a brand name or code literal`() {
    check
      .check(
        params("Anmodning om iManage-adgangskontrol mislykkedes", "iManage access check request failed"),
      ).assertNoIssues()
    check.check(params("Télécharger index.html", "index.html download")).assertNoIssues()
    check.check(params("Rozsah refresh_token je povinný", "refresh_token scope is required")).assertNoIssues()
    check.check(params("Adresa name@example.com je obsazená", "name@example.com is taken")).assertNoIssues()
  }

  @Test
  fun `does not flag when the translation starts with a brand name or code literal`() {
    check.check(params("iManage yüklemelerine izin ver", "Allow iManage uploads", languageTag = "tr")).assertNoIssues()
    check.check(params("vLLM sunucusunu seç", "Select the vLLM server", languageTag = "tr")).assertNoIssues()
    check.check(params("index.html herunterladen", "Download index.html", languageTag = "de")).assertNoIssues()
  }

  @Test
  fun `still flags a plain word followed by sentence punctuation`() {
    check.check(params("hotovo. Další krok", "Done. Next step")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
    }
  }

  @Test
  fun `still flags an all-caps acronym moved from the start`() {
    check.check(params("clé API", "API key", languageTag = "fr")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
    }
  }

  @Test
  fun `skips languages written in a script without letter case`() {
    listOf("ja", "ko", "zh-Hans", "zh-TW", "ar", "he", "th", "hi").forEach { tag ->
      check.check(params("test value", "Copy the value", languageTag = tag)).assertNoIssues()
    }
  }

  @Test
  fun `still checks languages written in cased scripts`() {
    listOf("cs", "ru", "el", "hy", "sr-Cyrl").forEach { tag ->
      check.check(params("test", "Copy the value", languageTag = tag)).assertSingleIssue {
        message(QaIssueMessage.QA_CASE_CAPITALIZE)
      }
    }
  }

  @Test
  fun `does not flag when the base starts with a lowercase abbreviation`() {
    check.check(params("Bijv. Alleen retailbankieren", "e.g. Only retail banking", languageTag = "nl")).assertNoIssues()
    check.check(params("P. ex. Réviseur", "e.g. Reviewer", languageTag = "fr")).assertNoIssues()
    check.check(params("Tj. vše", "i.e. everything", languageTag = "cs")).assertNoIssues()
    check.check(params("Atd. je zkratka", "etc. is an abbreviation", languageTag = "cs")).assertNoIssues()
    check.check(params("Versus", "vs. the rest", languageTag = "cs")).assertNoIssues()
  }

  @Test
  fun `still flags hyphenated and slash-joined title-case words`() {
    check.check(params("anmeldung erforderlich", "Sign-In required")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
    }
    check.check(params("ano/ne otázka", "Yes/No question")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
    }
    check.check(params("E-Mail odeslán", "email sent")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
    }
  }

  @Test
  fun `still flags a word followed by an ellipsis without a space`() {
    check.check(params("načítání...prosím", "Loading...please")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
    }
  }

  @Test
  fun `still compares case when a non-plural string starts with a hash`() {
    check.check(params("#Obecné kanál", "#general channel")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
      position(1, 2)
    }
  }

  @Test
  fun `does not suggest lowercasing an acronym that starts the translation`() {
    check.check(params("URL du fichier", "file URL", languageTag = "fr")).assertNoIssues()
    check.check(params("ID klíče", "key id")).assertNoIssues()
  }

  @Test
  fun `still suggests lowercasing a single capital letter word`() {
    check.check(params("A soubor", "and a file")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
    }
  }

  @Test
  fun `skips a base language written in a script without letter case`() {
    check.check(params("Hello", "გამარჯობა", baseLanguageTag = "ka", languageTag = "en")).assertNoIssues()
  }

  @Test
  fun `uses Turkish casing for a language tag written with an underscore`() {
    check.check(params("istanbul", "Istanbul", languageTag = "tr_TR")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("İ")
    }
  }
}
