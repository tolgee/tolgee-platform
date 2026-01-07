package io.tolgee.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender

@TestConfiguration
class TestEmailConfiguration {
  /**
   * It is not a @MockitoBean for several reasons. But the main is
   * 1. Registration Timing: @MockitoBean (and its predecessor @MockBean) registers a mock that is tied
   * to the ApplicationContext cache key. When used in a shared @TestConfiguration, it sometimes fails
   * to properly override the "real" bean if that bean is already defined in the main configuration,
   * especially if the ApplicationContext is already partially initialized or if there are conflicts
   * in how the override is registered.
   *
   * 2. Context Pollution/Sharing: @MockitoBean is highly integrated with the test context framework.
   * Using it in a shared configuration can sometimes lead to unexpected behavior where the mock isn't
   * correctly "seen" by other components (like EmailTestUtil) because it's not being treated as a standard
   * Spring bean with high priority.
   */
  @Bean
  @Primary
  fun javaMailSender(): JavaMailSender {
    return Mockito.mock(JavaMailSender::class.java)
  }
}
