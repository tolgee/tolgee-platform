package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.rate-limits")
@DocProperty(
  displayName = "Rate limits",
  description =
    "Configuration of the rate limit behavior of the server.\n" +
      "Rate limits are used to protect against server overload and/or abuse, and are enabled by default.\n\n" +
      // TODO: dedicated documentation section somewhere about rate limits & link it here
      "You can disable global, per-endpoint and auth-related rate limits, or configure global rate limits.\n" +
      "Per-endpoint and auth-related rate limits are fixed and cannot be configured.\n\n",
)
class RateLimitProperties(
  @DocProperty(
    description =
      "To turn all rate limits off, set this value to `false`.\n\n" +
        ":::danger\n" +
        "This field is **deprecated** and will be removed with Tolgee 4. If set to `false`, it will take priority\n" +
        "over the more granular `global-limits`, `endpoint-limits` and `authentication-limits` and all limits\n" +
        "will be disabled.\n" +
        ":::\n\n",
  )
  @Deprecated(message = "Use `global-limits`, `endpoint-limits` and `authentication-limits` individually instead.")
  var enabled: Boolean = true,
  @DocProperty(description = "Control whether global limits on the API are enabled or not.")
  var globalLimits: Boolean = true,
  @DocProperty(
    description =
      "Control whether per-endpoint limits on the API are enabled or not.\n" +
        "Does not affect authentication-related endpoints, these are controlled by `authentication-limits`.",
  )
  var endpointLimits: Boolean = true,
  @DocProperty(
    description =
      "Control whether per-endpoint limits on authentication-related endpoints are enabled or not.\n" +
        ":::warning\n" +
        "It is **strongly** recommended to keep these limits enabled. They act as a protection layer against\n" +
        "brute-force attacks on the login (and register) prompt.\n" +
        ":::\n\n",
  )
  var authenticationLimits: Boolean = true,
  @DocProperty(description = "Amount of requests an IP address can do in a single time window.")
  var ipRequestLimit: Int = 20_000,
  @DocProperty(
    description = "Size, in milliseconds, of the time window for IP-based limiting.",
    defaultExplanation = "= 5 minutes",
  )
  var ipRequestWindow: Long = 5 * 60 * 1000,
  @DocProperty(description = "Amount of requests a user can do in a single time window.")
  var userRequestLimit: Int = 400,
  @DocProperty(
    description = "Size, in milliseconds, of the time window for user-based limiting.",
    defaultExplanation = "= 1 minute",
  )
  var userRequestWindow: Long = 1 * 60 * 1000,
  var emailVerificationRequestLimit: Int = 2,
  var emailVerificationRequestWindow: Long = 1 * 60 * 1000,
  var emailVerificationRequestLimitEnabled: Boolean = true,
)
