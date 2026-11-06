package io.tolgee.ee.aiReview

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import java.security.MessageDigest

@SpringBootTest
@AutoConfigureMockMvc
class SourceChangeBehaviourTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          name = "server-provider",
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
          model = "gpt-test",
        ),
      )
    internalProperties.fakeLlmProviders = true
    initMachineTranslationProperties(-1)
    internalProperties.fakeMtProviders = true
    testData = AiReviewTestData()
    testData.projectBuilder.addAutoTranslationConfig {
      targetLanguage = testData.german
      usingPrimaryMtService = true
    }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
    internalProperties.fakeLlmProviders = false
    internalProperties.fakeMtProviders = false
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `a machine translated text is translated again`() {
    testData.deTranslation.auto = true
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      de.auto.assert.isTrue()
    }
  }

  @Test
  fun `an AI approval is translated again and reviewed by default`() {
    aiApproval()
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      val row = review(testData.deTranslation.id)
      row?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row!!.textHash.assert.isEqualTo(sha256Hex(de.text!!))
      de.state.assert.isEqualTo(TranslationState.REVIEWED)
    }
  }

  @Test
  fun `an AI approval with Keep is marked outdated, loses auto and keeps its text`() {
    setAiApprovedAction(SourceChangeAction.MARK_OUTDATED)
    aiApproval()
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.outdated.assert.isTrue()
      de.state.assert.isEqualTo(TranslationState.TRANSLATED)
      de.auto.assert.isFalse()
      review(testData.deTranslation.id).assert.isNull()
    }
    Thread.sleep(2000)
    translation(testData.deTranslation.id).text.assert.isEqualTo("Jetzt bezahlen")
  }

  @Test
  fun `under Protect reviewed an AI approval stays REVIEWED and is translated and reviewed again`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    setAiApprovedAction(SourceChangeAction.MARK_OUTDATED)
    aiApproval()
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      de.state.assert.isEqualTo(TranslationState.REVIEWED)
      de.auto.assert.isTrue()
      val row = review(testData.deTranslation.id)
      row?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row!!.textHash.assert.isEqualTo(sha256Hex(de.text!!))
    }
  }

  @Test
  fun `a bulk-reviewed text with auto is replaced like in main`() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      translation(testData.deTranslation.id).text.assert.isNotEqualTo("Jetzt bezahlen")
    }
  }

  @Test
  fun `under Protect reviewed a bulk-reviewed text with auto stays REVIEWED and is replaced`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      de.state.assert.isEqualTo(TranslationState.REVIEWED)
    }
  }

  @Test
  fun `a human text approved by the AI follows the human-edited setting`() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.outdated.assert.isTrue()
      de.state.assert.isEqualTo(TranslationState.TRANSLATED)
      review(testData.deTranslation.id).assert.isNull()
    }
    Thread.sleep(2000)
    translation(testData.deTranslation.id).text.assert.isEqualTo("Jetzt bezahlen")
  }

  @Test
  fun `a human-edited text with RETRANSLATE is replaced and reviewed`() {
    setHumanEditedAction(SourceChangeAction.RETRANSLATE)
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      review(testData.deTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
  }

  @Test
  fun `a human-edited text is kept and marked outdated by default`() {
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      val de = translation(testData.deTranslation.id)
      de.outdated.assert.isTrue()
      de.auto.assert.isFalse()
    }
    Thread.sleep(2000)
    translation(testData.deTranslation.id).text.assert.isEqualTo("Jetzt bezahlen")
  }

  @Test
  fun `a human-reviewed text with RETRANSLATE is replaced without protection`() {
    setHumanReviewedAction(SourceChangeAction.RETRANSLATE)
    testData.deTranslation.state = TranslationState.REVIEWED
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      translation(testData.deTranslation.id).text.assert.isNotEqualTo("Jetzt bezahlen")
    }
  }

  @Test
  fun `under Protect reviewed a human-reviewed text is kept and marked outdated despite RETRANSLATE`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    setHumanReviewedAction(SourceChangeAction.RETRANSLATE)
    testData.deTranslation.state = TranslationState.REVIEWED
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      val de = translation(testData.deTranslation.id)
      de.outdated.assert.isTrue()
      de.state.assert.isEqualTo(TranslationState.REVIEWED)
    }
    Thread.sleep(2000)
    translation(testData.deTranslation.id).text.assert.isEqualTo("Jetzt bezahlen")
  }

  @Test
  fun `RETRANSLATE falls back to Keep when the language has no auto translation`() {
    setHumanEditedAction(SourceChangeAction.RETRANSLATE)
    save()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      val fr = translation(testData.frTranslation.id)
      fr.outdated.assert.isTrue()
      fr.auto.assert.isFalse()
    }
    Thread.sleep(2000)
    translation(testData.frTranslation.id).text.assert.isEqualTo("Payer maintenant")
  }

  @Test
  fun `with AI review off an AI approval behaves like today and loses its row`() {
    aiApproval()
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    changeBaseText()

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = translation(testData.deTranslation.id)
      de.text.assert.isNotEqualTo("Jetzt bezahlen")
      review(testData.deTranslation.id).assert.isNull()
    }
  }

  private fun aiApproval() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      rule = testData.rule
      threshold = 85
    }
  }

  private fun setAiApprovedAction(action: SourceChangeAction) {
    testData.project.aiApprovedSourceChangeAction = action
  }

  private fun setHumanEditedAction(action: SourceChangeAction) {
    testData.project.humanEditedSourceChangeAction = action
  }

  private fun setHumanReviewedAction(action: SourceChangeAction) {
    testData.project.humanReviewedSourceChangeAction = action
  }

  private fun changeBaseText() {
    performAuthPut(
      "$projectUrl/translations",
      mapOf("key" to "checkout.pay", "translations" to mapOf("en" to "Pay immediately")),
    ).andIsOk
  }

  private fun sha256Hex(text: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(text.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }

  private fun translation(id: Long): Translation =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }
}
