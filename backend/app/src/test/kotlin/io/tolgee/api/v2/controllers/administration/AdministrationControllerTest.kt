package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.AdministrationTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders

@SpringBootTest
@AutoConfigureMockMvc
class AdministrationControllerTest : AuthorizedControllerTest() {
  lateinit var testData: AdministrationTestData

  @BeforeEach
  fun createData() {
    testData = AdministrationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
  }

  @Test
  fun `returns organizations`() {
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
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsOk
    assertThat(userAccountService.get(testData.user.id).role).isEqualTo(UserAccount.Role.ADMIN)
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/USER", null).andIsOk
    assertThat(userAccountService.get(testData.user.id).role).isEqualTo(UserAccount.Role.USER)
  }

  @Test
  fun `generates user jwt token`() {
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
    userAccount = testData.user
    performAuthGet("/v2/administration/organizations").andIsForbidden
    performAuthGet("/v2/administration/users").andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsForbidden
    performAuthGet("/v2/administration/users/${testData.user.id}/generate-token").andIsForbidden
  }

  @Test
  fun `read only endpoints are allowed for supporter`() {
    userAccount = testData.supporter
    performAuthGet("/v2/administration/organizations").andIsOk
    performAuthGet("/v2/administration/users").andIsOk
  }

  @Test
  fun `write endpoints are forbidden for supporter`() {
    userAccount = testData.supporter
    performAuthPut("/v2/administration/users/${testData.user.id}/set-role/ADMIN", null).andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/enable", null).andIsForbidden
    performAuthPut("/v2/administration/users/${testData.user.id}/disable", null).andIsForbidden
  }
}
