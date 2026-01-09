/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.recaptchaValidation

import io.tolgee.AbstractSpringTest
import io.tolgee.service.security.ReCaptchaValidationService
import io.tolgee.service.security.ReCaptchaValidationService.Companion
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.util.Date

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.recaptcha.secretKey=dummy_key",
  ],
)
class RecaptchaValidationTest : AbstractSpringTest() {
  @Autowired
  lateinit var reCaptchaValidationService: ReCaptchaValidationService

  @Autowired
  @MockitoBean
  lateinit var restTemplate: RestTemplate

  @BeforeEach
  fun setup() {
    Mockito.reset(restTemplate)
    whenever(
      restTemplate.postForEntity(any<String>(), any(), any<Class<Companion.Response>>()),
    ).then {
      ResponseEntity(
        Companion.Response().apply {
          success = true
          challengeTs = Date()
          hostname = ""
          errorCodes = null
        },
        HttpStatus.OK,
      )
    }
  }

  @Test
  fun `validates token`() {
    assertThat(reCaptchaValidationService.validate("dummy_token", "10.10.10.10")).isEqualTo(true)

    verify(restTemplate, times(1))
      .postForEntity(
        eq("https://www.google.com/recaptcha/api/siteverify"),
        any(),
        eq(Companion.Response::class.java),
      )
  }

  @Test
  fun `returns false when invalid`() {
    whenever(
      restTemplate.postForEntity(any<String>(), any(), any<Class<Companion.Response>>()),
    ).then {
      ResponseEntity(
        Companion.Response().apply {
          success = false
          challengeTs = Date()
          hostname = ""
          errorCodes = null
        },
        HttpStatus.OK,
      )
    }

    assertThat(reCaptchaValidationService.validate("dummy_token", "10.10.10.10")).isEqualTo(false)

    verify(restTemplate, times(1))
      .postForEntity(
        eq("https://www.google.com/recaptcha/api/siteverify"),
        any(),
        eq(Companion.Response::class.java),
      )
  }
}
