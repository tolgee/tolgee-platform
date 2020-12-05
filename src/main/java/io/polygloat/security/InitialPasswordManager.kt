package io.polygloat.security

import io.polygloat.configuration.polygloat.PolygloatProperties
import org.springframework.stereotype.Component
import java.io.File
import kotlin.random.Random

@Component
class InitialPasswordManager(
        private val polygloatProperties: PolygloatProperties
) {
    private lateinit var cachedInitialPassword: String;

    val initialPassword: String
        get() {
            if (this::cachedInitialPassword.isInitialized) {
                return cachedInitialPassword
            }

            val file = File(pwdFile)
            if (file.exists()) {
                cachedInitialPassword = file.readText();
                return cachedInitialPassword;
            }

            val password = generatePassword()
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.writeText(password)
            cachedInitialPassword = password
            return cachedInitialPassword
        }

    private fun generatePassword(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..40).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
    }

    private val pwdFile
        get() = polygloatProperties.dataPath + "/initial.pwd"
}