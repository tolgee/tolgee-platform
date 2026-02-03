package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.SuggestionBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationState

class SuggestionsTestData(
  suggestionsMode: SuggestionsMode = SuggestionsMode.DISABLED,
) : BaseTestData("suggestionsuggestionsTestUser", "Project with suggestions") {
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
  val czechTranslations: MutableList<TranslationBuilder> = mutableListOf()
  lateinit var pluralKey: KeyBuilder
  lateinit var pluralSuggestion: SuggestionBuilder
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
            addTranslation("en", "Translation $it").apply {
              self.state = TranslationState.REVIEWED
            }
            addTranslation("cs", "Překlad $it").apply {
              self.state = TranslationState.REVIEWED
              czechTranslations.add(this)
            }
          },
        )
      }

      keys[0].apply {
        czechSuggestions.add(
          addSuggestion {
            this.language = czechLanguage
            this.author = projectTranslator.self
            this.translation = "Navržený překlad 0-1"
          },
        )

        czechSuggestions.add(
          addSuggestion {
            this.language = czechLanguage
            this.author = projectReviewer.self
            this.translation = "Navržený překlad 0-2"
          },
        )

        englishSuggestions.add(
          addSuggestion {
            this.language = englishLanguage
            this.author = projectTranslator.self
            this.translation = "Suggested translation 0-1"
          },
        )

        englishSuggestions.add(
          addSuggestion {
            this.language = englishLanguage
            this.author = projectReviewer.self
            this.translation = "Suggested translation 0-2"
          },
        )
      }

      pluralKey =
        addKey(null, "pluralKey").apply {
          self.isPlural = true
          addTranslation("en", "{value, plural, one {# key} other {# keys}}")
          addTranslation("cs", "{value, plural, one {# klíč} few {# klíče} other {# klíčů}}")
        }

      pluralKey.apply {
        pluralSuggestion =
          addSuggestion {
            this.language = czechLanguage
            this.author = projectTranslator.self
            this.translation = "{value, plural, one {# překlad} few {# překlady} other {# překladů}}"
            this.isPlural = true
          }
      }
    }
  }

  fun disableTranslation() {
    val translation = czechTranslations[0].self

    translation.state = TranslationState.DISABLED
    translation.text = null
  }
}
