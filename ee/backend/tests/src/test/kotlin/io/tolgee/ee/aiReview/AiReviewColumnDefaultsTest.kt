package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Feature
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewColumnDefaultsTest : AbstractSpringTest() {
  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `AI_REVIEW is the last feature`() {
    Feature.entries
      .last()
      .assert
      .isEqualTo(Feature.AI_REVIEW)
  }
}
