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
import io.tolgee.fixtures.andIsCreated
import io.tolgee.repository.NotificationsRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class NotificationDispatchTest : AuthorizedControllerTest() {
  lateinit var testData: NotificationsTestData

  @Autowired
  lateinit var notificationService: NotificationService

  @SpyBean
  @Autowired
  lateinit var notificationsRepository: NotificationsRepository

  lateinit var semaphore: Semaphore

  @BeforeEach
  fun setupEnvironment() {
    testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)

    semaphore = Semaphore(0)

    doAnswer {
      entityManager.persist(it.arguments[0])
      entityManager.flush()

      entityManager.refresh(it.arguments[0])
      semaphore.release()
      it.arguments[0]
    }.`when`(notificationsRepository).save(any())
  }

  @AfterEach
  fun clearEnvironment() {
    Mockito.reset(notificationsRepository)
  }

  @Test
  fun `it dispatches notifications to everyone in project`() {
    performAuthPost(
      "/v2/projects/${testData.project1.id}/keys/create",
      CreateKeyDto(name = "test-key")
    ).andIsCreated

    waitUntilNotificationDispatch(6)
    val adminNotifications = notificationsRepository.findAllByRecipient(testData.admin)
    val projectManagerNotifications = notificationsRepository.findAllByRecipient(testData.projectManager)
    val frenchTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchTranslator)
    val czechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.czechTranslator)
    val germanTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.germanTranslator)
    val frenchCzechTranslatorNotifications = notificationsRepository.findAllByRecipient(testData.frenchCzechTranslator)
    val bobNotifications = notificationsRepository.findAllByRecipient(testData.bob)
    val aliceNotifications = notificationsRepository.findAllByRecipient(testData.alice)

    adminNotifications.assert.hasSize(0)
    projectManagerNotifications.assert.hasSize(1)
    frenchTranslatorNotifications.assert.hasSize(1)
    czechTranslatorNotifications.assert.hasSize(1)
    germanTranslatorNotifications.assert.hasSize(1)
    frenchCzechTranslatorNotifications.assert.hasSize(1)
    bobNotifications.assert.hasSize(1)
    aliceNotifications.assert.hasSize(0)
  }

  private fun waitUntilNotificationDispatch(count: Int = 1) {
    val dispatched = semaphore.tryAcquire(count, 1L, TimeUnit.SECONDS)
    dispatched.assert
      .withFailMessage("Expected at least $count notification(s) to be dispatched.")
      .isTrue()
  }

  private fun ensureNoNotificationDispatch() {
    val dispatched = semaphore.tryAcquire(1L, TimeUnit.SECONDS)
    dispatched.assert
      .withFailMessage("Expected no notifications to be dispatched.")
      .isFalse()
  }
}
