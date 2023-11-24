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
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.notifications.NotificationService
import io.tolgee.notifications.dto.NotificationDispatchParamsDto
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
  private val notificationService: NotificationService,
  private val entityManager: EntityManager,
) : Logging {
  @Async
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
    when (e.notification.activityRevision!!.type) {
      ActivityType.CREATE_LANGUAGE,
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.KEY_NAME_EDIT,
      ActivityType.CREATE_KEY,
      ActivityType.NAMESPACE_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.BATCH_UNTAG_KEYS,
      ActivityType.BATCH_SET_KEYS_NAMESPACE,
      ActivityType.COMPLEX_EDIT ->
        handleGenericActivityNotification(e)

      ActivityType.SCREENSHOT_ADD ->
        handleScreenshotActivityNotification(e)

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
      ActivityType.BATCH_SET_TRANSLATION_STATE ->
        handleTranslationActivityNotification(e)

      ActivityType.IMPORT ->
        handleImportActivityNotification(e)

      // Do not show a notification about those type of events
      // Intentionally not done as an else block to make this not compile if new activities are added
      ActivityType.EDIT_PROJECT,
      ActivityType.EDIT_LANGUAGE,
      ActivityType.DELETE_LANGUAGE,
      ActivityType.KEY_DELETE,
      ActivityType.TRANSLATION_COMMENT_DELETE,
      ActivityType.SCREENSHOT_DELETE,
      ActivityType.CREATE_PROJECT,
      ActivityType.UNKNOWN,
      null -> {}
    }
  }

  private fun handleBatchJobNotification(e: NotificationCreateEvent) {
    // Only send a full notification for job failures.
    if (e.notification.meta["status"] != BatchJobStatus.FAILED) return

    val batchJob = e.notification.batchJob!!
    val author = batchJob.author ?: return
    notificationService.dispatchNotification(
      e.notification,
      NotificationDispatchParamsDto(author)
    )
  }

  private fun handleGenericActivityNotification(e: NotificationCreateEvent) {
    val revision = e.notification.activityRevision!!
    val users = userAccountService.getAllPermissionInformationOfPermittedUsersInProject(revision.projectId!!)

    val dispatches = users
      .filter { it.id != e.responsibleUser?.id }
      .map {
        NotificationDispatchParamsDto(
          recipient = entityManager.getReference(UserAccount::class.java, it.id),
          activityModifiedEntities = revision.modifiedEntities,
        )
      }

    notificationService.dispatchNotifications(e.notification, dispatches)
  }

  private fun handleScreenshotActivityNotification(e: NotificationCreateEvent) {
    val revision = e.notification.activityRevision!!
    val users = userAccountService.getAllPermissionInformationOfPermittedUsersInProject(revision.projectId!!)

    val dispatches = users
      .filter {
        if (it.id == e.responsibleUser?.id) return@filter false
        val computedPermissions = permissionService.computeProjectPermission(it)
        computedPermissions.expandedScopes.contains(Scope.SCREENSHOTS_VIEW)
      }
      .map {
        NotificationDispatchParamsDto(
          recipient = entityManager.getReference(UserAccount::class.java, it.id),
          activityModifiedEntities = revision.modifiedEntities,
        )
      }

    notificationService.dispatchNotifications(e.notification, dispatches)
  }

  private fun handleTranslationActivityNotification(e: NotificationCreateEvent) {
    val revision = e.notification.activityRevision!!
    val users = userAccountService.getAllPermissionInformationOfPermittedUsersInProject(revision.projectId!!)

    val translationEntities = revision.modifiedEntities
      .filter { it.entityClass == Translation::class.simpleName }

    val translationIds = translationEntities.map { it.entityId }
    val translationToLangMap = languageService.findLanguageIdsOfTranslations(translationIds)

    val dispatches = users
      .filter {
        if (it.id == e.responsibleUser?.id) return@filter false
        val computedPermissions = permissionService.computeProjectPermission(it)
        computedPermissions.expandedScopes.contains(Scope.TRANSLATIONS_VIEW)
      }
      .map {
        val visibleModifiedEntities = translationEntities.filter { entity ->
          val languageId = translationToLangMap[entity.entityId] ?: return@filter false
          it.permittedViewLanguages?.contains(languageId) != false
        }

        NotificationDispatchParamsDto(
          recipient = entityManager.getReference(UserAccount::class.java, it.id),
          activityModifiedEntities = visibleModifiedEntities,
        )
      }
      .filter {
        it.activityModifiedEntities.isNotEmpty()
      }

    notificationService.dispatchNotifications(e.notification, dispatches)
  }

  private fun handleImportActivityNotification(e: NotificationCreateEvent) {
    val revision = e.notification.activityRevision!!

    val keysCount = revision.modifiedEntities.count { it.entityClass == Key::class.simpleName }

    // Count translations per locale
    val translationIds = revision.modifiedEntities
      .filter { it.entityClass == Translation::class.simpleName }
      .map { it.entityId }

    val translationToLangMap = languageService.findLanguageIdsOfTranslations(translationIds)

    val importedTranslationsPerLocale = mutableMapOf<Long, Int>()
    translationToLangMap.values.forEach {
      importedTranslationsPerLocale.merge(it, 1) { a, b -> a + b }
    }

    // Filter users
    val users = userAccountService.getAllPermissionInformationOfPermittedUsersInProject(revision.projectId!!)

    val dispatches = users
      .filter {
        if (it.id == e.responsibleUser?.id) return@filter false
        val computedPermissions = permissionService.computeProjectPermission(it)
        computedPermissions.expandedScopes.contains(Scope.TRANSLATIONS_VIEW)
      }
      .map {
        val userVisibleTranslationCount = importedTranslationsPerLocale
          .filterKeys { l -> it.permittedViewLanguages?.contains(l) != false }
          .values.reduce { acc, i -> acc + i }

        NotificationDispatchParamsDto(
          recipient = entityManager.getReference(UserAccount::class.java, it.id),
          meta = mutableMapOf("keysCount" to keysCount, "translationsCount" to userVisibleTranslationCount)
        )
      }
      .filter {
        it.meta["translationsCount"] != 0
      }

    notificationService.dispatchNotifications(e.notification, dispatches)
  }
}
