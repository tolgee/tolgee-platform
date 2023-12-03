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

import com.vladmihalcea.hibernate.type.array.EnumArrayType
import io.tolgee.notifications.NotificationType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
@TypeDef(name = "enum-array", typeClass = EnumArrayType::class)
class NotificationPreferences(
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  val userAccount: UserAccount,

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = true)
  val project: Project?,

  @Type(type = "enum-array")
  @Column(nullable = false, columnDefinition = "varchar[]")
  val disabledNotifications: Array<NotificationType>,
) : Serializable
