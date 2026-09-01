package io.tolgee.api.v2.controllers.organization

import io.tolgee.api.v2.controllers.project.ProjectAppsController
import io.tolgee.development.testDataBuilder.data.AppsWithInstallsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestPropertySource(properties = ["tolgee.apps.enabled=false"])
class OrganizationAppsDisabledTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appTokenService: AppTokenService

  lateinit var testData: AppsWithInstallsTestData

  @BeforeEach
  fun setup() {
    testData = AppsWithInstallsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `public configuration reports apps as disabled`() {
    performAuthGet("/api/public/configuration").andIsOk.andAssertThatJson {
      node("appsEnabled").isEqualTo(false)
    }
  }

  @Test
  fun `apps controllers are not registered when the feature is disabled`() {
    applicationContext
      .getBeanNamesForType(OrganizationAppsController::class.java)
      .toList()
      .assert
      .isEmpty()
    applicationContext
      .getBeanNamesForType(ProjectAppsController::class.java)
      .toList()
      .assert
      .isEmpty()
  }

  // Unmatched GETs forward to the SPA fallback (see WebConfiguration.addViewControllers),
  // so absence is only observable through non-GET methods, which the fallback rejects with 405.
  @Test
  fun `apps mutation endpoints reject requests when the feature is disabled`() {
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andExpect(status().isMethodNotAllowed)
    performAuthPut("/v2/projects/${testData.project.id}/apps/1", null)
      .andExpect(status().isMethodNotAllowed)
    performAuthDelete("/v2/projects/${testData.project.id}/apps/1")
      .andExpect(status().isMethodNotAllowed)
  }

  /**
   * The kill switch: disabling the feature must stop an already-minted app token too, not just new
   * mints — otherwise a compromised app keeps its access until the token expires. The install is real
   * and enabled, so only the disabled-feature guard makes this a 401 rather than reaching the handler.
   */
  @Test
  fun `an already-minted app token stops authenticating once the feature is disabled`() {
    val token = appTokenService.mintInstallContextToken(testData.enabledInstall.id)
    logout()
    perform(
      get("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    ).andIsUnauthorized
  }
}
