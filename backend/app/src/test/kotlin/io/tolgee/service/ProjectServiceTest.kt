/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
class ProjectServiceTest : AbstractSpringTest() {

  @Test
  fun testFindAllPermitted() {
    val users = dbPopulator.createUsersAndOrganizations()
    dbPopulator.createBase("Test", users[3].username)
    val projects = projectService.findAllPermitted(users[3])
    assertThat(projects).hasSize(10)
  }

  @Test
  fun testFindAllEmpty() {
    dbPopulator.createUsersAndOrganizations() // create some data
    val user = dbPopulator.createUserIfNotExists("user")
    val projects = projectService.findAllPermitted(user)
    assertThat(projects).hasSize(0)
  }

  @Test
  fun testFindAllSingleProject() {
    dbPopulator.createUsersAndOrganizations() // create some data
    val repo = dbPopulator.createBase("Hello world", generateUniqueString())
    val projects = projectService.findAllPermitted(repo.userOwner!!)
    assertThat(projects).hasSize(1)
    assertThat(projects[0].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
  }

  @Test
  @Transactional
  fun testFindMultiple() {
    val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("helga") // create some data
    val repo = dbPopulator.createBase("Hello world")
    repo.userOwner = userAccountService.get(repo.userOwner!!.id).get()
    val organization = usersWithOrganizations[0].organizationRoles[0].organization
    organizationRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)

    val user3 = entityManager.merge(usersWithOrganizations[3])
    entityManager.refresh(user3)

    val organization2 = user3.organizationRoles[0].organization
    organizationRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)
    val projects = projectService.findAllPermitted(repo.userOwner!!)
    assertThat(projects).hasSize(7)
    assertThat(projects[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    assertThat(projects[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
    assertThat(projects[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
  }

  @Test
  fun testFindMultiplePermissions() {
    val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("agnes") // create some data
    val repo = dbPopulator.createBase("Hello world")
    repo.userOwner = userAccountService.get(repo.userOwner!!.id).get()
    val organization = usersWithOrganizations[0].organizationRoles[0].organization
    organizationRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)

    val user3 = entityManager.merge(usersWithOrganizations[3])
    entityManager.refresh(user3)

    val organization2 = user3.organizationRoles[0].organization
    organizationRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)

    val customPermissionRepo = usersWithOrganizations[0].organizationRoles[0].organization!!.projects[2]
    val customPermissionRepo2 = user3.organizationRoles[0].organization!!.projects[2]
    permissionService.create(
      Permission(
        user = repo.userOwner,
        project = customPermissionRepo,
        type = Permission.ProjectPermissionType.TRANSLATE
      )
    )
    permissionService.create(
      Permission(
        user = repo.userOwner,
        project = customPermissionRepo2,
        type = Permission.ProjectPermissionType.TRANSLATE
      )
    )

    val projects = projectService.findAllPermitted(repo.userOwner!!)
    assertThat(projects).hasSize(7)
    assertThat(projects[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    assertThat(projects[2].permissionType).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)
    assertThat(projects[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
    assertThat(projects[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
  }

  @Test
  fun testDeleteProjectWithTags() {
    val testData = TagsTestData()
    testData.generateVeryLotOfData()
    testDataService.saveTestData(testData.root)
    val start = System.currentTimeMillis()
    projectService.deleteProject(testData.projectBuilder.self.id)
    entityManager.flush()
    entityManager.clear()
    val time = System.currentTimeMillis() - start
    println(time)
    assertThat(time).isLessThan(10000)
    assertThat(tagService.find(testData.existingTag.id)).isNull()
  }

  @Test
  fun `deletes project with MT Settings`() {
    val testData = MtSettingsTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    projectService.deleteProject(testData.projectBuilder.self.id)
  }

  @Test
  fun `deletes project with `() {
    val testData = AutoTranslateTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    projectService.deleteProject(testData.projectBuilder.self.id)
  }

  @Test
  fun `test get statistics`() {
    val projectTestData = ProjectsTestData()
    testDataService.saveTestData(projectTestData.root)
    val result = projectService.getProjectsStatistics(
      listOf(
        projectTestData.projectBuilder.self.id,
        projectTestData.project2.id
      )
    )
    result[0]
  }
}
