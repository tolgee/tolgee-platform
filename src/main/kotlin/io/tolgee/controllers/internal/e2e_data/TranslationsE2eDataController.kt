package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.SetTranslationsWithKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.security.InternalController
import io.tolgee.service.KeyService
import io.tolgee.service.ProjectService
import io.tolgee.service.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/translations"])
@Transactional
@InternalController
class TranslationsE2eDataController(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val testDataService: TestDataService,
  private val userAccountService: UserAccountService
) {
  @GetMapping(value = ["/generate/{projectId}/{number}"])
  @Transactional
  fun generateKeys(@PathVariable projectId: Long, @PathVariable number: Long) {
    val project = projectService.get(projectId).orElseThrow {
      NotFoundException()
    }
    (0..99).forEach { num ->
      val paddedNum = num.toString().padStart(2, '0')
      keyService.create(
        project,
        SetTranslationsWithKeyDto(
          "Cool key $paddedNum",
          mapOf(
            Pair("en", "Cool translated text $paddedNum"),
            Pair("cs", "Studený přeložený text $paddedNum")
          )
        )
      )
    }
  }

  @GetMapping(value = ["/generate-for-filters"])
  @Transactional
  fun generateForFilters(): Project {
    val testData = TranslationsTestData()
    testData.addKeysWithScreenshots()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    return testData.project
  }

  @GetMapping(value = ["/cleanup-for-filters"])
  @Transactional
  fun cleanupForFilters() {
    userAccountService.getByUserName("franta").orElse(null)?.let {
      projectService.findAllPermitted(it).forEach { repo ->
        projectService.deleteProject(repo.id!!)
      }
      userAccountService.delete(it)
    }
  }
}
