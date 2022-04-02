package io.tolgee.model.enums

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException

enum class ApiScope(var value: String) {
  TRANSLATIONS_VIEW("translations.view"),
  TRANSLATIONS_EDIT("translations.edit"),
  KEYS_EDIT("keys.edit"),
  SCREENSHOTS_UPLOAD("screenshots.upload"),
  SCREENSHOTS_DELETE("screenshots.delete"),
  SCREENSHOTS_VIEW("screenshots.view"),
  ACTIVITY_VIEW("activity.view");

  companion object {
    fun fromValue(value: String): ApiScope {
      for (scope in values()) {
        if (scope.value == value) {
          return scope
        }
      }
      throw NotFoundException(Message.SCOPE_NOT_FOUND)
    }
  }
}
