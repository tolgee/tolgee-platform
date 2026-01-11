/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.security

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class InitialPasswordManager(
  private val tolgeeProperties: TolgeeProperties,
  private val fileStorage: FileStorage,
) {
  private lateinit var cachedInitialPassword: String

  val initialPassword: String
    get() {
      if (this::cachedInitialPassword.isInitialized) {
        return cachedInitialPassword
      }

      if (tolgeeProperties.authentication.initialPassword != null) {
        cachedInitialPassword = tolgeeProperties.authentication.initialPassword!!
        return cachedInitialPassword
      }

      val filename = "initial.pwd"
      if (fileStorage.fileExists(filename)) {
        cachedInitialPassword = fileStorage.readFile(filename).toString(charset("UTF-8"))
        return cachedInitialPassword
      }

      val password = generatePassword()
      fileStorage.storeFile(filename, password.toByteArray(charset("UTF-8")))
      cachedInitialPassword = password
      return cachedInitialPassword
    }

  private fun generatePassword(): String {
    return (1..40)
      .asSequence()
      .map { secureRandom.nextInt(charPool.size) }
      .map(charPool::get)
      .joinToString("")
  }

  val charPool by lazy {
    ('a'..'z') + ('A'..'Z') + ('0'..'9')
  }

  val secureRandom by lazy {
    SecureRandom()
  }
}
