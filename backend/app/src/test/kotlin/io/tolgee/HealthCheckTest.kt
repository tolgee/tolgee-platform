/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee

import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class HealthCheckTest : AbstractControllerTest() {
  @Test
  fun `health check works`() {
    performGet("/actuator/health").andPrettyPrint.andIsOk
  }
}
