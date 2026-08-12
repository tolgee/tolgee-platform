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

package io.tolgee.security.oauth2

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.tolgee.component.LockingProvider
import io.tolgee.component.fileStorage.FileStorage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

// RS256 signing keys; the private JWK (with kid) is persisted so restarts/replicas keep the same kid — else live tokens fail against the republished JWKS.
@Configuration
class OAuth2KeyConfig(
  private val fileStorage: FileStorage,
  private val lockingProvider: LockingProvider,
) {
  @Bean
  fun jwkSource(): JWKSource<SecurityContext> {
    return ImmutableJWKSet(JWKSet(loadOrGenerate(ACTIVE_KEY_FILE)))
  }

  @Bean
  fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder {
    return NimbusJwtEncoder(jwkSource)
  }

  @Bean("oauth2AccessTokenDecoder")
  fun oauth2AccessTokenDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
  }

  @Bean
  fun oauth2TokenGenerator(
    jwtEncoder: JwtEncoder,
    jwtCustomizer: OAuth2TokenCustomizer<JwtEncodingContext>,
  ): OAuth2TokenGenerator<OAuth2Token> {
    val jwtGenerator = JwtGenerator(jwtEncoder).apply { setJwtCustomizer(jwtCustomizer) }
    return DelegatingOAuth2TokenGenerator(
      jwtGenerator,
      OAuth2AccessTokenGenerator(),
      PublicClientRefreshTokenGenerator(),
    )
  }

  private fun loadOrGenerate(name: String): RSAKey {
    loadIfExists(name)?.let { return it }
    // Serialize first-boot generation across replicas: without the cluster-wide lock each replica of a fresh
    // deployment would generate a distinct key, and tokens signed by one would fail JWKS validation on another.
    return lockingProvider.withLocking(name) {
      loadIfExists(name) ?: generateRsaKey().also {
        fileStorage.storeFile(name, it.toJSONString().toByteArray(Charsets.UTF_8))
      }
    }
  }

  private fun loadIfExists(name: String): RSAKey? {
    if (!fileStorage.fileExists(name)) return null
    return RSAKey.parse(String(fileStorage.readFile(name), Charsets.UTF_8))
  }

  private fun generateRsaKey(): RSAKey {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    return RSAKey
      .Builder(keyPair.public as RSAPublicKey)
      .privateKey(keyPair.private as RSAPrivateKey)
      .keyID(UUID.randomUUID().toString())
      .build()
  }

  companion object {
    const val ACTIVE_KEY_FILE = "oauth2/jwk-active.json"
  }
}
