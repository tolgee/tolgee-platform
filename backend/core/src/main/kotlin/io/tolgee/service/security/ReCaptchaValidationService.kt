package io.tolgee.service.security

import com.fasterxml.jackson.annotation.JsonProperty
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.UnexpectedGoogleApiResponseException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.Date

@Service
class ReCaptchaValidationService(
  private val tolgeeProperties: TolgeeProperties,
  private val restTemplate: RestTemplate,
) {
  fun validate(
    token: String?,
    ip: String,
  ): Boolean {
    if (tolgeeProperties.recaptcha.secretKey == null ||
      // for e2e testing purposes
      tolgeeProperties.recaptcha.secretKey == "dummy_secret_key"
    ) {
      return true
    }

    // to E2E test negative result
    if (tolgeeProperties.recaptcha.secretKey == "negative_dummy_secret_key") {
      return false
    }

    if (token == null) {
      return false
    }

    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

    val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
    requestBody.add("secret", tolgeeProperties.recaptcha.secretKey)
    requestBody.add("response", token)

    val response =
      restTemplate.postForEntity<Response>(
        "https://www.google.com/recaptcha/api/siteverify",
        requestBody,
      )

    return response.body?.success
      ?: throw UnexpectedGoogleApiResponseException(response)
  }

  companion object {
    class Response {
      var success: Boolean = false

      @JsonProperty("challenge_ts")
      var challengeTs: Date = Date()
      var hostname: String = ""

      @JsonProperty("error-codes")
      var errorCodes: List<String>? = null
    }
  }
}
