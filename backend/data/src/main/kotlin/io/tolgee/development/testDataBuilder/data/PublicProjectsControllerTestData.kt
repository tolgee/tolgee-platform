package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

class PublicProjectsControllerTestData : BaseTestData() {
  val privateProject: Project get() = project

  lateinit var publicProject: Project
  lateinit var otherOrg: Organization
  lateinit var otherOrgPublicProject: Project
  lateinit var nonMember: UserAccount

  // holds a DIRECT project permission on otherOrgPublicProject without belonging to its org
  lateinit var directPermissionUser: UserAccount

  lateinit var noBaseLanguageProject: Project
  lateinit var softDeletedBaseProject: Project
  lateinit var orgLessProject: Project
  lateinit var deletedPublicProject: Project

  init {
    root.apply {
      nonMember =
        addUserAccount {
          username = "non_member"
          name = "Non Member"
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

      otherOrg =
        addOrganization {
          name = "Vibrant translators"
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

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "No base language project"
        public = true
      }.build {
        noBaseLanguageProject = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Soft-deleted base project"
        public = true
      }.build {
        softDeletedBaseProject = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Org-less project"
        public = true
      }.build {
        orgLessProject = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Deleted public project"
        public = true
      }.build {
        deletedPublicProject = self
        addBaseLanguage()
      }
    }
  }

  private fun ProjectBuilder.addBaseLanguage() {
    addLanguage {
      name = "English"
      tag = "en"
      originalName = "English"
      this@addBaseLanguage.self.baseLanguage = this
    }
  }
}
