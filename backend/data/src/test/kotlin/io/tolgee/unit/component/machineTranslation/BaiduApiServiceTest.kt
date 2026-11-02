package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.BaiduApiService
import io.tolgee.component.machineTranslation.providers.BaiduApiService.Companion.BaiduResponse
import io.tolgee.component.machineTranslation.providers.BaiduApiService.Companion.BaiduTranslation
import io.tolgee.configuration.tolgee.machineTranslation.BaiduMachineTranslationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class BaiduApiServiceTest {
  @Test
  fun `keeps every line of a multi-line translation`() {
    val service = serviceWithMockedResponse("First line.", "Second line.")

    val result = service.translate("第一行。\n第二行。", "zh", "en")

    assertThat(result).isEqualTo("First line.\nSecond line.")
  }

  @Test
  fun `keeps blank lines that Baidu returns no result for`() {
    val service = serviceWithMockedResponse("First paragraph.", "Second paragraph.")

    val result = service.translate("第一段。\n\n第二段。\n", "zh", "en")

    assertThat(result).isEqualTo("First paragraph.\n\nSecond paragraph.\n")
  }

  @Test
  fun `restores source HTML tags that Baidu returned with spaces`() {
    val service = serviceWithMockedResponse("First < b >sentence< / b >.< br >Second sentence.")

    val result = service.translate("第一<b>句</b>。<br>第二句。", "zh", "en")

    assertThat(result).isEqualTo("First <b>sentence</b>.<br>Second sentence.")
  }

  @Test
  fun `keeps each form of a tag that appears in the source in two spellings`() {
    val service = serviceWithMockedResponse("A<br/>B<br />C")

    val result = service.translate("甲<br/>乙<br />丙", "zh", "en")

    assertThat(result).isEqualTo("A<br/>B<br />C")
  }

  @Test
  fun `leaves spaced angle brackets alone when the source had no such tag`() {
    val service = serviceWithMockedResponse("1 < b > 0")

    val result = service.translate("1 大于 0", "zh", "en")

    assertThat(result).isEqualTo("1 < b > 0")
  }

  private fun serviceWithMockedResponse(vararg lines: String): BaiduApiService {
    val response =
      BaiduResponse().apply {
        transResult = lines.map { BaiduTranslation().apply { dst = it } }
      }
    val restTemplate =
      mock<RestTemplate> {
        on { postForEntity(any<String>(), any(), eq(BaiduResponse::class.java)) } doReturn
          ResponseEntity.ok(response)
      }
    return BaiduApiService(BaiduMachineTranslationProperties(appId = "id", appSecret = "secret"), restTemplate)
  }
}
