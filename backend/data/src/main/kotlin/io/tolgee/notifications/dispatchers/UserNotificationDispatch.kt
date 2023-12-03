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

import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.UserAccountProjectNotificationDataView
import io.tolgee.notifications.NotificationType
import io.tolgee.notifications.UserNotificationService
import io.tolgee.notifications.dto.UserNotificationParamsDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.service.LanguageService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
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
    val users = userAccountService.getAllPermissionInformationOfPermittedUsersInProject(e.notification.project.id)
    val translationToLanguageMap = e.notification.modifiedEntities!!
      .filter { it.entityClass == Translation::class.simpleName }
      .map { it.entityId }
      .let { if (it.isEmpty()) emptyMap() else languageService.findLanguageIdsOfTranslations(it) }

    val notifications = users.mapNotNull {
      if (it.id != e.responsibleUser?.id)
        handleActivityNotificationForUser(e, translationToLanguageMap, it)
      else
        null
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
      UserNotificationParamsDto(author)
    )
  }

  private fun handleActivityNotificationForUser(
    e: NotificationCreateEvent,
    translationToLanguageMap: Map<Long, Long>,
    userPermissionData: UserAccountProjectNotificationDataView,
  ): UserNotificationParamsDto? {
    val permissions = permissionService.computeProjectPermission(userPermissionData).expandedScopes

    // Filter the entities the user is allowed to see
    val filteredModifiedEntities = filterModifiedEntities(
      e.notification.modifiedEntities!!,
      permissions,
      userPermissionData.permittedViewLanguages,
      translationToLanguageMap,
    )

    if (filteredModifiedEntities.isEmpty()) return null

    return UserNotificationParamsDto(
      recipient = entityManager.getReference(UserAccount::class.java, userPermissionData.id),
      modifiedEntities = filteredModifiedEntities.toSet(),
    )
  }

  private fun filterModifiedEntities(
    modifiedEntities: List<ActivityModifiedEntity>,
    permissions: Array<Scope>,
    permittedViewLanguages: List<Long>?,
    translationToLanguageMap: Map<Long, Long>,
  ): Set<ActivityModifiedEntity> {
    return modifiedEntities
      .filter {
        when (it.entityClass) {
          Screenshot::class.simpleName ->
            isUserAllowedToSeeScreenshot(permissions)
          Translation::class.simpleName ->
            isUserAllowedToSeeTranslation(it, permissions, permittedViewLanguages, translationToLanguageMap)
          else -> true
        }
      }.toSet()
  }

  private fun isUserAllowedToSeeScreenshot(permissions: Array<Scope>): Boolean {
    return permissions.contains(Scope.SCREENSHOTS_VIEW)
  }

  private fun isUserAllowedToSeeTranslation(
    entity: ActivityModifiedEntity,
    permissions: Array<Scope>,
    permittedViewLanguages: List<Long>?,
    translationToLanguageMap: Map<Long, Long>,
  ): Boolean {
    if (!permissions.contains(Scope.TRANSLATIONS_VIEW)) return false

    val language = translationToLanguageMap[entity.entityId] ?: return false
    return permittedViewLanguages?.contains(language) != false
  }
}
