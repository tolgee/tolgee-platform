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

import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.notifications.UserNotification
import io.tolgee.notifications.NotificationType

data class NotificationCreateDto(
  val type: NotificationType,
  val project: Project,
  val modifiedEntities: MutableList<ActivityModifiedEntity>? = null,
  val batchJob: BatchJob? = null
) {
  fun toUserNotificationEntity(params: UserNotificationParamsDto): UserNotification {
    return UserNotification(
      type = type,
      recipient = params.recipient,
      project = project,
      modifiedEntities = params.modifiedEntities.toMutableList()
    )
  }

  fun mergeIntoUserNotificationEntity(userNotification: UserNotification, params: UserNotificationParamsDto) {
    when {
      NotificationType.ACTIVITY_NOTIFICATIONS.contains(userNotification.type) ->
        mergeIntoNotificationEntityActivity(userNotification, params)

      else ->
        throw IllegalArgumentException("Cannot merge notifications of type ${userNotification.type}")
    }
  }

  private fun mergeIntoNotificationEntityActivity(
    userNotification: UserNotification,
    params: UserNotificationParamsDto,
  ) {
    userNotification.modifiedEntities.addAll(params.modifiedEntities)
  }
}
