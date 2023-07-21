/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.AbstractSpringTest
import io.tolgee.configuration.tolgee.TelemetryProperties
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.TelemetryReportRequest
import io.tolgee.fixtures.mockHttpRequest
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

@SpringBootTest(
  properties = [
    "tolgee.telemetry.report-period-ms=200",
    "tolgee.telemetry.enabled=false"
  ]
)
class TelemetryServiceTest : AbstractSpringTest() {

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Autowired
  lateinit var telemetryProperties: TelemetryProperties

  @AfterEach
  fun clean() {
    telemetryProperties.enabled = false
  }

  @Test
  fun `doesn't report when disabled`() {
    telemetryProperties.enabled = false
    Mockito.reset(restTemplate)
    mockHttpRequest(restTemplate) {
      whenReq {
        url = { it.contains("/v2/public/telemetry") }
        method = { it == HttpMethod.POST }
      }
      thenAnswer { }
      verify {
        Thread.sleep(5000)
        this.captor.allValues.assert.hasSize(0)
      }
    }
  }

  @Test
  fun `reports when enabled`() {
    telemetryProperties.enabled = true
    val testData = BaseTestData().apply {
      this.root.addProject { name = "bbbb" }.build {
        val en = addEnglish()
        addKey("a") {
          addTranslation {
            language = en.self
            text = "Hello"
          }
        }
      }
    }
    testDataService.saveTestData(testData.root)
    mockHttpRequest(restTemplate) {
      whenReq {
        url = { it.contains("/v2/public/telemetry") }
        method = { it == HttpMethod.POST }
      }
      thenAnswer { }
      verify {
        waitForNotThrowing {
          val first = this.captor.allValues[0].body as String
          val data = jacksonObjectMapper().readValue<TelemetryReportRequest>(first)
          data.instanceId.assert.isNotBlank()
          data.projectsCount.assert.isEqualTo(2)
          data.translationsCount.assert.isEqualTo(1)
          data.languagesCount.assert.isEqualTo(2)
          data.distinctLanguagesCount.assert.isEqualTo(1)
          data.usersCount.assert.isEqualTo(1)
        }
      }
    }
  }
}
