package io.tolgee.util

import io.tolgee.Application
import org.springframework.core.io.ClassPathResource
import java.io.FileNotFoundException

class VersionProvider {
  companion object {
    val version: String by lazy {
      try {
        val resource = ClassPathResource(".VERSION", this::class.java.classLoader)
        return@lazy if (!resource.exists()) "??" else resource.file.readText()
      } catch (e: FileNotFoundException) {
        return@lazy Application::class.java.getPackage().implementationVersion
      }
    }
  }
}
