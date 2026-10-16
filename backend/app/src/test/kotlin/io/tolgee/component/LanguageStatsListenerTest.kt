package io.tolgee.component

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class LanguageStatsListenerTest : AbstractControllerTest() {
  @Autowired
  private lateinit var projectHolder: ProjectHolder

  @Autowired
  private lateinit var lockingProvider: LockingProvider

  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `updates stats when added key`() {
    val stats = getProjectStats("de")

    createKey("ho ho ho new key", mapOf("en" to "hello"))

    // it's async, so lets wait until it passes
    waitForNotThrowing(AssertionFailedError::class) {
      val newStats = getProjectStats("de")
      assertThat(newStats!!.untranslatedWords).isEqualTo(stats?.untranslatedWords?.plus(1))
      assertThat(newStats.translationsUpdatedAt!!.after(stats!!.translationsUpdatedAt))
    }
  }

  @Test
  fun `coalesced refresh is skipped while another refresh waits for the lock`() {
    val lock = lockingProvider.getLock(languageStatsLockName())
    lock.lock()
    val waitingRefresh: CompletableFuture<Void>
    try {
      waitingRefresh = startCoalescedRefresh()
      Thread.sleep(500)
      val skippedRefresh = startCoalescedRefresh()
      skippedRefresh.get(5, TimeUnit.SECONDS)
      waitingRefresh.assert.matches({ !it.isDone }, "not done")
    } finally {
      lock.unlock()
    }
    waitingRefresh.get(30, TimeUnit.SECONDS)
  }

  @Test
  fun `coalesced refresh runs again once the waiting one has taken the lock`() {
    startCoalescedRefresh().get(30, TimeUnit.SECONDS)

    val lock = lockingProvider.getLock(languageStatsLockName())
    lock.lock()
    val refresh: CompletableFuture<Void>
    try {
      refresh = startCoalescedRefresh()
      Thread.sleep(500)
      refresh.assert.matches({ !it.isDone }, "not done")
    } finally {
      lock.unlock()
    }
    refresh.get(30, TimeUnit.SECONDS)
  }

  private fun languageStatsLockName() = "refresh-lang-stats-${testData.project.id}-default"

  private fun startCoalescedRefresh() =
    CompletableFuture.runAsync { languageStatsService.refreshLanguageStatsCoalesced(testData.project.id) }

  private fun getProjectStats(languageTag: String): LanguageStatsDto? {
    val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }
    return executeInNewTransaction(platformTransactionManager) {
      languageStatsService
        .getLanguageStats(projectId = testData.project.id, projectLanguages.keys, null)
        .find { projectLanguages[it.languageId]!!.tag == languageTag }
    }
  }

  private fun createKey(
    name: String,
    translations: Map<String, String>,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.project)
      projectHolder.project = projectDto
      keyService.create(
        testData.project,
        CreateKeyDto(name = name, translations = translations),
      )
    }
  }
}
