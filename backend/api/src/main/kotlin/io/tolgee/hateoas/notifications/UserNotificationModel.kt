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

import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.model.views.activity.SimpleModifiedEntityView
import io.tolgee.notifications.NotificationType
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable
import java.util.*

@Suppress("unused")
class UserNotificationModel(
  val id: Long,
  val type: NotificationType,
  val project: SimpleProjectModel?,
  val batchJob: BatchJobModel?,
  val modifiedEntities: List<SimpleModifiedEntityView>?,
  val unread: Boolean,
  val markedDoneAt: Date?,
  val lastUpdated: Date,
) : RepresentationModel<UserNotificationModel>(), Serializable
