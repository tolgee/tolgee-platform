package io.tolgee.service.security

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.context.ApplicationContext

/**
 * When a language is deleted, we have to adjust permissions,
 * so the user doesn't gain access to all languaes when last
 * permitted language is deleted
 */
class LanguageDeletedPermissionUpdater(
  private val applicationContext: ApplicationContext,
  private val permission: Permission,
  private val language: Language,
) {
  operator fun invoke() {
    if (permission.scopes.isNotEmpty()) {
      handleStateChangeLanguagesGranular()
      handleTranslateLanguagesGranular()
      handleViewLanguagesGranular()
    }

    if (permission.type != null) {
      handleStateChangeLanguagesType()
      handleTranslateLanguagesType()
      handleViewLanguagesType()
    }

    removeLangFromLanguages()
    saveOrDelete()
  }

  private fun removeLangFromLanguages() {
    arrayOf(
      permission.translateLanguages,
      permission.viewLanguages,
      permission.stateChangeLanguages,
    ).forEach { languages ->
      languages.removeIf { it.id == language.id }
    }
  }

  private fun saveOrDelete() {
    if (permission.type == null && permission.scopes.isEmpty()) {
      permissionService.delete(permission)
      return
    }
    permissionService.save(permission)
  }

  private fun handleViewLanguagesType() {
    if (shouldLowerPermissions(permission.viewLanguages, ProjectPermissionType.VIEW)) {
      permission.type = ProjectPermissionType.NONE
    }
  }

  private fun handleViewLanguagesGranular() {
    if (shouldLowerPermissions(permission.viewLanguages, Scope.TRANSLATIONS_VIEW)) {
      permission.scopes = scopesWithout(Scope.TRANSLATIONS_VIEW)
    }
  }

  private fun handleTranslateLanguagesType() {
    if (shouldLowerPermissions(permission.translateLanguages, ProjectPermissionType.TRANSLATE)) {
      permission.type = ProjectPermissionType.VIEW
    }
  }

  private fun handleTranslateLanguagesGranular() {
    if (shouldLowerPermissions(permission.translateLanguages, Scope.TRANSLATIONS_EDIT)) {
      permission.scopes = scopesWithout(Scope.TRANSLATIONS_EDIT)
    }
  }

  private fun handleStateChangeLanguagesType() {
    if (shouldLowerPermissions(permission.stateChangeLanguages, ProjectPermissionType.REVIEW)) {
      permission.type = ProjectPermissionType.TRANSLATE
    }
  }

  private fun handleStateChangeLanguagesGranular() {
    if (shouldLowerPermissions(permission.stateChangeLanguages, Scope.TRANSLATIONS_STATE_EDIT)) {
      permission.scopes = scopesWithout(Scope.TRANSLATIONS_STATE_EDIT)
    }
  }

  private fun shouldLowerPermissions(
    languages: MutableSet<Language>,
    type: ProjectPermissionType,
  ): Boolean {
    return hasOnlyAccessToDeletedLanguage(languages) && hasPermissionType(type)
  }

  private fun shouldLowerPermissions(
    languages: MutableSet<Language>,
    scope: Scope,
  ): Boolean {
    return hasOnlyAccessToDeletedLanguage(languages) && hasScope(scope)
  }

  private fun scopesWithout(scope: Scope) =
    Scope
      .expand(permission.scopes)
      .toMutableList()
      .also { it.remove(scope) }
      .toTypedArray()

  private fun hasScope(scope: Scope) = permission.granular && permission.scopes.contains(scope)

  private fun hasPermissionType(type: ProjectPermissionType) = permission.type == type

  private fun hasOnlyAccessToDeletedLanguage(languages: MutableSet<Language>) =
    languages.size == 1 &&
      languages.first().id == language.id

  val permissionService: PermissionService
    get() = applicationContext.getBean(PermissionService::class.java)
}
