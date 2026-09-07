package io.tolgee.controllers.internal

import io.tolgee.development.testDataBuilder.data.TestClockTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.util.Date

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.internal.controller-enabled=false",
    "tolgee.internal.test-clock-enabled=true",
  ],
)
class TestClockFlagTest : AuthorizedControllerTest() {
  lateinit var testData: TestClockTestData

  @BeforeEach
  fun setup() {
    testData = TestClockTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `admin sets and releases the clock`() {
    loginAsUser(testData.admin)
    performAuthPut("/internal/time/1700000000000", null).andIsOk
    assertThat(currentDateProvider.forcedDate).isEqualTo(Date(1700000000000))
    performAuthDelete("/internal/time", null).andIsOk
    assertThat(currentDateProvider.forcedDate).isNull()
  }

  @Test
  fun `regular user cannot set the clock`() {
    loginAsUser(testData.user)
    performAuthPut("/internal/time/1700000000000", null).andIsForbidden
    assertThat(currentDateProvider.forcedDate).isNull()
  }

  @Test
  fun `other internal endpoints stay denied`() {
    loginAsUser(testData.admin)
    performAuthPost("/internal/sql/execute", "select 1").andIsForbidden
  }

  @Test
  fun `cross-origin preflight is answered`() {
    mvc
      .perform(
        options("/internal/time/1700000000000")
          .header("Origin", "http://localhost:3000")
          .header("Access-Control-Request-Method", "PUT"),
      ).andIsOk
      .andExpect(header().string("Access-Control-Allow-Origin", "*"))
  }

  @Test
  fun `public configuration exposes the flag`() {
    performGet("/api/public/configuration").andIsOk.andAssertThatJson {
      node("testClockEnabled").isEqualTo(true)
      node("internalControllerEnabled").isEqualTo(false)
    }
  }
}
