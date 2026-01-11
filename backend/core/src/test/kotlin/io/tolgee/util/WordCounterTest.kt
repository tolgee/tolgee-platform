package io.tolgee.util

import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class WordCounterTest {
  @Test
  fun `counts words correctly`() {
    assertThat(WordCounter.countWords("你好，這是一個繁體中文文本。", "zh-Hant")).isEqualTo(6)
    assertThat(WordCounter.countWords("你好，这是一个简体中文文本。", "zh-Hans")).isEqualTo(7)
    assertThat(WordCounter.countWords("Hello, I am fred. (Super friend!)", "en-US")).isEqualTo(6)
    assertThat(WordCounter.countWords("Hello, I am fred. <html>yep!</html>", "en-US")).isEqualTo(7)
    assertThat(WordCounter.countWords("What about {var_ids-var-ids}", "en-US")).isEqualTo(5) // 3 or 2?
  }
}
