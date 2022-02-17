/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.security

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import kotlin.random.Random

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
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..40).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
  }
}
