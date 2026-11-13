package io.tolgee.service.notification

import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.enums.Scope
import io.tolgee.model.notifications.NotificationType

object NotificationEligibility {
  /**
   * Returns null when the user cannot act on [type] at all, otherwise the part of [languageIds] they can act on.
   * Key-scoped types carry no languages, so an eligible user gets an empty list for them.
   * [followedLanguageIds] null means the user's languages are not narrowed, empty means the project is muted.
   */
  fun actionableLanguageIds(
    type: NotificationType,
    permission: ComputedPermissionDto,
    languageIds: List<Long>,
    nonBaseLanguageIds: List<Long>,
    followedLanguageIds: Set<Long>? = null,
  ): List<Long>? {
    val scopes = permission.expandedScopes
    val followed = languageIds.onlyFollowed(followedLanguageIds)
    return when (type) {
      NotificationType.KEYS_ADDED, NotificationType.SOURCE_CHANGED ->
        languageIds.takeIf {
          Scope.TRANSLATIONS_EDIT in scopes &&
            permission.filterTranslatePermitted(nonBaseLanguageIds.onlyFollowed(followedLanguageIds)).isNotEmpty()
        }
      NotificationType.STRINGS_TRANSLATED, NotificationType.AUTOMATICALLY_TRANSLATED ->
        permission.filterStateChangePermitted(followed).takeIf { Scope.TRANSLATIONS_STATE_EDIT in scopes }
      NotificationType.STRINGS_REVIEWED ->
        permission.filterViewPermitted(followed).takeIf { Scope.TRANSLATIONS_VIEW in scopes }
      else -> null
    }
  }

  /**
   * Non-base languages the user can edit or review, the ones they can choose to follow.
   */
  fun followableLanguageIds(
    permission: ComputedPermissionDto,
    nonBaseLanguageIds: List<Long>,
  ): List<Long> {
    val editable = editableLanguageIds(permission, nonBaseLanguageIds)
    if (Scope.TRANSLATIONS_STATE_EDIT !in permission.expandedScopes) return editable
    val reviewable = permission.filterStateChangePermitted(nonBaseLanguageIds)
    return nonBaseLanguageIds.filter { it in editable || it in reviewable }
  }

  fun editableLanguageIds(
    permission: ComputedPermissionDto,
    nonBaseLanguageIds: List<Long>,
  ): List<Long> {
    if (Scope.TRANSLATIONS_EDIT !in permission.expandedScopes) return emptyList()
    return permission.filterTranslatePermitted(nonBaseLanguageIds)
  }

  private fun List<Long>.onlyFollowed(followedLanguageIds: Set<Long>?): List<Long> {
    followedLanguageIds ?: return this
    return filter { it in followedLanguageIds }
  }
}
