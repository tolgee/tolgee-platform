package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.dataImport.Import
import io.tolgee.security.InternalController
import io.tolgee.service.ProjectService
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
  private val projectService: ProjectService,
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

  @GetMapping(value = ["/generate-with-long-text"])
  @Transactional
  fun generateWithLongText(): Import {
    val data = ImportTestData()
    data.importBuilder.data.importFiles[0].data.importTranslations[0].self {
      text = "Hello, I am translation, with pretty long long long long long long long long long " +
        "long long long long long long long long long long long long long long long long " +
        "long long long long long long long long long long text"
      conflict!!.text = "Hello, I am old translation, with pretty long long long long long long long long long " +
        "long long long long long long long long long long long long long long long long " +
        "long long long long long long long long long long text"
    }
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

  @GetMapping(value = ["/generate-many-languages"])
  @Transactional
  fun addManyLanguages(): Import {
    val data = ImportTestData()
    val file = data.importBuilder.data.importFiles[0]
    (0..90).forEach {
      file.addImportLanguage {

        name = "lng $it"
      }
    }
    testDataService.saveTestData(data.root)
    return data.importBuilder.self
  }

  @GetMapping(value = ["/generate-base"])
  @Transactional
  fun generateBaseData(): Project {
    val data = testDataService.saveTestData {
      addUserAccount {
        username = "franta"
        name = "Frantisek Dobrota"
      }.apply {
        addProject {
          userOwner = this@apply.self
          name = "Repo"
        }.build buildProject@{
          addPermission {
            type = Permission.ProjectPermissionType.MANAGE
            user = this@apply.self
            project = this@buildProject.self
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
    entityManager.createQuery("select i from Import i").resultList.forEach {
      importService.deleteImport(it as Import)
    }
    userAccountService.findOptional("franta").orElse(null)?.let {
      projectService.findAllPermitted(it).forEach { repo ->
        projectService.deleteProject(repo.id!!)
      }
      userAccountService.delete(it)
    }
  }
}
