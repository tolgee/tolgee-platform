/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Project
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.repository.qa.LanguageQaConfigRepository
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewRepeatableTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectHardDeletingServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var bigMetaService: BigMetaService

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Autowired
  private lateinit var projectQaConfigRepository: ProjectQaConfigRepository

  @Autowired
  private lateinit var languageQaConfigRepository: LanguageQaConfigRepository

  @Autowired
  private lateinit var translationQaIssueRepository: TranslationQaIssueRepository

  @Test
  fun `deletes project with MT Settings`() {
    val testData =
      executeInNewTransaction {
        val testData = MtSettingsTestData()
        testDataService.saveTestData(testData.root)
        return@executeInNewTransaction testData
      }
    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self)
    }
  }

  @Test
  fun `deletes project with batch jobs`() {
    val testData = BatchJobsTestData()
    val keys = testData.addTranslationOperationData(10)
    testDataService.saveTestData(testData.root)

    val job =
      batchJobService.startJob(
        request =
          DeleteKeysRequest().apply {
            keyIds = keys.map { it.id }
          },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.DELETE_KEYS,
      )

    waitFor {
      executeInNewTransaction {
        batchJobService.getJobDto(job.id).status.completed
      }
    }

    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self)
    }

    executeInNewTransaction {
      projectService.find(testData.projectBuilder.self.id).assert.isNull()
    }
  }

  @Test
  fun `deletes project with big meta`() {
    val testData = BaseTestData()
    val key1 = testData.projectBuilder.addKey(keyName = "hello").self
    val key2 = testData.projectBuilder.addKey(keyName = "hello1").self

    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      bigMetaService.store(
        BigMetaDto().apply {
          relatedKeysInOrder =
            mutableListOf(
              RelatedKeyDto(keyName = key1.name),
              RelatedKeyDto(keyName = key2.name),
            )
        },
        testData.projectBuilder.self,
      )
    }
    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self)
    }
  }

  @Test
  fun `deletes project with Content Delivery Configs`() {
    val testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    executeInNewRepeatableTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }
  }

  @Test
  fun `deletes project with webhooks`() {
    val testData = WebhooksTestData()
    testDataService.saveTestData(testData.root)
    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }
  }

  @Test
  fun `deletes project with suggestions`() {
    val testData = SuggestionsTestData()
    testDataService.saveTestData(testData.root)
    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }
  }

  @Test
  fun `deletes project with QA entities`() {
    val testData = BaseTestData()
    testData.projectBuilder.addKey(keyName = "test-key") {
      addTranslation("en", "Hello world.")
    }
    testDataService.saveTestData(testData.root)

    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.projectBuilder.self.id)
      val language = languageService.getEntity(testData.englishLanguage.id)
      val key = keyService.get(project.id, "test-key", null)
      val translation = translationService.find(key, language).get()

      projectQaConfigRepository.save(ProjectQaConfig(project = project))
      languageQaConfigRepository.save(LanguageQaConfig(language = language))
      translationQaIssueRepository.save(
        TranslationQaIssue(
          type = QaCheckType.EMPTY_TRANSLATION,
          message = QaIssueMessage.QA_EMPTY_TRANSLATION,
          translation = translation,
        ),
      )
      entityManager.flush()
    }

    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      projectService.find(testData.projectBuilder.self.id).assert.isNull()
    }
  }

  fun Project.refresh(): Project {
    return projectService.get(this.id)
  }
}
