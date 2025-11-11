package io.tolgee.api.v2.controllers

import com.posthog.PostHogInterface
import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean

class BusinessEventControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it accepts header`() {
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "TEST_EVENT",
        "organizationId" to testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
        "projectId" to testData.projectBuilder.self.id,
        "data" to mapOf("test" to "test"),
      ),
      HttpHeaders().also {
        it["Authorization"] = listOf(AuthorizedRequestFactory.getBearerTokenString(generateJwtToken(userAccount!!.id)))
      },
    ).andIsOk

    val params = assertPostHogEventReported("TEST_EVENT")
    params["organizationId"].assert.isNotNull
    params["organizationName"].assert.isEqualTo("test_username")
    params["test"].assert.isEqualTo("test")
  }

  private fun assertPostHogEventReported(eventName: String): Map<String, Any> {
    var params: Map<String, Any> = emptyMap()
    waitForNotThrowing(timeout = 10000) {
      val mockingDetails = Mockito.mockingDetails(postHog)
      val invocations = mockingDetails.invocations
      val captureInvocation = invocations.find {
        it.method.name == "capture" && it.arguments[1] == eventName
      }
      captureInvocation.assert.isNotNull()
      params = captureInvocation!!.arguments[2] as Map<String, Any>
    }
    return params
  }
}
