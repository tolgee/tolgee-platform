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

  @Test
  fun `detects python-brace-format flag`() {
    assertThat(PoFormatDetector().detectByFlag("python-brace-format"))
      .isEqualTo(ImportFormat.PO_PYTHON_BRACE)
  }

  @Test
  fun `returns Python brace format for distinctive brace placeholders`() {
    val detected = PoFormatDetector().detectFormat(listOf("{name:.2f}", "{count!r}", "{} items"))
    assertThat(detected).isEqualTo(ImportFormat.PO_PYTHON_BRACE)
  }

  @Test
  fun `prefers ICU over Python brace for plain placeholders`() {
    // a plain "{name}" matches both ICU and brace detection; ICU must win so brace detection
    // doesn't steal ICU files that lack a python-brace-format flag
    val detected = PoFormatDetector().detectFormat(listOf("{name}", "{count} items"))
    assertThat(detected).isEqualTo(ImportFormat.PO_ICU)
  }
}
