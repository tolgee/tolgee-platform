package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class BaseOrganizationControllerTest : AuthorizedControllerTest() {
  protected lateinit var dummyDto: OrganizationDto
  protected lateinit var dummyDto2: OrganizationDto

  @Autowired
  open lateinit var organizationRoleRepository: OrganizationRoleRepository

  @BeforeEach
  fun setup() {
    resetDto()
  }

  private fun resetDto() {
    dummyDto =
      OrganizationDto(
        "Test org",
        "This is description",
        "test-org",
      )

    dummyDto2 =
      OrganizationDto(
        "Test org 2",
        "This is description 2",
        "test-org-2",
      )
  }

  protected fun withOwnerInOrganization(
    fn: (organization: Organization, owner: UserAccount, ownerRole: OrganizationRole) -> Unit,
  ) {
    executeInNewTransaction { this.organizationService.create(dummyDto, userAccount!!) }
      .let { organization ->
        dbPopulator.createUserIfNotExists("superuser").let { createdUser ->
          OrganizationRole(
            user = createdUser,
            organization = organization,
            type = OrganizationRoleType.OWNER,
          ).let { createdOwnerRole ->
            organizationRoleRepository.save(createdOwnerRole)
            fn(organization, createdUser, createdOwnerRole)
          }
        }
      }
  }
}
