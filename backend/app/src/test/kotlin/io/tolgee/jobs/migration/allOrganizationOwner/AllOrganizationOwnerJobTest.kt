package io.tolgee.jobs.migration.allOrganizationOwner

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AllOrganizationOwnerMigrationTestData
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class AllOrganizationOwnerJobTest : AbstractSpringTest() {
  @Autowired
  @Qualifier("translationStatsJob")
  lateinit var translationStatsJob: Job

  @Autowired
  lateinit var allOrganizationOwnerJobRunner: AllOrganizationOwnerJobRunner

  @Autowired
  lateinit var translationRepository: TranslationRepository

  lateinit var testData: AllOrganizationOwnerMigrationTestData

  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var permissionRepository: PermissionRepository

  lateinit var project1: Project

  @BeforeEach
  fun setup() {
    val user =
      userAccountRepository.save(
        UserAccount(
          name = "User with 2 projects",
          username = "user",
          password = "pass",
        ),
      )
    project1 = projectRepository.save(Project(name = "Project").also { it.userOwner = user })
    permissionRepository.save(
      Permission(
        user = user,
        project = project1,
        type = ProjectPermissionType.MANAGE,
      ),
    )

    val project2 = projectRepository.save(Project(name = "Project 2").also { it.userOwner = user })
    permissionRepository.save(
      Permission(
        user = user,
        project = project2,
        type = ProjectPermissionType.MANAGE,
      ),
    )

    val user2 =
      userAccountRepository.save(
        UserAccount(
          name = "User with no role",
          username = "user2",
          password = "pass",
        ),
      )

    permissionService.grantFullAccessToProject(user2, project2)
  }

  @Test
  fun `creates organizations and moves projects`() {
    allOrganizationOwnerJobRunner.run()
    transactionTemplate.execute {
      val allProjects = projectRepository.findAll()
      val first = allProjects.find { it.name == "Project" }
      first!!.assertHasOrganizationOwner("User with 2 projects")
      val second = allProjects.find { it.name == "Project 2" }
      second!!.assertHasOrganizationOwner("User with 2 projects")
      assertThat(first.organizationOwner.id).isEqualTo(second.organizationOwner.id)
      assertThat(first.organizationOwner.memberRoles).hasSize(1)
    }
  }

  @Test
  fun `deletes permission`() {
    allOrganizationOwnerJobRunner.run()
    transactionTemplate.execute {
      val firstProject = projectRepository.getById(project1.id)
      assertThat(firstProject.permissions).isEmpty()
    }
  }

  @Test
  fun `reuses existing organization`() {
    allOrganizationOwnerJobRunner.run()
    transactionTemplate.execute {
      val firstProject = projectRepository.getById(project1.id)
      assertThat(firstProject.permissions).isEmpty()
    }
  }

  @Test
  fun `creates the organization for user with no organization membership`() {
    allOrganizationOwnerJobRunner.run()
    transactionTemplate.execute {
      val createdOrganization =
        userAccountRepository
          .findByUsername("user2")
          .get()
          .organizationRoles
          .single()
          .organization
      assertThat(createdOrganization?.name).isEqualTo("User with no role")
    }
  }

  @Test
  fun `it keeps user direct permissions`() {
    allOrganizationOwnerJobRunner.run()
    transactionTemplate.execute {
      val user2 = userAccountRepository.findByUsername("user2").get()
      assertThat(
        user2.permissions
          .single()
          .project
          ?.name,
      ).isEqualTo("Project 2")
    }
  }

  @Test
  fun `it does not run multiple times for same params`() {
    // first - it really runs
    val instance = allOrganizationOwnerJobRunner.run()
    // nothing to migrate, no run
    val instance2 = allOrganizationOwnerJobRunner.run()

    assertThat(instance).isNotNull
    assertThat(instance2).isNull()
  }

  private fun Project.assertHasOrganizationOwner(name: String) {
    assertThat(this.userOwner).isNull()
    assertThat(this.organizationOwner.name).isEqualTo(name)
  }
}
