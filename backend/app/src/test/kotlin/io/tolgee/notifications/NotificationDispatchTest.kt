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
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class NotificationDispatchTest : AbstractNotificationTest() {
  lateinit var testData: NotificationsTestData

  @BeforeEach
  fun createTestData() {
    testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `it dispatches notifications to everyone in project`() {
    performAuthPost(
      "/v2/projects/${testData.project1.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilNotificationDispatch(7)
    val adminNotifications = notificationsRepository.findAllByRecipient(testData.admin)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)
    val aliceNotifications = notificationsRepository.findAllByRecipient(testData.alice)

    adminNotifications.assert.hasSize(0)
    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)
    aliceNotifications.assert.hasSize(0)

    ensureNoNotificationDispatch()
  }

  @Test
  fun `it does not dispatch notifications to people without the permission to see the change`() {
    performAuthMultipart(
      url = "/v2/projects/${testData.project1.id}/keys/${testData.keyProject1.id}/screenshots",
      files = listOf(
        MockMultipartFile(
          "screenshot", "originalShot.png", "image/png",
          generateImage(100, 100).inputStream.readAllBytes()
        )
      )
    ).andIsCreated

    waitUntilNotificationDispatch(6)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)

    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(0)

    ensureNoNotificationDispatch()
  }

  @Test
  fun `it does not dispatch notifications to people without the permission to see the target language`() {
    performAuthPut(
      url = "/v2/projects/${testData.project1.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = testData.keyProject1.name,
        translations = mapOf("fr" to "Superb French translation!")
      )
    ).andIsOk

    waitUntilNotificationDispatch(5)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)

    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(0)
    germanTranslatorNotifications.assert.hasSize(0)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)

    ensureNoNotificationDispatch()
  }

  @Test
  fun `it does dispatch notifications with trimmed data to people who can only see part of the changes`() {
    performAuthPut(
      url = "/v2/projects/${testData.project1.id}/translations",
      content = SetTranslationsWithKeyDto(
        key = testData.keyProject1.name,
        translations = mapOf("fr" to "Superb French translation!", "cs" to "Superb Czech translation!")
      )
    ).andIsOk

    waitUntilNotificationDispatch(6)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)

    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(0)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)

    acmeChiefNotifications[0].activityModifiedEntities.assert.hasSize(2)
    projectManagerNotifications[0].activityModifiedEntities.assert.hasSize(2)
    frenchTranslatorNotifications[0].activityModifiedEntities.assert.hasSize(1)
    czechTranslatorNotifications[0].activityModifiedEntities.assert.hasSize(1)
    frenchCzechTranslatorNotifications[0].activityModifiedEntities.assert.hasSize(2)
    bobNotifications[0].activityModifiedEntities.assert.hasSize(2)

    frenchTranslatorNotifications[0].activityModifiedEntities.first()
      .entityId.assert.isEqualTo(testData.key1FrTranslation.id)

    czechTranslatorNotifications[0].activityModifiedEntities.first()
      .entityId.assert.isEqualTo(testData.key1CzTranslation.id)

    ensureNoNotificationDispatch()
  }

  @Test
  fun `it does properly compute metadata for import-related notifications`() {
    performAuthMultipart(
      url = "/v2/projects/${testData.project1.id}/import",
      files = listOf(
        MockMultipartFile(
          "files", "en.json", "application/json",
          """ {"new-key1": "New string 1", "new-key2": "New string 2"} """.toByteArray()
        ),
        MockMultipartFile(
          "files", "fr.json", "application/json",
          """ {"new-key1": "New FR string 1", "new-key2": "New FR string 2"} """.toByteArray()
        ),
        MockMultipartFile(
          "files", "cs.json", "application/json",
          """ {"new-key1": "New CZ string 1", "new-key2": "New CZ string 2"} """.toByteArray()
        )
      )
    ).andIsOk

    performAuthPut("/v2/projects/${testData.project1.id}/import/apply", null).andIsOk

    waitUntilNotificationDispatch(7)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)

    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)

    acmeChiefNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 6)
    )
    projectManagerNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 6)
    )
    frenchTranslatorNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 4)
    )
    czechTranslatorNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 4)
    )
    germanTranslatorNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 2)
    )
    frenchCzechTranslatorNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 6)
    )
    bobNotifications[0].meta.assert.isEqualTo(
      mapOf("keysCount" to 2, "translationsCount" to 6)
    )

    ensureNoNotificationDispatch()
  }

  @Test
  fun `it does not dispatch modifications to the responsible user`() {
    loginAsUser(testData.projectManager)
    performAuthPost(
      "/v2/projects/${testData.project1.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilNotificationDispatch(6)
    val adminNotifications = notificationsRepository.findAllByRecipient(testData.admin)
    val acmeChiefNotifications = notificationsRepository.findAllByRecipient(testData.orgAdmin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)
    val aliceNotifications = notificationsRepository.findAllByRecipient(testData.alice)

    adminNotifications.assert.hasSize(0)
    acmeChiefNotifications.assert.hasSize(1)
    projectManagerNotifications.assert.hasSize(0)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)
    aliceNotifications.assert.hasSize(0)

    ensureNoNotificationDispatch()
  }
}
