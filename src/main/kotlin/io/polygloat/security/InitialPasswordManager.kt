/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.security

import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.service.FileStorageService
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class InitialPasswordManager(
        private val polygloatProperties: PolygloatProperties,
        private val fileStorageService: FileStorageService,
) {
    private lateinit var cachedInitialPassword: String

    val initialPassword: String
        get() {
            if (this::cachedInitialPassword.isInitialized) {
                return cachedInitialPassword
            }

            if (polygloatProperties.authentication.initialPassword != null) {
                cachedInitialPassword = polygloatProperties.authentication.initialPassword!!
                return cachedInitialPassword
            }

            val filename = "initial.pwd"
            if (fileStorageService.fileExists(filename)) {
                cachedInitialPassword = fileStorageService.readFile(filename).toString(charset("UTF-8"))
                return cachedInitialPassword
            }

            val password = generatePassword()
            fileStorageService.storeFile(filename, password.toByteArray(charset("UTF-8")))
            cachedInitialPassword = password
            return cachedInitialPassword
        }

    private fun generatePassword(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..40).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
    }
}