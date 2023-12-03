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
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.notifications.NotificationType
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

private typealias SortedTranslations = List<Pair<NotificationType, MutableList<ActivityModifiedEntity>>>

@Component
class ActivityEventListener(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val entityManager: EntityManager,
) : Logging {
  @EventListener
  fun onActivityRevision(e: OnProjectActivityStoredEvent) {
    // Using the Stored variant so `modifiedEntities` is populated.

    logger.trace(
      "Received project activity event - {} on proj#{} ({} entities modified)",
      e.activityRevision.type,
      e.activityRevision.projectId,
      e.activityRevision.modifiedEntities.size
    )

    val projectId = e.activityRevision.projectId ?: return
    val project = entityManager.getReference(Project::class.java, projectId)
    val responsibleUser = e.activityRevision.authorId?.let {
      entityManager.getReference(UserAccount::class.java, it)
    }

    when (e.activityRevision.type) {
      // ACTIVITY_LANGUAGES_CREATED
      ActivityType.CREATE_LANGUAGE ->
        processSimpleActivity(NotificationType.ACTIVITY_LANGUAGES_CREATED, project, responsibleUser, e)

      // ACTIVITY_KEYS_CREATED
      ActivityType.CREATE_KEY ->
        processSimpleActivity(NotificationType.ACTIVITY_KEYS_CREATED, project, responsibleUser, e)

      // ACTIVITY_KEYS_UPDATED
      ActivityType.KEY_TAGS_EDIT,
      ActivityType.KEY_NAME_EDIT,
      ActivityType.BATCH_TAG_KEYS,
      ActivityType.BATCH_UNTAG_KEYS,
      ActivityType.BATCH_SET_KEYS_NAMESPACE ->
        processSimpleActivity(NotificationType.ACTIVITY_KEYS_UPDATED, project, responsibleUser, e)

      // ACTIVITY_KEYS_UPDATED, ACTIVITY_KEYS_SCREENSHOTS_UPLOADED, ACTIVITY_TRANSLATIONS_*
      ActivityType.COMPLEX_EDIT ->
        processComplexEdit(project, responsibleUser, e)

      // ACTIVITY_KEYS_SCREENSHOTS_UPLOADED
      ActivityType.SCREENSHOT_ADD ->
        processScreenshotUpdate(project, responsibleUser, e)

      // ACTIVITY_TRANSLATIONS_*, ACTIVITY_MARKED_AS_OUTDATED, ACTIVITY_MARKED_AS_(UN)REVIEWED
      ActivityType.SET_TRANSLATIONS,
      ActivityType.SET_TRANSLATION_STATE,
      ActivityType.SET_OUTDATED_FLAG,
      ActivityType.BATCH_PRE_TRANSLATE_BY_TM,
      ActivityType.BATCH_MACHINE_TRANSLATE,
      ActivityType.AUTO_TRANSLATE,
      ActivityType.BATCH_CLEAR_TRANSLATIONS,
      ActivityType.BATCH_COPY_TRANSLATIONS,
      ActivityType.BATCH_SET_TRANSLATION_STATE ->
        processSetTranslations(project, responsibleUser, e)

      // ACTIVITY_NEW_COMMENTS (ACTIVITY_COMMENTS_MENTION is user-specific and not computed here)
      ActivityType.TRANSLATION_COMMENT_ADD ->
        processSimpleActivity(NotificationType.ACTIVITY_NEW_COMMENTS, project, responsibleUser, e)

      // ACTIVITY_DATA_IMPORTED
      ActivityType.IMPORT ->
        processImport(project, responsibleUser, e)

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
      null -> {}
    }
  }

  /**
   * Handles activities that can be simply mapped to a corresponding notification without extra processing.
   */
  private fun processSimpleActivity(
    type: NotificationType,
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(
        NotificationCreateDto(type, project, e.activityRevision.modifiedEntities),
        responsibleUser,
        source = e,
      )
    )
  }

  /**
   * Emits multiple notification create events depending on the details of the complex edition.
   */
  private fun processComplexEdit(
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    processComplexEditKeyUpdate(project, responsibleUser, e)
    processScreenshotUpdate(project, responsibleUser, e)
    processSetTranslations(project, responsibleUser, e)
  }

  private fun processComplexEditKeyUpdate(
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    // The key was updated if:
    // The entity is a Key and its name or namespace was modified;
    // The entity is a KeyMeta and its tags were modified.
    val relevantEntities = e.activityRevision.modifiedEntities.filter {
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
          NotificationCreateDto(NotificationType.ACTIVITY_KEYS_UPDATED, project, relevantEntities.toMutableList()),
          responsibleUser,
          source = e,
        )
      )
    }
  }

  /**
   * Emits notifications based on whether a translation was added, updated or removed.
   *
   * Refer to the Hacking Documentation, Notifications, Activity Notifications - User Dispatch, §2.2.2.
   */
  private fun processSetTranslations(
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    val sortedTranslations = sortTranslations(e.activityRevision.modifiedEntities)
    for ((type, translations) in sortedTranslations) {
      if (translations.isNotEmpty()) {
        applicationEventPublisher.publishEvent(
          NotificationCreateEvent(
            NotificationCreateDto(type, project, translations),
            responsibleUser,
            source = e,
          )
        )
      }
    }
  }

  private fun sortTranslations(entities: List<ActivityModifiedEntity>): SortedTranslations {
    val updatedTranslations = mutableListOf<ActivityModifiedEntity>()
    val deletedTranslations = mutableListOf<ActivityModifiedEntity>()
    val outdatedTranslations = mutableListOf<ActivityModifiedEntity>()
    val reviewedTranslations = mutableListOf<ActivityModifiedEntity>()
    val unreviewedTranslations = mutableListOf<ActivityModifiedEntity>()

    entities.forEach {
      if (it.entityClass != Translation::class.simpleName) return@forEach

      val text = it.modifications["text"]
      val state = it.modifications["state"]
      val outdated = it.modifications["outdated"]

      if (text != null) {
        if (text.new == null) deletedTranslations.add(it)
        else updatedTranslations.add(it)
      }

      if (outdated?.new == true) outdatedTranslations.add(it)
      if (state?.new == TranslationState.REVIEWED.name) outdatedTranslations.add(it)
      if (state?.new == TranslationState.TRANSLATED.name && state.old == TranslationState.REVIEWED.name)
        unreviewedTranslations.add(it)
    }

    return listOf(
      Pair(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED, updatedTranslations),
      Pair(NotificationType.ACTIVITY_TRANSLATIONS_DELETED, deletedTranslations),
      Pair(NotificationType.ACTIVITY_MARKED_AS_OUTDATED, outdatedTranslations),
      Pair(NotificationType.ACTIVITY_MARKED_AS_REVIEWED, reviewedTranslations),
      Pair(NotificationType.ACTIVITY_MARKED_AS_UNREVIEWED, unreviewedTranslations),
    )
  }

  private fun processScreenshotUpdate(
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    val addedScreenshots = e.activityRevision.modifiedEntities
      .filter { it.entityClass == Screenshot::class.simpleName && it.revisionType == RevisionType.ADD }

    if (addedScreenshots.isNotEmpty()) {
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(
            type = NotificationType.ACTIVITY_KEYS_SCREENSHOTS_UPLOADED,
            project = project,
            modifiedEntities = addedScreenshots.toMutableList()
          ),
          responsibleUser,
          source = e,
        )
      )
    }
  }

  private fun processImport(
    project: Project,
    responsibleUser: UserAccount?,
    e: OnProjectActivityStoredEvent,
  ) {
    val createdKeys = e.activityRevision.modifiedEntities
      .filter { it.entityClass == Key::class.simpleName && it.revisionType == RevisionType.ADD }

    if (createdKeys.isNotEmpty()) {
      println("new keys")
      applicationEventPublisher.publishEvent(
        NotificationCreateEvent(
          NotificationCreateDto(NotificationType.ACTIVITY_KEYS_CREATED, project, createdKeys.toMutableList()),
          responsibleUser,
          source = e,
        )
      )
    }

    processSetTranslations(project, responsibleUser, e)
  }
}
