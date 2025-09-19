/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.recaptchaValidation

import io.tolgee.AbstractSpringTest
import io.tolgee.service.security.ReCaptchaValidationService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestTemplate

class RecaptchaValidationDisabledTest : AbstractSpringTest() {
  @Autowired
  lateinit var reCaptchaValidationService: ReCaptchaValidationService

  @Autowired
  lateinit var restTemplate: RestTemplate

  @Test
  fun `does not validate`() {
    val server = MockRestServiceServer.bindTo(restTemplate).build()
    server.expect(ExpectedCount.never()) {
      requestTo("https://www.google.com/recaptcha/api/siteverify")
    }

    assertThat(reCaptchaValidationService.validate("dummy_token", "10.10.10.10")).isEqualTo(true)
    server.verify()
  }
}
