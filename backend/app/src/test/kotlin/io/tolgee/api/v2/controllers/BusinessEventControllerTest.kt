package io.tolgee.api.v2.controllers

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BusinessEventControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

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
      bearerHeaders(generateJwtToken(userAccount!!.id)),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "TEST_EVENT")
    params["organizationId"].assert.isNotNull
    params["organizationName"].assert.isEqualTo("test_username")
    params["test"].assert.isEqualTo("test")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it does not let the free-form data map overwrite the attribution`() {
    val organizationId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id
    performPost(
      "/v2/public/business-events/report",
      mapOf(
        "eventName" to "DATA_MAP_EVENT",
        "organizationId" to organizationId,
        "data" to
          mapOf(
            "organizationId" to 999999,
            "organizationName" to "Acme",
            "${'$'}set_once" to mapOf("email" to "attacker@example.com"),
          ),
      ),
      bearerHeaders(generateJwtToken(userAccount!!.id)),
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "DATA_MAP_EVENT")
    params["organizationId"].assert.isEqualTo(organizationId)
    params["organizationName"].assert.isEqualTo("test_username")
    params.containsKey("${'$'}set_once").assert.isFalse
  }
}
