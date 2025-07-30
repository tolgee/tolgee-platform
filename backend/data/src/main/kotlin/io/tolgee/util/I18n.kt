package io.tolgee.util

import io.tolgee.constants.SupportedLocale
import org.springframework.stereotype.Component
import java.text.MessageFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class I18n : Logging {
  private val bundles = ConcurrentHashMap<SupportedLocale, ResourceBundle>()
  private val defaultLocale: SupportedLocale = SupportedLocale.DEFAULT

  internal fun getBundle(locale: SupportedLocale): ResourceBundle {
    return bundles.getOrPut(locale) {
      ResourceBundle.getBundle(
        "I18n",
        Locale.forLanguageTag(locale.code),
      )
    }
  }

  private fun getBundleString(key: String, locale: SupportedLocale): String {
    fun tryGetBundleString(locale: SupportedLocale): String? {
      return try {
        getBundle(locale).getString(key)
      } catch (_: MissingResourceException) {
        null
      } catch (_: Exception) {
        null
      }
    }

    tryGetBundleString(locale)?.let { return it }

    if (locale != defaultLocale) {
      tryGetBundleString(defaultLocale)?.let { return it }
    }

    logMissing(key, locale.code)
    return key
  }

  private fun logMissing(key: String, locale: String) {
    if (locale != defaultLocale.code) {
      logger.warn(
        "Key '$key' was not found in the resource bundle for locales $locale and ${defaultLocale.code} (default)"
      )
    } else {
      logger.warn("Key '$key' was not found in the resource bundle for locale $locale")
    }
  }

  fun translate(
    key: String,
    vararg parameters: String,
    locale: SupportedLocale = defaultLocale
  ): String {
    val bundleString = getBundleString(key, locale)

    return if (parameters.isEmpty()) {
      bundleString
    } else {
      MessageFormat(bundleString, Locale.forLanguageTag(locale.code)).format(parameters)
    }
  }
}
