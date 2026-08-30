package io.tolgee.api.v2.controllers.administration

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AdministrationTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.UserDisabledBy
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders

@SpringBootTest
@AutoConfigureMockMvc
class AdministrationControllerTest : AuthorizedControllerTest() {
  lateinit var testData: AdministrationTestData

  @BeforeEach
  fun createData() {
    testData = AdministrationTestData()
  }

  @AfterEach
  fun cleanUp() {
    testDataService.cleanTestData(testData.root)
  }

  @AfterEach
  fun cleanData() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `returns organizations`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    performAuthGet("/v2/administration/organizations").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.organizations") {
        isArray.hasSizeGreaterThan(1)
        node("[0]") {
          node("name").isEqualTo("John User")
        }
      }
    }
  }

  @Test
  fun `searches by id in organizations`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    val organizationId = testData.adminBuilder.defaultOrganizationBuilder.self.id
    performAuthGet("/v2/administration/organizations?search=$organizationId")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.organizations") {
          isArray.hasSize(1)
          node("[0]") {
            node("name").isEqualTo("Peter Administrator")
          }
        }
      }
  }

  @Test
  fun `returns users`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    performAuthGet("/v2/administration/users").andPrettyPrint.andAssertThatJson {
      node("_embedded.users") {
        isArray.hasSizeGreaterThan(1)
        node("[0]") {
          node("name").isEqualTo("John User")
        }
      }
    }
  }

  @Test
  fun `sets role`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsOk
    assertThat(userAccountService.get(testData.user.id).role).isEqualTo(UserAccount.Role.ADMIN)
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/USER", null).andIsOk
    assertThat(userAccountService.get(testData.user.id).role).isEqualTo(UserAccount.Role.USER)
  }

  @Test
  fun `does not delete initial user`() {
    testData.makeUserInitial()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    performAuthDelete("/v2/administration/users/${testData.user.id}")
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_DELETE_INITIAL_USER)
    userAccountService.findActive(testData.user.id).assert.isNotNull
  }

  @Test
  fun `generates user jwt token`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    val token =
      performAuthGet("/v2/administration/users/${testData.user.id}/generate-token")
        .andIsOk.andGetContentAsString

    performGet(
      "/v2/organizations",
      HttpHeaders().apply {
        set("Authorization", "Bearer $token")
      },
    ).andIsOk

    val org = OrganizationDto(name = "my lil organization 1")
    performPost(
      "/v2/organizations",
      org,
      HttpHeaders().apply {
        set("Authorization", "Bearer $token")
      },
    ).andIsCreated
  }

  @Test
  fun `generates read-only user jwt token for supporter`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.supporter

    val token =
      performAuthGet("/v2/administration/users/${testData.user.id}/generate-token")
        .andIsOk.andGetContentAsString

    performGet(
      "/v2/organizations",
      HttpHeaders().apply {
        set("Authorization", "Bearer $token")
      },
    ).andIsOk

    val org = OrganizationDto(name = "my lil organization 2")
    performPost(
      "/v2/organizations",
      org,
      HttpHeaders().apply {
        set("Authorization", "Bearer $token")
      },
    ).andIsForbidden
  }

  @Test
  fun `endpoints are forbidden to standard user`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performAuthGet("/v2/administration/organizations").andIsForbidden
    performAuthGet("/v2/administration/users").andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsForbidden
    performAuthGet("/v2/administration/users/${testData.user.id}/generate-token").andIsForbidden
  }

  @Test
  fun `read only endpoints are allowed for supporter`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.supporter
    performAuthGet("/v2/administration/organizations").andIsOk
    performAuthGet("/v2/administration/users").andIsOk
  }

  @Test
  fun `write endpoints are forbidden for supporter`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.supporter
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/enable", null).andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsForbidden
  }

  @Test
  fun `disable and enable are idempotent`() {
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsOk
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsOk
    userAccountService.findActive(testData.user.id).assert.isNull()

    performAuthPut("/v2/administration/users/${testData.user.id}/enable", null).andIsOk
    performAuthPut("/v2/administration/users/${testData.user.id}/enable", null).andIsOk
    userAccountService.findActive(testData.user.id).assert.isNotNull
  }

  @Test
  fun `admin disable records the admin as the origin`() {
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsOk
    assertThat(userAccountService.findActiveOrDisabled(testData.user.id)!!.disabledBy)
      .isEqualTo(UserDisabledBy.ADMIN)
  }

  @Test
  fun `admin enable clears the disable origin`() {
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsOk

    performAuthPut("/v2/administration/users/${testData.user.id}/enable", null).andIsOk
    assertThat(userAccountService.findActiveOrDisabled(testData.user.id)!!.disabledBy).isNull()
  }
}
