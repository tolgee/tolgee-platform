package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.constants.Message
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@Import(BatchJobBaseConfiguration::class)
class BatchMtTranslatePartialFailureTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    private const val FAILING_KEY_NAME = "key2"
  }

  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  @Autowired
  @MockitoSpyBean
  lateinit var googleTranslationProvider: GoogleTranslationProvider

  val testData
    get() = batchJobTestBase.testData

  @BeforeEach
  fun setup() {
    batchJobTestBase.setup()
    whenever(internalProperties.fakeMtProviders).thenReturn(false)
    doAnswer { invocation ->
      val params = invocation.arguments[0] as ProviderTranslateParams
      if (params.keyName == FAILING_KEY_NAME) {
        throw LlmProviderNotReturnedJsonException()
      }
      MtValueProvider.MtResult(
        translated =
          "${params.text} translated with GOOGLE " +
            "from ${params.sourceLanguageTag} to ${params.targetLanguageTag}",
        price = 100,
      )
    }.whenever(googleTranslationProvider).translate(any())
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `keeps translations of other keys when llm response is unparseable for one key`() {
    val keys = testData.addTranslationOperationData(12)
    batchJobTestBase.saveAndPrepare(this)
    val keyIds = keys.map { it.id }

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to
          listOf(
            testData.projectBuilder
              .getLanguageByTag("cs")!!
              .self.id,
          ),
      ),
    ).andIsOk

    waitForJobFailed()

    assertOnlyFailingKeyNotTranslated(keys.map { it.name })
    assertFailedChunkStoredSuccessfulTargetsAndOtherChunksSucceeded()
    assertNoKeyTranslatedTwice()
  }

  private fun waitForJobFailed() {
    waitForNotThrowing(pollTime = 500, timeout = 30000) {
      getSingleJob().status.assert.isEqualTo(BatchJobStatus.FAILED)
    }
  }

  private fun assertOnlyFailingKeyNotTranslated(allKeyNames: List<String>) {
    executeInNewTransaction {
      val translations =
        entityManager
          .createQuery(
            """from Translation t where t.key.name in :keyNames and t.language.tag = 'cs'""",
            Translation::class.java,
          ).setParameter("keyNames", allKeyNames)
          .resultList

      val translated = translations.filter { !it.text.isNullOrEmpty() }
      translated.map { it.key.name }.assert.containsExactlyInAnyOrderElementsOf(allKeyNames - FAILING_KEY_NAME)
      translated.forEach {
        it.text.assert.contains("translated with GOOGLE from en to cs")
      }
    }
  }

  private fun assertFailedChunkStoredSuccessfulTargetsAndOtherChunksSucceeded() {
    executeInNewTransaction {
      val executions =
        entityManager
          .createQuery(
            """from BatchJobChunkExecution e where e.batchJob.id = :jobId""",
            BatchJobChunkExecution::class.java,
          ).setParameter("jobId", getSingleJob().id)
          .resultList

      val failed = executions.filter { it.status == BatchJobChunkExecutionStatus.FAILED }
      failed.assert.hasSize(1)
      failed
        .single()
        .errorMessage.assert
        .isEqualTo(Message.LLM_PROVIDER_NOT_RETURNED_JSON)
      failed
        .single()
        .retry.assert
        .isFalse()
      // the 4 other items of the failing chunk were translated and stored
      failed
        .single()
        .successTargets.assert
        .hasSize(4)

      executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }.assert.isEqualTo(2)
    }
  }

  private fun assertNoKeyTranslatedTwice() {
    verify(googleTranslationProvider, times(1)).translate(argThat { keyName == FAILING_KEY_NAME })
    verify(googleTranslationProvider, times(12)).translate(any())
  }

  private fun getSingleJob(): BatchJob =
    executeInNewTransaction {
      entityManager
        .createQuery("""from BatchJob""", BatchJob::class.java)
        .singleResult
    }
}
