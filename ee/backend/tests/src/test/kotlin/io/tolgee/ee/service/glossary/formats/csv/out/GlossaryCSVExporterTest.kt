package io.tolgee.ee.service.glossary.formats.csv.out

import io.tolgee.ee.service.glossary.formats.createGlossary
import io.tolgee.ee.service.glossary.formats.glossary
import io.tolgee.ee.service.glossary.formats.glossaryTerm
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class GlossaryCSVExporterTest {
  @Test
  fun `exports CSV with all fields`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("A fruit")
          withTranslations("en" to "Apple", "cs" to "Jablko", "de" to "Apfel")
          translatable(true)
          caseSensitive(false)
          abbreviation(false)
          forbiddenTerm(false)
        }
        glossaryTerm {
          withDescription("Application Programming Interface")
          withTranslations("en" to "API", "cs" to "API", "de" to "API")
          translatable(false)
          caseSensitive(true)
          abbreviation(true)
          forbiddenTerm(false)
        }
        glossaryTerm {
          withDescription("Forbidden term")
          withTranslations("en" to "BadWord", "cs" to "SpatneSlovo", "de" to "SchlechteWort")
          translatable(true)
          caseSensitive(false)
          abbreviation(false)
          forbiddenTerm(true)
        }
      }

    val languageTags = setOf("en", "cs", "de")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","cs","de"
      "Apple","A fruit","Yes","No","No","No","Jablko","Apfel"
      "API","Application Programming Interface","No","Yes","Yes","No","API","API"
      "BadWord","Forbidden term","Yes","No","No","Yes","SpatneSlovo","SchlechteWort"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with minimal data`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withTranslation("en", "Minimal")
        }
      }

    val languageTags = setOf("en")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden"
      "Minimal","","Yes","No","No","No"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with only non-base language translations`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Hello term")
          withTranslations("en" to "Hello", "cs" to "Ahoj", "de" to "Hallo")
        }
      }

    val languageTags = setOf("cs", "de")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","cs","de"
      "Hello","Hello term","Yes","No","No","No","Ahoj","Hallo"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with semicolon delimiter`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Test description")
          withTranslations("en" to "TestValue", "cs" to "TestHodnota")
        }
      }

    val languageTags = setOf("en", "cs")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ';')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term";"description";"translatable";"casesensitive";"abbreviation";"forbidden";"cs"
      "TestValue";"Test description";"Yes";"No";"No";"No";"TestHodnota"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with mixed boolean flags`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Description 1")
          withTranslation("en", "Value1")
          translatable(true)
          caseSensitive(true)
          abbreviation(true)
          forbiddenTerm(true)
        }
        glossaryTerm {
          withDescription("Description 2")
          withTranslation("en", "Value2")
          translatable(false)
          caseSensitive(false)
          abbreviation(false)
          forbiddenTerm(false)
        }
      }

    val languageTags = setOf("en")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden"
      "Value1","Description 1","Yes","Yes","Yes","Yes"
      "Value2","Description 2","No","No","No","No"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with missing translations`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Test term")
          withTranslations("en" to "TestValue", "de" to "TestWert")
          // Missing cs translation
        }
        glossaryTerm {
          withDescription("Another term")
          withTranslation("cs", "AnotherValue")
          // Missing en and de translations
        }
      }

    val languageTags = setOf("en", "cs", "de")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","cs","de"
      "TestValue","Test term","Yes","No","No","No","","TestWert"
      "","Another term","Yes","No","No","No","AnotherValue",""
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports empty CSV when no terms`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
      }

    val languageTags = setOf("en", "cs")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","cs"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with special characters and escaping`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Description with \"quotes\" and commas, here")
          withTranslations(
            "en" to "Text with \"quotes\", commas and\nnew lines",
            "cs" to "Text s \"uvozovkami\", čárkami a\nnovými řádky",
          )
        }
      }

    val languageTags = setOf("en", "cs")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    // CSV should properly escape quotes and handle special characters
    assertThat(result).contains("\"Text with \"\"quotes\"\", commas and\nnew lines\"")
    assertThat(result).contains("\"Description with \"\"quotes\"\" and commas, here\"")
    assertThat(result).contains("\"Text s \"\"uvozovkami\"\", čárkami a\nnovými řádky\"")
  }

  @Test
  fun `exports CSV with different base language`() {
    val glossary =
      glossary {
        withBaseLanguageTag("cs")
        glossaryTerm {
          withDescription("Popis termínu")
          withTranslations("cs" to "ČeskýTermín", "en" to "CzechTerm", "de" to "TschechischBegriff")
        }
      }

    val languageTags = setOf("cs", "en", "de")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden","de","en"
      "ČeskýTermín","Popis termínu","Yes","No","No","No","TschechischBegriff","CzechTerm"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `exports CSV with only base language`() {
    val glossary =
      glossary {
        withBaseLanguageTag("en")
        glossaryTerm {
          withDescription("Only English term")
          withTranslation("en", "EnglishValue")
        }
      }

    val languageTags = setOf("en")
    val exporter = GlossaryCSVExporter(glossary, glossary.terms, languageTags, ',')

    val result = exporter.export().bufferedReader().readText()

    assertThat(result).isEqualToNormalizingNewlines(
      """
      "term","description","translatable","casesensitive","abbreviation","forbidden"
      "EnglishValue","Only English term","Yes","No","No","No"
      
      """.trimIndent(),
    )
  }

  @Test
  fun `header generation excludes base language from column headers`() {
    val glossary = createGlossary("en")
    val languageTags = setOf("en", "cs", "de", "fr")
    val exporter = GlossaryCSVExporter(glossary, emptyList(), languageTags, ',')

    val headers = exporter.headers

    assertThat(headers).contains("term", "description", "translatable", "casesensitive", "abbreviation", "forbidden")
    assertThat(headers).contains("cs", "de", "fr")
    assertThat(headers).doesNotContain("en") // base language should not be in additional columns
  }

  @Test
  fun `language tags are sorted alphabetically`() {
    val glossary = createGlossary("en")
    val languageTags = setOf("en", "fr", "cs", "de", "ar")
    val exporter = GlossaryCSVExporter(glossary, emptyList(), languageTags, ',')

    val sortedLanguages = exporter.languageTagsWithoutBaseLanguage

    assertThat(sortedLanguages).containsExactly("ar", "cs", "de", "fr")
  }
}
