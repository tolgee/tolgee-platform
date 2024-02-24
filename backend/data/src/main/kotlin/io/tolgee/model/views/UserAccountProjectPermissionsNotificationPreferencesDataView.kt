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

package io.tolgee.model.views

import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.notifications.NotificationPreferences
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

data class UserProjectMetadata(
  @Id
  val id: Long,
  @Id
  val projectId: Long,
  @OneToMany
  val organizationRole: OrganizationRole?,
  @OneToMany
  val basePermissions: Permission?,
  @OneToMany
  val permissions: Permission?,
  @OneToMany
  val globalNotificationPreferences: NotificationPreferences?,
  @OneToMany
  val projectNotificationPreferences: NotificationPreferences?,
) {
  val notificationPreferences
    get() = projectNotificationPreferences ?: globalNotificationPreferences
}

class UserAccountProjectPermissionsNotificationPreferencesDataView(data: Map<String, Any>) {
  init {
    println(data["permittedViewLanguages"])
  }

  val id = data["id"] as? Long ?: throw IllegalArgumentException()

  val projectId = data["projectId"] as? Long ?: throw IllegalArgumentException()

  val organizationRole = data["organizationRole"] as? OrganizationRoleType

  val basePermissionsBasic = data["basePermissionsBasic"] as? ProjectPermissionType

  val basePermissionsGranular =
    (data["basePermissionsGranular"] as? Array<*>)
      ?.map { enumValueOf<Scope>((it as? Enum<*>)?.name ?: throw IllegalArgumentException()) }

  val permissionsBasic = data["permissionsBasic"] as? ProjectPermissionType

  val permissionsGranular =
    (data["permissionsGranular"] as? Array<*>)
      ?.map { enumValueOf<Scope>((it as? Enum<*>)?.name ?: throw IllegalArgumentException()) }

  val permittedViewLanguages =
    (data["permittedViewLanguages"] as? Array<*>)
      ?.map { (it as? String)?.toLong() ?: throw IllegalArgumentException() }

  val notificationPreferences =
    (data["projectNotificationPreferences"] ?: data["globalNotificationPreferences"])
      as? NotificationPreferences
}
