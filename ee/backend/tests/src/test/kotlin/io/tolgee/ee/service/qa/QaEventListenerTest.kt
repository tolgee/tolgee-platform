package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.repository.TranslationRepository
import io.tolgee.security.ProjectHolder
import io.tolgee.service.QuickStartService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class QaEventListenerTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var applicationEventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var projectCreationService: ProjectCreationService

  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Autowired
  private lateinit var quickStartService: QuickStartService

  @Autowired
  private lateinit var translationRepository: TranslationRepository

  @Autowired
  private lateinit var projectHolder: ProjectHolder

  lateinit var testData: QaTestData
  private var demoProjectId: Long? = null

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    val demoProjId = demoProjectId
    demoProjectId = null
    demoProjId?.let { id ->
      try {
        executeInNewTransaction(platformTransactionManager) {
          val project = projectService.get(id)
          projectHardDeletingService.hardDeleteProject(project)
        }
      } catch (_: Exception) {
        // Demo project may have already been cleaned up or failed to create
      }
    }
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `disables QA when organization gains QA_CHECKS feature`() {
    // Verify QA is enabled initially
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      assertThat(project.useQaChecks).isTrue()
    }

    // Publish the event in a committed transaction so AFTER_COMMIT listener fires
    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
          gainedFeatures = setOf(Feature.QA_CHECKS),
          lostFeatures = emptySet(),
        ),
      )
    }

    // Wait for async listener to process
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val project = projectService.get(testData.project.id)
        assertThat(project.useQaChecks).isFalse()
      }
    }
  }

  @Test
  fun `disables QA when organization loses QA_CHECKS feature`() {
    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
          gainedFeatures = emptySet(),
          lostFeatures = setOf(Feature.QA_CHECKS),
        ),
      )
    }

    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val project = projectService.get(testData.project.id)
        assertThat(project.useQaChecks).isFalse()
      }
    }
  }

  @Test
  fun `auto-enables QA on project creation when org has feature`() {
    val project =
      executeInNewTransaction(platformTransactionManager) {
        projectCreationService.createProject(
          CreateProjectRequest(
            name = "qa-auto-enable-test",
            organizationId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
            languages =
              listOf(
                LanguageRequest(
                  name = "English",
                  tag = "en",
                  originalName = "English",
                  flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7",
                ),
              ),
          ),
        )
      }

    executeInNewTransaction(platformTransactionManager) {
      val created = projectService.get(project.id)
      assertThat(created.useQaChecks).isTrue()
    }

    // Cleanup the created project
    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(project)
    }
  }

  @Test
  fun `does not enable QA on project creation when org lacks feature`() {
    enabledFeaturesProvider.forceEnabled = emptySet()

    val project =
      executeInNewTransaction(platformTransactionManager) {
        projectCreationService.createProject(
          CreateProjectRequest(
            name = "qa-no-auto-enable-test",
            organizationId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id,
            languages =
              listOf(
                LanguageRequest(
                  name = "English",
                  tag = "en",
                  originalName = "English",
                  flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7",
                ),
              ),
          ),
        )
      }

    executeInNewTransaction(platformTransactionManager) {
      val created = projectService.get(project.id)
      assertThat(created.useQaChecks).isFalse()
    }

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(project)
    }
  }

  @Test
  fun `triggers recheck when base language changes`() {
    // Add a second language to use as new base
    val germanLanguage =
      executeInNewTransaction(platformTransactionManager) {
        languageService.createLanguage(
          LanguageRequest(
            name = "German",
            tag = "de",
            originalName = "Deutsch",
            flagEmoji = "\uD83C\uDDE9\uD83C\uDDEA",
          ),
          testData.project,
        )
      }

    executeInNewTransaction(platformTransactionManager) {
      projectHolder.project = ProjectDto.fromEntity(testData.project)
      projectService.editProject(
        testData.project.id,
        EditProjectRequest(
          name = testData.project.name,
          baseLanguageId = germanLanguage.id,
        ),
      )
    }

    // The activity listener triggers recheckTranslations, which starts a batch job
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }

  @Test
  fun `triggers recheck when language tag changes`() {
    executeInNewTransaction(platformTransactionManager) {
      projectHolder.project = ProjectDto.fromEntity(testData.project)
      languageService.editLanguage(
        languageId = testData.frenchLanguage.id,
        projectId = testData.project.id,
        dto =
          LanguageRequest(
            name = "French",
            tag = "fr-FR",
            originalName = "Francais",
            flagEmoji = "\uD83C\uDDEB\uD83C\uDDF7",
          ),
      )
    }

    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }

  @Test
  fun `does not trigger QA batch job when QA is disabled`() {
    // Disable QA for the project
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }

    // Change language tag — should NOT trigger QA recheck since QA is disabled
    executeInNewTransaction(platformTransactionManager) {
      projectHolder.project = ProjectDto.fromEntity(testData.project)
      languageService.editLanguage(
        languageId = testData.frenchLanguage.id,
        projectId = testData.project.id,
        dto =
          LanguageRequest(
            name = "French",
            tag = "fr-CA",
            originalName = "Francais",
            flagEmoji = "\uD83C\uDDEB\uD83C\uDDF7",
          ),
      )
    }

    // Wait a bit, then verify no QA batch job was created
    Thread.sleep(1000)
    executeInNewTransaction(platformTransactionManager) {
      val jobs = batchJobService.getAllByProjectId(testData.project.id)
      val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
      qaJobs.assert.isEmpty()
    }
  }

  @Test
  fun `demo project triggers activity and marks translations stale`() {
    val userId = testData.user.id
    val orgId = testData.userAccountBuilder.defaultOrganizationBuilder.self.id

    demoProjectId =
      executeInNewTransaction(platformTransactionManager) {
        val user = entityManager.find(io.tolgee.model.UserAccount::class.java, userId)
        val org = entityManager.find(io.tolgee.model.Organization::class.java, orgId)
        quickStartService.create(user, org)
        projectService.findAllInOrganization(orgId).first { it.name == "Demo project" }.id
      }

    // Demo project should have QA enabled (org has QA_CHECKS feature)
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(demoProjectId!!)
      assertThat(project.useQaChecks).isTrue()
    }

    // Demo translations should be marked stale (activity system captured the modifications)
    executeInNewTransaction(platformTransactionManager) {
      val translations = translationRepository.getAllByProjectId(demoProjectId!!)
      assertThat(translations).isNotEmpty
      val staleTranslations = translations.filter { it.qaChecksStale }
      assertThat(staleTranslations).isNotEmpty
    }
  }
}
