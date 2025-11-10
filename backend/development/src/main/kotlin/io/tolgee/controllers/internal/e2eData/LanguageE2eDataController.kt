package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/languages"])
class LanguageE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBaseData(): Map<String, Any> {
    val data =
      testDataService.saveTestData {
        val userAccountBuilder =
          addUserAccount {
            username = "franta"
            name = "Frantisek Dobrota"
          }
        val userAccount = userAccountBuilder.self
        userAccountBuilder.build {
          val projectBuilder =
            addProject {
              organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
              name = "Project"
            }

          projectBuilder.build {
            addPermission {
              type = ProjectPermissionType.MANAGE
              user = userAccount
              project = projectBuilder.self
            }
            addLanguage {
              name = "English"
              tag = "en"
              flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7"
              originalName = "English"
            }
            addLanguage {
              name = "German"
              tag = "de"
              flagEmoji = "\uD83C\uDDE9\uD83C\uDDEA"
              originalName = "Deutsch"
            }
          }
        }
      }
    return mapOf<String, Any>(
      "id" to
        data.data.projects[0]
          .self.id,
    )
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanup() {
    userAccountService.findActive("franta")?.let {
      projectService.findAllPermitted(it).forEach { repo ->
        projectService.deleteProject(repo.id!!)
      }
      userAccountService.delete(it)
    }
  }
}
