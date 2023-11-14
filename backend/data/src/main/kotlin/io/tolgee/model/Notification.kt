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

package io.tolgee.model

import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJob
import org.hibernate.annotations.Check
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
class Notification private constructor(
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val type: NotificationType,

  @ManyToOne(fetch = FetchType.EAGER, cascade = [ CascadeType.ALL ]) // We most definitely need this to show the notification: eager
  @JoinColumn(nullable = false)
  val project: Project,

  @Check(constraints = "activity_revision IS NULL OR type == \"ACTIVITY\"")
  @ManyToMany(fetch = FetchType.EAGER, cascade = [ CascadeType.ALL ]) // We most definitely need this to show the notification: eager
  @JoinTable(name = "notification_activity_revisions")
  val activityRevisions: MutableList<ActivityRevision>? = null,

  @Check(constraints = "batch_job IS NULL OR type == \"BATCH_JOB_FAILURE\"")
  @ManyToOne(fetch = FetchType.EAGER, cascade = [ CascadeType.ALL ]) // We most definitely need this to show the notification: eager
  @Column(name = "batch_job")
  val batchJob: BatchJob? = null,
) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0

  @Column(nullable = false)
  var unread: Boolean = false

  // It is a `lateinit` since during notification creation, the user is provided externally.
  // However, because it's non-nullable in DB it'll never actually be `null` besides during creation.
  // It allows not polluting the entire code with a nullable type.
  @ManyToOne(fetch = FetchType.LAZY, cascade = [ CascadeType.ALL ]) // This data is very likely to be useless: lazy
  @JoinColumn(nullable = false)
  lateinit var recipient: UserAccount

  var markedDoneAt: Date? = null

  @CreationTimestamp
  @UpdateTimestamp
  @OrderBy
  val lastUpdated: Date = Date()

  constructor(project: Project, activityRevision: ActivityRevision) :
    this(NotificationType.ACTIVITY, project, activityRevisions = mutableListOf(activityRevision))

  constructor(project: Project, batchJob: BatchJob) :
    this(NotificationType.BATCH_JOB_FAILURE, project, batchJob = batchJob)

  enum class NotificationType {
    ACTIVITY,
    BATCH_JOB_FAILURE,
  }
}
