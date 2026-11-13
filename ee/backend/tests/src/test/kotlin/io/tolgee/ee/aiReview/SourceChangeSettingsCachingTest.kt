package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
@AutoConfigureMockMvc
class SourceChangeSettingsCachingTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `PUT evicts the cached project so the new settings apply at once`() {
    projectService.getDto(testData.project.id)

    performAuthPut(
      "/v2/projects/${testData.project.id}/source-change-settings",
      mapOf(
        "aiApproved" to "MARK_OUTDATED",
        "humanEdited" to "RETRANSLATE",
        "humanReviewed" to "RETRANSLATE",
      ),
    ).andIsOk

    val cached = projectService.getDto(testData.project.id)
    cached.humanEditedSourceChangeAction.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
    cached.aiApprovedSourceChangeAction.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
  }
}
