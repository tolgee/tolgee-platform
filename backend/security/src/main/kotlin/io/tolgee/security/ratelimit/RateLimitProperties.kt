/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.ratelimit

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.rate-limits")
@DocProperty(
  displayName = "Rate limits",
  description = "Configuration of the rate limit behavior of the server.\n" +
    "Rate limits are used to protect against server overload and/or abuse, and are enabled by default.\n\n" +
    // TODO: dedicated documentation section somewhere about rate limits & link it here
    "You can disable global, per-endpoint and auth-related rate limits, or configure global rate limits.\n" +
    "Per-endpoint and auth-related rate limits are fixed and cannot be configured.\n\n"
)
class RateLimitProperties(
  @DocProperty(
    description = "To turn all rate limits off, set this value to `false`.\n\n" +
      ":::danger" +
      "This field is **deprecated** and will be removed with Tolgee 4. If set to `false`, it will take priority\n" +
      "over the more granular `global-limits`, `endpoint-limits` and `authentication-limits` and all limits\n" +
      "will be disabled." +
      ":::",
  )
  @Deprecated(message = "Use `global-limits`, `endpoint-limits` and `authentication-limits` individually instead.")
  val enabled: Boolean = true,

  @DocProperty(description = "Control whether global limits on the API are enabled or not.")
  val globalLimits: Boolean = true,

  @DocProperty(
    description = "Control whether per-endpoint limits on the API are enabled or not.\n" +
      "Does not affect authentication-related endpoints, these are controlled by `authentication-limits`."
  )
  val endpointLimits: Boolean = true,

  @DocProperty(
    description = "Control whether per-endpoint limits on authentication-related endpoints are enabled or not.\n" +
      ":::warning\n" +
      "It is **strongly** recommended to keep these limits enabled. They act as a protection layer against\n" +
      "brute-force attacks on the login (and register) prompt." +
      ":::"
  )
  val authenticationLimits: Boolean = true,

  @DocProperty(description = "Amount of requests an IP address can do in a single time window.")
  val ipRequestLimit: Int = 20_000,

  @DocProperty(
    description = "Size, in milliseconds, of the time window for IP-based limiting.",
    defaultExplanation = "= 5 minutes"
  )
  val ipRequestWindow: Long = 5 * 60 * 1000,

  @DocProperty(description = "Amount of requests a user can do in a single time window.")
  val userRequestLimit: Int = 400,

  @DocProperty(
    description = "Size, in milliseconds, of the time window for user-based limiting.",
    defaultExplanation = "= 1 minute"
  )
  val userRequestWindow: Long = 5 * 60 * 1000,
)
