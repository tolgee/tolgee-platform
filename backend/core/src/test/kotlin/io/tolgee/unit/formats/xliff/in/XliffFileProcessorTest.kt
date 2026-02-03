package io.tolgee.unit.formats.xliff.`in`

import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class XliffFileProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.xliff", "src/test/resources/import/xliff/example.xliff")
  }

  @Test
  fun `processes xliff 12 file`() {
    XliffFileProcessor(mockUtil.fileProcessorContext).process()
    assertThat(mockUtil.fileProcessorContext.languages).hasSize(2)
    assertThat(mockUtil.fileProcessorContext.translations).hasSize(176)
  }
}
