package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.ApiKey
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import java.util.UUID

class LanguagePermissionsTestData(
  val projectSuggestionsMode: SuggestionsMode = SuggestionsMode.DISABLED,
  val projectTranslationProtection: TranslationProtection = TranslationProtection.NONE,
) {
  lateinit var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language

  lateinit var bothLangsExplicitUserApiKey: ApiKey

  lateinit var englishTranslation: Translation
  lateinit var germanTranslation: Translation
  lateinit var projectBuilder: ProjectBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      addProject()
    }

  val reviewEnOnlyUser =
    addUserAccountWithPermissions {
      translateLanguages = mutableSetOf(englishLanguage)
      type = ProjectPermissionType.REVIEW
    }

  val translateEnOnlyUser =
    addUserAccountWithPermissions {
      translateLanguages = mutableSetOf(englishLanguage)
      type = ProjectPermissionType.TRANSLATE
    }

  val translateAllUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.TRANSLATE
    }

  val reviewAllUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.REVIEW
    }

  val translateAllExplicitUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(englishLanguage, germanLanguage)
    }

  val reviewUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.REVIEW
      translateLanguages = mutableSetOf(englishLanguage)
      viewLanguages = mutableSetOf(englishLanguage)
      stateChangeLanguages = mutableSetOf(englishLanguage)
    }

  val translateUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(englishLanguage)
      viewLanguages = mutableSetOf(englishLanguage)
      stateChangeLanguages = mutableSetOf()
    }

  val viewEnOnlyUser =
    addUserAccountWithPermissions {
      type = ProjectPermissionType.VIEW
      translateLanguages = mutableSetOf()
      viewLanguages = mutableSetOf(englishLanguage)
      stateChangeLanguages = mutableSetOf()
    }

  val viewScopeUser =
    addUserAccountWithPermissions {
      scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
      type = null
      translateLanguages = mutableSetOf()
      viewLanguages = mutableSetOf(englishLanguage)
      stateChangeLanguages = mutableSetOf()
    }

  val editScopeUser =
    addUserAccountWithPermissions {
      scopes = arrayOf(Scope.TRANSLATIONS_EDIT)
      type = null
      translateLanguages = mutableSetOf(englishLanguage)
    }

  val stateChangeScopeUser =
    addUserAccountWithPermissions {
      scopes = arrayOf(Scope.TRANSLATIONS_STATE_EDIT, Scope.TRANSLATIONS_EDIT)
      type = null
      stateChangeLanguages = mutableSetOf(englishLanguage)
    }

  val stateChangeScopeUserEnForAll =
    addUserAccountWithPermissions {
      scopes = arrayOf(Scope.TRANSLATIONS_STATE_EDIT, Scope.TRANSLATIONS_EDIT)
      type = null
      translateLanguages = mutableSetOf(englishLanguage)
      viewLanguages = mutableSetOf(englishLanguage)
      stateChangeLanguages = mutableSetOf(englishLanguage)
    }

  init {
    projectBuilder.apply {
      addApiKey {
        userAccount = translateAllExplicitUser
        scopesEnum = mutableSetOf(Scope.TRANSLATIONS_EDIT)
        bothLangsExplicitUserApiKey = this
      }
    }
  }

  private fun TestDataBuilder.addProject() {
    val organization =
      addOrganization {
        name = "Org"
      }

    addProject {
      project = this
      name = "Project"
      organizationOwner = organization.self
      suggestionsMode = projectSuggestionsMode
      translationProtection = projectTranslationProtection
    }.build {
      projectBuilder = this
      englishLanguage = addEnglish().self
      germanLanguage = addGerman().self

      self.baseLanguage = englishLanguage

      addKey {
        name = "key"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "english_translation"
          englishTranslation = this
        }
        addTranslation {
          language = germanLanguage
          text = "german_translation"
          germanTranslation = this
        }
      }
      addKey {
        name = "key2"
      }
      addKey {
        name = "reviewedKey"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "english_reviewed_translation"
          englishTranslation = this
          state = TranslationState.REVIEWED
        }
        addTranslation {
          language = germanLanguage
          text = "german_reviewed_translation"
          germanTranslation = this
          state = TranslationState.REVIEWED
        }
      }
    }
  }

  private fun addUserAccountWithPermissions(setPerms: Permission.() -> Unit): UserAccount {
    val userBuilder =
      this.root.addUserAccount {
        this@addUserAccount.username = UUID.randomUUID().toString()
      }

    projectBuilder.addPermission {
      user = userBuilder.self
      setPerms(this)
    }

    return userBuilder.self
  }
}
