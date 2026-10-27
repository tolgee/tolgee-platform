package io.tolgee.ee.aiReview

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.SystemActor
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.websocket.ActivityWebsocketListener
import io.tolgee.websocket.ActorType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewActivityActorTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var activityWebsocketListener: ActivityWebsocketListener

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `activity list shows the system actor without an author`() {
    saveRevision(systemActor = SystemActor.AI_REVIEWER)
    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].systemActor").isEqualTo("AI_REVIEWER")
      node("_embedded.activities[0].author").isNull()
    }
  }

  @Test
  fun `activity list shows the system actor and the author for a manual run`() {
    saveRevision(systemActor = SystemActor.AI_REVIEWER, withAuthor = true)
    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].systemActor").isEqualTo("AI_REVIEWER")
      node("_embedded.activities[0].author.id").isEqualTo(testData.user.id)
    }
  }

  @Test
  fun `activity without a system actor returns null`() {
    saveRevision(systemActor = null, withAuthor = true)
    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].systemActor").isNull()
    }
  }

  @Test
  fun `translation history shows the system actor even without an author`() {
    saveRevision(systemActor = SystemActor.AI_REVIEWER)
    performAuthGet("$projectUrl/translations/${testData.deTranslation.id}/history")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.revisions[0].systemActor").isEqualTo("AI_REVIEWER")
        node("_embedded.revisions[0].author").isNull()
      }
  }

  @Test
  fun `translation history keeps the author when both are set`() {
    saveRevision(systemActor = SystemActor.AI_REVIEWER, withAuthor = true)
    performAuthGet("$projectUrl/translations/${testData.deTranslation.id}/history")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.revisions[0].systemActor").isEqualTo("AI_REVIEWER")
        node("_embedded.revisions[0].author.id").isEqualTo(testData.user.id)
      }
  }

  @Test
  fun `the websocket actor is MACHINE without user data for a system actor`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    val machine = activityWebsocketListener.getActorInfo(testData.user.id, SystemActor.AI_REVIEWER)
    machine.type.assert.isEqualTo(ActorType.MACHINE)
    machine.data.assert.isNull()

    val human = activityWebsocketListener.getActorInfo(testData.user.id)
    human.type.assert.isEqualTo(ActorType.USER)
  }

  private fun saveRevision(
    systemActor: SystemActor?,
    withAuthor: Boolean = false,
  ) {
    testData.projectBuilder
      .addActivityRevision {
        type = ActivityType.SET_TRANSLATIONS
        this.systemActor = systemActor
      }.also { builder ->
        if (withAuthor) builder.authorIdProvider = { testData.user.id }
        builder.addModifiedEntity("Translation") { testData.deTranslation.id }
      }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }
}
