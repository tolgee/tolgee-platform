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

package io.tolgee.notifications.listeners

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Screenshot
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.notifications.NotificationType
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.service.language.LanguageService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private typealias SortedTranslations = List<Pair<NotificationType, MutableList<ActivityModifiedEntity>>>

@Component
class ActivityEventListener(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val languageService: LanguageService,
) : Logging {
  @EventListener
  @Transactional
  fun onActivityRevision(e: OnProjectActivityStoredEvent) {
    // Using the Stored variant so `modifiedEntities` is populated.

    logger.trace(
      "Received project activity event - {} on proj#{} ({} entities modified)",
      e.activityRevision.type,
      e.activityRevision.projectId,
      e.activityRevision.modifiedEntities.size,
    )

    val projectId = e.activityRevision.projectId ?: return
    val responsibleUserId = e.activityRevision.authorId

    when (e.activityRevision.type) {
      // ACTIVITY_LANGUAGES_CREATED
      ActivityType.CREATE_LANGUAGE ->
        processSimpleActivity(NotificationType.ACTIVITY_LANGUAGES_CREATED, projectId, responsibleUserId, e)

      // ACTIVITY_KEYS_CREATED
      ActivityType.CREATE_KEY ->
        processSimpleActivity(NotificationType.ACTIVITY_KEYS_CREATED, projectId, responsibleUserId, e)

      // ACTIVITY_KEYS_UPDATED
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.KEY_NAME_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.COMPLEX_TAG_OPERATION,
      ActivityType.BATCH_UNTAG_KEYS,
      ActivityType.BATCH_SET_KEYS_NAMESPACE,
      ->
        processSimpleActivity(NotificationType.ACTIVITY_KEYS_UPDATED, projectId, responsibleUserId, e)

      // ACTIVITY_KEYS_UPDATED, ACTIVITY_KEYS_SCREENSHOTS_UPLOADED, ACTIVITY_TRANSLATIONS_*
      ActivityType.COMPLEX_EDIT ->
        processComplexEdit(projectId, responsibleUserId, e)

      // ACTIVITY_KEYS_SCREENSHOTS_UPLOADED
      ActivityType.SCREENSHOT_ADD ->
        processScreenshotUpdate(projectId, responsibleUserId, e)

      // ACTIVITY_TRANSLATIONS_*
      ActivityType.SET_TRANSLATIONS,
      ActivityType.SET_TRANSLATION_STATE,
      ActivityType.BATCH_PRE_TRANSLATE_BY_TM,
      ActivityType.BATCH_MACHINE_TRANSLATE,
      ActivityType.AUTO_TRANSLATE,
      ActivityType.BATCH_CLEAR_TRANSLATIONS,
      ActivityType.BATCH_COPY_TRANSLATIONS,
      ActivityType.BATCH_SET_TRANSLATION_STATE,
      ->
        processSetTranslations(projectId, responsibleUserId, e)

      ActivityType.SET_OUTDATED_FLAG ->
        processOutdatedFlagUpdate(projectId, responsibleUserId, e)

      // ACTIVITY_NEW_COMMENTS (ACTIVITY_COMMENTS_MENTION is user-specific and not computed here)
      ActivityType.TRANSLATION_COMMENT_ADD ->
        processSimpleActivity(NotificationType.ACTIVITY_NEW_COMMENTS, projectId, responsibleUserId, e)

      // ACTIVITY_KEYS_CREATED, ACTIVITY_TRANSLATIONS_*
      ActivityType.IMPORT ->
        processImport(projectId, responsibleUserId, e)

      // We don't care about those, ignore them.
      // They're explicitly not written as a single `else` branch,
      // so it causes a compile error when new activities are added
      // and ensures the notification policy is adjusted accordingly.
      ActivityType.UNKNOWN,
      ActivityType.DISMISS_AUTO_TRANSLATED_STATE,
      ActivityType.TRANSLATION_COMMENT_DELETE,
      ActivityType.TRANSLATION_COMMENT_EDIT,
      ActivityType.TRANSLATION_COMMENT_SET_STATE,
      ActivityType.SCREENSHOT_DELETE,
      ActivityType.KEY_DELETE,
      ActivityType.EDIT_LANGUAGE,
      ActivityType.DELETE_LANGUAGE,
      ActivityType.CREATE_PROJECT,
      ActivityType.EDIT_PROJECT,
      ActivityType.NAMESPACE_EDIT,
      ActivityType.AUTOMATION,
      ActivityType.CONTENT_DELIVERY_CONFIG_CREATE,
      ActivityType.CONTENT_DELIVERY_CONFIG_UPDATE,
      ActivityType.CONTENT_DELIVERY_CONFIG_DELETE,
      ActivityType.CONTENT_STORAGE_CREATE,
      ActivityType.CONTENT_STORAGE_UPDATE,
      ActivityType.CONTENT_STORAGE_DELETE,
      ActivityType.WEBHOOK_CONFIG_CREATE,
      ActivityType.WEBHOOK_CONFIG_UPDATE,
      ActivityType.WEBHOOK_CONFIG_DELETE,
      ActivityType.HARD_DELETE_LANGUAGE,
      null
      -> {}
    }
  }

  /**
   * Handles activities that can be simply mapped to a corresponding notification without extra processing.
   */
  private fun processSimpleActivity(
    type: NotificationType,
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(
        NotificationCreateDto(type, projectId, e.activityRevision.modifiedEntities),
        responsibleUserId,
        source = e,
      ),
    )
  }

  /**
   * Emits multiple notification create events depending on the details of the complex edition.
   */
  private fun processComplexEdit(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    processComplexEditKeyUpdate(projectId, responsibleUserId, e)
    processScreenshotUpdate(projectId, responsibleUserId, e)
    processSetTranslations(projectId, responsibleUserId, e)
  }

  private fun processComplexEditKeyUpdate(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    // The key was updated if:
    // The entity is a Key and its name or namespace was modified;
    // The entity is a KeyMeta and its tags were modified.
    val relevantEntities =
      e.activityRevision.modifiedEntities.filter {
        (
          it.entityClass == Key::class.simpleName &&
            (it.modifications.containsKey("name") || it.modifications.containsKey("namespace"))
        ) ||
          (
            it.entityClass == KeyMeta::class.simpleName &&
              it.modifications.containsKey("tags")
          )
      }

    if (relevantEntities.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(NotificationType.ACTIVITY_KEYS_UPDATED, projectId, relevantEntities.toMutableList()),
          responsibleUserId,
          source = e,
        ),
      )
    }
  }

  /**
   * Emits notifications based on whether a translation was added, updated or removed.
   *
   * Refer to the Hacking Documentation, Notifications, Activity Notifications - User Dispatch, §2.2.2.
   */
  private fun processSetTranslations(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
    modifiedEntities: List<ActivityModifiedEntity> = e.activityRevision.modifiedEntities,
  ) {
    val baseLanguageId = languageService.getBaseLanguageForProjectId(projectId)
    val sortedTranslations =
      sortTranslations(
        modifiedEntities,
        baseLanguage = baseLanguageId ?: 0L,
      )

    for ((type, translations) in sortedTranslations) {
      if (translations.isNotEmpty()) {
        applicationEventPublisher.publishEvent(
          NotificationCreateEvent(
            NotificationCreateDto(type, projectId, translations),
            responsibleUserId,
            source = e,
          ),
        )
      }
    }
  }

  private fun sortTranslations(
    entities: List<ActivityModifiedEntity>,
    baseLanguage: Long,
  ): SortedTranslations {
    val updatedSourceTranslations = mutableListOf<ActivityModifiedEntity>()
    val updatedTranslations = mutableListOf<ActivityModifiedEntity>()
    val reviewedTranslations = mutableListOf<ActivityModifiedEntity>()
    val unreviewedTranslations = mutableListOf<ActivityModifiedEntity>()

    entities.forEach {
      if (it.entityClass != Translation::class.simpleName) return@forEach

      val text = it.modifications["text"]
      val state = it.modifications["state"]

      if (text != null) {
        val languageId = it.describingRelations?.get("language")?.entityId
        if (languageId == baseLanguage) {
          updatedSourceTranslations.add(it)
        } else {
          updatedTranslations.add(it)
        }
      }

      if (state?.new == TranslationState.REVIEWED.name) {
        reviewedTranslations.add(it)
      }
      if (state?.new == TranslationState.TRANSLATED.name && state.old == TranslationState.REVIEWED.name) {
        unreviewedTranslations.add(it)
      }
    }

    return listOf(
      Pair(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED, updatedSourceTranslations),
      Pair(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED, updatedTranslations),
      Pair(NotificationType.ACTIVITY_TRANSLATION_REVIEWED, reviewedTranslations),
      Pair(NotificationType.ACTIVITY_TRANSLATION_UNREVIEWED, unreviewedTranslations),
    )
  }

  private fun processOutdatedFlagUpdate(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    val outdatedTranslations =
      e.activityRevision.modifiedEntities
        .filter { it.modifications["outdated"]?.new == true }

    if (outdatedTranslations.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(
            type = NotificationType.ACTIVITY_TRANSLATION_OUTDATED,
            projectId = projectId,
            modifiedEntities = outdatedTranslations.toMutableList(),
          ),
          responsibleUserId,
          source = e,
        ),
      )
    }
  }

  private fun processScreenshotUpdate(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    val addedScreenshots =
      e.activityRevision.modifiedEntities
        .filter { it.entityClass == Screenshot::class.simpleName && it.revisionType == RevisionType.ADD }

    if (addedScreenshots.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(
            type = NotificationType.ACTIVITY_KEYS_SCREENSHOTS_UPLOADED,
            projectId = projectId,
            modifiedEntities = addedScreenshots.toMutableList(),
          ),
          responsibleUserId,
          source = e,
        ),
      )
    }
  }

  private fun processImport(
    projectId: Long,
    responsibleUserId: Long?,
    e: OnProjectActivityStoredEvent,
  ) {
    val createdKeys =
      e.activityRevision.modifiedEntities
        .filter { it.entityClass == Key::class.simpleName && it.revisionType == RevisionType.ADD }
        .toMutableList()

    val keyIds = createdKeys.map { it.entityId }.toSet()
    val updatedTranslations =
      e.activityRevision.modifiedEntities
        .filter {
          val isFromNewKey = keyIds.contains(it.describingRelations?.get("key")?.entityId ?: 0L)
          if (isFromNewKey) createdKeys.add(it)
          !isFromNewKey
        }

    if (createdKeys.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(NotificationType.ACTIVITY_KEYS_CREATED, projectId, createdKeys.toMutableList()),
          responsibleUserId,
          source = e,
        ),
      )
    }

    if (updatedTranslations.isNotEmpty()) {
      processSetTranslations(projectId, responsibleUserId, e, updatedTranslations)
    }
  }
}
