package io.tolgee.unit.formats.xliff

import io.tolgee.formats.xliff.`in`.Xliff12FileProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertTranslations
import io.tolgee.util.description
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class Xliff12FileProcessorTest {
  private val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()

  private lateinit var xmlStreamReader: XMLEventReader

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
    mockUtil.mockIt("example.xliff", "src/test/resources/import/xliff/example.xliff")
  }

  @Test
  fun `processes xliff 12 file correctly`() {
    Xliff12FileProcessor(mockUtil.fileProcessorContext, parsed).process()
    assertThat(mockUtil.fileProcessorContext.languages).hasSize(2)
    assertThat(mockUtil.fileProcessorContext.translations).hasSize(176)
    assertThat(mockUtil.fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].text).isEqualTo("Remove %1")
    assertThat(mockUtil.fileProcessorContext.translations["vpn.devices.removeA11Y"]!![0].language.name).isEqualTo("en")
    assertThat(mockUtil.fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].text).isEqualTo("Eliminar %1")
    assertThat(
      mockUtil.fileProcessorContext.translations["vpn.devices.removeA11Y"]!![1].language.name,
    ).isEqualTo("es-MX")

    val keyMeta = mockUtil.fileProcessorContext.keys["vpn.aboutUs.releaseVersion"]!!.keyMeta!!
    assertThat(keyMeta.description).isEqualTo(
      "Refers to the installed version." +
        " For example: \"Release Version: 1.23\"",
    )
    assertThat(keyMeta.codeReferences).hasSize(1)
    assertThat(keyMeta.codeReferences[0].path).isEqualTo("../src/ui/components/VPNAboutUs.qml")
    assertThat(mockUtil.fileProcessorContext.translations["systray.quit"]!![0].text).isEqualTo(
      "<x equiv-text=\"{{ favorite ?  'Remove from favorites' :" +
        " 'Add to favorites'}}\" id=\"INTERPOLATION\"></x>",
    )
    assertThat(mockUtil.fileProcessorContext.translations["systray.quit"]!![1].text)
      .isEqualTo(
        "<x equiv-text=\"{{ favorite ?  'Remove from favorites' :" +
          " 'Add to favorites'}}\" id=\"INTERPOLATION\"></x>",
      )
  }

  @Test
  fun `processes xliff 12 fast enough`() {
    mockUtil.mockIt("example.xliff", "src/test/resources/import/xliff/larger.xlf")
    xmlStreamReader = inputFactory.createXMLEventReader(mockUtil.importFileDto.data.inputStream())
    val start = System.currentTimeMillis()
    Xliff12FileProcessor(mockUtil.fileProcessorContext, parsed).process()
    assertThat(System.currentTimeMillis() - start).isLessThan(4000)
  }

  @Test
  fun `preserving spaces works correctly`() {
    mockUtil.mockIt("example.xliff", "src/test/resources/import/xliff/preserving-spaces.xliff")
    xmlStreamReader = inputFactory.createXMLEventReader(mockUtil.importFileDto.data.inputStream())
    Xliff12FileProcessor(mockUtil.fileProcessorContext, parsed).process()
    mockUtil
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "1")
      .assertSingle {
        hasText("  Back")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "2")
      .assertSingle {
        hasText("Back")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "3")
      .assertSingle {
        hasText("  Back")
      }
  }

  @Test
  fun `handles errors correctly`() {
    mockUtil.mockIt("error_example.xliff", "src/test/resources/import/xliff/error_example.xliff")
    Xliff12FileProcessor(mockUtil.fileProcessorContext, parsed).process()
    assertThat(mockUtil.fileProcessorContext.translations).hasSize(2)
    mockUtil.fileProcessorContext.fileEntity.issues.let { issues ->
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
