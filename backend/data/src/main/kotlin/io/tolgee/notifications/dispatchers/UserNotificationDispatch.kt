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

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.notifications.NotificationService
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.service.LanguageService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UserNotificationDispatch(
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val languageService: LanguageService,
  private val notificationService: NotificationService,
) : Logging {
  @EventListener
  fun onNotificationCreate(e: NotificationCreateEvent) {
    logger.trace("Received notification creation event {}", e)

    when {
      e.notification.activityRevision != null -> handleActivityNotification(e)
      e.notification.batchJob != null -> handleBatchJobNotification(e)
      else -> logger.warn("Encountered invalid notification create event {}", e)
    }
  }

  private fun handleActivityNotification(e: NotificationCreateEvent) {
    val revision = e.notification.activityRevision!!
    val users = getUsersConcernedByRevision(revision)
    if (users.isEmpty()) return

    notificationService.dispatchNotificationToUserIds(e.notification, users)
  }

  private fun handleBatchJobNotification(e: NotificationCreateEvent) {
    // Only send a full notification for job failures. The rest will be ephemeral WebSocket based.
    if (e.notification.meta["status"] != BatchJobStatus.FAILED) return

    val batchJob = e.notification.batchJob!!
    val author = batchJob.author ?: return
    notificationService.dispatchNotificationToUser(e.notification, author)
  }

  private fun getUsersConcernedByRevision(revision: ActivityRevision): List<Long> {
    return when (revision.type) {
      ActivityType.CREATE_LANGUAGE,
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.KEY_NAME_EDIT,
      ActivityType.CREATE_KEY,
      ActivityType.NAMESPACE_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.BATCH_UNTAG_KEYS,
      ActivityType.BATCH_SET_KEYS_NAMESPACE
      -> getAllUsersOfProject(revision)

      ActivityType.SET_TRANSLATION_STATE,
      ActivityType.SET_TRANSLATIONS,
      ActivityType.DISMISS_AUTO_TRANSLATED_STATE,
      ActivityType.SET_OUTDATED_FLAG,
      ActivityType.TRANSLATION_COMMENT_ADD,
      ActivityType.TRANSLATION_COMMENT_EDIT,
      ActivityType.TRANSLATION_COMMENT_SET_STATE,
      ActivityType.BATCH_PRE_TRANSLATE_BY_TM,
      ActivityType.BATCH_MACHINE_TRANSLATE,
      ActivityType.AUTO_TRANSLATE,
      ActivityType.BATCH_CLEAR_TRANSLATIONS,
      ActivityType.BATCH_COPY_TRANSLATIONS,
      ActivityType.BATCH_SET_TRANSLATION_STATE
      -> getUsersConcernedByTranslationChange(revision)

      ActivityType.SCREENSHOT_ADD
      -> getUsersConcernedByScreenshotChange(revision)

      ActivityType.IMPORT -> TODO()
      ActivityType.COMPLEX_EDIT -> TODO()

      // Do not show a notification about those type of events
      ActivityType.EDIT_PROJECT,
      ActivityType.EDIT_LANGUAGE,
      ActivityType.DELETE_LANGUAGE,
      ActivityType.KEY_DELETE,
      ActivityType.TRANSLATION_COMMENT_DELETE,
      ActivityType.SCREENSHOT_DELETE,
      ActivityType.CREATE_PROJECT,
      ActivityType.UNKNOWN,
      null -> emptyList()
    }
  }

  private fun getAllUsersOfProject(activityRevision: ActivityRevision): List<Long> {
    return userAccountService.getAllPermissionInformationOfPermittedUsersInProject(activityRevision.projectId!!)
      .map { it.id }
  }

  private fun getUsersConcernedByScreenshotChange(activityRevision: ActivityRevision): List<Long> {
    return userAccountService.getAllPermissionInformationOfPermittedUsersInProject(activityRevision.projectId!!)
      .filter {
        val computedPermissions = permissionService.computeProjectPermission(it)
        computedPermissions.expandedScopes.contains(Scope.SCREENSHOTS_VIEW)
      }
      .map { it.id }
  }

  private fun getUsersConcernedByTranslationChange(activityRevision: ActivityRevision): List<Long> {
    val translationIds = activityRevision.modifiedEntities
      .filter { it.entityClass == Translation::class.simpleName }
      .map { it.entityId }

    val langIds = languageService.findLanguageIdsOfTranslations(translationIds)
    return userAccountService.getAllPermissionInformationOfPermittedUsersInProject(
      activityRevision.projectId!!,
      langIds
    )
      .filter {
        val computedPermissions = permissionService.computeProjectPermission(it)
        println("--")
        println(computedPermissions.expandedScopes.joinToString(", "))
        computedPermissions.expandedScopes.contains(Scope.TRANSLATIONS_VIEW)
      }
      .map { it.id }
  }
}
