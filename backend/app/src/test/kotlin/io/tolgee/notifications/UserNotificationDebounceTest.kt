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
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserNotificationDebounceTest : AbstractNotificationTest() {
  lateinit var testData: NotificationsTestData

  @BeforeEach
  override fun setupTests() {
    testData = NotificationsTestData()
    testDataService.saveTestData(testData.root)

    super.setupTests()
  }

  @Test
  fun `it debounces notifications of the same type`() {
    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/keys/create",
      CreateKeyDto(name = "test-key-1")
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)

    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/keys/create",
      CreateKeyDto(name = "test-key-2")
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)

    performAuthPost(
      url = "/v2/projects/${testData.calmProject.id}/languages",
      content = LanguageDto(
        name = "Meow",
        originalName = "meow",
        tag = "meow-en",
      )
    ).andIsOk

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(2)
  }

  @Test
  fun `it only debounces notifications within the same project`() {
    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/keys/create",
      CreateKeyDto(name = "test-key-1")
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)

    performAuthPost(
      "/v2/projects/${testData.project2.id}/keys/create",
      CreateKeyDto(name = "test-key-2")
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(2)
  }

  @Test
  fun `it debounces comments only when they are under the same translation`() {
    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.keyCalmProject.id,
        languageId = testData.keyCalmEnTranslation.language.id,
        text = "This is a test"
      )
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)

    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.keyCalmProject.id,
        languageId = testData.keyCalmEnTranslation.language.id,
        text = "This is a test 2"
      )
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(1)

    performAuthPost(
      "/v2/projects/${testData.calmProject.id}/translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.keyCalmProject.id,
        languageId = testData.calmProjectFr.id,
        text = "This is a test"
      )
    ).andIsCreated

    waitUntilUserNotificationDispatch()
    userNotificationRepository.findAllByRecipient(testData.alice).assert.hasSize(2)
  }
}
