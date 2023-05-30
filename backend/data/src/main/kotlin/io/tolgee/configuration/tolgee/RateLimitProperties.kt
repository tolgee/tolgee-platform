package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.rate-limits")
@DocProperty(
  description = "Configuration of rate limits.\n" +
    "By default Tolgee Platform limits requests to endpoints according to these rules:\n" +
    "\n" +
    "1. Single IP is not allowed to request more than 20 000 times in 5 minutes\n" +
    "2. Single IP is not allowed to request public endpoints (authentication, sign-ups) " +
    "more than 1000 times per hour\n" +
    "3. Single authenticated user cannot do more than 400 requests per minute",
  displayName = "Rate limits"
)
class RateLimitProperties {
  @DocProperty(description = "To turn these rate limits off, set this value to `false`.")
  var enabled: Boolean = true
}
