package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.recaptcha")
@DocProperty(
  description =
    "When configured, reCAPTCHA v3 is used to protect the sign up page against bots. " +
      "By default, reCAPTCHA is disabled.\n" +
      "\n" +
      "To enable it, you first need to [register a new site on reCAPTCHA](https://www.google.com/recaptcha/admin). " +
      "Make sure to select reCAPTCHA v3 when registering your site.",
  displayName = "reCAPTCHA",
)
class ReCaptchaProperties {
  @E2eRuntimeMutable
  @DocProperty(description = "Site key for use the HTML code your site serves to users.")
  var siteKey: String? = null

  @DocProperty(description = "Secret key for communication between your site and reCAPTCHA.")
  @E2eRuntimeMutable
  var secretKey: String? = null
}
