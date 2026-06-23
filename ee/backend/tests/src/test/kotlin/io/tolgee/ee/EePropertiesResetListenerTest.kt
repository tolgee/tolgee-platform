package io.tolgee.ee

import io.tolgee.AbstractSpringTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EePropertiesResetListenerTest : AbstractSpringTest() {
  @Autowired
  lateinit var eeProperties: EeProperties

  @Test
  @Order(1)
  fun `mutation of an ee property is visible within the test`() {
    assertThat(eeProperties.scheduledReportingEnabled).isFalse()
    eeProperties.scheduledReportingEnabled = true
    assertThat(eeProperties.scheduledReportingEnabled).isTrue()
  }

  @Test
  @Order(2)
  fun `prior ee-property mutation is rolled back to the yaml baseline, not the kotlin default`() {
    assertThat(eeProperties.scheduledReportingEnabled).isFalse()
  }
}
