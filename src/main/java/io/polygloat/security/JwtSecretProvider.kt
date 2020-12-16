package io.polygloat.security

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.polygloat.configuration.polygloat.PolygloatProperties
import org.springframework.stereotype.Component
import java.io.File

@Component
class JwtSecretProvider(
        private val polygloatProperties: PolygloatProperties,
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


            val secretFilePath = polygloatProperties.dataPath + "/jwt.secret"
            val file = File(secretFilePath)
            if (!file.exists()) {
                val generated = Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded
                file.parentFile.mkdirs()
                file.writeBytes(generated)
                cachedSecret = generated
                return generated
            }
            cachedSecret = file.readBytes()
            return cachedSecret
        }
}