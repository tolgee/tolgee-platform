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

package io.tolgee.security.authentication

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.AuthenticationProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.Key
import java.security.MessageDigest

@Configuration
class AuthenticationConfig(
  private val authenticationProperties: AuthenticationProperties,
  private val fileStorage: FileStorage,
) {
  @Bean("jwt_signing_key")
  fun jwtSigningKey(
    @Qualifier("jwt_signing_secret") bytes: ByteArray,
  ): Key {
    return Keys.hmacShaKeyFor(bytes)
  }

  /**
   * App tokens are signed with a key domain-separated from the user-session key, so a leak or
   * confusion on one path cannot forge tokens for the other. Derived deterministically from the same
   * secret, so no extra configuration is required.
   */
  @Bean("apps_jwt_signing_key")
  fun appsJwtSigningKey(
    @Qualifier("jwt_signing_secret") bytes: ByteArray,
  ): Key {
    val derived = MessageDigest.getInstance("SHA-512").digest(bytes + APPS_KEY_DERIVATION_LABEL)
    return Keys.hmacShaKeyFor(derived)
  }

  @Bean("jwt_signing_secret")
  fun jwtSigningSecret(): ByteArray {
    return authenticationProperties.jwtSecret?.toByteArray()
      ?: getOrGenerateJwtSecretFromFile()
  }

  private fun getOrGenerateJwtSecretFromFile(): ByteArray {
    if (!fileStorage.fileExists(GENERATED_JWT_SECRET_FILE_NAME)) {
      val generated = Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded
      fileStorage.storeFile(GENERATED_JWT_SECRET_FILE_NAME, generated)
      return generated
    }

    return fileStorage.readFile(GENERATED_JWT_SECRET_FILE_NAME)
  }

  companion object {
    private val APPS_KEY_DERIVATION_LABEL = "tolgee-apps-token-v1".toByteArray()
    const val GENERATED_JWT_SECRET_FILE_NAME = "jwt.secret"
  }
}
