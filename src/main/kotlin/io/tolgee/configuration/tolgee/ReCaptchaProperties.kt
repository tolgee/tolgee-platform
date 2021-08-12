package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.recaptcha")
class ReCaptchaProperties {
  @E2eRuntimeMutable
  var siteKey: String? = null

  @E2eRuntimeMutable
  var secretKey: String? = null
}
