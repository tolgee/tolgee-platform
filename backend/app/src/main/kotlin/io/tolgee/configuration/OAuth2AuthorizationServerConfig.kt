/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
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
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.security.ratelimit.GlobalIpRateLimitFilter
import io.tolgee.security.ratelimit.GlobalUserRateLimitFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

/**
 * OAuth2 authorization-server filter chain. It is scoped to the protocol endpoints (the oauth2, well-known and connect
 * paths) via the configurer's endpoint matcher and runs at [Ordered.HIGHEST_PRECEDENCE], ahead of the internal
 * chain (@Order 10) and the unordered main chain.
 *
 * Unlike the main chain this one is NOT stateless: the authorization_code flow needs an HttpSession to carry the
 * authenticated principal through consent. The principal is established only by the session bootstrap (from a full
 * webapp login); Tolgee's [AuthenticationFilter] is deliberately NOT added here, so a delegated API credential
 * (PAK/PAT) or an OAuth token cannot authenticate `/oauth2/authorize` and self-escalate into a broader token.
 */
@Configuration
class OAuth2AuthorizationServerConfig(
  private val tolgeeProperties: TolgeeProperties,
) {
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  fun authorizationServerSecurityFilterChain(
    http: HttpSecurity,
    exceptionHandlerFilter: ExceptionHandlerFilter,
    globalIpRateLimitFilter: GlobalIpRateLimitFilter,
    globalUserRateLimitFilter: GlobalUserRateLimitFilter,
  ): SecurityFilterChain {
    val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
    val endpointsMatcher = authorizationServerConfigurer.endpointsMatcher

    http
      .securityMatcher(endpointsMatcher)
      .with(authorizationServerConfigurer) { configurer ->
        configurer.authorizationEndpoint { it.consentPage(CONSENT_PAGE_URI) }
        configurer.authorizationServerMetadataEndpoint { metadata ->
          metadata.authorizationServerMetadataCustomizer { claims ->
            claims.claim("client_id_metadata_document_supported", true)
          }
        }
      }.authorizeHttpRequests { it.anyRequest().authenticated() }
      .csrf { it.ignoringRequestMatchers(endpointsMatcher) }
      .cors(Customizer.withDefaults())
      .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter::class.java)
      // The global rate limiters are wired per-chain (registered disabled, added explicitly); this chain needs them
      // too so the token/authorize endpoints are throttled like the rest of the API.
      .addFilterBefore(globalUserRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(globalIpRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
      .exceptionHandling {
        it.defaultAuthenticationEntryPointFor(
          OAuth2BootstrapAuthenticationEntryPoint(BOOTSTRAP_PAGE_URI),
          MediaTypeRequestMatcher(MediaType.TEXT_HTML),
        )
      }

    return http.build()
  }

  @Bean
  fun authorizationServerSettings(): AuthorizationServerSettings {
    val builder = AuthorizationServerSettings.builder()
    issuer?.let { builder.issuer(it) }
    return builder.build()
  }

  private val issuer: String?
    get() = tolgeeProperties.backEndUrl ?: tolgeeProperties.frontEndUrl

  companion object {
    const val CONSENT_PAGE_URI = "/oauth2/consent"
    const val BOOTSTRAP_PAGE_URI = "/oauth2/bootstrap"
  }
}
