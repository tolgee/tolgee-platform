package io.tolgee.ee.service.glossary.formats.csv.`in`

import io.tolgee.ee.service.glossary.formats.ImportGlossaryTerm
import io.tolgee.ee.service.glossary.formats.assertSize
import io.tolgee.ee.service.glossary.formats.assertTerm
import io.tolgee.ee.service.glossary.formats.assertTermWithDescription
import io.tolgee.ee.service.glossary.formats.assertTermWithTranslation
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class GlossaryCSVParserTest {
  @Test
  fun `parses CSV with all fields`() {
    val csvContent =
      """
      term,description,translatable,casesensitive,abbreviation,forbidden,en,cs,de
      Apple,A fruit,true,false,false,false,Apple,Jablko,Apfel
      API,Application Programming Interface,false,true,true,false,API,API,API
      BadWord,Forbidden term,true,false,false,true,BadWord,SpatneSlovo,SchlechteWort
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(3)

    terms.assertTerm("Apple") {
      hasDescription("A fruit")
      hasTranslation("en", "Apple")
      hasTranslation("cs", "Jablko")
      hasTranslation("de", "Apfel")
      isTranslatable()
      isNotCaseSensitive()
      isNotAbbreviation()
      isNotForbidden()
    }

    terms.assertTerm("API") {
      hasDescription("Application Programming Interface")
      hasTranslation("en", "API")
      hasTranslation("cs", "API")
      hasTranslation("de", "API")
      isNotTranslatable()
      isCaseSensitive()
      isAbbreviation()
      isNotForbidden()
    }

    terms.assertTerm("BadWord") {
      hasDescription("Forbidden term")
      hasTranslation("en", "BadWord")
      hasTranslation("cs", "SpatneSlovo")
      hasTranslation("de", "SchlechteWort")
      isTranslatable()
      isNotCaseSensitive()
      isNotAbbreviation()
      isForbidden()
    }
  }

  @Test
  fun `parses CSV with minimal data`() {
    val csvContent =
      """
      term,en
      MinimalTerm,Minimal
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(1)
    terms.assertTerm("MinimalTerm") {
      hasNoDescription()
      hasTranslation("en", "Minimal")
      hasNoFlags()
    }
  }

  @Test
  fun `parses CSV with only translations`() {
    val csvContent =
      """
      en,cs,de
      Hello,Ahoj,Hallo
      World,Svět,Welt
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(2)
    terms.assertTermWithTranslation("en", "Hello") {
      hasNoTerm()
      hasNoDescription()
      hasTranslation("en", "Hello")
      hasTranslation("cs", "Ahoj")
      hasTranslation("de", "Hallo")
      hasNoFlags()
    }

    terms.assertTermWithTranslation("en", "World") {
      hasNoTerm()
      hasNoDescription()
      hasTranslation("en", "World")
      hasTranslation("cs", "Svět")
      hasTranslation("de", "Welt")
      hasNoFlags()
    }
  }

  @Test
  fun `parses CSV with semicolon delimiter`() {
    val csvContent =
      """
      term;description;en;cs
      TestTerm;Test description;TestValue;TestHodnota
      """.trimIndent()

    val terms = parseCSV(csvContent, ';')

    terms.assertSize(1)
    terms.assertTerm("TestTerm") {
      hasDescription("Test description")
      hasTranslation("en", "TestValue")
      hasTranslation("cs", "TestHodnota")
    }
  }

  @Test
  fun `parses boolean flag variations`() {
    val csvContent =
      """
      term,translatable,casesensitive,abbreviation,forbidden,en
      Term1,true,1,yes,t,Value1
      Term2,false,0,no,f,Value2
      Term3,TRUE,Y,T,FALSE,Value3
      Term4,invalid,n,false,gibberish,Value4
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(4)

    terms.assertTerm("Term1") {
      isTranslatable()
      isCaseSensitive()
      isAbbreviation()
      isForbidden()
    }

    terms.assertTerm("Term2") {
      isNotTranslatable()
      isNotCaseSensitive()
      isNotAbbreviation()
      isNotForbidden()
    }

    terms.assertTerm("Term3") {
      isTranslatable()
      isCaseSensitive()
      isAbbreviation()
      isNotForbidden()
    }

    terms.assertTerm("Term4") {
      isNotTranslatable() // invalid defaults to false
      isNotCaseSensitive()
      isNotForbidden() // invalid defaults to false
      isNotAbbreviation()
    }
  }

  @Test
  fun `skips empty rows`() {
    val csvContent =
      """
      term,en
      ValidTerm,ValidValue
      ,,
      
      AnotherTerm,AnotherValue
      ,
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(2)
    terms.assertTerm("ValidTerm") {
      hasTranslation("en", "ValidValue")
    }
    terms.assertTerm("AnotherTerm") {
      hasTranslation("en", "AnotherValue")
    }
  }

  @Test
  fun `handles case insensitive headers`() {
    val csvContent =
      """
      TERM,DESCRIPTION,TRANSLATABLE,CASESENSITIVE,ABBREVIATION,FORBIDDEN,EN
      TestTerm,Test Desc,true,false,false,false,TestValue
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(1)
    terms.assertTerm("TestTerm") {
      hasDescription("Test Desc")
      hasTranslation("EN", "TestValue")
      isTranslatable()
      isNotCaseSensitive()
      isNotAbbreviation()
      isNotForbidden()
    }
  }

  @Test
  fun `handles mixed header spacing`() {
    val csvContent =
      """
      term  , description,  en  ,cs
      TestTerm,Test description,TestValue,TestHodnota
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(1)
    terms.assertTerm("TestTerm") {
      hasDescription("Test description")
      hasTranslation("en", "TestValue")
      hasTranslation("cs", "TestHodnota")
    }
  }

  @Test
  fun `returns empty list for empty CSV`() {
    val csvContent =
      """
      term,en
      """.trimIndent()

    val terms = parseCSV(csvContent)
    terms.assertSize(0)
  }

  @Test
  fun `returns empty list for headers only`() {
    val csvContent = "term,description,en,cs"

    val terms = parseCSV(csvContent)
    terms.assertSize(0)
  }

  @Test
  fun `handles empty translation values`() {
    val csvContent =
      """
      term,en,cs,de
      TestTerm,TestValue,,
      AnotherTerm,,AnotherValue,
      EmptyTerm,,,
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(3)

    terms.assertTerm("TestTerm") {
      hasTranslation("en", "TestValue")
      hasNoTranslation("cs")
      hasNoTranslation("de")
    }

    terms.assertTerm("AnotherTerm") {
      hasNoTranslation("en")
      hasTranslation("cs", "AnotherValue")
      hasNoTranslation("de")
    }

    terms.assertTerm("EmptyTerm") {
      hasNoTranslation("en")
      hasNoTranslation("cs")
      hasNoTranslation("de")
    }
  }

  @Test
  fun `handles term with description only`() {
    val csvContent =
      """
      description,en,cs
      Just a description,,
      Another description,Value,
      """.trimIndent()

    val terms = parseCSV(csvContent)

    terms.assertSize(2)

    terms.assertTermWithDescription("Just a description") {
      hasNoTerm()
      hasDescription("Just a description")
      hasNoTranslation("en")
      hasNoTranslation("cs")
    }

    terms.assertTermWithDescription("Another description") {
      hasNoTerm()
      hasDescription("Another description")
      hasTranslation("en", "Value")
      hasNoTranslation("cs")
    }
  }

  private fun parseCSV(
    csvContent: String,
    delimiter: Char = ',',
  ): List<ImportGlossaryTerm> {
    return GlossaryCSVParser(ByteArrayInputStream(csvContent.toByteArray()), delimiter).parse()
  }
}
