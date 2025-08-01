package io.tolgee.unit.util

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.spyk
import io.tolgee.constants.SupportedLocale
import io.tolgee.util.I18n
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.*

class I18nTest {

  private lateinit var i18n: I18n
  private lateinit var logger: Logger
  private lateinit var listAppender: ListAppender<ILoggingEvent>

  @BeforeEach
  fun setUp() {
    i18n = spyk<I18n>()

    logger = LoggerFactory.getLogger(I18n::class.java) as Logger
    listAppender = ListAppender<ILoggingEvent>()
    listAppender.start()
    logger.addAppender(listAppender)
  }

  @Test
  fun `translates without parameters`() {
    every { i18n.getBundle(SupportedLocale.EN) } returns resourceBundleOf(
      "greeting" to "Hello world"
    )

    val result = i18n.translate("greeting", locale = SupportedLocale.EN)
    assertThat(result).isEqualTo("Hello world")
    assertThat(listAppender.list).isEmpty()
  }

  @Test
  fun `translates with parameters`() {
    every { i18n.getBundle(SupportedLocale.EN) } returns resourceBundleOf(
      "welcome_user" to "Welcome {0}"
    )

    val result = i18n.translate("welcome_user", "John", locale = SupportedLocale.EN)
    assertThat(result).isEqualTo("Welcome John")
  }

  @Test
  fun `falls back to default locale`() {
    every { i18n.getBundle(SupportedLocale.CS) } returns resourceBundleOf()
    every { i18n.getBundle(SupportedLocale.DEFAULT) } returns resourceBundleOf(
      "only_in_default" to "Default only text"
    )

    val result = i18n.translate("only_in_default", locale = SupportedLocale.CS)

    assertThat(result).isEqualTo("Default only text")
    assertThat(listAppender.list).isEmpty()
  }

  @Test
  fun `returns key if missing and having one bundle and logs warning`() {
    every { i18n.getBundle(any()) } returns resourceBundleOf()

    val result = i18n.translate("non_existing_key", locale = SupportedLocale.EN)

    assertThat(result).isEqualTo("non_existing_key")

    val warnEvents = listAppender.list.filter { it.level.toString() == "WARN" }
    assertThat(warnEvents).hasSize(1)
    assertThat(warnEvents.first().formattedMessage)
      .contains("Key 'non_existing_key' was not found in the resource bundle for locale en")
  }

  @Test
  fun `returns key if missing in requested and default bundle and logs warning`() {
    every { i18n.getBundle(SupportedLocale.CS) } returns resourceBundleOf()
    every { i18n.getBundle(any()) } returns resourceBundleOf()

    val result = i18n.translate("non_existing_key", locale = SupportedLocale.CS)

    assertThat(result).isEqualTo("non_existing_key")

    val warnEvents = listAppender.list.filter { it.level.toString() == "WARN" }
    assertThat(warnEvents).hasSize(1)
    assertThat(warnEvents.first().formattedMessage)
      .contains("Key 'non_existing_key' was not found in the resource bundle for locales cs and en (default)")
  }

  private fun resourceBundleOf(vararg entries: Pair<String, String>): ResourceBundle {
    val map = mapOf(*entries)
    return object : ResourceBundle() {
      override fun handleGetObject(key: String): Any? = map[key]
      override fun getKeys(): Enumeration<String> = Collections.enumeration(map.keys)
    }
  }
}
