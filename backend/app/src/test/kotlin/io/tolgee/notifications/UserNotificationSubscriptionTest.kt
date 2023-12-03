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

package io.tolgee.notifications

import io.tolgee.development.testDataBuilder.data.NotificationSubscriptionTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserNotificationSubscriptionTest : AbstractNotificationTest() {
  lateinit var testData: NotificationSubscriptionTestData

  @BeforeEach
  override fun setupTests() {
    testData = NotificationSubscriptionTestData()
    testDataService.saveTestData(testData.root)

    super.setupTests()
  }

  @Test
  fun `it respects global notification subscription settings`() {
    performAuthPost(
      "/v2/projects/${testData.project1.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilUserNotificationDispatch(1)
    val notifications1 = userNotificationRepository.findAllByRecipient(testData.user1)
    val notifications2 = userNotificationRepository.findAllByRecipient(testData.user2)

    notifications1.assert.hasSize(1)
    notifications2.assert.hasSize(0)
    ensureNoUserNotificationDispatch()
  }

  @Test
  fun `it respects project-level notification subscription settings`() {
    performAuthPost(
      "/v2/projects/${testData.project2.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilUserNotificationDispatch(1)
    val notifications1 = userNotificationRepository.findAllByRecipient(testData.user1)
    val notifications2 = userNotificationRepository.findAllByRecipient(testData.user2)

    notifications1.assert.hasSize(0)
    notifications2.assert.hasSize(1)
    ensureNoUserNotificationDispatch()
  }
}
