package io.tolgee.dtos

import io.tolgee.model.enums.Scope

data class ComputedPermissionDto(
  val scopes: Array<Scope>?,
  val translateLanguageIds: Set<Long>?
) {
  val allTranslateLanguagesPermitted: Boolean
    get() {
      if (scopes.isNullOrEmpty()) {
        return false
      }

      if (translateLanguageIds.isNullOrEmpty()) {
        return true
      }

      if (scopes.contains(Scope.ADMIN)) {
        return true
      }

      if (scopes.contains(Scope.TRANSLATIONS_EDIT)) {
        return translateLanguageIds.isNullOrEmpty()
      }

      return false
    }
}
