package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.HtmlNoTranslatePlaceholderProtector
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlNoTranslatePlaceholderProtectorTest {
  private val protector = HtmlNoTranslatePlaceholderProtector

  @Test
  fun `wraps each placeholder in a translate=no span`() {
    assertThat(protector.protect("{xx0xx} / {xx1xx}"))
      .isEqualTo("<span translate=\"no\">{xx0xx}</span> / <span translate=\"no\">{xx1xx}</span>")
  }

  @Test
  fun `leaves text without placeholders untouched`() {
    assertThat(protector.protect("no placeholders here")).isEqualTo("no placeholders here")
  }

  @Test
  fun `restores wrapped placeholders`() {
    val wrapped = "<span translate=\"no\">{xx0xx}</span> / <span translate=\"no\">{xx1xx}</span>"
    assertThat(protector.restore(wrapped)).isEqualTo("{xx0xx} / {xx1xx}")
  }

  @Test
  fun `restores when the engine drops the attribute quotes`() {
    assertThat(protector.restore("<span translate=no>{xx0xx}</span>")).isEqualTo("{xx0xx}")
  }

  @Test
  fun `restores the exact output AWS returns for the reported case`() {
    // AWS keeps the wrapped tokens verbatim but collapses the surrounding ` / ` to `/` for zh-TW.
    val awsOutput = "<span translate=\"no\">{xx0xx}</span>/<span translate=\"no\">{xx1xx}</span>"
    assertThat(protector.restore(awsOutput)).isEqualTo("{xx0xx}/{xx1xx}")
  }

  @Test
  fun `protect wraps only the sentinel and leaves a user-authored translate=no span untouched`() {
    val text = "<span translate=\"no\">Brand</span> has {xx0xx}"
    assertThat(protector.protect(text))
      .isEqualTo("<span translate=\"no\">Brand</span> has <span translate=\"no\">{xx0xx}</span>")
  }

  @Test
  fun `restore unwraps only our sentinel spans and preserves a user-authored translate=no span`() {
    val awsOutput = "<span translate=\"no\">Brand</span> 有 <span translate=\"no\">{xx0xx}</span>"
    assertThat(protector.restore(awsOutput)).isEqualTo("<span translate=\"no\">Brand</span> 有 {xx0xx}")
  }

  @Test
  fun `protect then restore is a round trip when the engine preserves the wrapping`() {
    val sentinelText = "{xx0xx} / {xx1xx}"
    assertThat(protector.restore(protector.protect(sentinelText))).isEqualTo(sentinelText)
  }
}
