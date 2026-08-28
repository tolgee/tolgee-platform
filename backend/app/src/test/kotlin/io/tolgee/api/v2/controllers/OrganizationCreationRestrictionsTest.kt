package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.exceptions.PermissionException
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.properties.Delegates

class OrganizationCreationRestrictionsTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  private var originalUserCanCreateOrganizations by Delegates.notNull<Boolean>()
  private var originalSsoBypass: Boolean? = null

  @BeforeEach
  fun setup() {
    originalUserCanCreateOrganizations = tolgeeProperties.authentication.userCanCreateOrganizations
    originalSsoBypass = tolgeeProperties.internal.verifySsoAccountAvailableBypass
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    tolgeeProperties.authentication.userCanCreateOrganizations = originalUserCanCreateOrganizations
    tolgeeProperties.internal.verifySsoAccountAvailableBypass = originalSsoBypass
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an SSO user is refused organization creation even where the server allows it`() {
    tolgeeProperties.authentication.userCanCreateOrganizations = true
    val ssoUser = userEntity(testData.ssoOrgLessUser)

    assertThat(organizationService.canUserCreateOrganization(ssoUser)).isEqualTo(false)

    val refusal =
      assertThrows<PermissionException> { organizationService.checkUserCanCreateOrganization(ssoUser) }
    assertThat(refusal.tolgeeMessage).isEqualTo(Message.SSO_USER_CANNOT_CREATE_ORGANIZATION)
  }

  @Test
  fun `the server property refusal wins over the SSO refusal`() {
    refuseOrganizationCreation()
    val ssoUser = userEntity(testData.ssoOrgLessUser)

    val refusal =
      assertThrows<PermissionException> { organizationService.checkUserCanCreateOrganization(ssoUser) }
    assertThat(refusal.tolgeeMessage).isEqualTo(Message.OPERATION_NOT_PERMITTED)
  }

  @Test
  fun `a server admin creates organizations even while creation is refused to everyone else`() {
    refuseOrganizationCreation()
    val admin = userEntity(testData.serverAdmin)

    assertThat(organizationService.canUserCreateOrganization(admin)).isEqualTo(true)

    userAccount = testData.serverAdmin
    performAuthPost(
      "/v2/organizations",
      mapOf("name" to ADMIN_CREATED_ORGANIZATION),
    ).andIsCreated
  }

  @Test
  fun `the create endpoint refuses an SSO user`() {
    tolgeeProperties.internal.verifySsoAccountAvailableBypass = true
    userAccount = testData.ssoOrgLessUser

    performAuthPost(
      "/v2/organizations",
      mapOf("name" to "Attempted organization"),
    ).andIsForbidden.andHasErrorMessage(Message.SSO_USER_CANNOT_CREATE_ORGANIZATION)

    assertThat(organizationService.findPreferred(testData.ssoOrgLessUser.id)).isNull()
  }

  @Test
  fun `server-wide SSO restricts authentication only, so its users create organizations like anyone else`() {
    assertThat(
      organizationService.canUserCreateOrganization(userEntity(testData.ssoGlobalOrgLessUser)),
    ).isEqualTo(true)
  }

  @Test
  fun `a server admin creates organizations even through SSO`() {
    refuseOrganizationCreation()

    assertThat(organizationService.canUserCreateOrganization(userEntity(testData.ssoServerAdmin))).isEqualTo(true)
  }

  @Test
  fun `a server supporter is refused organization creation like everyone else`() {
    refuseOrganizationCreation()
    val supporter = userEntity(testData.serverSupporter)

    assertThat(organizationService.canUserCreateOrganization(supporter)).isEqualTo(false)

    userAccount = testData.serverSupporter
    performAuthPost(
      "/v2/organizations",
      mapOf("name" to "Supporter organization"),
    ).andIsForbidden
  }

  private fun userEntity(user: UserAccount): UserAccount = executeInNewTransaction { userAccountService.get(user.id) }

  private fun refuseOrganizationCreation() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
  }

  companion object {
    private const val ADMIN_CREATED_ORGANIZATION = "Admin organization"
  }
}
