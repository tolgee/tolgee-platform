package io.tolgee.api.v2.controllers.batch

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.iterceptor.ActivityRevisionInitializer
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

/**
 * End-to-end repro for #3724 using only real production code.
 *
 * Drives the real `MACHINE_TRANSLATE` batch flow through the HTTP API and
 * captures DEBUG logs from [ActivityRevisionInitializer], which lives in the
 * activity-tracking interceptor chain and defensively reads
 * `ProjectHolder.project` / `OrganizationHolder.organization` while initialising
 * each batch chunk's activity revision. When the holders aren't populated the
 * initializer logs:
 *
 * > Project is not set in ProjectHolder. Activity will be stored without projectId.
 *
 * Before the fix this fires on batch worker coroutine threads on every chunk;
 * after [BatchJobActionService.populateHolders] runs the initializer reads
 * populated holders and the message never appears on those threads.
 *
 * Why this matters: this is the same missing-holder condition that production
 * LLM-MT code paths trip over — the difference is that
 * [ActivityRevisionInitializer] catches it defensively while the LLM-MT
 * downstream code doesn't. The defensive log is therefore a free natural
 * canary: if it fires for a batch worker, the bug is still present.
 *
 * Standard Google MT with `fakeMtProviders=true` keeps the test hermetic
 * (no external HTTP) while exercising every real layer of the batch +
 * activity-tracking pipeline.
 */
@Import(BatchJobBaseConfiguration::class)
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
  fun `batch worker thread has populated ProjectHolder during real MT workflow`() {
    val activityLogger =
      LoggerFactory.getLogger(ActivityRevisionInitializer::class.java) as Logger
    val originalLevel = activityLogger.level
    val appender = ListAppender<ILoggingEvent>()
    appender.start()
    activityLogger.level = Level.DEBUG
    activityLogger.addAppender(appender)

    try {
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

      // Real production assertion: no batch worker coroutine should ever hit
      // the defensive catch. If it does, holders weren't populated on the
      // worker thread — exactly the precondition that breaks LLM-MT in #3724.
      val batchWorkerWarnings =
        appender.list.filter { event ->
          if (!event.threadName.contains("coroutine")) return@filter false
          val msg = event.formattedMessage
          msg.contains("Project is not set in ProjectHolder") ||
            msg.contains("Organization is not set in OrganizationHolder")
        }

      batchWorkerWarnings.assert.isEmpty()
    } finally {
      activityLogger.detachAppender(appender)
      activityLogger.level = originalLevel
    }
  }
}
