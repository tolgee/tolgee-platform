package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/translations"])
@Transactional
class TranslationsE2eDataController(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val testDataService: TestDataService,
  private val userAccountService: UserAccountService,
) {
  @GetMapping(value = ["/generate/{projectId}/{number}"])
  @Transactional
  fun generateKeys(
    @PathVariable projectId: Long,
    @PathVariable number: Long,
  ) {
    val project = projectService.get(projectId)
    (0..(number - 1)).forEach { num ->
      val paddedNum = num.toString().padStart(2, '0')
      keyService.create(
        project,
        CreateKeyDto(
          "Cool key $paddedNum",
          null,
          mapOf(
            Pair("en", "Cool translated text $paddedNum"),
            Pair("cs", "Studený přeložený text $paddedNum"),
          ),
        ),
      )
    }
  }

  @GetMapping(value = ["/generate-for-filters"])
  @Transactional
  fun generateForFilters(): Map<String, Long> {
    val testData = TranslationsTestData()
    testData.addKeysWithScreenshots()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    return mapOf("id" to testData.project.id)
  }

  @GetMapping(value = ["/cleanup-for-filters"])
  @Transactional
  fun cleanupForFilters() {
    userAccountService.findActive("franta")?.let {
      projectService.findAllPermitted(it).forEach { repo ->
        projectService.deleteProject(repo.id!!)
      }
      userAccountService.delete(it)
    }
  }
}
