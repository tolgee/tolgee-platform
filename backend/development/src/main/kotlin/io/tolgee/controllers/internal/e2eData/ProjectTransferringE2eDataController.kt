package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.ProjectTransferringTestData
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/project-transferring"])
class ProjectTransferringE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val organizationService: OrganizationService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    val data = ProjectTransferringTestData()
    testDataService.saveTestData(data.root)
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanup() {
    val data = ProjectTransferringTestData()
    listOf(data.organization, data.notOwnedOrganization, data.anotherOrganization).forEach {
      organizationService.find(it.slug)?.let { found ->
        organizationService.delete(found)
      }
    }
    listOf(data.user, data.user2, data.user).forEach { user ->
      userAccountService.findActive(user.username)?.let {
        projectService.findAllPermitted(it).forEach { repo ->
          projectService.deleteProject(repo.id!!)
        }
        userAccountService.delete(it)
      }
    }
  }
}
