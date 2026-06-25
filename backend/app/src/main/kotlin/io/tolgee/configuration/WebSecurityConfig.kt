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
import io.tolgee.component.TransferEncodingHeaderDebugFilter
import io.tolgee.security.authentication.AdminAccessInterceptor
import io.tolgee.security.authentication.AuthenticationFilter
import io.tolgee.security.authentication.AuthenticationInterceptor
import io.tolgee.security.authentication.EmailValidationInterceptor
import io.tolgee.security.authentication.ReadOnlyModeInterceptor
import io.tolgee.security.authentication.SsoAuthenticationInterceptor
import io.tolgee.security.authorization.FeatureAuthorizationInterceptor
import io.tolgee.security.authorization.OrganizationAuthorizationInterceptor
import io.tolgee.security.authorization.ProjectAuthorizationInterceptor
import io.tolgee.security.ratelimit.GlobalIpRateLimitFilter
import io.tolgee.security.ratelimit.GlobalUserRateLimitFilter
import io.tolgee.security.ratelimit.RateLimitInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
  @Lazy
  private val authenticationFilter: AuthenticationFilter,
  @Lazy
  private val globalIpRateLimitFilter: GlobalIpRateLimitFilter,
  @Lazy
  private val globalUserRateLimitFilter: GlobalUserRateLimitFilter,
  @Lazy
  private val rateLimitInterceptor: RateLimitInterceptor,
  @Lazy
  private val authenticationInterceptor: AuthenticationInterceptor,
  @Lazy
  private val emailValidationInterceptor: EmailValidationInterceptor,
  @Lazy
  private val ssoAuthenticationInterceptor: SsoAuthenticationInterceptor,
  @Lazy
  private val readOnlyModeInterceptor: ReadOnlyModeInterceptor,
  @Lazy
  private val adminAccessInterceptor: AdminAccessInterceptor,
  @Lazy
  private val organizationAuthorizationInterceptor: OrganizationAuthorizationInterceptor,
  @Lazy
  private val projectAuthorizationInterceptor: ProjectAuthorizationInterceptor,
  @Lazy
  private val featureAuthorizationInterceptor: FeatureAuthorizationInterceptor,
  private val exceptionHandlerFilter: ExceptionHandlerFilter,
) : WebMvcConfigurer {
  @Bean
  fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
    return httpSecurity
      // -- Global configuration
      .csrf { it.disable() }
      .cors(Customizer.withDefaults())
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(globalUserRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(globalIpRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .authorizeHttpRequests {
        it.withObjectPostProcessor(
          object : ObjectPostProcessor<AuthorizationFilter> {
            override fun <O : AuthorizationFilter?> postProcess(filter: O): O {
              // otherwise it throws error when using StreamingResponseBody
              filter?.setFilterAsyncDispatch(false)
              return filter
            }
          },
        )
        it.requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
        it.requestMatchers(*ADMIN_ENDPOINTS).hasRole("SUPPORTER")
        it.requestMatchers("/api/**", "/v2/**").authenticated()
        it.anyRequest().permitAll()
      }.headers { headers ->
        headers.xssProtection(Customizer.withDefaults())
        headers.contentTypeOptions(Customizer.withDefaults())
        headers.frameOptions {
          it.deny()
        }
        headers.referrerPolicy {
          it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
        }
      }.build()
  }

  @Bean
  @Order(10)
  @ConditionalOnProperty(value = ["tolgee.internal.controller-enabled"], havingValue = "false", matchIfMissing = true)
  fun internalSecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
    return httpSecurity
      .securityMatcher(*INTERNAL_ENDPOINTS)
      .authorizeHttpRequests { it.anyRequest().denyAll() }
      .build()
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(rateLimitInterceptor)
    registry.addInterceptor(authenticationInterceptor)
    registry
      .addInterceptor(ssoAuthenticationInterceptor)
      .excludePathPatterns(*PUBLIC_ENDPOINTS, *INTERNAL_ENDPOINTS)
    registry
      .addInterceptor(emailValidationInterceptor)
      .excludePathPatterns(*PUBLIC_ENDPOINTS, *INTERNAL_ENDPOINTS)

    registry
      .addInterceptor(organizationAuthorizationInterceptor)
      .addPathPatterns(*ORGANIZATION_ENDPOINTS)
    registry
      .addInterceptor(projectAuthorizationInterceptor)
      .addPathPatterns(*PROJECT_ENDPOINTS)
    registry
      .addInterceptor(adminAccessInterceptor)
      .addPathPatterns(*ADMIN_ENDPOINTS)
    registry
      .addInterceptor(readOnlyModeInterceptor)
      .excludePathPatterns(*PUBLIC_ENDPOINTS, *INTERNAL_ENDPOINTS)
    registry.addInterceptor(featureAuthorizationInterceptor)
  }

  @Bean
  fun customFilterRegistration(): FilterRegistrationBean<TransferEncodingHeaderDebugFilter>? {
    val registration: FilterRegistrationBean<TransferEncodingHeaderDebugFilter> =
      FilterRegistrationBean<TransferEncodingHeaderDebugFilter>()
    registration.filter = TransferEncodingHeaderDebugFilter()
    registration.order = Ordered.HIGHEST_PRECEDENCE
    return registration
  }

  companion object {
    private val PUBLIC_ENDPOINTS =
      arrayOf(
        "/api/public/**",
        "/v2/public/**",
        "/avatars/**",
        "/v3/api-docs/**",
        "/screenshots/**",
        "/uploaded-images/**",
      )
    private val ADMIN_ENDPOINTS = arrayOf("/v2/administration/**", "/v2/ee-license/**")
    private val INTERNAL_ENDPOINTS = arrayOf("/internal/**")
    private val PROJECT_ENDPOINTS = arrayOf("/v2/projects/**", "/api/project/**", "/api/repository/**")
    private val ORGANIZATION_ENDPOINTS = arrayOf("/v2/organizations/**")
  }
}
