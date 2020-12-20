/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.security

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.service.FileStorageService
import org.springframework.stereotype.Component

@Component
class JwtSecretProvider(
        private val polygloatProperties: PolygloatProperties,
        private val fileStorageService: FileStorageService
) {
    private lateinit var cachedSecret: ByteArray

    val jwtSecret: ByteArray
        get() {
            if (this::cachedSecret.isInitialized) {
                return cachedSecret
            }

            if (polygloatProperties.authentication.jwtSecret != null) {
                return polygloatProperties.authentication.jwtSecret!!.toByteArray()
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