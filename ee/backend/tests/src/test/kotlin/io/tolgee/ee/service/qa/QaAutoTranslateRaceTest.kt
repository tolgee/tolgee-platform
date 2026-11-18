package io.tolgee.ee.service.qa

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.machineTranslation.MachineTranslationParams
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
class QaAutoTranslateRaceTest : MachineTranslationTest() {
  companion object {
    private const val OLD_FR_TEXT = "Bonjour monde."
    private const val AUTO_TRANSLATED_FR_TEXT = "Translated with Google"
  }

  @Autowired
  private lateinit var autoTranslationService: AutoTranslationService

  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  @MockitoSpyBean
  override lateinit var mtService: MtService

  private lateinit var testData: RaceTestData

  @BeforeEach
  fun setup() {
    testData = RaceTestData()
    testDataService.saveTestData(testData.root)
    initMachineTranslationMocks()
    initMachineTranslationProperties(150000L)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `QA check does not revert auto translation committed while QA was running`() {
    val secondMtCallStarted = CountDownLatch(1)
    val releaseSecondMtCall = CountDownLatch(1)
    blockSecondMachineTranslationCall(secondMtCallStarted, releaseSecondMtCall)

    val autoTranslate =
      CompletableFuture.runAsync {
        executeInNewTransaction {
          autoTranslationService.softAutoTranslate(testData.project.id, testData.key.id, testData.french.id)
          autoTranslationService.softAutoTranslate(testData.project.id, testData.key.id, testData.german.id)
        }
      }

    // The FR update is flushed (row locked, uncommitted) once the DE translation is loaded
    secondMtCallStarted.await(30, TimeUnit.SECONDS).assert.isTrue()

    val qaCheck =
      CompletableFuture.runAsync {
        qaCheckBatchService.runChecksAndPersist(testData.project.id, testData.key.id, testData.french.id)
      }

    waitUntilSomeStatementWaitsForRowLock()
    releaseSecondMtCall.countDown()

    autoTranslate.get(30, TimeUnit.SECONDS)
    qaCheck.get(30, TimeUnit.SECONDS)

    executeInNewTransaction {
      val fr = entityManager.find(Translation::class.java, testData.frTranslation.id)
      fr.text.assert.isEqualTo(AUTO_TRANSLATED_FR_TEXT)
      fr.outdated.assert.isFalse()
      fr.auto.assert.isTrue()
    }
  }

  private fun blockSecondMachineTranslationCall(
    secondCallStarted: CountDownLatch,
    releaseSecondCall: CountDownLatch,
  ) {
    val calls = AtomicInteger()
    doAnswer {
      if (calls.incrementAndGet() == 2) {
        secondCallStarted.countDown()
        releaseSecondCall.await(30, TimeUnit.SECONDS)
      }
      it.callRealMethod()
    }.whenever(mtService).getMachineTranslations(any(), any(), any<MachineTranslationParams.() -> Unit>())
  }

  private fun waitUntilSomeStatementWaitsForRowLock() {
    waitForNotThrowing(timeout = 10000) {
      jdbcTemplate
        .queryForObject(
          "select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'",
          Long::class.java,
        )!!
        .assert
        .isGreaterThan(0)
    }
  }

  class RaceTestData : BaseTestData() {
    lateinit var french: Language
    lateinit var german: Language
    lateinit var key: Key
    lateinit var frTranslation: Translation

    init {
      project.useQaChecks = true
      projectBuilder.build {
        french = addFrench().self
        german = addGerman().self
        addAutoTranslationConfig {
          usingPrimaryMtService = true
        }
        key =
          addKey { name = "export_translations_tags_hint" }
            .build {
              addTranslation("en", "Hello whole world.")
              frTranslation = addTranslation("fr", OLD_FR_TEXT).self.applyOutdatedAuto()
              addTranslation("de", "Hallo Welt.").self.applyOutdatedAuto()
            }.self
      }
    }

    private fun Translation.applyOutdatedAuto(): Translation =
      apply {
        auto = true
        outdated = true
        state = TranslationState.TRANSLATED
      }
  }
}
