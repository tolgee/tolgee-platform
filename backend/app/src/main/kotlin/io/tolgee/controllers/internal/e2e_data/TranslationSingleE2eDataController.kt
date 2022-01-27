package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.TranslationSingleTestData
import io.tolgee.model.Project
import io.tolgee.security.InternalController
import io.tolgee.service.ProjectService
import io.tolgee.service.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/translation-single"])
@Transactional
@InternalController
class TranslationSingleE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): Project {
    val data = TranslationSingleTestData()
    testDataService.saveTestData(data.root)
    return data.project
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanup() {
    val data = TranslationSingleTestData()

    listOf(data.user.username, data.pepa.username, "jindra", "vojta").forEach { user ->
      userAccountService.findOptional(user).orElse(null)?.let {
        projectService.findAllPermitted(it).forEach { repo ->
          projectService.deleteProject(repo.id!!)
        }
        userAccountService.delete(it)
      }
    }
  }
}
