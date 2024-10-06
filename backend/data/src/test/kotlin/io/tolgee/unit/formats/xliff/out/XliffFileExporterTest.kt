package io.tolgee.unit.formats.xliff.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.xliff.out.XliffFileExporter
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XliffFileExporterTest {
  @Test
  fun `exports translations`() {
    val translations = getBaseTranslations()

    val params = getExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files =
      XliffFileExporter(
        translations,
        exportParams = params,
        baseTranslationsProvider = baseProvider,
        baseLanguage = Language().apply { tag = "en" },
        projectIcuPlaceholdersSupport = true,
      ).produceFiles()

    assertThat(files).hasSize(2)
    var fileContent = files["de.xliff"]!!.bufferedReader().readText()
    var transUnit = assertHasTransUnitAndReturn(fileContent, "en", "de")
    assertThat(transUnit.attribute("id").value).isEqualTo("A key")
    assertThat(transUnit.selectNodes("./source")[0].text).isEqualTo("")
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("Z translation")

    fileContent = files["en.xliff"]!!.bufferedReader().readText()
    transUnit = assertHasTransUnitAndReturn(fileContent, "en", "en")
    assertThat(transUnit.attribute("id").value).isEqualTo("Z key")
    assertThat(transUnit.selectNodes("./source")[0].text).isEqualTo("A translation")
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("A translation")
  }

  @Test
  fun `honors the provided fileStructureTemplate`() {
    val exporter =
      getSimpleExporter(
        params =
          getExportParams().also {
            it.fileStructureTemplate = "{languageTag}/hello/{namespace}.{extension}"
          },
      )

    val files = exporter.produceFiles()
    files["cs/hello.xliff"].assert.isNotNull()
  }

  private fun getSimpleExporter(params: ExportParams): XliffFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "item",
          text = "simple",
        )
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams = params,
      )
    return exporter
  }

  private fun getBaseTranslations(): List<ExportTranslationView> {
    val zKey = ExportKeyView(1, "Z key")
    val aTranslation = ExportTranslationView(1, "A translation", TranslationState.TRANSLATED, zKey, "en")
    zKey.translations["en"] = aTranslation

    val aKey = ExportKeyView(1, "A key")
    val zTranslation = ExportTranslationView(1, "Z translation", TranslationState.TRANSLATED, aKey, "de")
    aKey.translations["de"] = zTranslation

    return listOf(aTranslation, zTranslation)
  }

  @Test
  fun `works with HTML`() {
    val translations = getHtmlTranslations()

    val params = getExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files =
      XliffFileExporter(
        translations,
        exportParams = params,
        baseTranslationsProvider = baseProvider,
        baseLanguage = Language().apply { tag = "en" },
        projectIcuPlaceholdersSupport = true,
      ).produceFiles()

    assertThat(files).hasSize(2)
    val fileContent = files["de.xliff"]!!.bufferedReader().readText()
    val document = fileContent.parseToDocument()
    val valid = document.selectNodes("//trans-unit[@id = 'html_key']/source/p")[0]
    assertThat(valid.text).isEqualTo("Sweat jesus, this is HTML!")
    val invalid = document.selectNodes("//trans-unit[@id = 'html_key']/target")[0]
    assertThat(invalid.text).isEqualTo("Sweat jesus, this is invalid < HTML!")
  }

  @Test
  fun `respects xml space preserve`() {
    val params = getExportParams()

    val files =
      XliffFileExporter(
        listOf(
          ExportTranslationView(
            1,
            "<p>Sweat jesus, this is HTML!</p>",
            TranslationState.TRANSLATED,
            ExportKeyView(1, "html_key", description = "Omg!\n  This is really.    \n preserved"),
            "en",
          ),
        ),
        exportParams = params,
        baseTranslationsProvider = { listOf() },
        baseLanguage = Language().apply { tag = "en" },
        projectIcuPlaceholdersSupport = true,
      ).produceFiles()

    val fileContent = files["en.xliff"]!!.bufferedReader().readText()
    fileContent.contains(
      "<note xml:space=\"preserve\">Omg!\n" +
        "  This is really.    \n" +
        " preserved</note>",
    )
  }

  private fun getHtmlTranslations(): List<ExportTranslationView> {
    val key = ExportKeyView(1, "html_key")
    val validHtmlTranslation =
      ExportTranslationView(
        1,
        "<p>Sweat jesus, this is HTML!</p>",
        TranslationState.TRANSLATED,
        key,
        "en",
      )
    val invalidHtmlTranslation =
      ExportTranslationView(
        1,
        "Sweat jesus, this is invalid < HTML!",
        TranslationState.TRANSLATED,
        key,
        "de",
      )
    key.translations["en"] = validHtmlTranslation
    key.translations["de"] = invalidHtmlTranslation
    val translations = listOf(validHtmlTranslation, invalidHtmlTranslation)
    return translations
  }

  private fun assertHasTransUnitAndReturn(
    text: String,
    sourceLanguage: String,
    targetLanguage: String,
  ): Element {
    val document = text.parseToDocument()
    val docFiles = document.selectNodes("//file")
    assertThat(docFiles).hasSize(1)
    val docFile = docFiles[0]
    assertThat((docFile as Element).attribute("source-language").value).isEqualTo(sourceLanguage)
    assertThat((docFile).attribute("target-language").value).isEqualTo(targetLanguage)
    val transUnits = docFile.selectNodes("./body/trans-unit")
    assertThat(transUnits).hasSize(1)
    val transUnit = transUnits[0]
    return transUnit as Element
  }

  private fun String.parseToDocument(): Document {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val input = InputSource()
    input.characterStream = StringReader(this)
    return db.parse(input)
  }

  private fun Node.selectNodes(xPath: String): List<Node> {
    val xpf: XPathFactory = XPathFactory.newInstance()
    val xpath: XPath = xpf.newXPath()
    val nodeList = xpath.evaluate(xPath, this, XPathConstants.NODESET) as NodeList
    return (0 until nodeList.length).map { nodeList.item(it) }.toList()
  }

  private val Node.text: String
    get() = this.textContent

  private fun Element.attribute(name: String): Attr {
    return this.getAttributeNode(name)
  }

  /**
   * Validate the xml file output against the xliff 1.2 Schema.
   */
  @Test
  fun `validate xml output`() {
    val translations = getBaseTranslations()
    val params = getExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files =
      XliffFileExporter(
        translations,
        exportParams = params,
        baseTranslationsProvider = baseProvider,
        baseLanguage = Language().apply { tag = "en" },
        projectIcuPlaceholdersSupport = true,
      ).produceFiles()

    val validator: Validator
    javaClass.classLoader.getResourceAsStream("import/xliff/xliff-core-1.2-transitional.xsd")
      .use { xsdInputStream ->
        validator =
          try {
            val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val schemaFile: Source = StreamSource(xsdInputStream)

            val schema = factory.newSchema(schemaFile)
            schema.newValidator()
          } catch (e: Exception) {
            throw e
          }
      }

    assertThat(files).hasSize(2)

    // de.xliff is invalid because of a missing a "source" element inside the "trans-unit". Should throw a SAXParseException.
    files["de.xliff"].use { invalidFileContent ->
      validator.validate(StreamSource(invalidFileContent))
    }

    // en.xliff is valid
    files["en.xliff"].use { validFileContent ->
      validator.validate(StreamSource(validFileContent))
    }
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="" source-language="en" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key3">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}</target>
    |      </trans-unit>
    |      <trans-unit id="item">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I will be first {icuParam, number}</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): XliffFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}'} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first {icuParam, number}",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="" source-language="en" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key3">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">{count, plural, one {# den {icuParam, number} '{hey}'} few {# dny} other {# dní}}</target>
    |      </trans-unit>
    |      <trans-unit id="item">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I will be first '{'icuParam'}' {hello, number}</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): XliffFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam, number} '{hey}'} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    return getExporter(built.translations, true)
  }

  @Test
  fun `respects message format prop`() {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams = ExportParams(messageFormat = ExportMessageFormat.RUBY_SPRINTF),
      )
    val data = getExported(exporter)
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="" source-language="en" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="item">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I will be first {icuParam} %&lt;hello&gt;d</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    exportParams: ExportParams = getExportParams(),
  ): XliffFileExporter {
    return XliffFileExporter(
      translations = translations,
      exportParams = exportParams,
      baseLanguage = Language().apply { tag = "en" },
      baseTranslationsProvider = { listOf() },
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
    )
  }

  private fun getExportParams() = ExportParams()
}
