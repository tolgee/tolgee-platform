package io.tolgee.util

import org.springframework.stereotype.Component
import java.util.*

@Component
class I18n {
  private val bundle by lazy { ResourceBundle.getBundle("I18n", Locale.ENGLISH) }

  fun translate(key: String): String {
    return try {
      bundle.getString(key)
    } catch (e: MissingResourceException) {
      key
    }
  }
}
