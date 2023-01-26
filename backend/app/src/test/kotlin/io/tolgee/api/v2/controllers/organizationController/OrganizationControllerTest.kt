package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest : BaseOrganizationControllerTest() {
  @Test
  fun testGetAll() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].name)

    performAuthGet("/v2/organizations?size=100")
      .andPrettyPrint.andAssertThatJson {
        node("_embedded.organizations") {
          isArray.hasSize(6)
          node("[0].name").isEqualTo("user-2's organization 1")
          node("[0].basePermissions").isEqualTo("VIEW")
          node("[0].currentUserRole").isEqualTo("OWNER")
        }
      }
  }

  @Test
  fun `get all returns also organizations with project with direct permission`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)

    userAccount = testData.franta

    performAuthGet("/v2/organizations?size=100")
      .andPrettyPrint.andAssertThatJson.let {
        it.node("_embedded.organizations").let {
          it.isArray.hasSize(1)
        }
      }
  }

  @Test
  fun `returns all project in organization without checking for permissions`() {
    val testData = OrganizationTestData()

    testDataService.saveTestData(testData.root)
    userAccount = testData.franta
    val organization = testData.root.data.organizations.map { it.self }
      .filter { it.name == "test_username" }
      .single()

    performAuthGet("/v2/organizations/${organization.slug}/projects?size=100")
      .andPrettyPrint.andAssertThatJson.let {
        it.node("_embedded.projects").let {
          it.isArray.hasSize(1)
        }
      }
  }

  @Test
  fun testGetAllFilterOwned() {
    val users = dbPopulator.createUsersAndOrganizations()

    loginAsUser(users[1].name)

    performAuthGet("/v2/organizations?size=100&filterCurrentUserOwner=true")
      .andPrettyPrint.andAssertThatJson.let {
        it.node("_embedded.organizations").let {
          it.isArray.hasSize(1)
          it.node("[0].name").isEqualTo("user-2's organization 1")
          it.node("[0].basePermissions").isEqualTo("VIEW")
          it.node("[0].currentUserRole").isEqualTo("OWNER")
        }
      }
  }

  @Test
  fun testGetAllSort() {
    val users = dbPopulator.createUsersAndOrganizations()

    loginAsUser(users[1].name)

    performAuthGet("/v2/organizations?size=100&sort=basePermissions,desc&sort=name,desc")
      .andPrettyPrint
      .andAssertThatJson
      .node("_embedded.organizations").node("[0].name").isEqualTo("user-4's organization 3")
  }

  @Test
  fun testGetAllUsers() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[0].username)
    val organizationId = users[1].organizationRoles[0].organization!!.id
    performAuthGet("/v2/organizations/$organizationId/users").andIsOk
      .also { println(it.andReturn().response.contentAsString) }
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(2)
          node("[0].organizationRole").isEqualTo("MEMBER")
          node("[1].organizationRole").isEqualTo("OWNER")
        }
      }
  }

  @Test
  fun testGetOneWithUrl() {
    createOrganization(dummyDto).let {
      performAuthGet("/v2/organizations/${it.slug}").andIsOk.andAssertThatJson {
        node("name").isEqualTo(dummyDto.name)
        node("description").isEqualTo(dummyDto.description)
      }
    }
  }

  @Test
  fun `returns one only with project base permission`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    val organization = testData.userAccountBuilder.defaultOrganizationBuilder.self
    userAccount = testData.pepa
    performAuthGet("/v2/organizations/${organization.id}").andIsOk
  }

  @Test
  fun `doesn't return without permission`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    val organization = testData.jirinaOrg
    userAccount = testData.pepa
    performAuthGet("/v2/organizations/${organization.id}").andIsForbidden
  }

  @Test
  fun testGetOneWithId() {
    createOrganization(dummyDto).let { organization ->
      performAuthGet("/v2/organizations/${organization.id}").andIsOk.andAssertThatJson {
        node("name").isEqualTo(dummyDto.name)
        node("id").isEqualTo(organization.id)
        node("description").isEqualTo(dummyDto.description)
        node("basePermissions").isEqualTo(dummyDto.basePermissions.name)
        node("slug").isEqualTo(dummyDto.slug)
      }
    }
  }

  @Test
  fun testGetOnePermissions() {
    val organization = createOrganization(dummyDto)
    performAuthGet("/v2/organizations/${organization.id}").andIsOk.andAssertThatJson {
      node("name").isEqualTo(dummyDto.name)
      node("description").isEqualTo(dummyDto.description)
    }
  }

  @Test
  fun testGetAllUsersNotPermitted() {
    val users = dbPopulator.createUsersAndOrganizations()
    val organizationId = users[1].organizationRoles[0].organization!!.id
    performAuthGet("/v2/organizations/$organizationId/users").andIsForbidden
  }

  @Test
  fun testCreate() {
    performAuthPost(
      "/v2/organizations",
      dummyDto
    ).andIsCreated.andPrettyPrint.andAssertThatJson {
      node("name").isEqualTo("Test org")
      node("slug").isEqualTo("test-org")
      node("_links.self.href").isEqualTo("http://localhost/v2/organizations/test-org")
      node("id").isNumber.satisfies {
        organizationService.find(it.toLong()) is Organization
      }
    }
  }

  @Test
  fun testCreateSlugValidation() {
    createOrganization(dummyDto2.also { it.slug = "hello-1" })

    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.slug = "hello-1" }
    ).andIsBadRequest.andAssertError.isCustomValidation.hasMessage("address_part_not_unique")
  }

  @Test
  fun testCreateNotAllowed() {
    this.tolgeeProperties.authentication.userCanCreateOrganizations = false
    performAuthPost(
      "/v2/organizations",
      dummyDto
    ).andIsForbidden
    this.tolgeeProperties.authentication.userCanCreateOrganizations = true
  }

  @Test
  fun testCreateValidation() {
    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.slug = "" }
    ).andIsBadRequest.let {
      assertThat(it.andReturn()).error().isStandardValidation.onField("slug")
    }
    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.name = "" }
    ).andIsBadRequest.let {
      assertThat(it.andReturn()).error().isStandardValidation.onField("name")
    }

    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.slug = "sahsaldlasfhl " }
    ).andIsBadRequest.let {
      assertThat(it.andReturn()).error().isStandardValidation.onField("slug")
    }

    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.slug = "a" }
    ).andIsBadRequest.let {
      assertThat(it.andReturn()).error().isStandardValidation.onField("slug")
    }
  }

  @Test
  fun testCreateGeneratesSlug() {
    performAuthPost(
      "/v2/organizations",
      dummyDto.also { it.slug = null }
    ).andIsCreated.andAssertThatJson { node("slug").isEqualTo("test-org") }
  }

  @Test
  fun `edits organization`() {
    val organization = createOrganization(dummyDto)
    performAuthPut(
      "/v2/organizations/${organization.id}",
      dummyDto.also { dto ->
        dto.name = "Hello"
        dto.slug = "hello-1"
        dto.basePermissions = Permission.ProjectPermissionType.TRANSLATE
        dto.description = "This is changed description"
      }
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("name").isEqualTo("Hello")
      node("slug").isEqualTo("hello-1")
      node("_links.self.href").isEqualTo("http://localhost/v2/organizations/hello-1")
      node("basePermissions").isEqualTo("TRANSLATE")
      node("description").isEqualTo("This is changed description")
    }
  }

  @Test
  fun `slug validation`() {
    createOrganization(dummyDto2.also { it.slug = "hello-1" })
    val organization = createOrganization(dummyDto)
    performAuthPut(
      "/v2/organizations/${organization.id}",
      dummyDto.also { organizationDto ->
        organizationDto.slug = "hello-1"
      }
    ).andIsBadRequest.andAssertError.isCustomValidation.hasMessage("address_part_not_unique")
  }

  @Test
  fun `it deletes organization`() {
    val organization2 = createOrganization(dummyDto2)
    createOrganization(dummyDto).let {
      performAuthDelete("/v2/organizations/${it.id}", null)
      assertThat(organizationService.find(it.id)).isNull()
      assertThat(organizationService.find(organization2.id)).isNotNull
    }
  }

  private fun createOrganization(organizationDto: OrganizationDto) = executeInNewTransaction {
    organizationService.create(
      organizationDto, userAccount!!
    )
  }

  @Test
  @Transactional
  fun `sets user role`() {
    withOwnerInOrganization { organization, owner, role ->
      performAuthPut(
        "/v2/organizations/${organization.id}/users/${owner.id}/set-role",
        SetOrganizationRoleDto(OrganizationRoleType.MEMBER)
      ).andIsOk
      role.let { assertThat(it.type).isEqualTo(OrganizationRoleType.MEMBER) }
    }
  }

  @Test
  @Transactional
  fun `cannot set own permission`() {
    withOwnerInOrganization { organization, owner, role ->
      loginAsUser(owner)
      performAuthPut(
        "/v2/organizations/${organization.id}/users/${owner.id}/set-role",
        SetOrganizationRoleDto(OrganizationRoleType.MEMBER)
      ).andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_YOUR_OWN_ROLE)
    }
  }
}
