package io.tolgee.service.export.exporters

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
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

    val params = ExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files = XliffFileExporter(
      translations,
      exportParams = params,
      baseTranslationsProvider = baseProvider,
      baseLanguage = Language().apply { tag = "en" }
    ).produceFiles()

    assertThat(files).hasSize(2)
    var fileContent = files["de.xlf"]!!.bufferedReader().readText()
    var transUnit = assertHasTransUnitAndReturn(fileContent, "en", "de")
    assertThat(transUnit.attribute("id").value).isEqualTo("A key")
    assertThat(transUnit.selectNodes("./source")).isEmpty()
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("Z translation")

    fileContent = files["en.xlf"]!!.bufferedReader().readText()
    transUnit = assertHasTransUnitAndReturn(fileContent, "en", "en")
    assertThat(transUnit.attribute("id").value).isEqualTo("Z key")
    assertThat(transUnit.selectNodes("./source")[0].text).isEqualTo("A translation")
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("A translation")
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

    val params = ExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files = XliffFileExporter(
      translations,
      exportParams = params,
      baseTranslationsProvider = baseProvider,
      baseLanguage = Language().apply { tag = "en" }
    ).produceFiles()

    assertThat(files).hasSize(2)
    val fileContent = files["de.xlf"]!!.bufferedReader().readText()
    val document = fileContent.parseToDocument()
    val valid = document.selectNodes("//trans-unit[@id = 'html_key']/source/p")[0]
    assertThat(valid.text).isEqualTo("Sweat jesus, this is HTML!")
    val invalid = document.selectNodes("//trans-unit[@id = 'html_key']/target")[0]
    assertThat(invalid.text).isEqualTo("Sweat jesus, this is invalid < HTML!")
  }

  private fun getHtmlTranslations(): List<ExportTranslationView> {
    val key = ExportKeyView(1, "html_key")
    val validHtmlTranslation = ExportTranslationView(
      1,
      "<p>Sweat jesus, this is HTML!</p>",
      TranslationState.TRANSLATED,
      key,
      "en"
    )
    val invalidHtmlTranslation = ExportTranslationView(
      1,
      "Sweat jesus, this is invalid < HTML!",
      TranslationState.TRANSLATED,
      key,
      "de"
    )
    key.translations["en"] = validHtmlTranslation
    key.translations["de"] = invalidHtmlTranslation
    val translations = listOf(validHtmlTranslation, invalidHtmlTranslation)
    return translations
  }

  private fun assertHasTransUnitAndReturn(text: String, sourceLanguage: String, targetLanguage: String): Element {
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
    val params = ExportParams()
    val baseProvider = { translations.filter { it.languageTag == "en" } }

    val files = XliffFileExporter(
      translations,
      exportParams = params,
      baseTranslationsProvider = baseProvider,
      baseLanguage = Language().apply { tag = "en" }
    ).produceFiles()

    val validator: Validator
    val xsdInputStream4 = javaClass.classLoader.getResourceAsStream("xliff/xliff-core-1.2-transitional.xsd")
      .use { xsdInputStream ->
        validator = try {
          val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
          val schemaFile: Source = StreamSource(xsdInputStream)

          val schema = factory.newSchema(schemaFile)
          schema.newValidator()
        } catch (e: Exception) {
          throw e;
        }
      }

    assertThat(files).hasSize(2)

    // de.xlf is invalid because of a missing a "source" element inside the "trans-unit". Should throw a SAXParseException.
    files["de.xlf"].use { invalidFileContent ->
      assertThrows<SAXParseException> {
        validator.validate(StreamSource(invalidFileContent))
      }
    }

    // en.xlf is valid
    files["en.xlf"].use { validFileContent ->
      validator.validate(StreamSource(validFileContent))
    }
  }
}
