/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.notifications.dispatchers

import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.UserProjectMetadataView
import io.tolgee.notifications.NotificationType
import io.tolgee.notifications.UserNotificationService
import io.tolgee.notifications.dto.UserNotificationParamsDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.service.LanguageService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component

@Component
@EnableAsync(proxyTargetClass = true)
class UserNotificationDispatch(
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val languageService: LanguageService,
  private val userNotificationService: UserNotificationService,
  private val entityManager: EntityManager,
) : Logging {
  @Async
  @EventListener
  fun onNotificationCreate(e: NotificationCreateEvent) {
    logger.trace("Received notification creation event {}", e)

    when {
      NotificationType.ACTIVITY_NOTIFICATIONS.contains(e.notification.type) ->
        handleActivityNotification(e)
      NotificationType.BATCH_JOB_NOTIFICATIONS.contains(e.notification.type) ->
        handleBatchJobNotification(e)
      else ->
        throw IllegalStateException("Encountered invalid notification type ${e.notification.type}")
    }
  }

  private fun handleActivityNotification(e: NotificationCreateEvent) {
    val users =
      userAccountService.getAllConnectedUserProjectMetadataViews(e.notification.projectId)
        .filter {
          it.notificationPreferences?.disabledNotifications?.contains(e.notification.type) != true &&
            it.userAccountId != e.responsibleUserId
        }

    val translationToLanguageMap =
      e.notification.modifiedEntities!!
        .filter { it.entityClass == Translation::class.simpleName }
        .map { it.entityId }
        .let { if (it.isEmpty()) emptyMap() else languageService.findLanguageIdsOfTranslations(it) }

    val notifications =
      users.mapNotNull {
        handleActivityNotificationForUser(e, translationToLanguageMap, it)
      }

    userNotificationService.dispatchNotifications(e.notification, notifications)
  }

  private fun handleBatchJobNotification(e: NotificationCreateEvent) {
    // Only send a full notification for job failures.
    if (e.notification.type != NotificationType.BATCH_JOB_ERRORED) return

    val batchJob = e.notification.batchJob!!
    val author = batchJob.author ?: return
    userNotificationService.dispatchNotification(
      e.notification,
      UserNotificationParamsDto(author),
    )
  }

  private fun handleActivityNotificationForUser(
    e: NotificationCreateEvent,
    translationToLanguageMap: Map<Long, Long>,
    userProjectMetadataView: UserProjectMetadataView,
  ): UserNotificationParamsDto? {
    val permissions =
      permissionService.computeProjectPermission(
        userProjectMetadataView.organizationRole,
        userProjectMetadataView.basePermissions,
        userProjectMetadataView.permissions,
      )

    // Filter the entities the user is allowed to see
    val filteredModifiedEntities =
      filterModifiedEntities(
        e.notification.modifiedEntities!!,
        permissions,
        translationToLanguageMap,
      )

    if (filteredModifiedEntities.isEmpty()) return null

    return UserNotificationParamsDto(
      recipient = entityManager.getReference(UserAccount::class.java, userProjectMetadataView.userAccountId),
      modifiedEntities = filteredModifiedEntities.toSet(),
    )
  }

  private fun filterModifiedEntities(
    modifiedEntities: List<ActivityModifiedEntity>,
    permissions: ComputedPermissionDto,
    translationToLanguageMap: Map<Long, Long>,
  ): Set<ActivityModifiedEntity> {
    return modifiedEntities
      .filter {
        when (it.entityClass) {
          Screenshot::class.simpleName ->
            isUserAllowedToSeeScreenshot(permissions)
          Translation::class.simpleName ->
            isUserAllowedToSeeTranslation(it, permissions, translationToLanguageMap)
          else -> true
        }
      }.toSet()
  }

  private fun isUserAllowedToSeeScreenshot(permissions: ComputedPermissionDto): Boolean {
    return permissions.expandedScopes.contains(Scope.SCREENSHOTS_VIEW)
  }

  private fun isUserAllowedToSeeTranslation(
    entity: ActivityModifiedEntity,
    permissions: ComputedPermissionDto,
    translationToLanguageMap: Map<Long, Long>,
  ): Boolean {
    if (!permissions.expandedScopes.contains(Scope.TRANSLATIONS_VIEW)) return false

    val language = translationToLanguageMap[entity.entityId] ?: return false
    return permissions.viewLanguageIds.isNullOrEmpty() || permissions.viewLanguageIds.contains(language)
  }
}
