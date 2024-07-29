package io.tolgee.repository

import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Project
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ProjectRepositoryTest {
  @Autowired
  lateinit var projectRepository: ProjectRepository

  @Autowired
  lateinit var dbPopulatorReal: DbPopulatorReal

  @Test
  fun testPermittedProjects() {
    val users = dbPopulatorReal.createUsersAndOrganizations()
    dbPopulatorReal.createBase("No org project", users[3].username)
    val result = projectRepository.findAllPermitted(users[3].id)
    assertThat(result).hasSize(10)
    assertThat(result[0][0]).isInstanceOf(Project::class.java)
    assertThat(result[8][1]).isNull()
    assertThat(result[8][0]).isInstanceOf(Project::class.java)
    assertThat(result[8][3]).isInstanceOf(OrganizationRole::class.java)
  }

  @Test
  fun testPermittedProjectsJustNoOrg() {
    val base = dbPopulatorReal.createBase("No org project", generateUniqueString())
    val result = projectRepository.findAllPermitted(base.userAccount.id)
    assertThat(result).hasSize(1)
  }

  @Test
  fun testPermittedJustOrg() {
    val users = dbPopulatorReal.createUsersAndOrganizations()
    dbPopulatorReal.createBase("No org project", users[1].username)
    val result = projectRepository.findAllPermitted(users[3].id)
    assertThat(result).hasSize(9)
  }

  @Test
  fun findAllPermittedPaged() {
    val users = dbPopulatorReal.createUsersAndOrganizations()
    dbPopulatorReal.createBase("No org project", users[3].username)
    val result =
      projectRepository.findAllPermitted(
        users[3].id,
        PageRequest.of(0, 20, Sort.by(Sort.Order.asc("id"))),
        filters = ProjectFilters(),
      )
    assertThat(result).hasSize(10)
    assertThat(result.content[0].organizationOwner?.name).isNotNull
    assertThat(result.content[8].organizationOwner?.slug).isNotNull
  }
}
