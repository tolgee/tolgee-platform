package io.tolgee.ee.aiReview

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.ee.data.aiReview.AiReviewSettingsRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.SourceChangeAction
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiReviewSourceChangeTest : AbstractAiReviewTest() {
  @BeforeEach
  fun enableAutoTranslation() {
    initMachineTranslationProperties(-1, enabledServices = setOf(MtServiceType.GOOGLE))
    internalProperties.fakeMtProviders = true
  }

  @AfterEach
  fun disableFakeMt() {
    internalProperties.fakeMtProviders = false
  }

  @Test
  fun `AI approval keeps the machine translation mark`() {
    testData.enableAutoTranslation()
    testData.addRule { tagIn = mutableListOf("ui") }
    saveTestData()

    performAuthPost(
      "/v2/projects/$projectId/keys",
      CreateKeyDto(name = "new-ui-key", translations = mapOf("en" to "Save changes"), tags = listOf("ui")),
    ).andIsCreated
    val translationId = waitForGermanTranslationOf("new-ui-key") { it.text.assert.contains("translated with") }.id

    waitForReview(translationId) { it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED) }
    val translation = getTranslation(translationId)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    translation.auto.assert.isTrue()
    translation.mtProvider.assert.isNotNull
  }

  @Test
  fun `source change translates an AI approved machine translation again and reviews it again`() {
    testData.enableAutoTranslation()
    testData.addRule { tagIn = mutableListOf("ui") }
    testData.uiGerman.approvedByAi()
    saveTestData()

    setText("ui-key", "en", "Pay later")

    val translation = waitForGermanTranslationOf("ui-key") { it.text.assert.contains("Pay later") }
    waitForReview(translation.id) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
      it.textHash.assert.isEqualTo(aiReviewTextHash(translation.text))
    }
    getTranslation(translation.id).auto.assert.isTrue()
  }

  @Test
  fun `translation of a changed source is not reviewed until the AI or a human reviews it`() {
    assertNewMachineTranslationIsNotReviewed(TranslationProtection.NONE)
  }

  @Test
  fun `translation of a changed source is not reviewed with reviewed translations protected`() {
    assertNewMachineTranslationIsNotReviewed(TranslationProtection.PROTECT_REVIEWED)
  }

  @Test
  fun `keeps an AI approved translation when the project says so`() {
    testData.enableAutoTranslation()
    testData.uiGerman.approvedByAi()
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeAiApproved = SourceChangeAction.KEEP))

    setText("ui-key", "en", "Pay later")
    waitForProjectJobsCompleted()

    val translation = getTranslation(testData.uiGerman.self.id)
    translation.text.assert.isEqualTo("Jetzt zahlen")
    translation.outdated.assert.isTrue()
    translation.auto.assert.isFalse()
  }

  @Test
  fun `keeps translations edited by a human by default`() {
    testData.enableAutoTranslation()
    saveTestData()

    setText("ui-key", "en", "Pay later")
    waitForProjectJobsCompleted()

    val translation = getTranslation(testData.uiGerman.self.id)
    translation.text.assert.isEqualTo("Jetzt zahlen")
    translation.outdated.assert.isTrue()
  }

  @Test
  fun `translates a translation edited by a human again when the project says so`() {
    testData.enableAutoTranslation()
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeHumanEdited = SourceChangeAction.RETRANSLATE))

    setText("ui-key", "en", "Pay later")

    waitForGermanTranslationOf("ui-key") { it.text.assert.contains("Pay later") }
  }

  @Test
  fun `translates a translation reviewed by a human again when the project says so`() {
    testData.enableAutoTranslation()
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.uiGerman.self.state = TranslationState.REVIEWED
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeHumanReviewed = SourceChangeAction.RETRANSLATE))

    setText("ui-key", "en", "Pay later")

    val translation = waitForGermanTranslationOf("ui-key") { it.text.assert.contains("Pay later") }
    translation.state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `ignores the settings when AI review is not available`() {
    testData.enableAutoTranslation()
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeHumanEdited = SourceChangeAction.RETRANSLATE))
    tolgeeProperties.featureFlags.enabledForAll = emptyList()

    setText("ui-key", "en", "Pay later")
    waitForProjectJobsCompleted()

    val translation = getTranslation(testData.uiGerman.self.id)
    translation.text.assert.isEqualTo("Jetzt zahlen")
    translation.outdated.assert.isTrue()
  }

  @Test
  fun `a human translation approved by AI follows the human edited setting`() {
    testData.enableAutoTranslation()
    testData.uiGerman.approvedByAi()
    testData.uiGerman.self.auto = false
    testData.uiGerman.self.mtProvider = null
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeHumanEdited = SourceChangeAction.RETRANSLATE))

    setText("ui-key", "en", "Pay later")

    waitForGermanTranslationOf("ui-key") { it.text.assert.contains("Pay later") }
  }

  @Test
  fun `demoting an AI approval keeps the machine translation mark`() {
    val rule = testData.addRule { tagIn = mutableListOf("ui") }
    testData.uiGerman.approvedByAi()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "/v2/projects/$projectId/ai-review/rules/${rule.self.id}",
      AiReviewRuleRequest(tagIn = listOf("ui"), mode = AiReviewRuleMode.ACTIVE, threshold = 100),
    ).andIsOk

    waitForReview(translationId) { it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD) }
    val translation = getTranslation(translationId)
    translation.state.assert.isEqualTo(TranslationState.TRANSLATED)
    translation.auto.assert.isTrue()
  }

  @Test
  fun `keeps human translations of languages without machine translation`() {
    saveTestData()
    updateSettings(AiReviewSettingsRequest(onSourceChangeHumanEdited = SourceChangeAction.RETRANSLATE))

    setText("ui-key", "en", "Pay later")
    waitForProjectJobsCompleted()

    val translation = getTranslation(testData.uiGerman.self.id)
    translation.text.assert.isEqualTo("Jetzt zahlen")
    translation.auto.assert.isFalse()
  }

  @Test
  fun `import applies the source change settings to machine text only`() {
    testData.enableAutoTranslation()
    testData.uiGerman.approvedByAi()
    testData.uiCzech.self.state = TranslationState.REVIEWED
    saveTestData()
    updateSettings(
      AiReviewSettingsRequest(
        onSourceChangeAiApproved = SourceChangeAction.KEEP,
        onSourceChangeHumanReviewed = SourceChangeAction.RETRANSLATE,
      ),
    )

    executeInNewTransaction(platformTransactionManager) {
      translationService.applySourceChangeOfKeys(projectId, listOf(testData.uiKey.self.id))
    }

    val german = getTranslation(testData.uiGerman.self.id)
    german.outdated.assert.isTrue()
    german.auto.assert.isFalse()
    german.state.assert.isEqualTo(TranslationState.REVIEWED)
    val czech = getTranslation(testData.uiCzech.self.id)
    czech.outdated.assert.isTrue()
    czech.auto.assert.isFalse()
    czech.state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `confirming an AI approval removes the machine translation mark`() {
    testData.uiGerman.approvedByAi()
    saveTestData()

    performAuthPost("/v2/projects/$projectId/ai-review/translations/${testData.uiGerman.self.id}/confirm", null)
      .andIsOk

    val translation = getTranslation(testData.uiGerman.self.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    translation.auto.assert.isFalse()
  }

  private fun assertNewMachineTranslationIsNotReviewed(protection: TranslationProtection) {
    testData.enableAutoTranslation()
    testData.project.translationProtection = protection
    testData.addRule {
      tagIn = mutableListOf("ui")
      mode = AiReviewRuleMode.DRY_RUN
    }
    testData.uiGerman.approvedByAi()
    saveTestData()

    setText("ui-key", "en", "Pay later")

    val translation = waitForGermanTranslationOf("ui-key") { it.text.assert.contains("Pay later") }
    waitForReview(translation.id) { it!!.outcome.assert.isEqualTo(AiReviewOutcome.WOULD_AUTO_REVIEW) }
    getTranslation(translation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  private fun TranslationBuilder.approvedByAi() {
    val text = self.text
    self.auto = true
    self.mtProvider = MtServiceType.GOOGLE
    self.state = TranslationState.REVIEWED
    addAiReview {
      outcome = AiReviewOutcome.AUTO_REVIEWED
      textHash = aiReviewTextHash(text)
      score = 95
      threshold = 85
    }
  }

  private fun updateSettings(request: AiReviewSettingsRequest) {
    performAuthPut("/v2/projects/$projectId/ai-review/settings", request).andIsOk
  }

  private fun waitForGermanTranslationOf(
    keyName: String,
    assertion: (Translation) -> Unit,
  ): Translation {
    var result: Translation? = null
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      executeInNewTransaction(platformTransactionManager) {
        val key = keyService.get(projectId, keyName, null)
        val translation = translationService.find(key, testData.german.self).orElse(null)
        translation.assert.isNotNull()
        assertion(translation!!)
        result = translation
      }
    }
    return result!!
  }
}
