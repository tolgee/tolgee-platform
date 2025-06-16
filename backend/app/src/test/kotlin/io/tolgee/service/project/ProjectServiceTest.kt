/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Test
  fun testFindAllPermitted() {
    executeInNewTransaction {
      val users = dbPopulator.createUsersAndOrganizations()
      dbPopulator.createBase(users[3].username)
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
      val base = dbPopulator.createBase(generateUniqueString())
      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(1)
      assertThat(projects[0].scopes).containsExactlyInAnyOrder(*ProjectPermissionType.MANAGE.availableScopes)
    }
  }

  @Test
  fun testFindMultiple() {
    executeInNewTransaction {
      val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("helga") // create some data
      val base = dbPopulator.createBase()
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
      val users = dbPopulator.createUsersAndOrganizations("agnes")
      val base = dbPopulator.createBase()

      val organizationUserIsMember = users[1].firstOrganization()
      organizationRoleService.grantRoleToUser(base.userAccount, organizationUserIsMember, OrganizationRoleType.MEMBER)

      val organizationUserIsOwner = users[2].firstOrganization()
      organizationRoleService.grantRoleToUser(base.userAccount, organizationUserIsOwner, OrganizationRoleType.OWNER)

      val customPermissionProject = organizationUserIsMember.projects[2]
      val customPermissionProject2 = organizationUserIsOwner.projects[2]

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

      val fetchedProjects = projectService.findAllPermitted(base.userAccount)

      assertThat(fetchedProjects).hasSize(7)

      val expectedPermissions =
        listOf(
          base.project to ProjectPermissionType.MANAGE,
          customPermissionProject to ProjectPermissionType.TRANSLATE,
          organizationUserIsMember.projects[0] to ProjectPermissionType.VIEW,
          organizationUserIsOwner.projects[0] to ProjectPermissionType.MANAGE,
        )

      expectedPermissions.forEach { (project, expectedPermission) ->
        assertThat(fetchedProjects.find { it.name.equals(project.name) }!!.scopes)
          .equalsPermissionType(expectedPermission)
      }
    }
  }

  private fun UserAccount.firstOrganization(): Organization = organizationRoles[0].organization!!

  @Test
  fun testDeleteProjectWithTags() {
    executeInNewTransaction(platformTransactionManager) {
      val testData = TagsTestData()
      testData.generateVeryLotOfData()
      testDataService.saveTestData(testData.root)
      val start = System.currentTimeMillis()
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self)
      entityManager.flush()
      entityManager.clear()
      val time = System.currentTimeMillis() - start
      println(time)
      assertThat(time).isLessThan(30000)
      assertThat(tagService.find(testData.existingTag.id)).isNull()
    }
  }

  @Test
  fun testDeleteProjectWithAiResults() {
    executeInNewTransaction(platformTransactionManager) {
      val testData = PromptTestData()
      testData.addLanguageConfig()
      testDataService.saveTestData(testData.root)
      projectHardDeletingService.hardDeleteProject(testData.promptProject.self)
      entityManager.flush()
      entityManager.clear()
    }
  }
}
