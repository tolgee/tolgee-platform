package io.tolgee.email

import com.transferwise.icu.ICUReloadableResourceBundleMessageSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.Locale

@TestConfiguration
class EmailTemplateTestConfig {
  @Bean("emailIcuMessageSource")
  @Primary
  fun testMessageSource(): MessageSource {
    val messageSource = ICUReloadableResourceBundleMessageSource()
    messageSource.setBasenames(
      "classpath:email-i18n/messages",
      "classpath:email-i18n-test/messages",
    )
    messageSource.setDefaultEncoding("UTF-8")
    messageSource.setDefaultLocale(Locale.ENGLISH)
    return messageSource
  }
}
