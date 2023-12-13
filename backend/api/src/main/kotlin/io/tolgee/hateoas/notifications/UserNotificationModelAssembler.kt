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

package io.tolgee.hateoas.notifications

import io.tolgee.activity.views.ModifiedEntitiesViewProvider
import io.tolgee.api.v2.controllers.notifications.NotificationsController
import io.tolgee.batch.BatchJobService
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.notifications.UserNotification
import io.tolgee.model.views.activity.SimpleModifiedEntityView
import org.springframework.context.ApplicationContext
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserNotificationModelAssembler(
  private val batchJobService: BatchJobService,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
  private val batchJobModelAssembler: BatchJobModelAssembler,
  private val applicationContext: ApplicationContext,
) : RepresentationModelAssemblerSupport<UserNotification, UserNotificationModel>(
  NotificationsController::class.java, UserNotificationModel::class.java
) {
  override fun toModel(entity: UserNotification): UserNotificationModel {
    val project = entity.project?.let { simpleProjectModelAssembler.toModel(it) }
    val modifiedEntities = assembleEntityChanges(entity.modifiedEntities).ifEmpty { null }
    val batchJob = entity.batchJob?.let {
      val view = batchJobService.getView(it)
      batchJobModelAssembler.toModel(view)
    }

    return UserNotificationModel(
      id = entity.id,
      type = entity.type,
      project = project,
      batchJob = batchJob,
      modifiedEntities = modifiedEntities,
      unread = entity.unread,
      markedDoneAt = entity.markedDoneAt,
      lastUpdated = entity.lastUpdated,
    )
  }

  private fun assembleEntityChanges(modifiedEntities: List<ActivityModifiedEntity>): List<SimpleModifiedEntityView> {
    val provider = ModifiedEntitiesViewProvider(
      applicationContext,
      modifiedEntities
    )

    return provider.getSimple()
  }
}
