package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.service.aiReview.AiReviewTextHash
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewTextHashAgreementTest : AbstractSpringTest() {
  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `AiReviewTextHash Kotlin, SQL and JPQL forms give the same hash`() {
    val unicode = testData.addKeyWithTranslations("k.unicode", deText = "Zürich – naïve 😀 {count}")
    val empty = testData.addKeyWithTranslations("k.empty", deText = "")
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      listOf(testData.deTranslation.id, unicode.de.self.id, empty.de.self.id).forEach { id ->
        val kotlinHash = AiReviewTextHash.of(translationService.get(id))
        val sqlHash =
          entityManager
            .createNativeQuery(
              "select ${AiReviewTextHash.CURRENT_TEXT_SQL} from translation t where t.id = :id",
            ).setParameter("id", id)
            .singleResult as String
        val jpqlHash =
          entityManager
            .createQuery(
              "select ${AiReviewTextHash.CURRENT_TEXT_JPQL} from Translation t where t.id = :id",
              String::class.java,
            ).setParameter("id", id)
            .singleResult
        sqlHash.assert.isEqualTo(kotlinHash)
        jpqlHash.assert.isEqualTo(kotlinHash)
      }
    }
  }
}
