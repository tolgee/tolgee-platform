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

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.events.*
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
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
  private val batchJobService: BatchJobService,
) : Logging {
  @EventListener
  fun onBatchEventQueued(e: OnBatchJobCreated) {
    logger.trace(
      "Received batch job created event - job#{} on proj#{}",
      e.job.id,
      e.job.project.id,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.PENDING

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  @EventListener
  fun onBatchEventStarted(e: OnBatchJobStarted) {
    logger.trace(
      "Received batch job started event - job#{} on proj#{}",
      e.job.id,
      e.job.projectId,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.RUNNING
    notification.meta["processed"] = 0
    notification.meta["total"] = e.job.totalChunks

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  @EventListener
  fun onBatchEventProgress(e: OnBatchJobProgress) {
    logger.trace(
      "Received batch job progress event - job#{} on proj#{} ({}/{})",
      e.job.id,
      e.job.projectId,
      e.processed,
      e.total,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.RUNNING
    notification.meta["processed"] = e.processed
    notification.meta["total"] = e.total

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  @EventListener
  fun onBatchEventSuccess(e: OnBatchJobSucceeded) {
    logger.trace(
      "Received batch job success event - job#{} on proj#{}",
      e.job.id,
      e.job.projectId,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.SUCCESS

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  @EventListener
  fun onBatchEventSuccess(e: OnBatchJobCancelled) {
    logger.trace(
      "Received batch job cancel event - job#{} on proj#{}",
      e.job.id,
      e.job.projectId,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.CANCELLED

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  @EventListener
  fun onBatchJobError(e: OnBatchJobFailed) {
    logger.trace(
      "Received batch job failure event - job#{} on proj#{}",
      e.job.id,
      e.job.projectId,
    )

    val notification = createNotificationBase(e.job)
    notification.meta["status"] = BatchJobStatus.FAILED

    applicationEventPublisher.publishEvent(
      NotificationCreateEvent(notification, e)
    )
  }

  private fun createNotificationBase(batchJob: BatchJob): NotificationCreateDto {
    return NotificationCreateDto(
      project = batchJob.project,
      batchJob = batchJob
    )
  }

  private fun createNotificationBase(batchJobDto: BatchJobDto): NotificationCreateDto {
    val job = batchJobService.getJobEntity(batchJobDto.id)
    return createNotificationBase(job)
  }
}
