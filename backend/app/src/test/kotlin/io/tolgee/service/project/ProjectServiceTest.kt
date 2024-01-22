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
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewRepeatableTransaction
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var bigMetaService: BigMetaService

  @Test
  fun testFindAllPermitted() {
    executeInNewTransaction {
      val users = dbPopulator.createUsersAndOrganizations()
      dbPopulator.createBase("Test", users[3].username)
      val projects = projectService.findAllPermitted(users[3])
      assertThat(projects).hasSize(10)
    }
  }

  @Test
  fun testFindAllEmpty() {
    executeInNewTransaction {
      dbPopulator.createUsersAndOrganizations() // create some data
      val user = dbPopulator.createUserIfNotExists("user")
      val projects = projectService.findAllPermitted(user)
      assertThat(projects).hasSize(0)
    }
  }

  @Test
  fun testFindAllSingleProject() {
    executeInNewTransaction {
      dbPopulator.createUsersAndOrganizations() // create some data
      val base = dbPopulator.createBase("Hello world", generateUniqueString())
      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(1)
      assertThat(projects[0].scopes).containsExactlyInAnyOrder(*ProjectPermissionType.MANAGE.availableScopes)
    }
  }

  @Test
  fun testFindMultiple() {
    executeInNewTransaction {
      val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("helga") // create some data
      val base = dbPopulator.createBase("Hello world")
      val organization = usersWithOrganizations[0].organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization!!, OrganizationRoleType.MEMBER)

      val user3 = userAccountService.get(usersWithOrganizations[3].id)

      val organization2 = user3.organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization2!!, OrganizationRoleType.OWNER)
      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(7)
      assertThat(projects[6].scopes).containsExactlyInAnyOrder(*ProjectPermissionType.MANAGE.availableScopes)
      assertThat(projects[1].scopes).containsExactlyInAnyOrder(*ProjectPermissionType.VIEW.availableScopes)
      assertThat(projects[5].scopes).containsExactlyInAnyOrder(*ProjectPermissionType.MANAGE.availableScopes)
    }
  }

  @Test
  fun testFindMultiplePermissions() {
    executeInNewTransaction(platformTransactionManager) {
      val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("agnes") // create some data
      val base = dbPopulator.createBase("Hello world")
      val organization = usersWithOrganizations[0].organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization!!, OrganizationRoleType.MEMBER)

      val user3 = userAccountService.get(usersWithOrganizations[3].id) // entityManager.merge(usersWithOrganizations[3])

      val organization2 = user3.organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization2!!, OrganizationRoleType.OWNER)

      val customPermissionProject = usersWithOrganizations[0].organizationRoles[0].organization!!.projects[2]
      val customPermissionProject2 = user3.organizationRoles[0].organization!!.projects[2]
      permissionService.create(
        Permission(
          user = base.userAccount,
          project = customPermissionProject,
          type = ProjectPermissionType.TRANSLATE,
        ),
      )
      permissionService.create(
        Permission(
          user = base.userAccount,
          project = customPermissionProject2,
          type = ProjectPermissionType.TRANSLATE,
        ),
      )

      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(7)
      assertThat(projects[6].scopes).equalsPermissionType(ProjectPermissionType.MANAGE)
      assertThat(projects[2].scopes).equalsPermissionType(ProjectPermissionType.TRANSLATE)
      assertThat(projects[1].scopes).equalsPermissionType(ProjectPermissionType.VIEW)
      assertThat(projects[5].scopes).equalsPermissionType(ProjectPermissionType.MANAGE)
    }
  }

  @Test
  fun testDeleteProjectWithTags() {
    executeInNewTransaction(platformTransactionManager) {
      val testData = TagsTestData()
      testData.generateVeryLotOfData()
      testDataService.saveTestData(testData.root)
      val start = System.currentTimeMillis()
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
      entityManager.flush()
      entityManager.clear()
      val time = System.currentTimeMillis() - start
      println(time)
      assertThat(time).isLessThan(30000)
      assertThat(tagService.find(testData.existingTag.id)).isNull()
    }
  }

  @Test
  fun `deletes project with MT Settings`() {
    val testData =
      executeInNewTransaction {
        val testData = MtSettingsTestData()
        testDataService.saveTestData(testData.root)
        return@executeInNewTransaction testData
      }
    executeInNewTransaction(platformTransactionManager) {
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
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

    executeInNewTransaction(platformTransactionManager) {
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
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
    executeInNewTransaction(platformTransactionManager) {
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
    }
  }

  @Test
  fun `deletes project with Content Delivery Configs`() {
    val testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    executeInNewRepeatableTransaction(platformTransactionManager) {
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
    }
  }

  @Test
  fun `deletes project with webhooks`() {
    val testData = WebhooksTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction(platformTransactionManager) {
      projectService.hardDeleteProject(testData.projectBuilder.self.id)
    }
  }
}
