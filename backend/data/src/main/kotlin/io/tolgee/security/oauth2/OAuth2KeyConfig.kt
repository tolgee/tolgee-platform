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
import io.tolgee.component.fileStorage.FileStorage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

/**
 * Asymmetric signing key material for the OAuth2 authorization server.
 *
 * This is deliberately separate from the symmetric HMAC secret in [io.tolgee.security.authentication.AuthenticationConfig]:
 * the legacy webapp JWT is HS512 (verifier holds the shared secret), while OAuth2 access tokens are RS256 so external
 * resource servers can verify them against the published JWKS without holding a secret.
 *
 * The private JWK is persisted via [FileStorage] (same backend as `jwt.secret`) so restarts and replicas keep the same
 * `kid`; tokens carry the `kid` and validators fetch the matching public key from `/oauth2/jwks`.
 */
@Configuration
class OAuth2KeyConfig(
  private val fileStorage: FileStorage,
) {
  @Bean
  fun jwkSource(): JWKSource<SecurityContext> {
    val active = loadOrGenerate(ACTIVE_KEY_FILE)
    val previous = loadIfExists(PREVIOUS_KEY_FILE)
    val jwkSet = JWKSet(listOfNotNull(active, previous))
    return ImmutableJWKSet(jwkSet)
  }

  @Bean
  fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder {
    return NimbusJwtEncoder(jwkSource)
  }

  @Bean("oauth2AccessTokenDecoder")
  fun oauth2AccessTokenDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
  }

  private fun loadOrGenerate(name: String): RSAKey {
    loadIfExists(name)?.let { return it }
    val generated = generateRsaKey()
    fileStorage.storeFile(name, generated.toJSONString().toByteArray(Charsets.UTF_8))
    return generated
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
    const val PREVIOUS_KEY_FILE = "oauth2/jwk-previous.json"
  }
}
