package io.tolgee.ee.component.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.translationMemory.TranslationMemoryType
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.repository.translationMemory.TranslationMemoryProjectRepository
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
@AutoConfigureMockMvc
class TranslationMemoryFeatureListenerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var applicationEventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var translationMemoryProjectRepository: TranslationMemoryProjectRepository

  @Autowired
  private lateinit var translationMemoryEntryRepository: TranslationMemoryEntryRepository

  @Autowired
  private lateinit var translationMemoryManagementService: TranslationMemoryManagementService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `creates project TM when TRANSLATION_MEMORY is gained`() {
    val projectId = testData.projectWithoutTm.id
    val orgId = testData.projectWithoutTm.organizationOwner.id

    // Seed translations before the TM exists. Project TMs no longer back these into stored
    // entries — the content surfaces virtually from the translations at read time — but the
    // setup still mirrors the original "legacy project gets uplifted" scenario.
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(projectId)
      val english = languageService.findEntitiesByTags(setOf("en"), projectId).first()
      val german = languageService.findEntitiesByTags(setOf("de"), projectId).first()

      val key = keyService.create(project, CreateKeyDto("legacy-key", null, null))
      val en = translationService.getOrCreate(key, english).apply { text = "Legacy hello" }
      translationService.save(en)
      val de = translationService.getOrCreate(key, german).apply { text = "Legacy hallo" }
      translationService.save(de)
    }

    // Sanity: project has no TM yet
    executeInNewTransaction(platformTransactionManager) {
      assertThat(translationMemoryProjectRepository.findByProjectId(projectId)).isEmpty()
    }

    // Publish the event in a committed transaction so AFTER_COMMIT listener fires
    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = orgId,
          gainedFeatures = setOf(Feature.TRANSLATION_MEMORY),
          lostFeatures = emptySet(),
        ),
      )
    }

    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val project = projectService.get(projectId)
        val tm = translationMemoryManagementService.getProjectTm(project.id)
        assertThat(tm).isNotNull()
        assertThat(tm!!.type).isEqualTo(TranslationMemoryType.PROJECT)

        // Project TM is virtual — it must NOT carry stored entries, even when the project
        // already has translations. Content reads compute groups from translations on the fly.
        val entries = translationMemoryEntryRepository.findByTranslationMemoryId(tm.id)
        assertThat(entries).isEmpty()
      }
    }
  }

  @Test
  fun `does not create duplicate project TM when one already exists`() {
    val projectId = testData.projectWithTm.id
    val orgId = testData.projectWithTm.organizationOwner.id
    val existingTmId = testData.projectTm.id

    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = orgId,
          gainedFeatures = setOf(Feature.TRANSLATION_MEMORY),
          lostFeatures = emptySet(),
        ),
      )
    }

    // Give the async listener a moment to process
    Thread.sleep(2000)

    executeInNewTransaction(platformTransactionManager) {
      val projectTms =
        translationMemoryProjectRepository
          .findByProjectId(projectId)
          .map { it.translationMemory }
          .filter { it.type == TranslationMemoryType.PROJECT }
      assertThat(projectTms).hasSize(1)
      assertThat(projectTms.first().id).isEqualTo(existingTmId)
    }
  }

  @Test
  fun `ignores events that do not gain TRANSLATION_MEMORY`() {
    val projectId = testData.projectWithoutTm.id
    val orgId = testData.projectWithoutTm.organizationOwner.id

    // Event with only a lost feature — should NOT create a TM
    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = orgId,
          gainedFeatures = emptySet(),
          lostFeatures = setOf(Feature.TRANSLATION_MEMORY),
        ),
      )
    }

    // Event gaining a different feature — should NOT create a TM
    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = orgId,
          gainedFeatures = setOf(Feature.QA_CHECKS),
          lostFeatures = emptySet(),
        ),
      )
    }

    Thread.sleep(2000)

    executeInNewTransaction(platformTransactionManager) {
      assertThat(translationMemoryProjectRepository.findByProjectId(projectId)).isEmpty()
    }
  }
}
