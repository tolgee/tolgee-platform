package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.ee.data.AiPlaygroundResultRequest
import io.tolgee.ee.repository.AiPlaygroundResultRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.PromptType
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiReviewPlaygroundTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var aiPlaygroundResultRepository: AiPlaygroundResultRepository

  @Autowired
  lateinit var batchProperties: BatchProperties

  private fun startPlayground(type: PromptType) =
    performAuthPost(
      "/v2/projects/$projectId/start-batch-job/ai-playground-translate",
      MachineTranslationRequest().apply {
        keyIds = listOf(testData.uiKey.self.id, testData.legalKey.self.id)
        targetLanguageIds = listOf(testData.german.self.id)
        llmPrompt = PromptDto(name = "", providerName = "default", type = type)
      },
    )

  private fun getResults(type: PromptType) =
    performAuthPost(
      "/v2/projects/$projectId/ai-playground-result",
      AiPlaygroundResultRequest(
        keys = listOf(testData.uiKey.self.id, testData.legalKey.self.id),
        languages = listOf(testData.german.self.id),
        type = type,
      ),
    )

  @Test
  fun `review preview stores review results`() {
    testData.legalGerman.self.text = "Bedingungen FAKE_AI_REVIEW_MAJOR"
    saveTestData()

    startPlayground(PromptType.REVIEW).andIsOk
    waitForProjectJobsCompleted()

    getResults(PromptType.REVIEW).andIsOk.andAssertThatJson {
      node("_embedded.results").isArray.hasSize(2)
      node("_embedded.results[0].type").isEqualTo("REVIEW")
    }
    val results = executeInNewTransaction { aiPlaygroundResults() }
    results.assert.hasSize(2)
    results.map { it.type }.assert.containsOnly(PromptType.REVIEW)
    results
      .first { it.key!!.id == testData.uiKey.self.id }
      .reviewScore.assert
      .isEqualTo(100)
    val legal = results.first { it.key!!.id == testData.legalKey.self.id }
    legal.reviewScore.assert.isEqualTo(75)
    legal.reviewIssues!!
      .single()
      .points.assert
      .isEqualTo(25)
    getResults(PromptType.TRANSLATE).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `translate preview is unaffected`() {
    saveTestData()

    startPlayground(PromptType.TRANSLATE).andIsOk
    waitForProjectJobsCompleted()

    getResults(PromptType.TRANSLATE).andIsOk.andAssertThatJson {
      node("_embedded.results").isArray.hasSize(2)
      node("_embedded.results[0].translation").isEqualTo("response from: default")
      node("_embedded.results[0].type").isEqualTo("TRANSLATE")
      node("_embedded.results[0].reviewScore").isNull()
    }
    getResults(PromptType.REVIEW).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
    jobsOfType(BatchJobType.AI_PLAYGROUND_TRANSLATE).assert.hasSize(1)
  }

  @Test
  fun `preview runs with the machine translation per-job concurrency`() {
    saveTestData()
    val original = batchProperties.maxPerMtJobConcurrency
    batchProperties.maxPerMtJobConcurrency = 4
    try {
      startPlayground(PromptType.REVIEW).andIsOk
      waitForProjectJobsCompleted()
    } finally {
      batchProperties.maxPerMtJobConcurrency = original
    }
    jobsOfType(BatchJobType.AI_PLAYGROUND_TRANSLATE)
      .single()
      .maxPerJobConcurrency.assert
      .isEqualTo(4)
  }

  @Test
  fun `review preview is hidden when the feature flag is off`() {
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = emptyList()

    startPlayground(PromptType.REVIEW).andIsNotFound
    getResults(PromptType.REVIEW).andIsNotFound
    jobsOfType(BatchJobType.AI_PLAYGROUND_TRANSLATE).assert.isEmpty()
  }

  private fun aiPlaygroundResults() =
    aiPlaygroundResultRepository
      .findAll()
      .filter { it.project.id == projectId }
      .onEach { it.key?.id }
}
