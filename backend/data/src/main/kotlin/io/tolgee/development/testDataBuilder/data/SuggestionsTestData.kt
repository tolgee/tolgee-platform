package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.SuggestionBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.SuggestionsMode
import kotlin.collections.forEach

class SuggestionsTestData(suggestionsMode: SuggestionsMode = SuggestionsMode.DISABLED) :
  BaseTestData("suggestionsTestUser", "Project with suggestions") {
  var projectReviewer: UserAccountBuilder
  var orgAdmin: UserAccountBuilder
  var orgMember: UserAccountBuilder
  var projectTranslator: UserAccountBuilder
  var czechTranslator: UserAccountBuilder
  var czechReviewer: UserAccountBuilder
  var relatedProject: ProjectBuilder
  var keys: MutableList<KeyBuilder> = mutableListOf()
  val czechSuggestions: MutableList<SuggestionBuilder> = mutableListOf()
  val englishSuggestions: MutableList<SuggestionBuilder> = mutableListOf()
  lateinit var czechLanguage: Language

  init {
    user.name = "Tasks test user"
    projectReviewer =
      root.addUserAccount {
        username = "reviewer@test.com"
        name = "Project reviewer"
      }

    orgMember =
      root.addUserAccount {
        username = "organization.member@test.com"
        name = "Organization member"
      }

    orgAdmin =
      root.addUserAccount {
        username = "organization.owner@test.com"
        name = "Organization owner"
      }

    projectTranslator =
      root.addUserAccount {
        username = "translator@test.com"
        name = "Project translator"
      }

    czechTranslator =
      root.addUserAccount {
        username = "cs.translator@test.com"
        name = "Czech translator"
      }

    czechReviewer =
      root.addUserAccount {
        username = "cs.reviewer@test.com"
        name = "Czech reviewer"
      }

    userAccountBuilder.defaultOrganizationBuilder.apply {
      addRole {
        user = orgMember.self
        type = OrganizationRoleType.MEMBER
      }

      addRole {
        user = orgAdmin.self
        type = OrganizationRoleType.OWNER
      }
    }

    projectBuilder.apply {
      relatedProject = this

      this.self.suggestionsMode = suggestionsMode

      addLanguage {
        name = "Czech"
        tag = "cs"
        originalName = "Čeština"
        czechLanguage = this
      }

      addPermission {
        user = projectReviewer.self
        type = ProjectPermissionType.REVIEW
      }

      addPermission {
        user = projectTranslator.self
        type = ProjectPermissionType.TRANSLATE
      }

      addPermission {
        user = czechTranslator.self
        type = ProjectPermissionType.TRANSLATE
        translateLanguages = mutableSetOf(czechLanguage)
        suggestLanguages = mutableSetOf(czechLanguage)
      }

      addPermission {
        user = czechReviewer.self
        type = ProjectPermissionType.REVIEW
        translateLanguages = mutableSetOf(czechLanguage)
        suggestLanguages = mutableSetOf(czechLanguage)
        stateChangeLanguages = mutableSetOf(czechLanguage)
      }

      (0 until 4).forEach {
        keys.add(
          addKey(null, "key $it").apply {
            addTranslation("en", "Translation $it")
            addTranslation("cs", "Překlad $it")
          },
        )
      }

      keys[0].apply {
        czechSuggestions.add(
          addSuggestion {
            this.language = czechLanguage
            this.author = projectTranslator.self
            this.translation = "Navržený překlad 0-1"
          }
        )

        czechSuggestions.add(
          addSuggestion {
            this.language = czechLanguage
            this.author = projectReviewer.self
            this.translation = "Navržený překlad 0-2"
          }
        )

        englishSuggestions.add(
          addSuggestion {
            this.language = englishLanguage
            this.author = projectTranslator.self
            this.translation = "Suggested translation 0-1"
          }
        )

        englishSuggestions.add(
          addSuggestion {
            this.language = englishLanguage
            this.author = projectReviewer.self
            this.translation = "Suggested translation 0-2"
          }
        )
      }
    }
  }
}
