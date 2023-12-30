/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.recaptchaValidation

import io.tolgee.AbstractSpringTest
import io.tolgee.service.security.ReCaptchaValidationService
import io.tolgee.service.security.ReCaptchaValidationService.Companion
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.util.*

class RecaptchaValidationDisabledTest : AbstractSpringTest() {
  @Autowired
  lateinit var reCaptchaValidationService: ReCaptchaValidationService

  @Autowired
  @MockBean
  lateinit var restTemplate: RestTemplate

  @Test
  fun `does not validate`() {
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

    assertThat(reCaptchaValidationService.validate("dummy_token", "10.10.10.10")).isEqualTo(true)

    verify(restTemplate, times(0))
      .postForEntity(
        eq("https://www.google.com/recaptcha/api/siteverify"),
        any(),
        eq(Companion.Response::class.java),
      )
  }
}
