package io.tolgee.unit.formats.xliff

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.xliff.`in`.Xliff12FileProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class Xliff12FileProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext
  private val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
  private lateinit var xmlStreamReader: XMLEventReader

  private val xmlEventReader: XMLEventReader
    get() {
      val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
      return inputFactory.createXMLEventReader(importFileDto.data.inputStream())
    }

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("exmample.xliff", importMock)
    importFileDto =
      ImportFileDto(
        "exmample.xliff",
        File("src/test/resources/import/xliff/example.xliff")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `processes xliff 12 file correctly`() {
    Xliff12FileProcessor(fileProcessorContext, xmlEventReader).process()
    assertThat(fileProcessorContext.languages).hasSize(2)
    assertThat(fileProcessorContext.translations).hasSize(176)
    assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].text).isEqualTo("Remove %1")
    assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].language.name).isEqualTo("en")
    assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].text).isEqualTo("Eliminar %1")
    assertThat(fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].language.name).isEqualTo("es-MX")

    val keyMeta = fileProcessorContext.keys["vpn.aboutUs.releaseVersion"]!!.keyMeta!!
    assertThat(keyMeta.comments).hasSize(1)
    assertThat(keyMeta.comments[0].text).isEqualTo(
      "Refers to the installed version." +
        " For example: \"Release Version: 1.23\"",
    )
    assertThat(keyMeta.codeReferences).hasSize(1)
    assertThat(keyMeta.codeReferences[0].path).isEqualTo("../src/ui/components/VPNAboutUs.qml")
    assertThat(fileProcessorContext.translations["systray.quit"]!![0].text).isEqualTo(
      "<x equiv-text=\"{{ favorite ?  'Remove from favorites' :" +
        " 'Add to favorites'}}\" id=\"INTERPOLATION\"></x>",
    )
    assertThat(fileProcessorContext.translations["systray.quit"]!![1].text)
      .isEqualTo(
        "<x equiv-text=\"{{ favorite ?  'Remove from favorites' :" +
          " 'Add to favorites'}}\" id=\"INTERPOLATION\"></x>",
      )
  }

  @Test
  fun `processes xliff 12 fast enough`() {
    importFileDto =
      ImportFileDto(
        "exmample.xliff",
        File("src/test/resources/import/xliff/larger.xlf")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
    xmlStreamReader = inputFactory.createXMLEventReader(importFileDto.data.inputStream())
    val start = System.currentTimeMillis()
    Xliff12FileProcessor(fileProcessorContext, xmlEventReader).process()
    assertThat(System.currentTimeMillis() - start).isLessThan(4000)
  }

  @Test
  fun `handles errors correctly`() {
    importFileDto =
      ImportFileDto(
        "exmample.xliff",
        File("src/test/resources/import/xliff/error_example.xliff").readBytes(),
      )
    xmlStreamReader = inputFactory.createXMLEventReader(importFileDto.data.inputStream())
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
    Xliff12FileProcessor(fileProcessorContext, xmlEventReader).process()
    assertThat(fileProcessorContext.translations).hasSize(2)
    fileProcessorContext.fileEntity.issues.let { issues ->
      assertThat(issues).hasSize(4)
      assertThat(issues[0].type).isEqualTo(FileIssueType.TARGET_NOT_PROVIDED)
      assertThat(issues[0].params[0].type).isEqualTo(FileIssueParamType.KEY_NAME)
      assertThat(issues[0].params[0].value).isEqualTo("vpn.main.back")
      assertThat(issues[1].type).isEqualTo(FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED)
      assertThat(issues[1].params[0].type).isEqualTo(FileIssueParamType.FILE_NODE_ORIGINAL)
      assertThat(issues[1].params[0].value).isEqualTo("../src/platforms/android/androidauthenticationview.qml")
    }
  }
}
