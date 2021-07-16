package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.request.SetTranslationsWithKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.InternalController
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/keys"])
@Transactional
@InternalController
class TranslationsE2eDataController(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
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
}
