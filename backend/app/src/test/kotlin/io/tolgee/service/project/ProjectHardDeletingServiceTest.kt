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
import io.tolgee.development.testDataBuilder.data.ProjectWithQaEntitiesTestData
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Project
import io.tolgee.model.key.Namespace
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewRepeatableTransaction
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest
class ProjectHardDeletingServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var bigMetaService: BigMetaService

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  private var testDataToClean: BaseTestData? = null

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @AfterEach
  fun cleanTestDataAfterTest() {
    testDataToClean?.let { testDataService.cleanTestData(it.root) }
    testDataToClean = null
  }

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
    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }
  }

  @Test
  fun `deletes project with suggestions`() {
    val testData = SuggestionsTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }
  }

  @Test
  fun `deletes project that has a default namespace`() {
    val testData = BaseTestData()
    val namespace = testData.projectBuilder.addNamespace { name = "homepage" }
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.projectBuilder.self.id)
      project.defaultNamespace = entityManager.find(Namespace::class.java, namespace.self.id)
      projectService.save(project)
    }

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      projectService.find(testData.projectBuilder.self.id).assert.isNull()
    }
  }

  @Test
  fun `deletes project with QA entities`() {
    val testData = ProjectWithQaEntitiesTestData()
    testDataService.saveTestData(testData.root)

    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      projectService.find(testData.projectBuilder.self.id).assert.isNull()
    }
  }

  @Test
  fun `deletes project with contributor rows`() {
    val testData = BaseTestData()
    testDataToClean = testData
    testDataService.saveTestData(testData.root)
    val projectId = testData.projectBuilder.self.id
    val userId = testData.user.id

    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createNativeQuery(
          "insert into project_contributor (project_id, user_id, first_contribution_at, last_contribution_at) " +
            "values (:projectId, :userId, now(), now())",
        ).setParameter("projectId", projectId)
        .setParameter("userId", userId)
        .executeUpdate()
    }

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      val remaining =
        (
          entityManager
            .createNativeQuery(
              "select count(*) from project_contributor where project_id = :projectId and user_id = :userId",
            ).setParameter("projectId", projectId)
            .setParameter("userId", userId)
            .singleResult as Number
        ).toLong()
      remaining.assert.isEqualTo(0)
    }
  }

  @Test
  fun `deletes project with tasks`() {
    val testData = TaskTestData()
    testDataToClean = testData
    testData.addNotifications()
    testDataService.saveTestData(testData.root)
    val taskNotificationId = testData.taskNotification.self.id
    val projectNotificationId = testData.projectNotification.self.id

    io.tolgee.util.executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      projectService.find(testData.projectBuilder.self.id).assert.isNull()
      notificationRepository
        .findById(taskNotificationId)
        .isPresent.assert
        .isFalse()
      notificationRepository
        .findById(projectNotificationId)
        .isPresent.assert
        .isFalse()
    }
  }

  @Test
  fun `deletes project with a soft-deleted import referencing an existing language`() {
    val testData = BaseTestData()
    testDataToClean = testData
    val importBuilder =
      testData.projectBuilder
        .addImport {
          author = testData.user
          deletedAt = Date()
        }.build {
          addImportFile {
            name = "en.json"
          }.build {
            addImportLanguage {
              name = "en"
              existingLanguage = testData.englishLanguage
            }
          }
        }
    testDataService.saveTestData(testData.root)
    val projectId = testData.projectBuilder.self.id
    val importId = importBuilder.self.id

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self.refresh())
    }

    executeInNewTransaction {
      projectService.find(projectId).assert.isNull()
      val remainingImports =
        (
          entityManager
            .createNativeQuery("select count(*) from import where id = :importId")
            .setParameter("importId", importId)
            .singleResult as Number
        ).toLong()
      remainingImports.assert.isEqualTo(0)
    }
  }

  fun Project.refresh(): Project {
    return projectService.get(this.id)
  }
}
