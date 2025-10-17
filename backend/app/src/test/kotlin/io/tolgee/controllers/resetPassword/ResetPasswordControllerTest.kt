package io.tolgee.controllers.resetPassword

import io.tolgee.config.TestEmailConfiguration
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.auth.ResetPasswordRequest
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import

@AutoConfigureMockMvc
@Import(TestEmailConfiguration::class)
class ResetPasswordControllerTest : AbstractControllerTest() {
  private var defaultFrontendUrl: String? = null

  @BeforeEach
  fun setup() {
    defaultFrontendUrl = tolgeeProperties.frontEndUrl
    emailTestUtil.initMocks()
    testData = BaseTestData()
    saveTestData()
  }

  @AfterEach
  fun tearDown() {
    tolgeeProperties.frontEndUrl = defaultFrontendUrl
  }

  private lateinit var testData: BaseTestData

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Test
  fun `email contains correct callback url with frontend url provided`() {
    executePasswordChangeRequest()
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.firstMessageContent.assert.contains("https://dummy-url.com/reset_password/")
      // We don't want double slashes
      emailTestUtil.firstMessageContent.assert.doesNotContain("reset_password//")
    }
  }

  @Test
  fun `email contains correct callback url without frontend url provided`() {
    tolgeeProperties.frontEndUrl = null
    executePasswordChangeRequest()
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.firstMessageContent.assert.contains("https://hello.com/aa/")
      // We don't want double slashes
      emailTestUtil.firstMessageContent.assert.doesNotContain("aa//")
    }
  }

  private fun executePasswordChangeRequest() {
    val dto =
      ResetPasswordRequest(
        email = testData.user.username,
        callbackUrl = "https://hello.com/aa/",
      )
    performPost("/api/public/reset_password_request", dto).andIsOk
  }

  private fun saveTestData() {
    testData.user.username = "example@example.com"
    testDataService.saveTestData(testData.root)
  }
}
