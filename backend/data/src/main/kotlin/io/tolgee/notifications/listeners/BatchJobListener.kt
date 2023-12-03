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

import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.model.Project
import io.tolgee.model.batch.BatchJob
import io.tolgee.notifications.NotificationType
import io.tolgee.notifications.dto.NotificationCreateDto
import io.tolgee.notifications.events.NotificationCreateEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class BatchJobListener(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val entityManager: EntityManager,
) : Logging {
  @EventListener
  fun onBatchJobError(e: OnBatchJobFailed) {
    logger.trace(
      "Received batch job failure event - job#{} on proj#{}",
      e.job.id,
      e.job.projectId,
    )

    val job = entityManager.getReference(BatchJob::class.java, e.job.id)
    val project = entityManager.getReference(Project::class.java, e.job.projectId)
    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(
        NotificationCreateDto(
          type = NotificationType.BATCH_JOB_ERRORED,
          project = project,
          batchJob = job,
        ),
        source = e,
        responsibleUser = null,
      )
    )
  }
}
