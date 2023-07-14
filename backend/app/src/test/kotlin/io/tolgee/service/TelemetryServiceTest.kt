/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.fixtures.mockHttpRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

@SpringBootTest(properties = ["tolgee.telemetry.report-period-ms=200"])
class TelemetryServiceTest : AbstractSpringTest() {

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Test
  fun `doesn't report when disabled`() {
    mockHttpRequest(restTemplate) {
      whenReq {
        url = { it.contains("/v2/public/telemetry") }
        method = { it == HttpMethod.POST }
      }
      thenAnswer { throw RuntimeException("Should not be called") }
    }
  }
}
