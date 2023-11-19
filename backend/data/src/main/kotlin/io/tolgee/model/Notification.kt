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

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.batch.BatchJob
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
@TypeDefs(
  value = [
    TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
  ]
)
class Notification private constructor(
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val type: NotificationType,

  // This data is very likely to be useless: lazy
  @ManyToOne(fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  @JoinColumn(nullable = false)
  val recipient: UserAccount,

  // We most definitely need this to show the notification: eager
  @ManyToOne(fetch = FetchType.EAGER, cascade = [ CascadeType.REMOVE ])
  @JoinColumn(nullable = false)
  val project: Project,

  // We most definitely need this to show the notification: eager
  @ManyToMany(fetch = FetchType.EAGER, cascade = [ CascadeType.REMOVE ])
  @JoinTable(name = "notification_activity_revisions")
  val activityRevisions: MutableList<ActivityRevision>? = null,

  // We most definitely need this to show the notification: eager
  @ManyToOne(fetch = FetchType.EAGER, cascade = [ CascadeType.REMOVE ])
  val batchJob: BatchJob? = null,

  @Type(type = "jsonb")
  val meta: MutableMap<String, Any?>
) {
  @Id
  @SequenceGenerator(name = "notification_seq", sequenceName = "sequence_notifications")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
  val id: Long = 0

  @Column(nullable = false)
  @ColumnDefault("true")
  var unread: Boolean = true

  @Temporal(TemporalType.TIMESTAMP)
  var markedDoneAt: Date? = null

  @OrderBy
  @UpdateTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  val lastUpdated: Date = Date()

  constructor(
    recipient: UserAccount,
    project: Project,
    activityRevision: ActivityRevision,
    meta: Map<String, Any?>? = null,
  ) :
    this(
      NotificationType.ACTIVITY,
      recipient,
      project,
      activityRevisions = mutableListOf(activityRevision),
      meta = meta?.toMutableMap() ?: mutableMapOf(),
    )

  constructor(
    recipient: UserAccount,
    project: Project,
    batchJob: BatchJob,
    meta: Map<String, Any?>? = null,
  ) :
    this(
      NotificationType.BATCH_JOB_FAILURE,
      recipient,
      project,
      batchJob = batchJob,
      meta = meta?.toMutableMap() ?: mutableMapOf(),
    )

  enum class NotificationType {
    ACTIVITY,
    BATCH_JOB_FAILURE,
  }
}
