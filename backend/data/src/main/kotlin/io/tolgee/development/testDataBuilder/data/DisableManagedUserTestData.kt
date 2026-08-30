package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.UserDisabledBy
import java.util.Date

class DisableManagedUserTestData : BaseTestData() {
  companion object {
    private val DISABLED_AT: Date = Date(1700000000000)
  }

  val organization: Organization get() = userAccountBuilder.defaultOrganizationBuilder.self
  val owner: UserAccount get() = user

  lateinit var managedMember: UserAccount
  lateinit var managedMemberPat: Pat
  lateinit var nonManagedMember: UserAccount
  lateinit var disabledNonManagedMember: UserAccount
  lateinit var adminDisabledManagedMember: UserAccount
  lateinit var orgDisabledManagedMember: UserAccount
  lateinit var nullOriginDisabledManagedMember: UserAccount
  lateinit var managedPlatformAdmin: UserAccount
  lateinit var managedPlatformSupporter: UserAccount
  lateinit var orgDisabledManagedPlatformAdmin: UserAccount
  lateinit var outsidePlatformAdmin: UserAccount
  lateinit var projectOnlyMember: UserAccount
  lateinit var multiProjectMember: UserAccount

  lateinit var managedByOtherOrg: UserAccount
  lateinit var disabledByOtherOrgPlainMember: UserAccount

  init {
    root.apply {
      addUserAccountWithoutOrganization {
        username = "managed@acting.org"
        name = "Managed Member"
        managedMember = this
      }.build {
        addPat {
          description = "kill-switch"
          managedMemberPat = this
        }
      }
      addUserAccountWithoutOrganization {
        username = "member@acting.org"
        name = "Non Managed Member"
        nonManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "disabled@acting.org"
        name = "Disabled Member"
        disabledAt = DISABLED_AT
        disabledNonManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "byadmin@acting.org"
        name = "Admin Disabled Managed Member"
        disabledAt = DISABLED_AT
        disabledBy = UserDisabledBy.ADMIN
        adminDisabledManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "byorg@acting.org"
        name = "Org Disabled Managed Member"
        disabledAt = DISABLED_AT
        disabledBy = UserDisabledBy.ORGANIZATION
        orgDisabledManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "byunknown@acting.org"
        name = "Unknown Origin Disabled Managed Member"
        disabledAt = DISABLED_AT
        nullOriginDisabledManagedMember = this
      }
      addUserAccountWithoutOrganization {
        username = "platformadmin@acting.org"
        name = "Platform Admin Managed Member"
        role = UserAccount.Role.ADMIN
        managedPlatformAdmin = this
      }
      addUserAccountWithoutOrganization {
        username = "platformsupporter@acting.org"
        name = "Platform Supporter Managed Member"
        role = UserAccount.Role.SUPPORTER
        managedPlatformSupporter = this
      }
      addUserAccountWithoutOrganization {
        username = "legacyadmin@acting.org"
        name = "Legacy Org Disabled Platform Admin"
        role = UserAccount.Role.ADMIN
        disabledAt = DISABLED_AT
        disabledBy = UserDisabledBy.ORGANIZATION
        orgDisabledManagedPlatformAdmin = this
      }
      addUserAccountWithoutOrganization {
        username = "outsideadmin@tolgee.io"
        name = "Outside Platform Admin"
        role = UserAccount.Role.ADMIN
        outsidePlatformAdmin = this
      }
      addUserAccountWithoutOrganization {
        username = "managed@other.org"
        name = "Managed By Other Org"
        managedByOtherOrg = this
      }
      addUserAccountWithoutOrganization {
        username = "disabledbyother@other.org"
        name = "Disabled By Other Org"
        disabledAt = DISABLED_AT
        disabledBy = UserDisabledBy.ORGANIZATION
        disabledByOtherOrgPlainMember = this
      }
      addUserAccountWithoutOrganization {
        username = "projectonly@acting.org"
        name = "Project Only Member"
        projectOnlyMember = this
      }
      addUserAccountWithoutOrganization {
        username = "multiproject@acting.org"
        name = "Multi Project Member"
        multiProjectMember = this
      }

      userAccountBuilder.defaultOrganizationBuilder.build {
        addRole {
          user = managedMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = nonManagedMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = disabledNonManagedMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = adminDisabledManagedMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = orgDisabledManagedMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = nullOriginDisabledManagedMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = multiProjectMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = managedPlatformAdmin
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = managedPlatformSupporter
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = orgDisabledManagedPlatformAdmin
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = disabledByOtherOrgPlainMember
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          user = managedByOtherOrg
          type = OrganizationRoleType.MEMBER
        }
      }

      projectBuilder.build {
        addPermission {
          user = projectOnlyMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = multiProjectMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = disabledNonManagedMember
          type = ProjectPermissionType.VIEW
        }
        addPermission {
          user = managedMember
          type = ProjectPermissionType.VIEW
        }
      }

      addProject {
        name = "second_project"
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
      }.build {
        addPermission {
          user = multiProjectMember
          type = ProjectPermissionType.VIEW
        }
      }

      addOrganization {
        name = "Other Org"
      }.build {
        addRole {
          user = managedByOtherOrg
          type = OrganizationRoleType.MEMBER
          managed = true
        }
        addRole {
          user = disabledByOtherOrgPlainMember
          type = OrganizationRoleType.MEMBER
          managed = true
        }
      }
    }
  }
}
