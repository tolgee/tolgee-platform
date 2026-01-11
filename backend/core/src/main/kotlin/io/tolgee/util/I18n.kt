package io.tolgee.util

import org.springframework.stereotype.Component
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

@Component
class I18n : Logging {
  private val bundle by lazy { ResourceBundle.getBundle("I18n", Locale.ENGLISH) }

  fun translate(
    key: String,
    vararg parameters: String,
  ): String {
    val bundleString =
      try {
        bundle.getString(key)
      } catch (e: MissingResourceException) {
        logger.warn("Key '$key' was not found in the resource bundle", e)
        key
      }

    return if (parameters.isEmpty()) {
      bundleString
    } else {
      MessageFormat(bundleString, Locale.ENGLISH).format(parameters)
    }
  }
}
