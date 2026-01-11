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
    const val GENERATED_JWT_SECRET_FILE_NAME = "jwt.secret"
  }
}
