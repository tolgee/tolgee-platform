package io.tolgee.unit.formats

import io.tolgee.formats.path.buildPath
import io.tolgee.formats.path.getPathItems
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PathParserTest {
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

  @Test
  fun `array and object`() {
    testSameBothWays("[10].a")
  }

  @Test
  fun `invalid brackets stuff`() {
    getRebuiltPath("[10a].a.[1[oh]].[a][10]a").assert.isEqualTo("[10a].a.[1[oh]].[a][10]a")
  }

  @Test
  fun `invalid array like string`() {
    getRebuiltPath("[a][10]a").assert.isEqualTo("[a][10]a")
  }

  @Test
  fun `multiple arrays`() {
    testSameBothWays("[10][10][10]")
  }

  @Test
  fun `multiple arrays with objects`() {
    testSameBothWays("a[10][10][10].a")
  }

  @Test
  fun `escaped dot after array leads to array being ignored`() {
    getRebuiltPath("[10]\\.a").assert.isEqualTo("[10]\\.a")
  }

  @Test
  fun `just dots`() {
    getRebuiltPath("...").assert.isEqualTo("...")
  }

  @Test
  fun `returns valid original string`() {
    getPathItems("a[10]a.he\\llo.[a10]a.yay\\x.a[10].he\\.llo[10][18]", true)
      .map { it.originalPathString }.assert.isEqualTo(
        listOf(
          "a[10]a",
          "he\\llo",
          "[a10]a",
          "yay\\x",
          "a",
          "10",
          "he\\.llo",
          "10",
          "18",
        ),
      )
  }

  fun testSameBothWays(pathString: String) {
    getRebuiltPath(pathString).assert.isEqualTo(pathString)
  }

  fun getRebuiltPath(pathString: String): String {
    val pathItems = getPathItems(pathString, true)
    return buildPath(pathItems)
  }
}
