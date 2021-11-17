package io.tolgee.unit.unit

import io.tolgee.helpers.TextHelper
import org.assertj.core.api.Assertions
import org.testng.annotations.Test

class TextHelperTest {
  @Test
  fun splitOnNonEscapedDelimiter() {
    val str = "this.is.escaped\\.delimiter.aaa.once\\.more.and.multiple\\\\\\.and.\\\\\\\\.text"
    val split = TextHelper.splitOnNonEscapedDelimiter(str, '.')
    Assertions.assertThat(split).isEqualTo(
      listOf("this", "is", "escaped.delimiter", "aaa", "once.more", "and", "multiple\\.and", "\\\\", "text"))
  }

  companion object {
    private const val testFullPath = "item1.item2.item1.item1.last"
  }
}
