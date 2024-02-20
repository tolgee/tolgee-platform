package io.tolgee.unit.formats

import io.tolgee.formats.buildPath
import io.tolgee.formats.getPathItems
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PathItemUtilTest {
  @Test
  fun `test with escaping`() {
    testSameBothWays("map\\.item[0].item[1].value")
  }

  @Test
  fun `array start end`() {
    testSameBothWays("[0].map\\.item[0].item[1].value[1]")
  }

  @Test
  fun `escaped array`() {
    testSameBothWays("\\[0\\].map\\.item\\[0\\].item[1].value\\[1\\]")
  }

  @Test
  fun `starts with dot`() {
    testSameBothWays(".hello")
  }

  @Test
  fun `ends with dot`() {
    testSameBothWays(".hello")
  }

  fun testSameBothWays(pathString: String)  {
    val pathItems = getPathItems(pathString)
    buildPath(pathItems).assert.isEqualTo(pathString)
  }
}
