package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrganizationCommunityAccessTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  @BeforeEach
  fun setup() {
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `community user gets basic info of an organization with a public project by id`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.otherOrg.id}").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("basePermissions").isNotNull
    }
  }

  @Test
  fun `community user gets basic info of an organization with a public project by slug`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.otherOrg.slug}").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
    }
  }

  @Test
  fun `community user cannot see an organization without public projects`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.noPublicOrg.id}").andIsNotFound
    performAuthGet("/v2/organizations/${testData.noPublicOrg.slug}").andIsNotFound
  }

  @Test
  fun `community user cannot see an organization whose only public project is not publicly visible`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.noBaseLangOnlyOrg.id}").andIsNotFound
  }

  @Test
  fun `anonymous cannot see any organization`() {
    performGet("/v2/organizations/${testData.otherOrg.id}").andIsForbidden
    performGet("/v2/organizations/${testData.otherOrg.slug}").andIsForbidden
    performGet("/v2/organizations/${testData.noPublicOrg.id}").andIsForbidden
  }

  @Test
  fun `community access does not open write methods on the same path`() {
    userAccount = testData.nonMember
    performAuthPut(
      "/v2/organizations/${testData.otherOrg.id}",
      OrganizationDto(name = "Hijacked", slug = testData.otherOrg.slug),
    ).andIsForbidden
  }

  @Test
  fun `community access does not open member endpoints`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/users").andIsForbidden
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/usage").andIsForbidden
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/invitations").andIsForbidden
  }
}
