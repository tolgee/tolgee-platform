package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope

class CommunityGuestTestData : CommunityStatsTestData() {
  lateinit var deletedPublicProject: Project
  lateinit var softDeletedOrgPublicProject: Project
  lateinit var softDeletedBaseProject: Project
  lateinit var noBaseLanguageProject: Project

  val memberWithoutActivityScope: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "rita@community.test"
        name = "Rita Restricted"
      }.self

  val serverAdmin: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "admin@community.test"
        name = "Ada Admin"
        role = UserAccount.Role.ADMIN
      }.self

  val serverAdminApiKey: ApiKey =
    publicProjectBuilder
      .addApiKey {
        key = "server-admin-activity-api-key"
        scopesEnum = mutableSetOf(Scope.ACTIVITY_VIEW)
        userAccount = serverAdmin
      }.self

  init {
    publicProjectBuilder.addPermission {
      user = memberWithoutActivityScope
      type = null
      scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
    }
    publicKeys[0].addTranslation {
      language = publicProject.baseLanguage!!
      text = "Hello"
    }
    root.apply {
      val organization = userAccountBuilder.defaultOrganizationBuilder.self

      addProject(organizationOwner = organization) {
        name = "Deleted public project"
        public = true
      }.build {
        deletedPublicProject = self
        addBaseLanguage()
        setDeletedAt()
      }

      addProject(organizationOwner = organization) {
        name = "Soft-deleted base project"
        public = true
      }.build {
        softDeletedBaseProject = self
        addBaseLanguage().setDeletedAt()
      }

      addProject(organizationOwner = organization) {
        name = "No base language project"
        public = true
      }.build {
        noBaseLanguageProject = self
        addBaseLanguage()
        clearBaseLanguage()
      }

      val softDeletedOrg =
        addOrganization {
          name = "Soft deleted org"
        }.build {
          setDeletedAt()
        }.self

      addProject(organizationOwner = softDeletedOrg) {
        name = "Soft deleted org public project"
        public = true
      }.build {
        softDeletedOrgPublicProject = self
        addBaseLanguage()
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
