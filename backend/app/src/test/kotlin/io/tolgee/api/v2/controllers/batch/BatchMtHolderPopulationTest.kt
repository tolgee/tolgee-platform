package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener

/**
 * End-to-end repro for issue #3724.
 *
 * Drives the real `MACHINE_TRANSLATE` batch flow through the HTTP API — the
 * same path the bug reporter exercises in production. No mocking of
 * `autoTranslationService`: the chunk processor calls the real
 * `autoTranslateSync`, which calls the real `MtService`, which publishes the
 * real `OnBeforeMachineTranslationEvent` through Spring's event bus.
 *
 * [HolderProbingListenerConfig] registers a real Spring `@EventListener`
 * that dereferences `ProjectHolder.project` and `OrganizationHolder.organization`
 * inside the chunk's transaction. That is exactly the kind of access pattern
 * production LLM-MT code paths perform deep in the EE prompt / charging chain
 * — without [BatchJobActionService.populateHolders] the dereference throws
 * `ProjectNotSelectedException` on the batch worker thread, `MtProviderCatching`
 * aggregates it, and the chunk fails repeatedly. With the fix the holders are
 * populated, the listener returns normally, the fake Google provider
 * (`fakeMtProviders = true`) completes the chunk, and translations land in DB.
 */
@Import(BatchJobBaseConfiguration::class, BatchMtHolderPopulationTest.HolderProbingListenerConfig::class)
class BatchMtHolderPopulationTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  @BeforeEach
  fun setup() {
    batchJobTestBase.setup()
  }

  private val testData
    get() = batchJobTestBase.testData

  @Test
  @ProjectJWTAuthTestMethod
  fun `MT batch job persists translations when downstream listeners read ProjectHolder`() {
    val keyCount = 5
    val keys = testData.addTranslationOperationData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val targetLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keys.map { it.id },
        "targetLanguageIds" to listOf(targetLanguageId),
      ),
    ).andIsOk

    batchJobTestBase.waitForAllTranslated(keys.map { it.id }, keyCount)

    executeInNewTransaction {
      val jobs =
        entityManager
          .createQuery("""from BatchJob""", BatchJob::class.java)
          .resultList
      jobs.assert.hasSize(1)
      jobs[0].status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  @TestConfiguration
  class HolderProbingListenerConfig {
    @Bean
    fun holderProbingListener(
      projectHolder: ProjectHolder,
      organizationHolder: OrganizationHolder,
    ): HolderProbingListener = HolderProbingListener(projectHolder, organizationHolder)
  }

  class HolderProbingListener(
    private val projectHolder: ProjectHolder,
    private val organizationHolder: OrganizationHolder,
  ) {
    @EventListener(OnBeforeMachineTranslationEvent::class)
    @Suppress("UNUSED_PARAMETER")
    fun onBeforeMt(event: OnBeforeMachineTranslationEvent) {
      // Throws ProjectNotSelectedException / OrganizationNotSelectedException
      // if the batch worker hasn't populated the holders — same failure mode
      // as real LLM-MT downstream code that depends on these holders.
      projectHolder.project
      organizationHolder.organization
    }
  }
}
