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

package io.tolgee.notifications.dto

import io.tolgee.model.Notification
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJob

data class NotificationCreateDto(
  val project: Project,
  val activityRevision: ActivityRevision? = null,
  val batchJob: BatchJob? = null,
  val meta: MutableMap<String, Any?> = mutableMapOf(),
) {
  val type = when {
    activityRevision != null -> Notification.NotificationType.ACTIVITY
    batchJob != null -> Notification.NotificationType.BATCH_JOB_FAILURE
    else -> throw IllegalStateException("No entity attached to this DTO!")
  }

  constructor(project: Project, activityRevision: ActivityRevision) :
    this(project, activityRevision = activityRevision, batchJob = null)

  constructor(project: Project, batchJob: BatchJob) :
    this(project, activityRevision = null, batchJob = batchJob)

  init {
    if (activityRevision == null && batchJob == null) {
      throw IllegalArgumentException("No entity attached to the notification")
    }

    if (activityRevision != null && batchJob != null) {
      throw IllegalArgumentException("Too many entities attached to the notification")
    }
  }

  fun toNotificationEntity(params: NotificationDispatchParamsDto): Notification {
    val mergedMeta = meta.toMutableMap()
    mergedMeta.putAll(params.meta)

    return Notification(
      type = type,
      recipient = params.recipient,
      project = project,
      activityRevisions = activityRevision?.let { mutableSetOf(activityRevision) } ?: mutableSetOf(),
      activityModifiedEntities = params.activityModifiedEntities.toMutableSet(),
      meta = mergedMeta,
    )
  }

  fun mergeIntoNotificationEntity(notification: Notification, params: NotificationDispatchParamsDto) {
    when (notification.type) {
      Notification.NotificationType.ACTIVITY ->
        mergeIntoNotificationEntityActivity(notification, params)

      Notification.NotificationType.BATCH_JOB_FAILURE ->
        throw IllegalArgumentException("Cannot merge notifications of type ${notification.type}")
    }
  }

  private fun mergeIntoNotificationEntityActivity(notification: Notification, params: NotificationDispatchParamsDto) {
    if (activityRevision == null)
      throw IllegalArgumentException("Tried to merge notifications of incompatible type")
    if (notification.recipient.id != params.recipient.id)
      throw IllegalArgumentException("Tried to merge a notification for user#${notification.recipient.id}, " +
        "but specified ${params.recipient.id} as recipient in notification dispatch parameters")

    notification.activityRevisions.add(activityRevision)
    params.activityModifiedEntities.forEach {
      val existing = notification.activityModifiedEntities.find { ex ->
        ex.activityRevision.id == it.activityRevision.id &&
          ex.entityId == it.entityId &&
          ex.entityClass == it.entityClass
      }

      if (existing == null) {
        notification.activityModifiedEntities.add(it)
      }
    }

    notification.meta.putAll(params.meta)
  }
}
