package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.model.Permission
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
@RequestMapping(value = ["internal/e2e-data/languages"])
@Transactional
@InternalController
class LanguageE2eDataController(
        private val testDataService: TestDataService,
        private val projectService: ProjectService,
        private val userAccountService: UserAccountService
) {
    @GetMapping(value = ["/generate"])
    @Transactional
    fun generateBaseData(): Project {
        val data = testDataService.saveTestData {
            addUserAccount {
                self {
                    username = "franta"
                    name = "Frantisek Dobrota"
                }
                addProject {
                    self {
                        userOwner = this@addUserAccount.self
                        name = "Repo"
                    }
                    addPermission {
                        self {
                            type = Permission.ProjectPermissionType.MANAGE
                            user = this@addUserAccount.self
                            project = this@addProject.self
                        }
                    }
                    addLanguage {
                        self {
                            name = "English"
                            tag = "en"
                            flagEmoji = "\uD83C\uDDEC\uD83C\uDDE7"
                            originalName = "English"
                        }
                    }
                    addLanguage {
                        self {
                            name = "German"
                            tag = "de"
                            flagEmoji = "\uD83C\uDDE9\uD83C\uDDEA"
                            originalName = "Deutsch"
                        }
                    }
                }
            }
        }

        testDataService.saveTestData(data)
        return data.data.projects[0].self
    }


    @GetMapping(value = ["/clean"])
    @Transactional
    fun cleanup() {
        userAccountService.getByUserName("franta").orElse(null)?.let {
            projectService.findAllPermitted(it).forEach { repo ->
                projectService.deleteProject(repo.id!!)
            }
            userAccountService.delete(it)
        }
    }
}
