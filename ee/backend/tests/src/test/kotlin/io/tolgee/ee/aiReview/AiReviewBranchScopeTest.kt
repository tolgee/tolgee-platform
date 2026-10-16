package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.data.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.model.branching.Branch
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiReviewBranchScopeTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var triggerService: AiReviewTriggerService

  private lateinit var featureKey: KeyBuilder

  override fun createTestData() =
    AiReviewTestData().apply {
      projectBuilder.apply {
        self.useBranching = true
        lateinit var main: Branch
        addBranch {
          name = "main"
          project = self
          isDefault = true
        }.build { main = self }
        lateinit var feature: Branch
        addBranch {
          name = "feature/checkout"
          project = self
          originBranch = main
        }.build { feature = self }
        featureKey =
          addKey {
            name = "feature-key"
            branch = feature
          }.build {
            addTranslation("en", "Pay")
            addTranslation("de", "Zahlen")
          }
      }
    }

  @Test
  fun `key scopes carry the branch name and the default branch for keys without one`() {
    saveTestData()
    val scopes =
      executeInNewTransaction(platformTransactionManager) {
        triggerService.getKeyScopes(listOf(testData.uiKey.self.id, featureKey.self.id))
      }
    scopes[testData.uiKey.self.id]!!.branch.assert.isEqualTo("main")
    scopes[featureKey.self.id]!!.branch.assert.isEqualTo("feature/checkout")
  }
}
