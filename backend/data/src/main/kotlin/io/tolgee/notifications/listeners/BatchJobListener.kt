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
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.model.Notification
import io.tolgee.notifications.NotificationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BatchJobListener(
  private val userAccountService: UserAccountService,
  private val projectService: ProjectService,
  private val batchJobService: BatchJobService,
  private val notificationService: NotificationService,
) {
  @EventListener
  fun onBatchJobError(e: OnBatchJobFailed) {
    val userId = e.job.authorId ?: return
    val projectId = e.job.projectId

    val user = userAccountService.get(userId)
    val project = projectService.get(projectId)
    val job = batchJobService.getJobEntity(e.job.id)

    val notification = Notification(
      project,
      job,
    )

    notificationService.dispatchNotification(notification, user)
  }
}
