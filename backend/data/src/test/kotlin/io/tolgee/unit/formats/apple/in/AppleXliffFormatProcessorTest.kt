package io.tolgee.unit.formats.apple.`in`

import io.tolgee.formats.apple.`in`.xliff.AppleXliffFileProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.testing.assert
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertAllSame
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.customEquals
import io.tolgee.util.description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AppleXliffFormatProcessorTest {
  private val xmlEventReader: XMLEventReader
    get() {
      val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
      return inputFactory.createXMLEventReader(mockUtil.importFileDto.data.inputStream())
    }

  private val parsed get() = XliffParser(xmlEventReader).parse()

  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `returns correct parsed result`() {
    mockFile("cs", "cs.xliff")
    processFile()
    mockUtil.fileProcessorContext
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Dogs %lld")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          zero {No dogs here!}
          one {One dog is here!}
          other {# dogs here}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Order %lld")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          zero {Order # Ticket}
          one {Order # Ticket}
          other {Order # Tickets}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello!")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "key")
      .assertSingle {
        hasText("Ahoj!")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "label")
      .assertSingle {
        hasText("label")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "CFBundleName")
      .assertSingle {
        hasText("Localization test")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "menu")
      .assertSingle {
        hasText("menu")
      }
    mockUtil.fileProcessorContext.assertKey("Dogs %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/en.lproj/Localizable.stringsdict",
            "_appleXliffPropertyName" : "dog",
            "_appleXliffStringsFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The count of dogs in the app")
    }
    mockUtil.fileProcessorContext.assertKey("Order %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/en.lproj/Localizable.stringsdict",
            "_appleXliffPropertyName" : "Ticket",
            "_appleXliffStringsFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("No comment provided by engineer.")
    }
    mockUtil.fileProcessorContext.assertKey("key") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("Localizable.strings\n  Localization test\n Created by Jan Cizmar on 06.02.2024.")
    }
    mockUtil.fileProcessorContext.assertKey("label") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("This is just random label")
    }
    mockUtil.fileProcessorContext.assertKey("CFBundleName") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/Localization test-InfoPlist.xcstrings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("Bundle name")
    }
    mockUtil.fileProcessorContext.assertKey("menu") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/Menu.xcstrings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
  }

  @Test
  fun `correctly parses xcstrings xliff`() {
    mockFile("en", "en_xcstrings.xliff")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "CFBundleName")
      .assertAllSame {
        hasText("apple-xliff-localization-test")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "standard_key")
      .assertAllSame {
        hasText("I am normal key!")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogs_cout_%lld")
      .assertAllSame {
        hasText("{0, plural,\none {One dog}\nother {# dogs}\n}")
      }
    mockUtil.fileProcessorContext.assertKey("CFBundleName") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "apple-xliff-localization-test-InfoPlist.xcstrings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("Bundle name")
    }
    mockUtil.fileProcessorContext.assertKey("standard_key") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localizable.xcstrings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
    mockUtil.fileProcessorContext.assertKey("dogs_cout_%lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localizable.xcstrings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Dogs %lld")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          zero {No dogs here %@ '{'icuParam'}'!}
          one {One dog is here %@ '{'icuParam'}'!}
          other {%lld dogs here %@ '{'icuParam'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Hi %lld")
      .assertSingle {
        hasText("Hi %lld {icuParam}")
      }
    mockUtil.fileProcessorContext.assertKey("Dogs %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/en.lproj/Localizable.stringsdict",
            "_appleXliffPropertyName" : "dog",
            "_appleXliffStringsFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The count of dogs in the app")
    }
    mockUtil.fileProcessorContext.assertKey("Hi %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Dogs %lld")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          zero {No dogs here %@ '{'icuParam'}'!}
          one {One dog is here %@ '{'icuParam'}'!}
          other {%lld dogs here %@ '{'icuParam'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Hi %lld")
      .assertSingle {
        hasText("Hi %lld '{'icuParam'}'")
      }
    mockUtil.fileProcessorContext.assertKey("Dogs %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/en.lproj/Localizable.stringsdict",
            "_appleXliffPropertyName" : "dog",
            "_appleXliffStringsFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The count of dogs in the app")
    }
    mockUtil.fileProcessorContext.assertKey("Hi %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Dogs %lld")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          zero {No dogs here {0} '{'icuParam'}'!}
          one {One dog is here {0} '{'icuParam'}'!}
          other {# dogs here {1} '{'icuParam'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Hi %lld")
      .assertSingle {
        hasText("Hi {0, number} '{'icuParam'}'")
      }
    mockUtil.fileProcessorContext.assertKey("Dogs %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "Localization test/en.lproj/Localizable.stringsdict",
            "_appleXliffPropertyName" : "dog",
            "_appleXliffStringsFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The count of dogs in the app")
    }
    mockUtil.fileProcessorContext.assertKey("Hi %lld") {
      customEquals(
        """
        {
            "_appleXliffFileOriginal" : "en.lproj/Localizable.strings"
          }
        """.trimIndent(),
      )
      description.assert.isNull()
    }
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "cs.xliff",
      "src/test/resources/import/apple/params_everywhere_cs.xliff",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun mockFile(
    languageTag: String,
    fileName: String = "cs.xliff",
  ) {
    mockUtil.mockIt("$languageTag.xliff", "src/test/resources/import/apple/$fileName")
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "cs.xliff",
      "src/test/resources/import/apple/params_everywhere_cs.xliff",
      assertBeforeSettingsApplication =
        listOf(
          "{0, plural,\n" +
            "zero {No dogs here {0} '{'icuParam'}'!}\n" +
            "one {One dog is here {0} '{'icuParam'}'!}\n" +
            "other {# dogs here {1} '{'icuParam'}'}\n" +
            "}",
          "Hi {0, number} '{'icuParam'}'",
        ),
      assertAfterDisablingConversion =
        listOf(
          "{value, plural,\n" +
            "zero {No dogs here %@ '{'icuParam'}'!}\n" +
            "one {One dog is here %@ '{'icuParam'}'!}\n" +
            "other {%lld dogs here %@ '{'icuParam'}'}\n" +
            "}",
          "Hi %lld '{'icuParam'}'",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{0, plural,\n" +
            "zero {No dogs here {0} '{'icuParam'}'!}\n" +
            "one {One dog is here {0} '{'icuParam'}'!}\n" +
            "other {# dogs here {1} '{'icuParam'}'}\n}",
          "Hi {0, number} '{'icuParam'}'",
        ),
    )
  }

  private fun processFile() {
    AppleXliffFileProcessor(mockUtil.fileProcessorContext, parsed).process()
  }
}
