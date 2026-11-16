package io.tolgee.api.v2.controllers.notification

import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.dtos.request.notification.NotificationLanguagesRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationLanguagesControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun putLanguages(languageIds: List<Long>?) =
    performAuthPut(
      "/v2/notification-settings/languages/${testData.project.id}",
      NotificationLanguagesRequest(languageIds),
    )

  private fun getLanguages() = performAuthGet("/v2/notification-settings/languages").andIsOk

  @Test
  fun `lists followable non-base languages with the default`() {
    userAccount = testData.reviewerAll
    getLanguages().andAssertThatJson {
      isArray.hasSize(1)
      node("[0].projectId").isEqualTo(testData.project.id)
      node("[0].projectName").isEqualTo("Notification project")
      node("[0].availableLanguages").isArray.hasSize(2)
      node("[0].availableLanguages[0].tag").isEqualTo("de")
      node("[0].availableLanguages[1].tag").isEqualTo("fr")
      node("[0].availableLanguages[0].flagEmoji").isEqualTo("🇩🇪")
      node("[0].followedLanguageIds").isEqualTo(listOf(testData.german.id, testData.french.id))
      node("[0].state").isEqualTo("DEFAULT")
    }
  }

  @Test
  fun `restricted user sees only their languages`() {
    userAccount = testData.reviewerFr
    getLanguages().andAssertThatJson {
      node("[0].availableLanguages").isArray.hasSize(1)
      node("[0].availableLanguages[0].tag").isEqualTo("fr")
      node("[0].followedLanguageIds").isEqualTo(listOf(testData.french.id))
    }
  }

  @Test
  fun `viewer has no project to set up`() {
    userAccount = testData.viewerDe
    getLanguages().andAssertThatJson { isArray.isEmpty() }
  }

  @Test
  fun `chooses, mutes and resets to default`() {
    userAccount = testData.reviewerAll
    putLanguages(listOf(testData.german.id)).andIsOk
    getLanguages().andAssertThatJson {
      node("[0].followedLanguageIds").isEqualTo(listOf(testData.german.id))
      node("[0].state").isEqualTo("CHOSEN")
    }
    putLanguages(emptyList()).andIsOk
    getLanguages().andAssertThatJson {
      node("[0].followedLanguageIds").isArray.isEmpty()
      node("[0].state").isEqualTo("MUTED")
    }
    putLanguages(null).andIsOk
    getLanguages().andAssertThatJson {
      node("[0].followedLanguageIds").isEqualTo(listOf(testData.german.id, testData.french.id))
      node("[0].state").isEqualTo("DEFAULT")
    }
  }

  @Test
  fun `rejects the base language`() {
    userAccount = testData.reviewerAll
    putLanguages(listOf(testData.englishLanguage.id)).andIsBadRequest
    getLanguages().andAssertThatJson { node("[0].state").isEqualTo("DEFAULT") }
  }

  @Test
  fun `rejects a language the user cannot edit or review`() {
    userAccount = testData.reviewerFr
    putLanguages(listOf(testData.french.id, testData.german.id)).andIsBadRequest
    getLanguages().andAssertThatJson { node("[0].state").isEqualTo("DEFAULT") }
  }

  @Test
  fun `non-member cannot set a choice`() {
    userAccount = testData.outsider
    putLanguages(listOf(testData.french.id)).andIsNotFound
  }
}
