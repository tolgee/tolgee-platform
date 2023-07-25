package io.tolgee.api.v2.controllers

import com.posthog.java.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders

class BusinessEventControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

  @MockBean
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
        "data" to mapOf("test" to "test")
      ),
      HttpHeaders().also {
        it["Authorization"] = listOf(AuthorizedRequestFactory.getBearerTokenString(generateJwtToken(userAccount!!.id)))
      }
    ).andIsOk

    var params: Map<String, Any?>? = null
    waitForNotThrowing(timeout = 10000) {
      verify(postHog, times(1)).capture(
        any(), eq("TEST_EVENT"),
        argThat {
          params = this
          true
        }
      )
    }
    params!!["organizationId"].assert.isNotNull
    params!!["organizationName"].assert.isEqualTo("test_username")
    params!!["test"].assert.isEqualTo("test")
  }
}
