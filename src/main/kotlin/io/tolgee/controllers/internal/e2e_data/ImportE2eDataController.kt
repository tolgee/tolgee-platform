package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.model.Permission
import io.tolgee.model.Repository
import io.tolgee.model.dataImport.Import
import io.tolgee.security.InternalController
import io.tolgee.service.RepositoryService
import io.tolgee.service.UserAccountService
import io.tolgee.service.dataImport.ImportService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.persistence.EntityManager

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/import"])
@Transactional
@InternalController
class ImportE2eDataController(
        private val importService: ImportService,
        private val entityManager: EntityManager,
        private val testDataService: TestDataService,
        private val repositoryService: RepositoryService,
        private val userAccountService: UserAccountService
) {
    @GetMapping(value = ["/generate"])
    @Transactional
    fun generateBasicTestData(): Import {
        val data = ImportTestData()
        data.addFileIssues()
        testDataService.saveTestData(data.root)
        return data.importBuilder.self
    }

    @GetMapping(value = ["/generate-applicable"])
    @Transactional
    fun generateApplicableTestData(): Import {
        val data = ImportTestData()
        data.setAllResolved()
        data.addFileIssues()
        data.importFrench.existingLanguage = data.french
        testDataService.saveTestData(data.root)
        return data.importBuilder.self
    }

    @GetMapping(value = ["/generate-all-selected"])
    @Transactional
    fun generateAllSelectedTestData(): Import {
        val data = ImportTestData()
        data.addFileIssues()
        data.importFrench.existingLanguage = data.french
        testDataService.saveTestData(data.root)
        return data.importBuilder.self
    }

    @GetMapping(value = ["/generate-lot-of-data"])
    @Transactional
    fun generateLotTestData(): Import {
        val data = ImportTestData()
        data.addFileIssues()
        data.addManyFileIssues()
        data.addManyTranslations()
        testDataService.saveTestData(data.root)
        return data.importBuilder.self
    }

    @GetMapping(value = ["/generate-base"])
    @Transactional
    fun generateBaseData(): Repository {
        val data = testDataService.saveTestData {
            addUserAccount {
                self {
                    username = "franta"
                    name = "Frantisek Dobrota"
                }
                addRepository {
                    self {
                        userOwner = this@addUserAccount.self
                        name = "Repo"
                    }
                    addPermission {
                        self {
                            type = Permission.RepositoryPermissionType.MANAGE
                            user = this@addUserAccount.self
                            repository = this@addRepository.self
                        }
                    }
                }
            }
        }

        testDataService.saveTestData(data)
        return data.data.repositories[0].self
    }

    @GetMapping(value = ["/clean"])
    @Transactional
    fun cleanup() {
        entityManager.createQuery("select i from Import i").resultList.forEach {
            importService.deleteImport(it as Import)
        }
        userAccountService.getByUserName("franta").orElse(null)?.let {
            repositoryService.findAllPermitted(it).forEach { repo ->
                repositoryService.deleteRepository(repo.id!!)
            }
            userAccountService.delete(it)
        }
    }
}
