package io.tolgee.util

import org.springframework.core.io.ClassPathResource

class VersionProvider {
  companion object {
    val version: String by lazy {
      val resource = ClassPathResource(".VERSION", this::class.java.classLoader)
      if (!resource.exists()) "??" else resource.file.readText()
    }
  }
}
