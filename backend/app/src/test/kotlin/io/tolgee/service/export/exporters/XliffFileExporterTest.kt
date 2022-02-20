package io.tolgee.service.export.exporters

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XliffFileExporterTest {

  @Test
  fun `exports translations`() {
    val testData = TranslationsTestData()
    val translations = testData.projectTranslations
    val params = ExportParams()
    val baseProvider = { translations.filter { it.language === testData.englishLanguage } }

    val files = XliffFileExporter(
      testData.projectTranslations,
      exportParams = params,
      baseTranslationsProvider = baseProvider,
      baseLanguage = testData.englishLanguage
    ).produceFiles()

    assertThat(files).hasSize(2)
    var fileContent = files["/de.xlf"]!!.bufferedReader().readText()
    var transUnit = assertHasTransUnitAndReturn(fileContent, "en", "de")
    assertThat(transUnit.attribute("id").value).isEqualTo("A key")
    assertThat(transUnit.selectNodes("./source")).isEmpty()
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("Z translation")

    fileContent = files["/en.xlf"]!!.bufferedReader().readText()
    transUnit = assertHasTransUnitAndReturn(fileContent, "en", "en")
    assertThat(transUnit.attribute("id").value).isEqualTo("Z key")
    assertThat(transUnit.selectNodes("./source")[0].text).isEqualTo("A translation")
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("A translation")
  }

  @Test
  fun `works with HTML`() {
    val testData = TranslationsTestData()
    testData.addTranslationWithHtml()
    val translations = testData.projectTranslations
    val params = ExportParams()
    val baseProvider = { translations.filter { it.language === testData.englishLanguage } }

    val files = XliffFileExporter(
      testData.projectTranslations,
      exportParams = params,
      baseTranslationsProvider = baseProvider,
      baseLanguage = testData.englishLanguage
    ).produceFiles()

    assertThat(files).hasSize(2)
    val fileContent = files["/de.xlf"]!!.bufferedReader().readText()
    val document = fileContent.parseToDocument()
    val valid = document.selectNodes("//trans-unit[@id = 'html_key']/source/p")[0]
    assertThat(valid.text).isEqualTo("Sweat jesus, this is HTML!")
    val invalid = document.selectNodes("//trans-unit[@id = 'html_key']/target")[0]
    assertThat(invalid.text).isEqualTo("Sweat jesus, this is invalid < HTML!")
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
}
