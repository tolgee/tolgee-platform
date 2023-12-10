package io.tolgee.testing

import io.tolgee.testing.mocking.MockWrappedBeanResetBeanProcessor
import org.springframework.context.annotation.Bean

class TestsConfiguration {
  @Bean
  fun mockWrappedBeanResetBeanProcessor(): MockWrappedBeanResetBeanProcessor {
    return MockWrappedBeanResetBeanProcessor()
  }
}
