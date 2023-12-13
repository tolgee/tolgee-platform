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

import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserNotificationTranslationTest : AbstractNotificationTest() {
  lateinit var testData: NotificationsTestData

  @BeforeEach
  override fun setupTests() {
    testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)

    super.setupTests()
  }

  @Test
  fun `it does not dispatch the same type of notification for source strings and translated strings`() {
    performAuthPut(
      url = "/v2/projects/${testData.calmProject.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = testData.keyCalmProject.name,
        translations = mapOf("en" to "Superb English translation!")
      )
    ).andIsOk

    waitUntilUserNotificationDispatch()

    userNotificationRepository.findAllByRecipient(testData.alice).assert
      .satisfiesOnlyOnce { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED) }

    performAuthPut(
      url = "/v2/projects/${testData.calmProject.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = testData.keyCalmProject.name,
        translations = mapOf("fr" to "Superb French translation!")
      )
    ).andIsOk

    waitUntilUserNotificationDispatch()

    userNotificationRepository.findAllByRecipient(testData.alice).assert
      .satisfiesOnlyOnce { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce { it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED) }
  }

  @Test
  fun `it does debounce key creation and setting strings as a single notification`() {
    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilUserNotificationDispatch()

    performAuthPut(
      url = "/v2/projects/${testData.calmProject.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = "test-key",
        translations = mapOf("en" to "Superb English translation!", "fr" to "Superb French translation!")
      )
    ).andIsOk

    waitUntilUserNotificationDispatch()

    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)
  }

  @Test
  fun `it does not dispatch outdated notifications if it was not done manually`() {
    performAuthPut(
      url = "/v2/projects/${testData.calmProject.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = testData.keyCalmProject.name,
        translations = mapOf("en" to "Superb English translation!")
      )
    ).andIsOk

    waitUntilUserNotificationDispatch()

    userNotificationRepository.findAllByRecipient(testData.alice).assert
      .noneMatch { it.type == NotificationType.ACTIVITY_TRANSLATION_OUTDATED }
      .noneMatch { it.type == NotificationType.ACTIVITY_TRANSLATIONS_UPDATED }
  }
}
