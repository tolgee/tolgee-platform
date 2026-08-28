package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserPreferencesSetPreferredOrganizationTest : AuthorizedControllerTest() {
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
  fun `non-member sets preferred organization owning a public project`() {
    userAccount = testData.nonMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null).andIsOk
    assertPreferredOrganization(testData.nonMember.id, testData.otherOrg.id)
  }

  @Test
  fun `the write answers with the organization it stored, so no follow-up read is needed`() {
    userAccount = testData.nonMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null)
      .andIsOk
      .andAssertThatJson {
        node("id").isEqualTo(testData.otherOrg.id)
        node("name").isEqualTo("Vibrant translators")
        node("currentUserRole").isEqualTo(null)
        node("limitedView").isEqualTo(true)
      }
  }

  @Test
  fun `the write answers with the full model for a member`() {
    userAccount = testData.otherOrgMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null)
      .andIsOk
      .andAssertThatJson {
        node("currentUserRole").isEqualTo("MEMBER")
        node("limitedView").isEqualTo(false)
      }
  }

  @Test
  fun `non-member cannot set preferred organization without public projects`() {
    userAccount = testData.nonMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.noPublicOrg.id}", null)
      .andIsForbidden
  }

  @Test
  fun `non-member cannot set preferred organization whose only public project is not publicly visible`() {
    userAccount = testData.nonMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.noBaseLangOnlyOrg.id}", null)
      .andIsForbidden
  }

  @Test
  fun `member still sets preferred organization`() {
    userAccount = testData.otherOrgMember
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null).andIsOk
    assertPreferredOrganization(testData.otherOrgMember.id, testData.otherOrg.id)
  }

  @Test
  fun `direct project permission user still sets preferred organization`() {
    userAccount = testData.directPermissionUser
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null).andIsOk
    assertPreferredOrganization(testData.directPermissionUser.id, testData.otherOrg.id)
  }

  @Test
  fun `server admin sets preferred organization without public projects`() {
    userAccount = testData.serverAdmin
    performAuthPut("/v2/user-preferences/set-preferred-organization/${testData.noPublicOrg.id}", null).andIsOk
    assertPreferredOrganization(testData.serverAdmin.id, testData.noPublicOrg.id)
  }

  @Test
  fun `anonymous cannot set preferred organization`() {
    performPut("/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}", null)
      .andIsForbidden
  }

  private fun assertPreferredOrganization(
    userId: Long,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)!!.preferredOrganization!!.id).isEqualTo(organizationId)
    }
  }
}
