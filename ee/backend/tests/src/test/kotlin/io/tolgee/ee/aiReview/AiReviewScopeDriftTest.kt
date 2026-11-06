package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.dtos.request.key.UpdateNamespaceDto
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewScopeDriftTest : AbstractSpringTest() {
  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

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
  fun `deleting the only scoped language disables the rule`() {
    testData.rule.languages = mutableSetOf(testData.german)
    testDataService.saveTestData(testData.root)

    deleteLanguage(testData.german.id)

    loadRule(testData.rule.id).let {
      it.enabled.assert.isFalse()
      it.languages.assert.isEmpty()
    }
  }

  @Test
  fun `deleting one of two scoped languages keeps the rule enabled`() {
    testData.rule.languages = mutableSetOf(testData.german, testData.french)
    testDataService.saveTestData(testData.root)

    deleteLanguage(testData.german.id)

    loadRule(testData.rule.id).let {
      it.enabled.assert.isTrue()
      it.languages
        .map { language -> language.tag }
        .assert
        .containsExactly("fr")
    }
  }

  @Test
  fun `rule with no languages stays enabled when a language is deleted`() {
    testDataService.saveTestData(testData.root)

    deleteLanguage(testData.german.id)

    loadRule(testData.rule.id).enabled.assert.isTrue()
  }

  @Test
  fun `deleting a language removes its override`() {
    testData.ruleBuilder.addOverride(testData.german, threshold = 95)
    testData.ruleBuilder.addOverride(testData.french, threshold = 90)
    testDataService.saveTestData(testData.root)

    deleteLanguage(testData.german.id)

    loadRule(testData.rule.id)
      .overrides
      .map { it.language.tag }
      .assert
      .containsExactly("fr")
  }

  @Test
  fun `renaming a namespace updates namespaceIn and namespaceNotIn`() {
    testData.addKeyWithTranslations("k.checkout").keyBuilder.setNamespace("checkout")
    testData.rule.namespaceIn = mutableListOf("checkout", "")
    val other = testData.addRule { namespaceNotIn = mutableListOf("checkout") }
    testDataService.saveTestData(testData.root)

    renameNamespace("checkout", "cart")

    loadRule(testData.rule.id).namespaceIn.assert.containsExactly("cart", "")
    loadRule(other.self.id).namespaceNotIn.assert.containsExactly("cart")
  }

  @Test
  fun `renaming to a name already in the list leaves one entry`() {
    testData.addKeyWithTranslations("k.checkout").keyBuilder.setNamespace("checkout")
    testData.rule.namespaceIn = mutableListOf("checkout", "cart")
    testDataService.saveTestData(testData.root)

    renameNamespace("checkout", "cart")

    loadRule(testData.rule.id).namespaceIn.assert.containsExactly("cart")
  }

  private fun deleteLanguage(languageId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      languageService.deleteLanguage(languageId)
    }
  }

  private fun renameNamespace(
    oldName: String,
    newName: String,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val namespace = namespaceService.find(oldName, testData.project.id)!!
      namespaceService.update(namespace, UpdateNamespaceDto().apply { name = newName })
    }
  }

  private fun loadRule(id: Long): AiReviewRule =
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.findById(id).get().also {
        it.languages.size
        it.overrides.forEach { override -> override.language.tag }
      }
    }
}
