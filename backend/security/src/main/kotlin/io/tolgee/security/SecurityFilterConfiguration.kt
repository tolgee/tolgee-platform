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

package io.tolgee.security

import io.tolgee.security.authentication.AuthenticationFilter
import io.tolgee.security.ratelimit.GlobalIpRateLimitFilter
import io.tolgee.security.ratelimit.GlobalUserRateLimitFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityFilterConfiguration {
  @Bean("filterRegistrationAuth")
  fun authenticationFilter(filter: AuthenticationFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(filter)
    registration.isEnabled = false
    return registration
  }

  @Bean("filterRegistrationGlobalIpRateLimit")
  fun globalIpRateLimitFilter(filter: GlobalIpRateLimitFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(filter)
    registration.isEnabled = false
    return registration
  }

  @Bean("filterRegistrationGlobalUserRateLimit")
  fun globalUserRateLimitFilter(filter: GlobalUserRateLimitFilter): FilterRegistrationBean<*> {
    val registration = FilterRegistrationBean(filter)
    registration.isEnabled = false
    return registration
  }
}
