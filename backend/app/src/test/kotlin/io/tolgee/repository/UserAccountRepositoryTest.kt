package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.task.UserAccountFilters
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class UserAccountRepositoryTest : AbstractSpringTest() {
  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var dbPopulatorReal: DbPopulatorReal

  @Test
  fun getAllInOrganizationHasMemberRole() {
    val usersAndOrganizations = dbPopulatorReal.createUsersAndOrganizations()
    val org = usersAndOrganizations[1].organizationRoles[0].organization
    val returned = userAccountRepository.getAllInOrganization(org!!.id, PageRequest.of(0, 20), "")
    assertThat(returned.content).hasSize(2)
    assertThat(returned.content[0]).isInstanceOf(UserAccountWithOrganizationRoleView::class.java)
  }

  @Test
  fun getAllInOrganizationCorrectAccounts() {
    val usersAndOrganizations = dbPopulatorReal.createUsersAndOrganizations()
    val user = entityManager.merge(usersAndOrganizations[2])
    entityManager.refresh(user)

    val org = user.organizationRoles[0].organization
    val returned = userAccountRepository.getAllInOrganization(org!!.id, PageRequest.of(0, 20), "")
    assertThat(returned.content).hasSize(3)
  }

  @Test
  fun getAllInProjectSearch() {
    val franta = dbPopulatorReal.createUserIfNotExists("franta")
    val usersAndOrganizations = dbPopulatorReal.createUsersAndOrganizations()
    val repo = usersAndOrganizations[0].organizationRoles[0].organization!!.projects[0]

    permissionService.grantFullAccessToProject(franta, repo)

    val returned =
      userAccountRepository.getAllInProject(
        repo.id,
        PageRequest.of(0, 20),
        "franta",
        filters = UserAccountFilters(),
      )
    assertThat(returned.content).hasSize(1)
  }
}
