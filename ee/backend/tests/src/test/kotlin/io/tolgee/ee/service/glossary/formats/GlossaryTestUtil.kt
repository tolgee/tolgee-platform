package io.tolgee.ee.service.glossary.formats

import io.tolgee.testing.assertions.Assertions.assertThat

fun List<ImportGlossaryTerm>.assertSize(expectedSize: Int) {
  assertThat(this).hasSize(expectedSize)
}

fun List<ImportGlossaryTerm>.assertTerm(
  term: String,
  block: TermAssertion.() -> Unit = {},
) {
  val foundTerm = find { it.term == term }
  assertThat(foundTerm).describedAs("Term '$term' should be found").isNotNull
  TermAssertion(foundTerm!!).apply(block)
}

fun List<ImportGlossaryTerm>.assertTermWithTranslation(
  languageTag: String,
  translation: String,
  block: TermAssertion.() -> Unit = {},
) {
  val foundTerm = find { it.translations[languageTag] == translation }
  assertThat(foundTerm)
    .describedAs("Term with translation '$languageTag'='$translation' should be found")
    .isNotNull
  TermAssertion(foundTerm!!).apply(block)
}

fun List<ImportGlossaryTerm>.assertTermWithDescription(
  description: String,
  block: TermAssertion.() -> Unit = {},
) {
  val foundTerm = find { it.description == description }
  assertThat(foundTerm)
    .describedAs("Term with description '$description' should be found")
    .isNotNull
  TermAssertion(foundTerm!!).apply(block)
}

class TermAssertion(
  private val term: ImportGlossaryTerm,
) {
  fun hasDescription(description: String) {
    assertThat(term.description).isEqualTo(description)
  }

  fun hasNoDescription() {
    assertThat(term.description).isNull()
  }

  fun hasTranslation(
    languageTag: String,
    text: String,
  ) {
    assertThat(term.translations[languageTag]).isEqualTo(text)
  }

  fun hasNoTranslation(languageTag: String) {
    assertThat(term.translations.containsKey(languageTag)).isFalse()
  }

  fun hasNoTerm() {
    assertThat(term.term).isNull()
  }

  fun isTranslatable() {
    assertThat(term.flagNonTranslatable).isFalse()
  }

  fun isNotTranslatable() {
    assertThat(term.flagNonTranslatable).isTrue()
  }

  fun isCaseSensitive() {
    assertThat(term.flagCaseSensitive).isTrue()
  }

  fun isNotCaseSensitive() {
    assertThat(term.flagCaseSensitive).isFalse()
  }

  fun isAbbreviation() {
    assertThat(term.flagAbbreviation).isTrue()
  }

  fun isNotAbbreviation() {
    assertThat(term.flagAbbreviation).isFalse()
  }

  fun isForbidden() {
    assertThat(term.flagForbiddenTerm).isTrue()
  }

  fun isNotForbidden() {
    assertThat(term.flagForbiddenTerm).isFalse()
  }

  fun hasNoFlags() {
    assertThat(term.flagNonTranslatable).isNull()
    assertThat(term.flagCaseSensitive).isNull()
    assertThat(term.flagAbbreviation).isNull()
    assertThat(term.flagForbiddenTerm).isNull()
  }
}
