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

import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.fixtures.andGetContentAsJsonMap
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.assert
import io.tolgee.util.generateImage
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class RemappedNotificationsTest : AbstractNotificationTest() {
  @Test
  fun `it does properly remap imports to key and translation notifications`() {
    performAuthMultipart(
      url = "/v2/projects/${testData.project1.id}/import",
      files = listOf(
        MockMultipartFile(
          "files", "en.json", "application/json",
          """{"new-key1": "New string 1", "new-key2": "New string 2"}""".toByteArray()
        ),
        MockMultipartFile(
          "files", "fr.json", "application/json",
          """{"some-key": "Updated", "new-key1": "New FR string 1", "new-key2": "New FR string 2"}""".toByteArray()
        ),
        MockMultipartFile(
          "files", "cs.json", "application/json",
          """{"new-key1": "New CZ string 1", "new-key2": "New CZ string 2"}""".toByteArray()
        )
      )
    ).andIsOk

    performAuthPut("/v2/projects/${testData.project1.id}/import/apply?forceMode=OVERRIDE", null).andIsOk

    waitUntilUserNotificationDispatch(12)
    val acmeChiefNotifications = userNotificationRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = userNotificationRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = userNotificationRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = userNotificationRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = userNotificationRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications =
      userNotificationRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = userNotificationRepository.findAllByRecipient(testData.bob)

    acmeChiefNotifications.assert.hasSize(2)
    projectManagerNotifications.assert.hasSize(2)
    frenchTranslatorNotifications.assert.hasSize(2)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(2)
    bobNotifications.assert.hasSize(2)

    acmeChiefNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(8) // 2 keys + 6 translations
      }

    projectManagerNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(8) // 2 keys + 6 translations
      }

    frenchTranslatorNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(6) // 2 keys + 4 translations
      }

    czechTranslatorNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(6) // 2 keys + 4 translations
      }

    germanTranslatorNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(4) // 2 keys + 2 translations
      }

    frenchCzechTranslatorNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(8) // 2 keys + 6 translations
      }

    bobNotifications.assert
      .noneSatisfy { it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED) }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_CREATED)
        it.modifiedEntities.assert.hasSize(8) // 2 keys + 6 translations
      }

    ensureNoUserNotificationDispatch()
  }

  @Test
  fun `it does remap complex key edits to relevant notification types`() {
    val screenshotId = performAuthMultipart(
      url = "/v2/image-upload",
      files = listOf(
        MockMultipartFile(
          "image", "image.png", "image/png",
          generateImage(100, 100).inputStream.readAllBytes()
        )
      )
    ).andIsCreated.andGetContentAsJsonMap["id"].let { (it as Int).toLong() }

    performAuthPut(
      "/v2/projects/${testData.project1.id}/keys/${testData.keyProject1.id}/complex-update",
      ComplexEditKeyDto(
        name = "new-name",
        namespace = "new-namespace",
        translations = mapOf("en" to "New EN string", "fr" to "New FR string"),
        screenshotsToAdd = listOf(
          KeyScreenshotDto(uploadedImageId = screenshotId)
        )
      )
    ).andIsOk

    waitUntilUserNotificationDispatch(25)
    val acmeChiefNotifications = userNotificationRepository.findAllByRecipient(testData.orgAdmin)
    val czechTranslatorNotifications = userNotificationRepository.findAllByRecipient(testData.czechTranslator)
    val bobNotifications = userNotificationRepository.findAllByRecipient(testData.bob)

    bobNotifications.assert
      .hasSize(3)
      .noneSatisfy {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_SCREENSHOTS_UPLOADED)
      }

    acmeChiefNotifications.assert
      .hasSize(4)
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_UPDATED)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_SCREENSHOTS_UPLOADED)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_TRANSLATIONS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }

    czechTranslatorNotifications.assert
      .hasSize(3)
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_UPDATED)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_KEYS_SCREENSHOTS_UPLOADED)
      }
      .satisfiesOnlyOnce {
        it.type.assert.isEqualTo(NotificationType.ACTIVITY_SOURCE_STRINGS_UPDATED)
        it.modifiedEntities.assert.hasSize(1)
      }

    ensureNoUserNotificationDispatch()
  }
}
