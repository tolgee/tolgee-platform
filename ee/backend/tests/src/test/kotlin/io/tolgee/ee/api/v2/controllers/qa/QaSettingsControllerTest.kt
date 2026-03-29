package io.tolgee.ee.api.v2.controllers.qa

import com.posthog.server.PostHog
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class QaSettingsControllerTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var batchJobService: BatchJobService

  lateinit var testData: QaTestData

  private val settingsUrl
    get() = "/v2/projects/${testData.project.id}/qa-settings"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `returns default settings when no config in DB`() {
    performAuthGet(settingsUrl).andIsOk.andAssertThatJson {
      node("settings").isObject
      // Should fall back to defaultSeverity for each check type
      node("settings.EMPTY_TRANSLATION").isEqualTo("OFF")
      node("settings.SPACES_MISMATCH").isEqualTo("WARNING")
      node("settings.CHARACTER_CASE_MISMATCH").isEqualTo("WARNING")
    }
  }

  @Test
  fun `updates global settings`() {
    performAuthPut(
      settingsUrl,
      mapOf(
        "settings" to
          mapOf(
            "EMPTY_TRANSLATION" to "WARNING",
            "SPACES_MISMATCH" to "OFF",
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("settings.EMPTY_TRANSLATION").isEqualTo("WARNING")
      node("settings.SPACES_MISMATCH").isEqualTo("OFF")
    }

    // Verify persistence by re-reading, including that unmentioned checks retain defaults
    performAuthGet(settingsUrl).andIsOk.andAssertThatJson {
      node("settings.EMPTY_TRANSLATION").isEqualTo("WARNING")
      node("settings.SPACES_MISMATCH").isEqualTo("OFF")
      node("settings.CHARACTER_CASE_MISMATCH").isEqualTo("WARNING")
    }
  }

  @Test
  fun `enabling QA triggers batch recheck`() {
    // First disable QA
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }

    // Save QA config so there's something to recheck
    qa.saveDefaultQaConfig()

    // Now enable via API
    performAuthPut(
      "$settingsUrl/enabled",
      mapOf("enabled" to true),
    ).andIsOk

    // Verify batch job was created
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }

  @Test
  fun `disabling QA does not trigger batch recheck`() {
    qa.saveDefaultQaConfig()

    performAuthPut(
      "$settingsUrl/enabled",
      mapOf("enabled" to false),
    ).andIsOk

    Thread.sleep(1000)
    executeInNewTransaction(platformTransactionManager) {
      val jobs = batchJobService.getAllByProjectId(testData.project.id)
      val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
      qaJobs.assert.isEmpty()
    }
  }

  @Test
  fun `sets and reads per-language overrides`() {
    qa.saveDefaultQaConfig()

    val langUrl = "$settingsUrl/languages/${testData.frenchLanguage.id}"

    performAuthPut(
      langUrl,
      mapOf(
        "settings" to
          mapOf(
            "SPACES_MISMATCH" to "OFF",
            "PUNCTUATION_MISMATCH" to "OFF",
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("settings.SPACES_MISMATCH").isEqualTo("OFF")
      node("settings.PUNCTUATION_MISMATCH").isEqualTo("OFF")
    }

    // Verify the override is persisted
    performAuthGet(langUrl).andIsOk.andAssertThatJson {
      node("settings.SPACES_MISMATCH").isEqualTo("OFF")
      node("settings.PUNCTUATION_MISMATCH").isEqualTo("OFF")
    }
  }

  @Test
  fun `setting language override to null inherits from global`() {
    qa.saveDefaultQaConfig()
    val langUrl = "$settingsUrl/languages/${testData.frenchLanguage.id}"

    // First set an override
    performAuthPut(
      langUrl,
      mapOf("settings" to mapOf("SPACES_MISMATCH" to "OFF")),
    ).andIsOk

    // Then set it to null to inherit from global
    performAuthPut(
      langUrl,
      mapOf("settings" to mapOf("SPACES_MISMATCH" to null)),
    ).andIsOk.andAssertThatJson {
      // No override should remain for SPACES_MISMATCH
      node("settings").isObject.doesNotContainKey("SPACES_MISMATCH")
    }
  }

  @Test
  fun `deleting language overrides reverts to global`() {
    qa.saveDefaultQaConfig()
    val langUrl = "$settingsUrl/languages/${testData.frenchLanguage.id}"

    // Set overrides
    performAuthPut(
      langUrl,
      mapOf("settings" to mapOf("SPACES_MISMATCH" to "OFF")),
    ).andIsOk

    // Delete all overrides
    performAuthDelete(langUrl, null).andIsOk

    // Verify no overrides remain — settings is null after deleting all overrides
    performAuthGet(langUrl).andIsOk.andAssertThatJson {
      node("settings").isNull()
    }
  }

  @Test
  fun `returns check types grouped by category`() {
    performAuthGet("$settingsUrl/check-types").andIsOk.andAssertThatJson {
      isArray.isNotEmpty
      isArray.anySatisfy {
        assertThatJson(it).node("category").isNotNull
        assertThatJson(it).node("checkTypes").isArray.isNotEmpty
      }
    }
  }

  @Test
  fun `resolved language settings merge global with overrides`() {
    qa.saveDefaultQaConfig()
    val langUrl = "$settingsUrl/languages/${testData.frenchLanguage.id}"

    // Set a language override for one check
    performAuthPut(
      langUrl,
      mapOf("settings" to mapOf("SPACES_MISMATCH" to "OFF")),
    ).andIsOk

    // Get resolved settings — should merge global + override
    performAuthGet("$langUrl/resolved").andIsOk.andAssertThatJson {
      node("settings").isObject
      // Override should take effect
      node("settings.SPACES_MISMATCH").isEqualTo("OFF")
      // Non-overridden checks should have global values
      node("settings.CHARACTER_CASE_MISMATCH").isEqualTo("WARNING")
    }
  }

  @Test
  fun `returns all language overrides`() {
    qa.saveDefaultQaConfig()
    val langUrl = "$settingsUrl/languages/${testData.frenchLanguage.id}"

    // Set an override for French
    performAuthPut(
      langUrl,
      mapOf("settings" to mapOf("SPACES_MISMATCH" to "OFF")),
    ).andIsOk

    // Get all language settings
    performAuthGet("$settingsUrl/languages").andIsOk.andAssertThatJson {
      isArray.isNotEmpty
      isArray.anySatisfy {
        assertThatJson(it).node("language.id").isEqualTo(testData.frenchLanguage.id)
        assertThatJson(it).node("customSettings.SPACES_MISMATCH").isEqualTo("OFF")
      }
    }
  }
}
