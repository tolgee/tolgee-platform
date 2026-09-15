package io.tolgee.ee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.ConnectedAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.UserSessionType
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@SpringBootTest
@AutoConfigureMockMvc
class ConnectedAppsControllerTest : AuthorizedControllerTest() {
  lateinit var testData: ConnectedAppsTestData
  lateinit var connected: OAuth2Grant
  lateinit var foreign: OAuth2Grant

  @BeforeEach
  fun setup() {
    testData = ConnectedAppsTestData()
    connected = testData.addConnectedGrant()
    foreign = testData.addConnectedGrant(testData.otherUserAccountBuilder)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists the callers connected apps with client name and project binding`() {
    performAuthGet("/v2/user/connected-apps").andIsOk.andAssertThatJson {
      node("_embedded.connectedApps") {
        node("[0].id").isEqualTo(connected.id)
        node("[0].clientName").isEqualTo("Tolgee Browser Extension")
        node("[0].allProjects").isEqualTo(true)
        node("[0].scopes").isArray.containsExactly("translations.view")
      }
    }
  }

  @Test
  fun `revoking removes the app from the listing`() {
    performAuthDelete("/v2/user/connected-apps/${connected.id}").andIsOk

    listedIds().assert.doesNotContain(connected.id)
  }

  @Test
  fun `hides a grant of another user rather than forbidding it`() {
    performAuthDelete("/v2/user/connected-apps/${foreign.id}").andIsNotFound
    performAuthDelete("/v2/user/connected-apps/${foreign.id + 999999}").andIsNotFound
  }

  @Test
  fun `listing does not require a super token`() {
    val token = jwtService.emitToken(testData.user.id, type = UserSessionType.LOGIN_NATIVE, isSuper = false)

    performWithToken(HttpMethod.GET, "/v2/user/connected-apps", token).andIsOk
  }

  @Test
  fun `carries no @AllowApiAccess, so an OAuth access token stays refused by the interceptor`() {
    val getAllMethod = ConnectedAppsController::class.java.getDeclaredMethod("getAll", Pageable::class.java)
    val revokeMethod = ConnectedAppsController::class.java.getDeclaredMethod("revoke", Long::class.javaPrimitiveType)

    getAllMethod.getAnnotation(AllowApiAccess::class.java).assert.isNull()
    revokeMethod.getAnnotation(AllowApiAccess::class.java).assert.isNull()
  }

  private fun listedIds(): List<Long> {
    val body =
      performAuthGet("/v2/user/connected-apps?size=100")
        .andIsOk
        .andReturn()
        .response.contentAsString
    val embedded = mapper.readTree(body)["_embedded"] ?: return emptyList()
    val apps = embedded["connectedApps"] ?: return emptyList()
    return apps.values().map { it["id"].asLong() }
  }

  private fun performWithToken(
    method: HttpMethod,
    url: String,
    token: String,
  ) = perform(
    MockMvcRequestBuilders.request(method, url).header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
  )
}
