package io.tolgee.unit.formats.apple.`in`

import StringsdictFileProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StringsdictFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.stringsdict", "src/test/resources/import/apple/example.stringsdict")
  }

  @Test
  fun `returns correct parsed result`() {
    StringsdictFileProcessor(mockUtil.fileProcessorContext).process()
    Assertions.assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(mockUtil.fileProcessorContext.translations).hasSize(2)
    mockUtil.fileProcessorContext.translations["what-a-key-plural"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Peter has # dog}\n" +
        "other {Peter hase # dogs}\n" +
        "}",
    )
    mockUtil.fileProcessorContext.translations["what-a-key-plural-2"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Lucy has %la '{'dog'}'}\n" +
        "other {Lucy has %la '{'dogs'}'}\n" +
        "}",
    )
  }
}
