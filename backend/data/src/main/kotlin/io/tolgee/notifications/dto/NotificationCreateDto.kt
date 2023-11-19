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
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJob

data class NotificationCreateDto(
  val project: Project,
  val activityRevision: ActivityRevision? = null,
  val batchJob: BatchJob? = null,
) {
  val meta: MutableMap<String, Any?> = mutableMapOf()

  constructor(project: Project, activityRevision: ActivityRevision):
    this(project, activityRevision = activityRevision, batchJob = null)

  constructor(project: Project, batchJob: BatchJob):
    this(project, activityRevision = null, batchJob = batchJob)

  init {
    if (activityRevision == null && batchJob == null) {
      throw IllegalArgumentException("No entity attached to the notification")
    }

    if (activityRevision != null && batchJob != null) {
      throw IllegalArgumentException("Too many entities attached to the notification")
    }
  }

  fun toNotificationEntity(user: UserAccount): Notification {
    return when {
      activityRevision != null ->
        Notification(user, project, activityRevision, meta)
      batchJob != null ->
        Notification(user, project, batchJob, meta)
      else ->
        throw IllegalStateException("No entity attached to this DTO??")
    }
  }
}
