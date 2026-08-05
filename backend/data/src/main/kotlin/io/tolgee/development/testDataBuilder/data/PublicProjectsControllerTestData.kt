package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class PublicProjectsControllerTestData : BaseTestData() {
  val privateProject: Project get() = project

  lateinit var publicProject: Project
  lateinit var otherOrg: Organization
  lateinit var otherOrgPublicProject: Project
  lateinit var nonMember: UserAccount

  lateinit var directPermissionUser: UserAccount

  lateinit var noBaseLanguageProject: Project
  lateinit var softDeletedBaseProject: Project
  lateinit var orgLessProject: Project
  lateinit var deletedPublicProject: Project

  lateinit var nonMemberPersonalOrg: Organization
  lateinit var otherOrgMember: UserAccount
  lateinit var otherOrgOwner: UserAccount
  lateinit var storedGuest: UserAccount
  lateinit var otherOrgPrivateProject: Project
  lateinit var serverAdmin: UserAccount

  lateinit var guestWithPermission: UserAccount
  lateinit var granularPermissionUser: UserAccount
  lateinit var noneOnlyUser: UserAccount
  lateinit var revokedOnlyUser: UserAccount

  lateinit var noPublicOrg: Organization
  lateinit var noPublicOrgMember: UserAccount
  lateinit var noPublicOrgOwner: UserAccount
  lateinit var privateOrgMember: UserAccount

  lateinit var noBaseLangOnlyOrg: Organization
  lateinit var noBaseLangOnlyOrgProject: Project
  lateinit var softDeletedBaseLangOnlyOrg: Organization
  lateinit var softDeletedBaseLangOnlyOrgProject: Project
  lateinit var deletedProjectOnlyOrg: Organization
  lateinit var deletedProjectOnlyOrgProject: Project
  lateinit var softDeletedOrg: Organization
  lateinit var softDeletedOrgPublicProject: Project
  lateinit var softDeletedOrgMember: UserAccount
  lateinit var orgLessCommunityUser: UserAccount

  init {
    root.apply {
      val nonMemberBuilder =
        addUserAccount {
          username = "non_member"
          name = "Non Member"
        }
      nonMember = nonMemberBuilder.self
      nonMemberPersonalOrg = nonMemberBuilder.defaultOrganizationBuilder.self

      orgLessCommunityUser =
        addUserAccountWithoutOrganization {
          username = "org_less_community_user"
          name = "Org Less Community User"
        }.self

      directPermissionUser =
        addUserAccount {
          username = "direct_perm_user"
          name = "Direct Perm User"
        }.self

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Public project"
        public = true
      }.build {
        publicProject = self
        addBaseLanguage()
      }

      otherOrgMember =
        addUserAccount {
          username = "other_org_member"
          name = "Other Org Member"
        }.self

      val storedGuestBuilder =
        addUserAccount {
          username = "stored_guest"
          name = "Stored Guest"
        }
      storedGuest = storedGuestBuilder.self

      otherOrgOwner =
        addUserAccount {
          username = "other_org_owner"
          name = "Other Org Owner"
        }.self

      guestWithPermission =
        addUserAccount {
          username = "guest_with_permission"
          name = "Guest With Permission"
        }.self

      granularPermissionUser =
        addUserAccount {
          username = "granular_perm_user"
          name = "Granular Perm User"
        }.self

      noneOnlyUser =
        addUserAccount {
          username = "none_only_user"
          name = "None Only User"
        }.self

      serverAdmin =
        addUserAccount {
          username = "server_admin"
          name = "Server Admin"
          role = UserAccount.Role.ADMIN
        }.self

      otherOrg =
        addOrganization {
          name = "Vibrant translators"
        }.build {
          addRole {
            user = this@PublicProjectsControllerTestData.otherOrgMember
            type = OrganizationRoleType.MEMBER
          }
          addRole {
            user = this@PublicProjectsControllerTestData.otherOrgOwner
            type = OrganizationRoleType.OWNER
          }
        }.self

      addProject(organizationOwner = otherOrg) {
        name = "Other org public project"
        public = true
      }.build {
        otherOrgPublicProject = self
        addBaseLanguage()
        addPermission {
          user = this@PublicProjectsControllerTestData.directPermissionUser
          type = ProjectPermissionType.TRANSLATE
        }
      }

      addProject(organizationOwner = otherOrg) {
        name = "Other org private project"
      }.build {
        otherOrgPrivateProject = self
        val privateProjectSelf = self
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
          privateProjectSelf.baseLanguage = this
        }
        addPermission {
          user = this@PublicProjectsControllerTestData.guestWithPermission
          type = ProjectPermissionType.TRANSLATE
        }
        addPermission {
          user = this@PublicProjectsControllerTestData.granularPermissionUser
          scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
        }
        addPermission {
          user = this@PublicProjectsControllerTestData.noneOnlyUser
          type = ProjectPermissionType.NONE
        }
      }

      noPublicOrgMember =
        addUserAccount {
          username = "no_public_org_member"
          name = "No Public Org Member"
        }.self

      revokedOnlyUser =
        addUserAccount {
          username = "revoked_only_user"
          name = "Revoked Only User"
        }.self

      noPublicOrgOwner =
        addUserAccount {
          username = "no_public_org_owner"
          name = "No Public Org Owner"
        }.self

      privateOrgMember =
        addUserAccount {
          username = "private_org_member"
          name = "Private Org Member"
        }.self

      noPublicOrg =
        addOrganization {
          name = "Members only outfit"
        }.build {
          addRole {
            user = this@PublicProjectsControllerTestData.noPublicOrgMember
            type = OrganizationRoleType.MEMBER
          }
          addRole {
            user = this@PublicProjectsControllerTestData.noPublicOrgOwner
            type = OrganizationRoleType.OWNER
          }
          addRole {
            user = this@PublicProjectsControllerTestData.privateOrgMember
            type = OrganizationRoleType.MEMBER
          }
        }.self

      addProject(organizationOwner = noPublicOrg) {
        name = "Members only private project"
      }.build {
        addBaseLanguage()
        addPermission {
          user = this@PublicProjectsControllerTestData.revokedOnlyUser
          type = ProjectPermissionType.NONE
        }
      }

      noBaseLangOnlyOrg =
        addOrganization {
          name = "No base lang only org"
        }.self

      addProject(organizationOwner = noBaseLangOnlyOrg) {
        name = "No base lang only org project"
        public = true
      }.build {
        noBaseLangOnlyOrgProject = self
        addBaseLanguage()
        clearBaseLanguage()
      }

      softDeletedBaseLangOnlyOrg =
        addOrganization {
          name = "Soft deleted base lang only org"
        }.self

      addProject(organizationOwner = softDeletedBaseLangOnlyOrg) {
        name = "Soft deleted base lang only org project"
        public = true
      }.build {
        softDeletedBaseLangOnlyOrgProject = self
        addBaseLanguage().setDeletedAt()
      }

      deletedProjectOnlyOrg =
        addOrganization {
          name = "Deleted project only org"
        }.self

      addProject(organizationOwner = deletedProjectOnlyOrg) {
        name = "Deleted project only org project"
        public = true
      }.build {
        deletedProjectOnlyOrgProject = self
        addBaseLanguage()
        setDeletedAt()
      }

      softDeletedOrgMember =
        addUserAccount {
          username = "soft_deleted_org_member"
          name = "Soft Deleted Org Member"
        }.self

      softDeletedOrg =
        addOrganization {
          name = "Soft deleted org"
        }.build {
          addRole {
            user = this@PublicProjectsControllerTestData.softDeletedOrgMember
            type = OrganizationRoleType.MEMBER
          }
          setDeletedAt()
        }.self

      addProject(organizationOwner = softDeletedOrg) {
        name = "Soft deleted org public project"
        public = true
      }.build {
        softDeletedOrgPublicProject = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "No base language project"
        public = true
      }.build {
        noBaseLanguageProject = self
        addBaseLanguage()
        clearBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Soft-deleted base project"
        public = true
      }.build {
        softDeletedBaseProject = self
        addBaseLanguage().setDeletedAt()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Org-less project"
        public = true
      }.build {
        orgLessProject = self
        addBaseLanguage()
        clearOrganizationOwner()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Deleted public project"
        public = true
      }.build {
        deletedPublicProject = self
        addBaseLanguage()
        setDeletedAt()
      }
    }
  }

  private fun ProjectBuilder.addBaseLanguage(): LanguageBuilder {
    return addLanguage {
      name = "English"
      tag = "en"
      originalName = "English"
      this@addBaseLanguage.self.baseLanguage = this
    }
  }
}
