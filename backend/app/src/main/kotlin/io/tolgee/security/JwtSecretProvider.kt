/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.security

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

@Component
class JwtSecretProvider(
  private val tolgeeProperties: TolgeeProperties,
  private val fileStorage: FileStorage
) {
  private lateinit var cachedSecret: ByteArray

  val jwtSecret: ByteArray
    get() {
      if (this::cachedSecret.isInitialized) {
        return cachedSecret
      }

      if (tolgeeProperties.authentication.jwtSecret != null) {
        return tolgeeProperties.authentication.jwtSecret!!.toByteArray()
      }

      val fileName = "jwt.secret"

      if (!fileStorage.fileExists(fileName)) {
        val generated = Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded
        fileStorage.storeFile(fileName, generated)
        cachedSecret = generated
        return generated
      }

      cachedSecret = fileStorage.readFile(fileName)
      return cachedSecret
    }
}
