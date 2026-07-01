package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.PublicProjectsE2eData
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/public-projects"])
class PublicProjectsE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generate() {
    testDataService.saveTestData(PublicProjectsE2eData().root)
  }

  @GetMapping(value = ["/generate-few"])
  @Transactional
  fun generateFew() {
    testDataService.saveTestData(PublicProjectsE2eData(count = 5).root)
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun clean() {
    userAccountService.findActive("publicProjectsUser")?.let { user ->
      projectService.findAllPermitted(user).forEach { projectService.deleteProject(it.id!!) }
      userAccountService.delete(user)
    }
  }
}
