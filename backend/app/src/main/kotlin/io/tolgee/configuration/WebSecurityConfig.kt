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

package io.tolgee.configuration

import io.tolgee.component.ExceptionHandlerFilter
import io.tolgee.security.authentication.AuthenticationDisabledFilter
import io.tolgee.security.authentication.AuthenticationFilter
import io.tolgee.security.authentication.AuthenticationInterceptor
import io.tolgee.security.authorization.OrganizationAuthorizationInterceptor
import io.tolgee.security.authorization.ProjectAuthorizationInterceptor
import io.tolgee.security.ratelimit.GlobalIpRateLimitFilter
import io.tolgee.security.ratelimit.GlobalUserRateLimitFilter
import io.tolgee.security.ratelimit.RateLimitInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
  private val authenticationFilter: AuthenticationFilter,
  private val authenticationDisabledFilter: AuthenticationDisabledFilter,
  private val globalIpRateLimitFilter: GlobalIpRateLimitFilter,
  private val globalUserRateLimitFilter: GlobalUserRateLimitFilter,
  private val rateLimitInterceptor: RateLimitInterceptor,
  private val authenticationInterceptor: AuthenticationInterceptor,
  private val organizationAuthorizationInterceptor: OrganizationAuthorizationInterceptor,
  private val projectAuthorizationInterceptor: ProjectAuthorizationInterceptor,
  private val exceptionHandlerFilter: ExceptionHandlerFilter,
) : WebMvcConfigurer {
  @Bean
  fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
    return httpSecurity.csrf().disable()
      // -- Global configuration
      .headers()
      .referrerPolicy().policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN).and()
      .xssProtection().block(true).and()
      .contentTypeOptions().and()
      .frameOptions().deny()
      .and()
      .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(authenticationDisabledFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(globalUserRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(globalIpRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .authorizeRequests()
      .antMatchers("/api/public/**", "/v2/public/**").permitAll()
      .antMatchers("/v2/administration/**", "/v2/ee-license/**").hasRole("ADMIN")
      .antMatchers("/api/**", "/v2/**").authenticated()
      .anyRequest().permitAll()
      .and().build()
  }

  @Bean
  @Order(10)
  @ConditionalOnProperty(value = ["tolgee.internal.controller-enabled"], havingValue = "false", matchIfMissing = true)
  fun internalSecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
    return httpSecurity
      .antMatcher("/internal/**")
      .authorizeRequests()
      .anyRequest()
      .denyAll()
      .and()
      .build()
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(rateLimitInterceptor)
    registry.addInterceptor(authenticationInterceptor)

    registry.addInterceptor(organizationAuthorizationInterceptor)
      .addPathPatterns("/v2/organizations/**")
    registry.addInterceptor(projectAuthorizationInterceptor)
      .addPathPatterns("/v2/projects/**", "/api/project/**", "/api/repository/**")
  }
}
