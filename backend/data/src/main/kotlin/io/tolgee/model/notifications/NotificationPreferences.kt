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

package io.tolgee.model.notifications

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.notifications.NotificationType
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.springframework.data.annotation.AccessType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@TypeDef(
  name = "enum-array",
  typeClass = EnumArrayType::class,
  parameters = [
    Parameter(
      name = EnumArrayType.SQL_ARRAY_TYPE,
      value = "varchar"
    )
  ]
)
@Table(
  indexes = [
    Index(
      name = "notification_preferences_user_project",
      columnList = "user_account_id, project_id",
      unique = true,
    ),
    Index(
      name = "notification_preferences_user",
      columnList = "user_account_id"
    ),
    Index(
      name = "notification_preferences_project",
      columnList = "project_id",
    )
  ]
)
class NotificationPreferences(
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  val userAccount: UserAccount,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = true)
  var project: Project?,

  @Type(type = "enum-array")
  @Column(nullable = false, columnDefinition = "varchar[]")
  var disabledNotifications: Array<NotificationType>,
) {
  @Id
  @AccessType(AccessType.Type.PROPERTY)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0L
}
