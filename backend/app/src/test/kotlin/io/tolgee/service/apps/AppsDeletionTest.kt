package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

class AppsDeletionTest : AbstractSpringTest() {
  @Autowired
  private lateinit var projectHardDeletingService: ProjectHardDeletingService

  @Autowired
  private lateinit var appInstallService: AppInstallService

  @Autowired
  private lateinit var appEnablementService: AppEnablementService

  @Autowired
  private lateinit var appAvailabilityService: AppAvailabilityService

  @MockitoBean
  @Autowired
  private lateinit var appManifestHttpClient: AppManifestHttpClient

  private lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `hard-deletes a project that has an app enabled`() {
    enableAppForProject()

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(testData.projectBuilder.self)
    }

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `app tables cascade from their owning organization, project and install`() {
    deleteRuleOf("fk_app_install_organization").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_enabled_for_project_project").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_enabled_for_project_app_install").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_available_for_organization_app_install").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_available_for_organization_organization").assert.isEqualTo("CASCADE")
  }

  @Test
  fun `removing a native install clears its availability rows`() {
    val installId =
      executeInNewTransaction(platformTransactionManager) {
        appInstallService
          .selfRegister(
            organization = null,
            manifestUrl = AppsTestFixtures.MANIFEST_URL,
            author = testData.user,
          ).install.id
      }
    appAvailabilityService.grant(
      installId = installId,
      organizationId = testData.organization.id,
      author = testData.user,
    )

    appInstallService.remove(organizationId = null, installId = installId)

    appAvailabilityService.listOrganizations(installId).assert.isEmpty()
  }

  @Test
  fun `removing a native install clears its enablements across every organization`() {
    val installId = registerNativeInstall()
    appAvailabilityService.grantToAllOrganizations(installId)
    enableForProject(testData.projectBuilder.self.id, installId)
    enableForProject(testData.otherProject.id, installId)

    appInstallService.remove(organizationId = null, installId = installId)

    appEnablementService.isEnabledForProject(testData.projectBuilder.self.id, installId).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.otherProject.id, installId).assert.isFalse()
    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
  }

  @Test
  fun `revoking the blanket availability keeps enablements of explicitly granted organizations`() {
    val installId = registerNativeInstall()
    appAvailabilityService.grantToAllOrganizations(installId)
    appAvailabilityService.grant(
      installId = installId,
      organizationId = testData.otherOrganization.id,
      author = testData.user,
    )
    enableForProject(testData.projectBuilder.self.id, installId)
    enableForProject(testData.otherProject.id, installId)

    appAvailabilityService.revokeFromAllOrganizations(installId)

    appEnablementService.isEnabledForProject(testData.projectBuilder.self.id, installId).assert.isFalse()
    appEnablementService.isEnabledForProject(testData.otherProject.id, installId).assert.isTrue()
  }

  @Test
  fun `transferring a project to another organization clears its enablements`() {
    val installId = registerOrganizationInstall()
    enableForProject(testData.projectBuilder.self.id, installId)

    executeInNewTransaction(platformTransactionManager) {
      projectService.transferToOrganization(testData.projectBuilder.self.id, testData.otherOrganization.id)
    }

    appEnablementService.isEnabledForProject(testData.projectBuilder.self.id, installId).assert.isFalse()
  }

  private fun registerOrganizationInstall(): Long {
    return executeInNewTransaction(platformTransactionManager) {
      appInstallService
        .register(
          organization = testData.organization,
          manifestUrl = AppsTestFixtures.MANIFEST_URL,
          author = testData.user,
        ).install.id
    }
  }

  private fun registerNativeInstall(): Long {
    return executeInNewTransaction(platformTransactionManager) {
      appInstallService
        .selfRegister(
          organization = null,
          manifestUrl = AppsTestFixtures.MANIFEST_URL,
          author = testData.user,
        ).install.id
    }
  }

  private fun enableForProject(
    projectId: Long,
    installId: Long,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      appEnablementService.enable(
        project = entityManager.find(Project::class.java, projectId),
        installId = installId,
        author = testData.user,
      )
    }
  }

  private fun deleteRuleOf(constraintName: String): String {
    return executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createNativeQuery(
          "SELECT delete_rule FROM information_schema.referential_constraints " +
            "WHERE lower(constraint_name) = :name",
        ).setParameter("name", constraintName)
        .singleResult as String
    }
  }

  private fun enableAppForProject() {
    val installId =
      executeInNewTransaction(platformTransactionManager) {
        appInstallService
          .register(
            organization = testData.organization,
            manifestUrl = AppsTestFixtures.MANIFEST_URL,
            author = testData.user,
          ).install.id
      }

    executeInNewTransaction(platformTransactionManager) {
      appEnablementService.enable(
        project = testData.projectBuilder.self,
        installId = installId,
        author = testData.user,
      )
    }
  }
}
