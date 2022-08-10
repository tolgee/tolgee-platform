package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.AdministrationTestData
import io.tolgee.security.InternalController
import io.tolgee.service.OrganizationService
import io.tolgee.service.UserAccountService
import io.tolgee.service.project.ProjectService
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/administration"])
@Transactional
@InternalController
class AdministrationE2eDataController(
  private val testDataService: TestDataService,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val organizationService: OrganizationService,
) {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    val data = AdministrationTestData()
    testDataService.saveTestData(data.root)
  }

  @GetMapping(value = ["/clean"])
  @Transactional
  fun cleanup(): Any? {
    try {
      val testData = AdministrationTestData()
      testData.root.data.userAccounts.forEach { user ->
        userAccountService.find(user.self.username)?.let { storedUser ->
          projectService.findAllPermitted(storedUser).forEach {
            it.id?.let { id -> projectService.deleteProject(id) }
          }
          testData.root.data.organizations.forEach { organizationBuilder ->
            organizationBuilder.self.name.let { name -> organizationService.deleteAllByName(name) }
          }
          userAccountService.delete(storedUser)
        }
      }
    } catch (e: FileNotFoundException) {
      return ResponseEntity.internalServerError().body(e.stackTraceToString())
    }
    return null
  }
}
