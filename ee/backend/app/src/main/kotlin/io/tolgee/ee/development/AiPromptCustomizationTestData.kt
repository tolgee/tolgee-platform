package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope

class AiPromptCustomizationTestData : BaseTestData() {
  val czech =
    this.projectBuilder.addCzech().self.also {
      it.aiTranslatorPromptDescription = "Czech description"
    }

  val french =
    this.projectBuilder.addFrench().self.also {
      it.aiTranslatorPromptDescription = "French description"
    }

  val ownerPat: Pat = userAccountBuilder.addPat { description = "owner" }.self

  private val memberBuilder = root.addUserAccount { username = "member" }
  val member = memberBuilder.self
  val memberPat: Pat = memberBuilder.addPat { description = "member" }.self

  private val outsiderBuilder = root.addUserAccount { username = "outsider" }
  val outsiderPat: Pat = outsiderBuilder.addPat { description = "outsider" }.self

  val editApiKey = addApiKey("edit_api_key", user, Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT)
  val projectEditApiKey = addApiKey("project_edit_api_key", user, Scope.PROJECT_EDIT)
  val translationsViewApiKey = addApiKey("translations_view_api_key", user, Scope.TRANSLATIONS_VIEW)
  val memberApiKey = addApiKey("member_api_key", member, Scope.PROJECT_EDIT, Scope.TRANSLATIONS_VIEW)

  val otherProjectBuilder: ProjectBuilder =
    root.addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
      name = "other_project"
      aiTranslatorPromptDescription = "Other project description"
    }

  val otherProjectGerman =
    otherProjectBuilder.addGerman().self.also {
      it.aiTranslatorPromptDescription = "Other project German description"
    }

  init {
    this.projectBuilder.self.aiTranslatorPromptDescription = "Project description"

    this.projectBuilder.addPermission {
      user = member
      type = null
      scopes = arrayOf(Scope.PROJECT_EDIT, Scope.LANGUAGES_EDIT)
    }

    otherProjectBuilder.build {
      val english = addEnglish().self
      self.baseLanguage = english
    }
  }

  private fun addApiKey(
    key: String,
    owner: UserAccount,
    vararg scopes: Scope,
  ): ApiKey =
    projectBuilder
      .addApiKey {
        this.key = key
        scopesEnum = mutableSetOf(*scopes)
        userAccount = owner
      }.self
}
