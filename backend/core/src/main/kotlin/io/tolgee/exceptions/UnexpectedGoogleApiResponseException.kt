package io.tolgee.exceptions

import io.tolgee.service.security.ReCaptchaValidationService
import org.springframework.http.ResponseEntity

class UnexpectedGoogleApiResponseException(
  val response: ResponseEntity<ReCaptchaValidationService.Companion.Response>,
) : Throwable()
