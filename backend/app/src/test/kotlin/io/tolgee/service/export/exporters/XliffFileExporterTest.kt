package io.tolgee.service.export.exporters

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.testing.assertions.Assertions.assertThat
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.testng.annotations.Test

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
    assertThat((transUnit as Element).attribute("id").value).isEqualTo("A key")
    assertThat(transUnit.selectNodes("./source")).isEmpty()
    assertThat(transUnit.selectNodes("./target")[0].text).isEqualTo("Z translation")

    fileContent = files["/en.xlf"]!!.bufferedReader().readText()
    transUnit = assertHasTransUnitAndReturn(fileContent, "en", "en")
    assertThat((transUnit as Element).attribute("id").value).isEqualTo("Z key")
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
    var fileContent = files["/de.xlf"]!!.bufferedReader().readText()
    val document = DocumentHelper.parseText(fileContent)
    val valid = document.selectNodes("//trans-unit[@id = 'html_key']/source/p")[0]
    assertThat(valid.text).isEqualTo("Sweat jesus, this is HTML!")
    val invalid = document.selectNodes("//trans-unit[@id = 'html_key']/target")[0]
    assertThat(invalid.text).isEqualTo("Sweat jesus, this is invalid < HTML!")
  }

  private fun assertHasTransUnitAndReturn(text: String, sourceLanguage: String, targetLanguage: String): Element {
    val document = DocumentHelper.parseText(text)
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
}
