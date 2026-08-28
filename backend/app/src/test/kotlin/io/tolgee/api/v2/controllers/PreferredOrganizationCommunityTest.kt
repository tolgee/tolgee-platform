package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates

class PreferredOrganizationCommunityTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  private var originalUserCanCreateOrganizations by Delegates.notNull<Boolean>()

  @BeforeEach
  fun setup() {
    originalUserCanCreateOrganizations = tolgeeProperties.authentication.userCanCreateOrganizations
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    tolgeeProperties.authentication.userCanCreateOrganizations = originalUserCanCreateOrganizations
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `community user gets the reduced organization model`() {
    setPreferred(testData.nonMember, testData.otherOrg.id)
    userAccount = testData.nonMember
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(true)
      node("basePermissions").isNotNull
    }
  }

  @Test
  fun `member gets the full organization model`() {
    setPreferred(testData.otherOrgMember, testData.otherOrg.id)
    userAccount = testData.otherOrgMember
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo("MEMBER")
      node("limitedView").isEqualTo(false)
    }
  }

  @Test
  fun `direct project permission user gets the reduced model with no role but a non-limited view`() {
    setPreferred(testData.directPermissionUser, testData.otherOrg.id)
    userAccount = testData.directPermissionUser
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
    }
  }

  @Test
  fun `stale preference heals to a member organization`() {
    setPreferred(testData.user, testData.otherOrg.id)
    unpublishOtherOrgProject()

    userAccount = testData.user
    val ownOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization.id").isEqualTo(ownOrg.id)
      node("preferredOrganization.currentUserRole").isEqualTo("OWNER")
    }
    assertStoredPreference(testData.user.id, ownOrg.id)
  }

  @Test
  fun `stale preference with no other viewable organization heals to a created one`() {
    setPreferred(testData.nonMember, testData.otherOrg.id)
    unpublishOtherOrgProject()
    executeInNewTransaction {
      organizationService.delete(organizationService.get(testData.nonMemberPersonalOrg.id))
    }

    userAccount = testData.nonMember
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization.name").isEqualTo("Non Member")
      node("preferredOrganization.currentUserRole").isEqualTo("OWNER")
    }
    executeInNewTransaction {
      val preferred = userPreferencesService.find(testData.nonMember.id)!!.preferredOrganization!!
      assertThat(preferred.id).isNotEqualTo(testData.otherOrg.id)
      assertThat(preferred.name).isEqualTo("Non Member")
    }
  }

  @Test
  fun `org-less user has no preferred organization until it adopts one with public projects`() {
    refuseOrganizationCreation()
    userAccount = testData.orgLessCommunityUser

    performAuthGet("/v2/preferred-organization").andIsForbidden
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization").isEqualTo(null)
    }

    performAuthPut(
      "/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}",
      null,
    ).andIsOk

    assertStoredPreference(testData.orgLessCommunityUser.id, testData.otherOrg.id)
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(true)
    }
  }

  @Test
  fun `org-less user cannot adopt an organization without public projects`() {
    refuseOrganizationCreation()
    userAccount = testData.orgLessCommunityUser

    performAuthPut(
      "/v2/user-preferences/set-preferred-organization/${testData.noPublicOrg.id}",
      null,
    ).andIsForbidden
    performAuthGet("/v2/preferred-organization").andIsForbidden
    assertNoStoredPreference(testData.orgLessCommunityUser.id)
  }

  @Test
  fun `the implicit personal organization follows the same refusal rule as explicit creation`() {
    refuseOrganizationCreation()
    userAccount = testData.orgLessCommunityUser

    performAuthPost(
      "/v2/organizations",
      mapOf("name" to "Attempted organization"),
    ).andIsForbidden
    performAuthGet("/v2/preferred-organization").andIsForbidden
    assertNoStoredPreference(testData.orgLessCommunityUser.id)

    tolgeeProperties.authentication.userCanCreateOrganizations = true

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo("OWNER")
    }
  }

  @Test
  fun `an org-less user whose stored preference is null adopts an organization they are added to`() {
    userAccount = testData.orgLessCommunityUser
    refuseOrganizationCreation()

    performAuthGet("/v2/preferred-organization").andIsForbidden
    assertNoStoredPreference(testData.orgLessCommunityUser.id)

    executeInNewTransaction {
      organizationRoleService.grantRoleToUser(
        userAccountService.get(testData.orgLessCommunityUser.id),
        organizationService.get(testData.otherOrg.id),
        OrganizationRoleType.MEMBER,
      )
    }

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.otherOrg.id)
      node("currentUserRole").isEqualTo("MEMBER")
    }
    assertStoredPreference(testData.orgLessCommunityUser.id, testData.otherOrg.id)
  }

  @Test
  fun `an SSO user gets no implicit personal organization`() {
    tolgeeProperties.authentication.userCanCreateOrganizations = true
    val ssoUser = userEntity(testData.ssoOrgLessUser)

    assertThat(executeInNewTransaction { organizationService.findOrCreatePreferred(ssoUser) }).isNull()
  }

  @Test
  fun `a stale preference heals to nothing while organization creation is refused`() {
    setPreferred(testData.orgLessCommunityUser, testData.otherOrg.id)
    unpublishOtherOrgProject()
    refuseOrganizationCreation()
    userAccount = testData.orgLessCommunityUser

    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization").isEqualTo(null)
    }
    performAuthGet("/v2/preferred-organization").andIsForbidden
    assertNoStoredPreference(testData.orgLessCommunityUser.id)
  }

  @Test
  fun `the implicit personal organization keeps a short name and pads it`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "李明")
      }

    assertThat(organization.name).isEqualTo("李明 Organization")
    assertThat(organization.slug).isNotEmpty()
  }

  @Test
  fun `the implicit personal organization truncates a name the entity would reject`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "N".repeat(120))
      }

    assertThat(organization.name).isEqualTo("N".repeat(50))
  }

  @Test
  fun `the implicit personal organization never truncates onto a lone surrogate`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "N".repeat(49) + "\uD83D\uDE00".repeat(5))
      }

    assertThat(organization.name).isEqualTo("N".repeat(49))
    assertThat(organization.name.none { it.isSurrogate() }).isTrue()
  }

  @Test
  fun `the implicit personal organization keeps a surrogate pair that ends exactly at the limit`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "N".repeat(48) + "\uD83D\uDE00".repeat(5))
      }

    assertThat(organization.name).isEqualTo("N".repeat(48) + "\uD83D\uDE00")
    assertThat(organization.name.length).isEqualTo(50)
  }

  @Test
  fun `the implicit personal organization falls back to the username when there is no name`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "")
      }

    assertThat(organization.name).isEqualTo("org Organization")
  }

  @Test
  fun `the username fallback drops a surrogate pair whole rather than cutting it`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        withUsernameRestoredBeforeCommit(user, "ab\uD83D\uDE00@example.com") {
          organizationService.createPreferredWithoutAuthorization(user, "")
        }
      }

    assertThat(organization.name).isEqualTo("ab Organization")
  }

  @Test
  fun `the implicit personal organization keeps a name the DTO accepts`() {
    val organization =
      executeInNewTransaction {
        val user = userAccountService.get(testData.orgLessCommunityUser.id)
        organizationService.createPreferredWithoutAuthorization(user, "Joe")
      }

    assertThat(organization.name).isEqualTo("Joe")
  }

  @Test
  fun `a server admin still gets an implicit personal organization while creation is refused`() {
    refuseOrganizationCreation()

    val created =
      executeInNewTransaction {
        val admin = userAccountService.get(testData.ssoServerAdmin.id)
        organizationService.findOrCreatePreferred(admin)
      }

    assertThat(created).isNotNull
    assertThat(organizationService.findPreferred(testData.ssoServerAdmin.id)).isNotNull
  }

  @Test
  fun `server admin viewing an organization they hold no role in is not limited`() {
    setPreferred(testData.serverAdmin, testData.otherOrg.id)
    userAccount = testData.serverAdmin

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
    }
  }

  @Test
  fun `server supporter viewing an organization they hold no role in is not limited`() {
    setPreferred(testData.serverSupporter, testData.otherOrg.id)
    userAccount = testData.serverSupporter

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
    }
  }

  private fun userEntity(user: UserAccount): UserAccount = executeInNewTransaction { userAccountService.get(user.id) }

  private fun refuseOrganizationCreation() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
  }

  private fun setPreferred(
    user: UserAccount,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(organizationId),
        userAccountService.get(user.id),
      )
    }
  }

  private fun unpublishOtherOrgProject() {
    executeInNewTransaction {
      projectService.get(testData.otherOrgPublicProject.id).public = false
    }
  }

  /**
   * `cleanTestData` looks users up by the username their builder gave them, so a rename that
   * reaches the database would strand this user and everything created under it.
   */
  private fun <T> withUsernameRestoredBeforeCommit(
    user: UserAccount,
    username: String,
    body: () -> T,
  ): T {
    val original = user.username
    user.username = username
    try {
      return body()
    } finally {
      user.username = original
    }
  }

  private fun assertNoStoredPreference(userId: Long) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)?.preferredOrganization).isNull()
    }
  }

  private fun assertStoredPreference(
    userId: Long,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)!!.preferredOrganization!!.id).isEqualTo(organizationId)
    }
  }
}
