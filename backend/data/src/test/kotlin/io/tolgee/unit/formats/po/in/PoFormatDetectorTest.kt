package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.po.`in`.PoFormatDetector
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PoFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.po", "src/test/resources/import/po/example.po")
  }

  @Test
  fun `returns C format`() {
    val detected = PoFormatDetector().detectFormat(listOf("%jd %hhd", "%d %s", "d %s"))
    assertThat(detected).isEqualTo(ImportFormat.PO_C)
  }

  @Test
  fun `returns PHP format`() {
    val detected = PoFormatDetector().detectFormat(listOf("%b %d", "%d %s", "d %s"))
    assertThat(detected).isEqualTo(ImportFormat.PO_PHP)
  }
}
