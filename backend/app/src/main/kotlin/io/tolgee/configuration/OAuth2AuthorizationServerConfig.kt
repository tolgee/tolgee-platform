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
import io.tolgee.security.oauth2.OAuth2AudienceResolver
import io.tolgee.security.oauth2.PublicClientRefreshAuthenticationConverter
import io.tolgee.security.oauth2.PublicClientRefreshAuthenticationProvider
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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

// [AuthenticationFilter] is deliberately NOT on this chain: the principal comes only from the session bootstrap, so a
// PAK/PAT/OAuth token can't authenticate /oauth2/authorize and self-escalate into a broader token.
@Configuration
class OAuth2AuthorizationServerConfig(
  private val audienceResolver: OAuth2AudienceResolver,
) {
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  fun authorizationServerSecurityFilterChain(
    http: HttpSecurity,
    exceptionHandlerFilter: ExceptionHandlerFilter,
    globalIpRateLimitFilter: GlobalIpRateLimitFilter,
    globalUserRateLimitFilter: GlobalUserRateLimitFilter,
    registeredClientRepository: RegisteredClientRepository,
  ): SecurityFilterChain {
    val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
    val endpointsMatcher = authorizationServerConfigurer.endpointsMatcher

    http
      .securityMatcher(endpointsMatcher)
      .with(authorizationServerConfigurer) { configurer ->
        configurer.clientAuthentication { clientAuth ->
          clientAuth.authenticationConverter(PublicClientRefreshAuthenticationConverter())
          clientAuth.authenticationProvider(PublicClientRefreshAuthenticationProvider(registeredClientRepository))
        }
        configurer.authorizationEndpoint {
          it.consentPage(CONSENT_PAGE_URI)
          it.authorizationResponseHandler(OAuth2SessionInvalidatingAuthorizationResponseHandler(issuer))
        }
        configurer.authorizationServerMetadataEndpoint { metadata ->
          metadata.authorizationServerMetadataCustomizer { claims ->
            claims.claim("client_id_metadata_document_supported", true)
            // Spring advertises jwks_uri unconditionally, but access tokens are opaque so no JWK set is published and
            // the advertised URL would 404. Drop the claim rather than point discovery at a dead endpoint.
            claims.claims { it.remove("jwks_uri") }
          }
        }
      }.authorizeHttpRequests { it.anyRequest().authenticated() }
      .csrf { it.ignoringRequestMatchers(endpointsMatcher) }
      .cors(Customizer.withDefaults())
      .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter::class.java)
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
    get() = audienceResolver.serverBaseUrl

  companion object {
    const val CONSENT_PAGE_URI = "/oauth2/consent"
    const val BOOTSTRAP_PAGE_URI = "/oauth2/bootstrap"
  }
}
