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
