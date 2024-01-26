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

import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.notifications.NotificationPreferences

data class UserProjectMetadataView(
  val userAccountId: Long,
  val projectId: Long,
  val organizationRole: OrganizationRoleType?,
  val basePermissions: Permission?,
  val permissions: Permission?,
  val globalNotificationPreferences: NotificationPreferences?,
  val projectNotificationPreferences: NotificationPreferences?,
) {
  val notificationPreferences
    get() = projectNotificationPreferences ?: globalNotificationPreferences
}
