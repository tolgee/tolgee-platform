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
    deleteRuleOf("fk_app_organization").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_install_app").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_secret_app").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_availability_app").assert.isEqualTo("CASCADE")
    deleteRuleOf("fk_app_availability_organization").assert.isEqualTo("CASCADE")
  }

  @Test
  fun `withdrawing blanket availability disables the app in non-owner projects`() {
    val install = registerOrganizationInstall()
    appAvailabilityService.setAvailableToAll(install.appId)
    // The non-owner organization self-installs the now-available app, then enables its own install.
    val otherInstallId = installForOtherOrganization()
    enableForProject(testData.projectBuilder.self.id, install.installId)
    enableForProject(testData.otherProject.id, otherInstallId)

    appAvailabilityService.clearAvailableToAll(install.appId)

    appEnablementService.isEnabledForProject(testData.projectBuilder.self.id, install.installId).assert.isTrue()
    appEnablementService.isEnabledForProject(testData.otherProject.id, otherInstallId).assert.isFalse()
  }

  @Test
  fun `a specific-organization grant keeps the app enabled after the blanket offer is withdrawn`() {
    val install = registerOrganizationInstall()
    appAvailabilityService.setAvailableToAll(install.appId)
    val otherInstallId = installForOtherOrganization()
    enableForProject(testData.otherProject.id, otherInstallId)

    appAvailabilityService.addAvailableOrganization(install.appId, testData.otherOrganization.id)
    appAvailabilityService.clearAvailableToAll(install.appId)

    appEnablementService.isEnabledForProject(testData.otherProject.id, otherInstallId).assert.isTrue()
  }

  @Test
  fun `transferring a project to another organization clears its enablements`() {
    val install = registerOrganizationInstall()
    enableForProject(testData.projectBuilder.self.id, install.installId)

    executeInNewTransaction(platformTransactionManager) {
      projectService.transferToOrganization(testData.projectBuilder.self.id, testData.otherOrganization.id)
    }

    appEnablementService.isEnabledForProject(testData.projectBuilder.self.id, install.installId).assert.isFalse()
  }

  private data class RegisteredInstall(
    val installId: Long,
    val appId: Long,
  )

  private fun registerOrganizationInstall(): RegisteredInstall {
    return executeInNewTransaction(platformTransactionManager) {
      val install =
        appInstallService
          .register(
            organization = testData.organization,
            manifestUrl = AppsTestFixtures.MANIFEST_URL,
            manifestHash = null,
            install = true,
          ).install!!
      RegisteredInstall(install.id, install.app.id)
    }
  }

  private fun installForOtherOrganization(): Long {
    return executeInNewTransaction(platformTransactionManager) {
      appInstallService
        .install(
          organization = testData.otherOrganization,
          manifestUrl = AppsTestFixtures.MANIFEST_URL,
          manifestHash = null,
        ).id
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
            manifestHash = null,
            install = true,
          ).install!!
          .id
      }

    executeInNewTransaction(platformTransactionManager) {
      appEnablementService.enable(
        project = testData.projectBuilder.self,
        installId = installId,
      )
    }
  }
}
