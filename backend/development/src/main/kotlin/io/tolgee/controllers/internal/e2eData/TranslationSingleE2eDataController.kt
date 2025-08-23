package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.TranslationSingleTestData
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/translation-single"])
class TranslationSingleE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): Map<String, Any> {
    val data = TranslationSingleTestData()
    testDataService.saveTestData(data.root)
    return mapOf<String, Any>("id" to data.project.id)
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanup() {
    val data = TranslationSingleTestData()

    listOf(data.user.username, data.pepa.username, "jindra", "vojta").forEach { user ->
      userAccountService.findActive(user)?.let {
        projectService.findAllPermitted(it).forEach { repo ->
          projectService.deleteProject(repo.id!!)
        }
        userAccountService.delete(it)
      }
    }
  }
}
