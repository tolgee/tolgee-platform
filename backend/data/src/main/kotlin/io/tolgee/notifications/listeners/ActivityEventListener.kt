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

import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Project
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

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
    val notificationDto = NotificationCreateDto(
      project = project,
      activityRevision = e.activityRevision
    )

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notificationDto, e)
    )
  }
}
