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

package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.notifications.NotificationType

class NotificationSubscriptionTestData {
  lateinit var user1: UserAccount
  lateinit var user2: UserAccount

  lateinit var organization: Organization

  lateinit var project1: Project
  lateinit var project2: Project

  val root: TestDataBuilder = TestDataBuilder()

  init {
    root.apply {
      addUserAccount {
        username = "admin"
        role = UserAccount.Role.ADMIN
      }

      val user1Builder =
        addUserAccountWithoutOrganization {
          name = "User 1"
          username = "user1"
          user1 = this
        }

      val user2Builder =
        addUserAccountWithoutOrganization {
          name = "User 2"
          username = "user2"
          user2 = this
        }

      addOrganization {
        name = "Test org"
        slug = "test-org"

        organization = this

        addProject {
          name = "Test project 1"
          slug = "project1"
          organizationOwner = organization

          project1 = this
        }

        addProject {
          name = "Test project 2"
          slug = "project2"
          organizationOwner = organization

          project2 = this
        }
      }.build {
        addRole {
          user = user1
          type = OrganizationRoleType.OWNER
        }

        addRole {
          user = user2
          type = OrganizationRoleType.OWNER
        }
      }

      user1Builder.build {
        addNotificationPreferences {
          project = project2
          disabledNotifications = arrayOf(NotificationType.ACTIVITY_KEYS_CREATED)
        }
      }

      user2Builder.build {
        addNotificationPreferences {
          disabledNotifications = arrayOf(NotificationType.ACTIVITY_KEYS_CREATED)
        }

        addNotificationPreferences {
          project = project2
        }
      }
    }
  }
}
