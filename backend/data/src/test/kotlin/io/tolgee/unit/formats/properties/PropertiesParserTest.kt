package io.tolgee.unit.formats.properties

import io.tolgee.service.dataImport.processors.PropertyFileProcessor
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PropertiesParserTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("messages_en.properties", "src/test/resources/import/example.properties")
  }

  @Test
  fun `returns correct parsed result`() {
    PropertyFileProcessor(mockUtil.fileProcessorContext).process()
    Assertions.assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(mockUtil.fileProcessorContext.translations).hasSize(4)
    val text = mockUtil.fileProcessorContext.translations["Register"]?.get(0)?.text
    Assertions.assertThat(text).isEqualTo("Veuillez vous enregistrer sur la page suivante.")
    val multiLineText = mockUtil.fileProcessorContext.translations["Cleanup"]?.get(0)?.text
    Assertions.assertThat(multiLineText).hasLineCount(3)
  }
}
