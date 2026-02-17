package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class QaCheckPreviewControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = BaseTestData()
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
  fun `returns empty translation issue for blank text`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.hasSize(1)
      node("_embedded.qaCheckResults[0].type").isEqualTo("EMPTY_TRANSLATION")
      node("_embedded.qaCheckResults[0].message").isEqualTo("qa_empty_translation")
      node("_embedded.qaCheckResults[0].replacement").isNull()
      node("_embedded.qaCheckResults[0].positionStart").isEqualTo(0)
      node("_embedded.qaCheckResults[0].positionEnd").isEqualTo(0)
    }
  }

  @Test
  fun `returns no issues for non-empty text`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "Hello world",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `returns bad request when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsBadRequest
  }
}
