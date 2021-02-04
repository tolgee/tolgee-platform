/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.security

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.FileStorageService
import org.springframework.stereotype.Component

@Component
class JwtSecretProvider(
        private val tolgeeProperties: TolgeeProperties,
        private val fileStorageService: FileStorageService
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


            if (!fileStorageService.fileExists(fileName)) {
                val generated = Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded
                fileStorageService.storeFile(fileName, generated)
                cachedSecret = generated
                return generated
            }

            cachedSecret = fileStorageService.readFile(fileName)
            return cachedSecret
        }
}